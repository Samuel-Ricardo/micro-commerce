# ADR-005: Event-Driven Architecture com Kafka & Saga Pattern

**Status:** ✅ Implementado (Parcial)  
**Data:** 2026-07-23  
**Decisores:** Architecture Team  
**Tech Stack:** Apache Kafka, Spring Kafka, AxonFramework

---

## 📋 Contexto

Sistema com transações distribuídas spanning múltiplos serviços:

```
Cenário: Um Cliente cria uma Order
├─ Validar Product existence (Product Service)
├─ Reservar Inventory (Inventory Service)
├─ Processar Payment (Payment Service - futuro)
├─ Criar Order (Order Service)
└─ Enviar Notificação (Notification Service)

❌ Problema: Uma falha no meio cancela tudo?
❌ Problema: Como garantir atomicidade distribuída?
❌ Problema: Como fazer rollback em serviços diferentes?
```

## 🎯 Decisão

**Implementar Event-Driven Architecture com Saga Pattern para transações distribuídas:**

```
┌─────────────────────────────────────────────┐
│      Order Creation Flow (Saga)              │
├─────────────────────────────────────────────┤
│                                              │
│  1. OrderCreatedEvent (Order Service)       │
│     │                                        │
│     └─→ [Kafka] order.created               │
│                                              │
│  2. InventoryService escuta & reserva       │
│     │                                        │
│     ├─ ✅ Sucesso → InventoryReservedEvent │
│     └─ ❌ Erro → InventoryReservationFailed │
│                                              │
│  3. OrderService processa reserva           │
│     │                                        │
│     ├─ ✅ Confirma Order                    │
│     └─ ❌ Cancela Order (Compensating)      │
│                                              │
│  4. NotificationService envia email         │
│                                              │
└─────────────────────────────────────────────┘
```

## 🔧 Implementação Técnica

### 1️⃣ **Event Definition**

```java
// Events do domínio
@Data
@AllArgsConstructor
public abstract class DomainEvent {
    private String eventId = UUID.randomUUID().toString();
    private String aggregateId;
    private LocalDateTime timestamp = LocalDateTime.now();
}

@Data
@AllArgsConstructor
public class OrderCreatedEvent extends DomainEvent {
    private String orderId;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    
    public OrderCreatedEvent(Order order) {
        super(UUID.randomUUID().toString(), order.getId());
        this.orderId = order.getId();
        this.customerId = order.getCustomerId();
        this.items = order.getItems();
        this.totalAmount = order.getTotalAmount();
    }
}

@Data
public class InventoryReservedEvent extends DomainEvent {
    private String orderId;
    private List<String> reservedProductIds;
    private LocalDateTime reservedAt;
}

@Data
public class InventoryReservationFailedEvent extends DomainEvent {
    private String orderId;
    private String reason;
    private List<String> unavailableProducts;
}

@Data
public class OrderConfirmedEvent extends DomainEvent {
    private String orderId;
    private String status = "CONFIRMED";
}

@Data
public class OrderCancelledEvent extends DomainEvent {
    private String orderId;
    private String reason;
}
```

### 2️⃣ **Saga Orchestrator**

```java
@Component
@Slf4j
public class OrderCreationSaga {
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderSagaRepository sagaRepository;
    
    // Step 1: Order Service publica evento
    public void startOrderCreation(Order order) {
        log.info("Starting Order Creation Saga for Order: {}", order.getId());
        
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        
        // Persistir estado da saga
        OrderSaga saga = OrderSaga.builder()
            .sagaId(UUID.randomUUID().toString())
            .orderId(order.getId())
            .status(SagaStatus.STARTED)
            .startedAt(LocalDateTime.now())
            .build();
        sagaRepository.save(saga);
        
        // Publicar evento
        kafkaTemplate.send("order.created", order.getId(), event)
            .thenRun(() -> log.info("Order creation event published: {}", order.getId()))
            .exceptionally(ex -> {
                log.error("Error publishing order creation event", ex);
                saga.setStatus(SagaStatus.FAILED);
                sagaRepository.save(saga);
                return null;
            });
    }
    
    // Step 2: Inventory Service escuta e tenta reservar
    @KafkaListener(topics = "order.created", groupId = "inventory-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Inventory Service: Processing order {}", event.getOrderId());
        
        try {
            // Verificar disponibilidade
            boolean canReserve = inventoryService.canReserve(
                event.getItems().stream()
                    .map(item -> new ReservationRequest(
                        item.getProductId(),
                        item.getQuantity()
                    ))
                    .toList()
            );
            
            if (canReserve) {
                // Reservar inventário
                List<String> reservedProducts = inventoryService
                    .reserveInventory(event.getOrderId(), event.getItems());
                
                // Publicar sucesso
                InventoryReservedEvent reservedEvent = 
                    new InventoryReservedEvent();
                reservedEvent.setOrderId(event.getOrderId());
                reservedEvent.setReservedProductIds(reservedProducts);
                
                kafkaTemplate.send("inventory.reserved", 
                    event.getOrderId(), reservedEvent);
                
            } else {
                // Publicar falha
                InventoryReservationFailedEvent failedEvent = 
                    new InventoryReservationFailedEvent();
                failedEvent.setOrderId(event.getOrderId());
                failedEvent.setReason("Insufficient inventory");
                
                kafkaTemplate.send("inventory.reservation.failed",
                    event.getOrderId(), failedEvent);
            }
            
        } catch (Exception e) {
            log.error("Error reserving inventory for order: {}", 
                event.getOrderId(), e);
            
            InventoryReservationFailedEvent failedEvent = 
                new InventoryReservationFailedEvent();
            failedEvent.setOrderId(event.getOrderId());
            failedEvent.setReason(e.getMessage());
            
            kafkaTemplate.send("inventory.reservation.failed",
                event.getOrderId(), failedEvent);
        }
    }
    
    // Step 3: Order Service escuta resultado da reserva
    @KafkaListener(topics = "inventory.reserved", groupId = "order-group")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Order Service: Inventory reserved for order {}",
            event.getOrderId());
        
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow();
        
        // Marcar como reserved
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        order.setReservedAt(event.getReservedAt());
        orderRepository.save(order);
        
        // Publicar confirmação
        OrderConfirmedEvent confirmedEvent = new OrderConfirmedEvent();
        confirmedEvent.setOrderId(event.getOrderId());
        
        kafkaTemplate.send("order.confirmed", 
            event.getOrderId(), confirmedEvent);
    }
    
    // Step 3b: Tratar falha de reserva (Compensating Transaction)
    @KafkaListener(topics = "inventory.reservation.failed", 
                   groupId = "order-group")
    public void handleInventoryReservationFailed(
            InventoryReservationFailedEvent event) {
        
        log.warn("Order Service: Inventory reservation failed for order {}",
            event.getOrderId());
        
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow();
        
        // Cancelar order (Compensating Transaction)
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(event.getReason());
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
        
        // Publicar evento de cancelamento
        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent();
        cancelledEvent.setOrderId(event.getOrderId());
        cancelledEvent.setReason(event.getReason());
        
        kafkaTemplate.send("order.cancelled", 
            event.getOrderId(), cancelledEvent);
    }
    
    // Step 4: Notification Service envia email
    @KafkaListener(topics = "order.confirmed", groupId = "notification-group")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Notification Service: Sending confirmation for order {}",
            event.getOrderId());
        
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow();
        
        // Enviar email
        emailService.sendOrderConfirmationEmail(
            order.getCustomer().getEmail(),
            order
        );
    }
    
    // Rastreamento da Saga
    @Transactional
    public void updateSagaStatus(String orderId, SagaStatus status) {
        OrderSaga saga = sagaRepository.findByOrderId(orderId)
            .orElseThrow();
        
        saga.setStatus(status);
        saga.setLastUpdatedAt(LocalDateTime.now());
        sagaRepository.save(saga);
    }
}
```

### 3️⃣ **Kafka Configuration**

**Configuração de Topics:**
```java
@Configuration
public class KafkaTopicConfig {
    
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
            .partitions(3)
            .replicas(2)
            .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 dias
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "snappy")
            .build();
    }
    
    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name("inventory.reserved")
            .partitions(3)
            .replicas(2)
            .build();
    }
    
    @Bean
    public NewTopic inventoryReservationFailedTopic() {
        return TopicBuilder.name("inventory.reservation.failed")
            .partitions(1)
            .replicas(2)
            .build();
    }
    
    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name("order.confirmed")
            .partitions(3)
            .replicas(2)
            .build();
    }
    
    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled")
            .partitions(1)
            .replicas(2)
            .build();
    }
}

// Serialization
@Configuration
public class KafkaSerializationConfig {
    
    @Bean
    public ProducerFactory<String, DomainEvent> 
            producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            "kafka:29092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        return new DefaultProducerFactory<>(configProps);
    }
    
    @Bean
    public ConsumerFactory<String, DomainEvent> 
            consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            "kafka:29092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG,
            "micro-commerce-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
            "com.js.microservices.events.DomainEvent");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest");
        
        return new DefaultConsumerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### 4️⃣ **Saga State Machine**

```java
@Data
@Entity
@Table(name = "order_sagas")
public class OrderSaga {
    
    @Id
    private String sagaId;
    
    private String orderId;
    
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    
    private LocalDateTime startedAt;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime completedAt;
    
    private String failureReason;
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "saga_id")
    private List<SagaStep> steps = new ArrayList<>();
}

@Data
@Entity
@Table(name = "saga_steps")
public class SagaStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sagaId;
    
    @Enumerated(EnumType.STRING)
    private StepType stepType; // RESERVE_INVENTORY, PROCESS_PAYMENT, etc
    
    @Enumerated(EnumType.STRING)
    private StepStatus status; // PENDING, SUCCESS, FAILED, COMPENSATED
    
    private LocalDateTime executedAt;
    private String failureReason;
}

enum SagaStatus {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    ORDER_CONFIRMED,
    COMPENSATING,
    FAILED,
    COMPLETED
}

enum StepType {
    RESERVE_INVENTORY,
    PROCESS_PAYMENT,
    CONFIRM_ORDER,
    SEND_NOTIFICATION
}

enum StepStatus {
    PENDING,
    SUCCESS,
    FAILED,
    COMPENSATED
}
```

### 5️⃣ **Dead Letter Queue (DLQ) para Falhas**

```java
@Configuration
public class KafkaDeadLetterQueueConfig {
    
    @Bean
    public NewTopic orderCreatedDlqTopic() {
        return TopicBuilder.name("order.created.dlq")
            .partitions(1)
            .replicas(2)
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 dias
            .build();
    }
    
    @Bean
    public ConsumerFactory<String, DomainEvent> 
            dlqConsumerFactory() {
        // Similar ao consumerFactory acima
        return new DefaultConsumerFactory<>();
    }
    
    // Listener para DLQ
    @KafkaListener(topics = "order.created.dlq", 
                   groupId = "dlq-handler")
    public void handleDeadLetter(
            ConsumerRecord<String, DomainEvent> record,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        
        log.error("Message received from DLQ - Key: {}, Exception: {}",
            record.key(), exceptionMessage);
        
        // Alert/notify ops team
        alertingService.notifyDeadLetterMessage(
            record.topic(),
            record.key(),
            exceptionMessage
        );
    }
}
```

## 📊 Matriz de Saga States

| Estado | Próximos Estados | Compensação |
|--------|-----------------|-------------|
| STARTED | INVENTORY_RESERVED, FAILED | - |
| INVENTORY_RESERVED | PAYMENT_PROCESSED, COMPENSATING | Release Inventory |
| PAYMENT_PROCESSED | ORDER_CONFIRMED, COMPENSATING | Refund Payment |
| ORDER_CONFIRMED | COMPLETED | Cancel Order |
| COMPENSATING | FAILED | N/A |
| FAILED | (terminal) | Already compensated |
| COMPLETED | (terminal) | N/A |

## ⚠️ Considerações Importantes

1. **Idempotência:** Cada serviço deve ser idempotente (mesma mensagem 2x = mesmo resultado)
2. **Timeouts:** Implementar timeouts para detectar serviços mortos
3. **Ordering:** Partição do Kafka garante ordering por orderId
4. **Monitoring:** Rastrear sagastatus em real-time

## 🚀 Próximas Melhorias

1. **Implementar Temporal (Workflow Orchestration)** para sagas mais complexas
2. **Implementar Outbox Pattern** para garantir exactly-once delivery
3. **Adicionar Retries automáticos com exponential backoff**

---

**Relacionado:** ADR-001, ADR-002, ADR-006  
**Referências:**
- [Saga Pattern - Chris Richardson](https://microservices.io/patterns/data/saga.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Guide](https://spring.io/guides/gs/messaging-kafka/)
