# ADR-007: Resilience Patterns (Circuit Breaker, Timeout, Retry)

**Status:** ✅ Implementado  
**Data:** 2026-07-23  
**Decisores:** Architecture Team  
**Tech Stack:** Resilience4j, Spring Cloud Circuit Breaker

---

## 📋 Contexto

Em sistemas distribuídos, falhas são inevitáveis:
- 🔴 Serviço A ↔ B latência sobe de 100ms → 30s
- 🔴 Serviço B crash, A continua chamando
- 🔴 Cascata de timeouts: A→B→C→D (todos ficam lentos)

## 🎯 Decisão

**Implementar Resilience4j com Circuit Breaker, Timeout e Retry:**

```
┌─────────────────────────────────────┐
│    Order Service                    │
│                                     │
│   GET /inventory/{productId}        │
│         │                           │
│         ▼                           │
│    ┌────────────────────────┐       │
│    │  Resilience4j          │       │
│    │ ┌──────────────────┐   │       │
│    │ │ Circuit Breaker  │◄──┼───┐   │
│    │ │ CLOSED           │   │   │   │
│    │ │ ├─ requests: OK  │   │   │   │
│    │ │ └─ fails: 5/10   │   │   │   │
│    │ └──────────────────┘   │   │   │
│    │                        │   │   │
│    │ ┌──────────────────┐   │   │   │
│    │ │ Timeout          │   │   │   │
│    │ │ max: 5 seconds   │   │   │   │
│    │ └──────────────────┘   │   │   │
│    │                        │   │   │
│    │ ┌──────────────────┐   │   │   │
│    │ │ Retry            │   │   │   │
│    │ │ max: 3 attempts  │   │   │   │
│    │ └──────────────────┘   │   │   │
│    └────────────────────────┘   │   │
│         │                       │   │
│         ├─ OPEN ─────────────────┘   │
│         │  (fast fail, cache)        │
│         │                            │
│         ├─ HALF_OPEN ─────────────┐  │
│         │  (test 1 request)       │  │
│         │                         │  │
│         └─ CLOSED ────────────────┘  │
│            (back to normal)          │
└─────────────────────────────────────┘
```

## 🔧 Implementação Técnica

### 1️⃣ **Circuit Breaker Configuration**

**Dependência:**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-timelimiter</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
</dependency>
```

**Configuração:**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        registerHealthIndicator: true
        slidingWindowSize: 10          # Track last 10 calls
        minimumNumberOfCalls: 5        # Need 5+ calls to decide
        failureRateThreshold: 50       # Open if 50%+ fail
        slowCallRateThreshold: 50      # Open if 50%+ slow
        slowCallDurationThreshold: 5000 # > 5s = slow
        waitDurationInOpenState: 30000  # Wait 30s before trying
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        
      product-service:
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        failureRateThreshold: 60
        waitDurationInOpenState: 10000
        
  retry:
    instances:
      inventory-service:
        maxAttempts: 3
        waitDuration: 1000              # 1 second between retries
        intervalFunction: exponentialBackoff  # exponential growth
        
      product-service:
        maxAttempts: 2
        waitDuration: 500
        
  timelimiter:
    instances:
      inventory-service:
        cancelRunningFuture: true
        timeoutDuration: 5s
        
      product-service:
        timeoutDuration: 3s
```

### 2️⃣ **OpenFeign com Resilience4j**

```java
// Client interface com CircuitBreaker
@FeignClient(
    name = "inventory-service",
    url = "http://inventory-service:8080",
    fallback = InventoryClientFallback.class
)
public interface InventoryClient {
    
    @CircuitBreaker(
        name = "inventory-service",
        fallbackMethod = "getInventoryFallback"
    )
    @Retry(name = "inventory-service")
    @TimeLimiter(name = "inventory-service")
    @GetMapping("/inventory/{productId}")
    CompletableFuture<InventoryResponse> getInventory(
        @PathVariable String productId);
    
    // Fallback method
    default CompletableFuture<InventoryResponse> getInventoryFallback(
            String productId, Exception ex) {
        
        log.warn("Inventory service unavailable for product: {}", 
            productId, ex);
        
        // Retornar valor cached ou default
        return CompletableFuture.completedFuture(
            InventoryResponse.builder()
                .productId(productId)
                .quantityAvailable(0)  // Assume out of stock
                .isCached(true)
                .message("Service unavailable, using cached data")
                .build()
        );
    }
}

@Component
public class InventoryClientFallback implements InventoryClient {
    
    @Autowired
    private CacheService cacheService;
    
    @Override
    public CompletableFuture<InventoryResponse> getInventory(String productId) {
        // Fallback para cache de última leitura bem-sucedida
        InventoryResponse cached = cacheService.get(
            "inventory:" + productId,
            InventoryResponse.class
        );
        
        if (cached != null) {
            cached.setIsCached(true);
            return CompletableFuture.completedFuture(cached);
        }
        
        // Se nem cache tem, retorna default
        return CompletableFuture.completedFuture(
            InventoryResponse.builder()
                .productId(productId)
                .quantityAvailable(0)
                .status("SERVICE_UNAVAILABLE")
                .build()
        );
    }
}
```

### 3️⃣ **Exemplos de Estados do Circuit Breaker**

```
┌────────────────────────────────────────────┐
│         CLOSED State (Normal)              │
├────────────────────────────────────────────┤
│ Request rate:    450 req/s                 │
│ Success rate:    98% (4 failures)          │
│ Latency (p95):   120ms                     │
│                                            │
│ ✓ All requests go through                 │
│ ✓ Some failures are tolerated              │
│                                            │
│ Transition to OPEN if:                     │
│ - Failure rate > 50% OR                    │
│ - Slow request rate > 50%                  │
└────────────────────────────────────────────┘
              ↓ (triggered)
┌────────────────────────────────────────────┐
│          OPEN State (Failing)              │
├────────────────────────────────────────────┤
│ Errors detected: 6 failures in last 10     │
│ Failure rate:    60%                       │
│                                            │
│ ✗ Requests immediately fail (fast fail)   │
│ ✗ No requests sent to failing service     │
│ ✓ Prevents cascading failures             │
│                                            │
│ Auto-transition to HALF_OPEN               │
│ after 30 seconds of inactivity             │
└────────────────────────────────────────────┘
              ↓ (after 30s)
┌────────────────────────────────────────────┐
│      HALF_OPEN State (Testing)             │
├────────────────────────────────────────────┤
│ Testing 1 request:  GET /inventory/prod1   │
│ Timeout: 5 seconds                         │
│                                            │
│ ✓ Single request allowed through           │
│ ✓ Determines if service recovered          │
│                                            │
│ Transition to:                             │
│ - CLOSED if success → normal operation     │
│ - OPEN if fail → back to fast-fail mode    │
└────────────────────────────────────────────┘
```

### 4️⃣ **Retry com Exponential Backoff**

```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private InventoryClient inventoryClient;
    
    @Autowired
    private ProductClient productClient;
    
    // Retry automático via anotação
    @Retry(name = "order-creation-retry")
    public Order createOrder(CreateOrderRequest request) {
        
        // Buscar produtos (com retry automático)
        List<Product> products = request.getItems().stream()
            .map(item -> productClient.getProduct(item.getProductId()))
            .toList();
        
        // Validar inventário (com retry automático)
        boolean canFulfill = request.getItems().stream()
            .allMatch(item -> {
                InventoryResponse inv = inventoryClient
                    .getInventory(item.getProductId())
                    .join();  // Wait for async response
                return inv.getQuantityAvailable() >= item.getQuantity();
            });
        
        if (!canFulfill) {
            throw new InsufficientInventoryException(
                "Cannot fulfill order: insufficient inventory");
        }
        
        Order order = new Order(request);
        return orderRepository.save(order);
    }
    
    // Retry customizado com lógica específica
    @CircuitBreaker(
        name = "payment-processor",
        fallbackMethod = "processPaymentFallback"
    )
    public PaymentResult processPayment(Order order) {
        
        // Retry logic inline
        int attempt = 0;
        long delayMs = 1000;
        
        while (attempt < 3) {
            try {
                return paymentService.process(order);
                
            } catch (TemporaryPaymentException e) {
                attempt++;
                
                if (attempt >= 3) {
                    throw new PaymentFailedException(
                        "Payment failed after 3 attempts", e);
                }
                
                // Exponential backoff: 1s, 2s, 4s
                try {
                    log.info("Payment retry {} after {}ms", 
                        attempt, delayMs);
                    Thread.sleep(delayMs);
                    delayMs *= 2;  // Double the delay
                    
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PaymentFailedException("Interrupted", ie);
                }
            }
        }
        
        throw new PaymentFailedException("Unexpected state");
    }
    
    // Fallback method
    public PaymentResult processPaymentFallback(Order order, Exception ex) {
        log.error("Payment service unavailable, using cached payment", ex);
        
        // Usar transação anterior se houver
        return PaymentResult.builder()
            .orderId(order.getId())
            .status("PENDING_MANUAL_REVIEW")
            .reason("Payment service unavailable")
            .isFallback(true)
            .build();
    }
}

// Configuração de retry
@Configuration
@EnableRetry
public class RetryConfiguration {
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000);  // 1 second
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}
```

### 5️⃣ **Bulkhead Pattern (Thread Pool Isolation)**

```java
// Prevenir que um serviço lento consuma todas as threads
@Configuration
public class BulkheadConfiguration {
    
    @Bean
    public Executor inventoryServiceExecutor() {
        return new ThreadPoolExecutor(
            10,      // corePoolSize
            20,      // maxPoolSize
            60,      // keepAliveTime
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),  // queue capacity
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger();
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("inventory-service-" + count.incrementAndGet());
                    return t;
                }
            }
        );
    }
    
    @Bean
    public Executor productServiceExecutor() {
        return new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50));
    }
}

@Service
public class OrderService {
    
    @Autowired
    @Qualifier("inventoryServiceExecutor")
    private Executor inventoryExecutor;
    
    @Autowired
    @Qualifier("productServiceExecutor")
    private Executor productExecutor;
    
    public Order createOrder(CreateOrderRequest request) {
        
        // Usar executor específico para isolamento
        CompletableFuture<List<Product>> products = 
            CompletableFuture.supplyAsync(
                () -> request.getItems().stream()
                    .map(item -> productClient.getProduct(item.getProductId()))
                    .toList(),
                productExecutor  // Isolated thread pool
            );
        
        CompletableFuture<Boolean> inventoryCheck = 
            CompletableFuture.supplyAsync(
                () -> canFulfill(request),
                inventoryExecutor  // Isolated thread pool
            );
        
        return CompletableFuture.allOf(products, inventoryCheck)
            .thenApply(v -> new Order(request))
            .join();
    }
}
```

## 📊 Health Check Integration

```java
@Configuration
public class HealthCheckConfiguration {
    
    @Bean
    public CircuitBreakerHealthIndicator circuitBreakerHealth(
            CircuitBreakerRegistry registry) {
        
        return new CircuitBreakerHealthIndicator(registry);
    }
    
    @Bean
    public EndpointObjectSupplier endpointObjectSupplier(
            CircuitBreakerRegistry registry) {
        
        return () -> registry.getAllCircuitBreakers();
    }
}

// GET http://localhost:8080/actuator/health
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "DOWN",
      "details": {
        "inventory-service": {
          "status": "DOWN",
          "details": {
            "state": "OPEN",
            "failureRate": 60.0,
            "slowCallRate": 30.0,
            "bufferedCalls": 10,
            "failedCalls": 6,
            "slowCalls": 3
          }
        },
        "product-service": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": 2.0,
            "slowCallRate": 5.0
          }
        }
      }
    }
  }
}
```

## 📊 Monitoring & Observability

```java
@Component
@Slf4j
public class ResilienceMetricsExporter {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private CircuitBreakerRegistry cbRegistry;
    
    @PostConstruct
    public void exportMetrics() {
        // Exportar métricas de circuit breaker
        cbRegistry.getAllCircuitBreakers().forEach(cb -> {
            
            // Circuit breaker state
            Gauge.builder("circuitbreaker.state",
                () -> cb.getState().getOrder())
                .tag("name", cb.getName())
                .register(meterRegistry);
            
            // Failure rate
            Gauge.builder("circuitbreaker.failure.rate",
                () -> cb.getMetrics().getFailureRate())
                .tag("name", cb.getName())
                .register(meterRegistry);
            
            // Slow call rate
            Gauge.builder("circuitbreaker.slow.calls.rate",
                () -> cb.getMetrics().getSlowCallRate())
                .tag("name", cb.getName())
                .register(meterRegistry);
        });
    }
}

// Prometheus metrics:
# HELP circuitbreaker_state Circuit breaker state (0=CLOSED, 1=HALF_OPEN, 2=OPEN)
circuitbreaker_state{name=\"inventory-service\"} 2

# HELP circuitbreaker_failure_rate Circuit breaker failure rate
circuitbreaker_failure_rate{name=\"inventory-service\"} 0.60

# HELP circuitbreaker_slow_calls_rate Circuit breaker slow call rate
circuitbreaker_slow_calls_rate{name=\"inventory-service\"} 0.30
```

## 🎯 Recomendações

1. **Monitorar SLAs:**
   - P99 Latency < 500ms (CLOSED state)
   - P99 Latency < 100ms (when circuit OPEN - fast fail)

2. **Alerts:**
   - Circuit breaker OPEN → Critical alert
   - Failure rate > 30% → Warning
   - Timeout rate > 10% → Info

3. **Testing:**
   - Simular serviço lento (5s latência)
   - Simular serviço com falhas (50% erro rate)
   - Validar comportamento do circuit breaker

---

**Relacionado:** ADR-001, ADR-004  
**Referências:**
- [Release It! - Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern - Chris Richardson](https://microservices.io/patterns/reliability/circuit-breaker.html)
