# ADR-003: Stack de Observabilidade (Prometheus + Grafana + Zipkin)

**Status:** ✅ Parcialmente Implementado  
**Data:** 2026-07-23  
**Decisores:** Platform & DevOps Team  
**Stack:** Prometheus, Grafana, Zipkin, Micrometer, Loki

---

## 📋 Contexto

Sistema distribuído com 5+ microserviços requer visibilidade em:

- 📊 **Métricas** - CPU, memória, requisições, latência
- 📝 **Logs** - Rastreamento de erros, auditoria
- 🔗 **Traces** - End-to-end request flow through services

**Antes (Problema):**

```
Debug em produção é impossível:
- Qual serviço causou a latência?
- Onde exatamente falhou?
- Qual era o estado do sistema?
```

## 🎯 Decisão

**Implementar Three Pillars of Observability:**

```
┌────────────────────────────────────────────────┐
│          Application Monitoring Stack          │
├────────────────────────────────────────────────┤
│                                                │
│  ┌──────────────┐  ┌──────────┐  ┌───────┐   │
│  │ Prometheus   │  │ Grafana  │  │ Loki  │   │
│  │ (Metrics)    │  │ (Viz)    │  │ (Logs)│   │
│  └──────────────┘  └──────────┘  └───────┘   │
│         ↑                ↑            ↑        │
│         └────────────────┴────────────┘        │
│                    ↑                           │
│         ┌──────────────────────┐              │
│         │ Micrometer Bridge    │              │
│         └──────────────────────┘              │
│              ↑              ↑                  │
│     ┌────────┴──────┬───────┴─────────┐      │
│     │                                  │      │
│  Services        [Zipkin Trace]     Kafka    │
│     │                │                │      │
│     └────────────────┴────────────────┘      │
│                                                │
└────────────────────────────────────────────────┘
```

## 🔧 Implementação Técnica

### 1️⃣ **Prometheus + Micrometer** (Métricas)

**Dependência:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configuração:**

```yaml
# application.properties
management.endpoints.web.exposure.include=metrics,prometheus
management.metrics.enable.all=true
management.metrics.export.prometheus.enabled=true

# Métricas customizadas
management.metrics.tags.application=${spring.application.name}
management.metrics.tags.environment=${environment}
```

**Métricas Automáticas (Spring Boot Actuator):**

```
GET http://localhost:8080/actuator/prometheus

# HELP jvm_memory_used_bytes The amount of used memory
jvm_memory_used_bytes{area="heap",id="PS Scavenge"} 1.234e8

# HELP http_server_requests_seconds HTTP requests
http_server_requests_seconds_bucket{method="GET",status="200",le="+Inf"} 156

# HELP process_cpu_usage The "recent cpu usage" for the Java Virtual Machine process
process_cpu_usage 0.25
```

**Métricas Customizadas:**

```java
@Component
public class OrderMetrics {
    private final Counter ordersCreated;
    private final Timer orderProcessingTime;
    private final MeterRegistry meterRegistry;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.ordersCreated = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("service", "order-service")
            .register(meterRegistry);

        this.orderProcessingTime = Timer.builder("orders.processing.time")
            .description("Order processing duration")
            .register(meterRegistry);
    }

    public void recordOrderCreated() {
        ordersCreated.increment();
    }

    public void recordProcessingTime(long milliseconds) {
        orderProcessingTime.record(Duration.ofMillis(milliseconds));
    }
}

@Service
public class OrderService {
    @Autowired
    private OrderMetrics metrics;

    public Order createOrder(CreateOrderRequest request) {
        long startTime = System.currentTimeMillis();

        Order order = new Order(request);
        orderRepository.save(order);

        metrics.recordOrderCreated();
        metrics.recordProcessingTime(System.currentTimeMillis() - startTime);

        return order;
    }
}
```

**Prometheus Configuration (docker-compose):**

```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"
  volumes:
    - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
  command:
    - "--config.file=/etc/prometheus/prometheus.yml"

# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: "product-service"
    static_configs:
      - targets: ["localhost:8080"]
    metrics_path: "/actuator/prometheus"

  - job_name: "order-service"
    static_configs:
      - targets: ["localhost:8081"]
    metrics_path: "/actuator/prometheus"
```

### 2️⃣ **Grafana** (Visualização)

**Dashboard para Microserviços:**

```
┌─────────────────────────────────────────────┐
│         Micro-Commerce Health               │
├─────────────────────────────────────────────┤
│ ┌────────┐ ┌────────┐ ┌────────┐           │
│ │ Serviços│ │ Latency│ │ Errors │           │
│ │ Online: │ │ P95:   │ │ Rate:  │           │
│ │  5/5   │ │ 234ms  │ │ 0.2%   │           │
│ └────────┘ └────────┘ └────────┘           │
├─────────────────────────────────────────────┤
│ Request Rate by Service (per second)        │
│ ████████████████ product-service: 450 req/s│
│ ████████████ order-service: 280 req/s      │
│ ████████ inventory-service: 150 req/s      │
├─────────────────────────────────────────────┤
│ Response Time Distribution (p50/p95/p99)    │
│ product-service:   45ms / 120ms / 234ms    │
│ order-service:     120ms / 456ms / 890ms   │
│ inventory-service: 30ms / 80ms / 145ms     │
├─────────────────────────────────────────────┤
│ Memory Usage by Service                     │
│ ██████████ product-service: 512MB / 1GB    │
│ ██████████ order-service: 768MB / 2GB      │
│ █████ inventory-service: 256MB / 512MB     │
└─────────────────────────────────────────────┘
```

**Prometheus QL Queries:**

```promql
# Taxa de requisições por segundo
rate(http_server_requests_seconds_count[5m])

# Latência p95
histogram_quantile(0.95, http_server_requests_seconds_bucket)

# Taxa de erro
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Alertas
ALERT OrderServiceLatencyHigh
  IF histogram_quantile(0.95, http_server_requests_seconds_bucket{service="order-service"}) > 0.5
  FOR 5m
  ANNOTATIONS {summary: "Order service latency exceeds 500ms"}
```

### 3️⃣ **Zipkin + Brave** (Distributed Tracing)

**Dependência:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Configuração:**

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 0.1 # Sample 10% of requests
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**Exemplo de Trace Distribuído:**

```
Request: POST /orders (Client)
│
├─→ [API Gateway] 10ms
│   │
│   ├─→ [Product Service] GET /products → 45ms
│   │   │
│   │   └─→ [MongoDB] Query → 12ms
│   │
│   ├─→ [Inventory Service] POST /reserve → 67ms
│   │   │
│   │   └─→ [MySQL] UPDATE + Optimistic Lock → 32ms
│   │
│   ├─→ [Order Service] POST /create → 89ms
│   │   │
│   │   ├─→ [PostgreSQL] INSERT → 45ms
│   │   │
│   │   └─→ [Kafka] Publish OrderCreated → 12ms
│   │
│   └─→ [Notification Service] Listen & Send Email → 145ms
│
└─ Total: 567ms
```

**Instrumentação Manual:**

```java
@RestController
public class OrderController {
    @Autowired
    private Tracer tracer;

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest req) {
        // Criar span customizado
        Span span = tracer.nextSpan().name("order.creation");

        try (Tracer.SpanInScope scope = tracer.withSpan(span.start())) {
            span.tag("order.items.count", String.valueOf(req.getItems().size()));
            span.tag("customer.id", req.getCustomerId());

            Order order = orderService.create(req);

            span.tag("order.id", order.getId());
            span.tag("order.total", order.getTotal().toPlainString());

            return ResponseEntity.ok(order);
        } finally {
            span.finish();
        }
    }
}
```

**Zipkin UI (docker-compose):**

```yaml
zipkin:
  image: openzipkin/zipkin:latest
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=mem
    - MEM_MAX_SPANS=100000
```

### 4️⃣ **Loki + Promtail** (Logs Agregados)

**Stack para cenralizar logs:**

```yaml
# docker-compose.yml
promtail:
  image: grafana/promtail:latest
  volumes:
    - /var/log:/var/log
    - ./docker/loki/promtail-config.yml:/etc/promtail/config.yml
  command: -config.file=/etc/promtail/config.yml

loki:
  image: grafana/loki:latest
  ports:
    - "3100:3100"
  volumes:
    - ./docker/loki/loki-config.yml:/etc/loki/local-config.yml
```

**LogQL (Query Language):**

```
# Erros no order-service
{job="order-service"} |= "ERROR"

# Latência alta
{job="product-service"} | json | duration > 1000

# Traces associados
{service="order-service"} | json | trace_id="abc123"
```

## 📊 Matriz de Observabilidade

| Componente        | Cobertura             | Status                 | Prioridade |
| ----------------- | --------------------- | ---------------------- | ---------- |
| **Prometheus**    | ✅ Todas métricas     | Implementado           | ⭐⭐⭐⭐⭐ |
| **Grafana**       | ✅ Dashboards         | Implementado           | ⭐⭐⭐⭐⭐ |
| **Zipkin**        | ✅ Distributed traces | Implementado           | ⭐⭐⭐⭐   |
| **Loki**          | ⚠️ Logs (agregação)   | Parcial                | ⭐⭐⭐⭐   |
| **OpenTelemetry** | ❌ Standarização      | Não implementado       | ⭐⭐⭐⭐⭐ |
| **Alerting**      | ⚠️ Básico             | PrometheusAlertmanager | ⭐⭐⭐⭐   |

## 🚀 Recomendação: Migrar para OpenTelemetry

**Por quê?**

- ✅ Padrão aberto (OTEL) agnóstico de vendor
- ✅ Suporte nativo a traces, métricas e logs
- ✅ Melhor integração com Spring Boot 3.2+
- ✅ Future-proof (CloudEvents, W3C Trace Context)

**Implementação:**

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-instrumentation-spring-boot-starter</artifactId>
    <version>1.32.0</version>
</dependency>

<dependency>
    <groupId>io.opentelemetry.exporter</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
    <version>1.32.0</version>
</dependency>
```

---

**Relacionado:** ADR-001, ADR-004  
**Referências:**

- [Three Pillars of Observability](https://www.lightstep.com/blog/three-pillars-of-observability)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Micrometer Documentation](https://micrometer.io/)
