# ADR-006: Data Consistency Strategy (Eventual Consistency)

**Status:** ✅ Implementado  
**Data:** 2026-07-23  
**Decisores:** Architecture Team  
**Context:** Distributed Data Management

---

## 📋 Contexto

Na arquitetura monolítica, uma única transação ACID garante consistência:

```sql
BEGIN TRANSACTION
  1. INSERT INTO orders (id, customer_id, total) VALUES (123, 456, 99.99)
  2. INSERT INTO order_items (order_id, product_id, qty) VALUES (123, prod1, 2)
  3. UPDATE inventory SET quantity = quantity - 2 WHERE product_id = prod1
  4. UPDATE customers SET credit = credit - 99.99 WHERE id = 456
COMMIT -- ATOMIC: tudo ou nada
```

**Em Microserviços, é impossível:**
- Order Service não tem acesso direto a Inventory
- Transações ACID distribuídas são caras (2PC)
- Network partitions sempre ocorrem

## 🎯 Decisão

**Adoptar Eventual Consistency com compensating transactions:**

```
┌──────────────────────────────────────┐
│     Order Service (PostgreSQL)       │
│   Status: PENDING → RESERVED → OK    │
├──────────────────────────────────────┤
│ INSERT order {status: PENDING}       │
│ ✓ Transação local: COMMITTED        │
│   (mesmo que inventory falhe depois) │
│                                      │
│ KAFKA: order.created → published     │
│                                      │
│ ESPERA: inventory.reserved event     │
│ TIMEOUT: 30s (após timeout: CANCEL)  │
│                                      │
│ UPDATE order {status: RESERVED}      │
└──────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│   Inventory Service (MySQL)          │
│   Quantidade: 100 → 98 → 100         │
├──────────────────────────────────────┤
│ KAFKA listener: order.created        │
│                                      │
│ BEGIN TRANSACTION                    │
│   UPDATE inventory SET qty = qty - 2 │
│   WHERE product_id = prod1           │
│ COMMIT                               │
│                                      │
│ ✓ If SUCCESS:                        │
│   KAFKA: inventory.reserved          │
│                                      │
│ ✗ If FAILED (ex: qty < 2):           │
│   KAFKA: inventory.failed            │
│   Order Service REVERTE (status back)│
└──────────────────────────────────────┘
```

## 🔧 Implementação Técnica

### 1️⃣ **Eventual Consistency Model**

```java
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;  // PENDING, RESERVED, CONFIRMED, CANCELLED
    
    @Version  // Optimistic locking para evitar concurrent updates
    private Long version;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    
    // Para rastrear estado de consistência
    @Transient
    private ConsistencyLevel consistencyLevel;  // EVENTUAL, STRONG
    
    @Transient
    private LocalDateTime consistencyReachedAt;
}

enum OrderStatus {
    PENDING("Initial state, local commit done"),
    INVENTORY_RESERVED("Waiting for inventory confirmation"),
    CONFIRMED("End-to-end consistency achieved"),
    COMPENSATING("Rolling back due to failure"),
    CANCELLED("Cancelled permanently")
}

@Data
@AllArgsConstructor
enum ConsistencyLevel {
    EVENTUAL("Data may be temporarily inconsistent"),
    STRONG("Strong consistency reached across services"),
    UNKNOWN("State unknown, requires manual intervention")
}
```

### 2️⃣ **Timeout & Compensation**

```java
@Component
@Slf4j
public class SagaTimeoutHandler {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Scheduled(fixedDelay = 5000)  // Check every 5 seconds
    public void checkSagaTimeouts() {
        // Buscar orders PENDING por mais de 30s
        List<Order> timedOutOrders = orderRepository.findByStatusAndCreatedAtBefore(
            OrderStatus.PENDING,
            LocalDateTime.now().minusSeconds(30)
        );
        
        for (Order order : timedOutOrders) {
            log.warn("Order {} timed out waiting for inventory confirmation", 
                order.getId());
            
            // Compensating transaction: Cancelar order
            order.setStatus(OrderStatus.CANCELLED);
            order.setLastUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            
            // Publicar evento de cancelamento
            OrderCancelledEvent event = new OrderCancelledEvent();
            event.setOrderId(order.getId());
            event.setReason("Inventory reservation timeout");
            
            kafkaTemplate.send("order.cancelled", order.getId(), event);
        }
    }
}

// Retry mechanism com exponential backoff
@Component
public class EventRetryHandler {
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Async
    public void retryWithBackoff(String topic, String key, 
                                 DomainEvent event, int retryCount) {
        
        if (retryCount > 3) {
            log.error("Max retries exceeded for event: {}", event.getEventId());
            // Enviar para DLQ
            kafkaTemplate.send(topic + ".dlq", key, event);
            return;
        }
        
        // Exponential backoff: 1s, 2s, 4s, 8s
        long delayMs = (long) Math.pow(2, retryCount) * 1000;
        
        try {
            Thread.sleep(delayMs);
            kafkaTemplate.send(topic, key, event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 3️⃣ **Read Replicas para Queries Cross-Service**

```
PROBLEMA: Como fazer query "listOrdersWithDetails" 
que precisa de dados de Order + Product?

SOLUÇÃO: CQRS + Event Sourcing
  - Command side: Order Service escreve em PostgreSQL
  - Query side: Read model em MongoDB/Elasticsearch atualizado 
    por eventos Kafka
```

**Implementação:**

```java
// Command side: OrderService
@Service
public class OrderCommandService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Transactional
    public Order createOrder(CreateOrderCommand cmd) {
        Order order = new Order(cmd);
        orderRepository.save(order);  // Commit local
        
        // Publicar evento para outras services
        kafkaTemplate.send("order.created", 
            order.getId(), new OrderCreatedEvent(order));
        
        return order;
    }
}

// Query side: OrderQueryService (lê de MongoDB)
@Service
public class OrderQueryService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    public OrderDetailsDTO getOrderDetails(String orderId) {
        // Busca de uma read replica (MongoDB) atualizada por eventos
        return mongoTemplate.findById(orderId, 
            OrderDetailsDTO.class, "order_views");
    }
    
    public List<OrderDetailsDTO> findOrdersByCustomer(String customerId) {
        Query query = new Query(Criteria.where("customerId")
            .is(customerId));
        return mongoTemplate.find(query, OrderDetailsDTO.class, 
            "order_views");
    }
}

// Event listener que atualiza read model
@Component
public class OrderEventProjector {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @KafkaListener(topics = "order.created")
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderDetailsDTO view = OrderDetailsDTO.builder()
            .orderId(event.getOrderId())
            .customerId(event.getCustomerId())
            .totalAmount(event.getTotalAmount())
            .items(event.getItems())
            .createdAt(event.getTimestamp())
            .status("PENDING")
            .build();
        
        mongoTemplate.insert(view, "order_views");
    }
    
    @KafkaListener(topics = "order.confirmed")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        mongoTemplate.updateFirst(
            new Query(Criteria.where("orderId").is(event.getOrderId())),
            new Update().set("status", "CONFIRMED"),
            "order_views"
        );
    }
}
```

### 4️⃣ **Idempotency para Garantir Segurança**

```java
@Data
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {
    
    @Id
    private String idempotencyKey;  // UUID gerado por client
    
    private String aggregateId;     // Order ID
    
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;  // PROCESSING, SUCCESS, FAILED
    
    private LocalDateTime processedAt;
    
    private String resultData;  // Resultado anterior (para retornar)
}

enum ProcessingStatus {
    PROCESSING,
    SUCCESS,
    FAILED
}

// Controller
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @Autowired
    private OrderCommandService orderService;
    
    @Autowired
    private IdempotencyRepository idempotencyRepo;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        
        // Verificar se já foi processado
        Optional<IdempotencyRecord> existing = 
            idempotencyRepo.findById(idempotencyKey);
        
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            
            if (record.getStatus() == ProcessingStatus.SUCCESS) {
                // Retornar resultado anterior
                Order order = (Order) deserialize(record.getResultData());
                return ResponseEntity.ok(order);
            } else if (record.getStatus() == ProcessingStatus.PROCESSING) {
                // Still processing, return 202 Accepted
                return ResponseEntity.accepted().build();
            } else {
                // Failed before, return error
                return ResponseEntity.badRequest().build();
            }
        }
        
        // Registrar como PROCESSING
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setStatus(ProcessingStatus.PROCESSING);
        idempotencyRepo.save(record);
        
        try {
            // Processar
            Order order = orderService.createOrder(request);
            
            // Marcar como SUCCESS
            record.setStatus(ProcessingStatus.SUCCESS);
            record.setResultData(serialize(order));
            record.setProcessedAt(LocalDateTime.now());
            idempotencyRepo.save(record);
            
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            // Marcar como FAILED
            record.setStatus(ProcessingStatus.FAILED);
            idempotencyRepo.save(record);
            
            throw e;
        }
    }
}
```

### 5️⃣ **Consistency Monitoring**

```java
@Component
@Slf4j
public class ConsistencyMonitor {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryClient inventoryClient;
    
    @Scheduled(fixedDelay = 60000)  // Check every 1 minute
    public void checkConsistency() {
        List<Order> confirmedOrders = orderRepository.findByStatus(
            OrderStatus.CONFIRMED);
        
        for (Order order : confirmedOrders) {
            for (OrderItem item : order.getItems()) {
                try {
                    // Verificar se inventory foi atualizado
                    InventoryResponse inventory = 
                        inventoryClient.checkInventory(
                            item.getProductId());
                    
                    // Valida consistência (aplicação-específica)
                    if (!isConsistent(item, inventory)) {
                        log.warn("Consistency check failed for order {} item {}",
                            order.getId(), item.getProductId());
                        
                        // Alertar + tomar ação
                        alertService.notifyInconsistency(order, item);
                    }
                    
                } catch (FeignException e) {
                    log.error("Error checking inventory for order {}", 
                        order.getId(), e);
                }
            }
        }
    }
}
```

## 📊 Matriz de Tradeoffs

| Aspecto | Eventual Consistency | Strong Consistency |
|---------|---------------------|-------------------|
| **Implementação** | ✅ Simples (eventos) | ❌ Complexa (2PC) |
| **Performance** | ✅ Alta | ❌ Baixa |
| **Escalabilidade** | ✅ Horizontal | ❌ Limitada |
| **Latência** | ✅ Baixa | ❌ Alta |
| **Inconsistência Temporária** | ⚠️ Sim | ✅ Não |
| **Risco de Falha** | ⚠️ Requer compensação | ✅ Atômico |
| **Operação** | ✅ Simples | ❌ Complexa |

## 🎯 SLAs para Consistência

```
┌─────────────────────────────────────┐
│ Eventual Consistency SLAs           │
├─────────────────────────────────────┤
│ P99 Consistency Time: < 5 seconds   │
│ P95 Consistency Time: < 2 seconds   │
│ P50 Consistency Time: < 500ms       │
│                                     │
│ Failure Recovery Time: < 60 seconds │
│ Data Divergence Alert: > 30 seconds │
└─────────────────────────────────────┘
```

---

**Relacionado:** ADR-001, ADR-002, ADR-005  
**Referências:**
- [Designing Data-Intensive Applications - Martin Kleppmann](https://dataintensive.net/)
- [Consistency Models - AWS](https://aws.amazon.com/blogs/architecture/eventual-vs-strong-consistency-demystified/)
