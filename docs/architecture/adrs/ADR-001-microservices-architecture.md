# ADR-001: Adopção de Arquitetura Microserviços

**Status:** ✅ Implementado  
**Data:** 2026-07-23  
**Decisores:** Architecture Team (Micro-Commerce)  
**Contexto Técnico:** Spring Boot 3.4.1, Java 21, Spring Cloud 2024.0.0

---

## 📋 Contexto

O projeto necessita escalar um e-commerce com múltiplos domínios independentes:

- **Product Catalog** - alta leitura, baixa escrita
- **Order Processing** - transações complexas, baixa latência
- **Inventory Management** - consistência crítica
- **Notifications** - processamento assíncrono
- **Authentication** - segurança e isolamento

## 🎯 Decisão

**Adoptar Arquitetura de Microserviços com isolamento de contextos de negócio (Bounded Contexts)**

Serviços independentes:

```
┌─────────────────────────────────────────────────────┐
│              API Gateway (Spring Gateway MVC)       │
├─────────────────────────────────────────────────────┤
│  Product Service  │ Order Service  │ Inventory      │
│   (MongoDB)       │ (PostgreSQL)    │   (MySQL)      │
│                   │                 │                │
│ OpenAPI 3.0       │ JPA + Flyway    │ Flyway        │
└─────────────────────────────────────────────────────┘
         │                  │                  │
    ┌────┴──────────────────┴──────────────────┘
    │
    └──→ [Kafka] Notification Service (Message-Driven)
         [Keycloak] Authorization Service
         [Eureka] Service Discovery
```

## ✅ Benefícios

| Aspecto                | Benefício                                                       |
| ---------------------- | --------------------------------------------------------------- |
| **Escalabilidade**     | Cada serviço escalável independentemente                        |
| **Autonomia de Dados** | Cada BD otimizado para seu domínio (MongoDB, PostgreSQL, MySQL) |
| **Resiliência**        | Falha isolada, não cascata (circuit breaker + Resilience4j)     |
| **Desenvolvimento**    | Times independentes, deploy independente                        |
| **Performance**        | Otimizações específicas por domínio                             |

## ⚠️ Tradeoffs & Desafios

| Desafio                      | Mitigação                                                  |
| ---------------------------- | ---------------------------------------------------------- |
| **Complexidade Operacional** | Docker Compose local, Kubernetes produção                  |
| **Transações Distribuídas**  | Saga Pattern + Kafka para eventual consistency             |
| **Network Latency**          | Service discovery (Eureka), Circuit breaker (Resilience4j) |
| **Monitoramento**            | Distributed tracing (Zipkin/Brave), Prometheus metrics     |
| **Versionamento de API**     | OpenAPI 3.0 + contract versioning strategy                 |

## 🔧 Implementação Técnica

### Service Discovery

```kotlin
// Eureka Client (cada serviço)
@EnableDiscoveryClient
@SpringBootApplication
class ProductServiceApplication
```

### Service-to-Service Communication

```java
// OpenFeign para chamadas síncronas
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    @GetMapping("/inventory/{productId}")
    InventoryResponse getStock(@PathVariable String productId);
}
```

### Event-Driven Communication

```java
// Kafka para operações assíncronas
@Component
public class OrderEventPublisher {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        kafkaTemplate.send("order.created", order.getId(),
            new OrderEvent(order.getId(), OrderStatus.CREATED));
    }
}
```

## 📊 Matriz de Qualidade

| Critério                  | Score      | Observações                                       |
| ------------------------- | ---------- | ------------------------------------------------- |
| Escalabilidade Horizontal | ⭐⭐⭐⭐⭐ | Excelente, stateless services                     |
| Isolamento de Falhas      | ⭐⭐⭐⭐⭐ | Circuit breaker + timeouts implementados          |
| Observabilidade           | ⭐⭐⭐⭐   | Prometheus + Grafana + Zipkin presente            |
| Consistência de Dados     | ⭐⭐⭐     | Eventual consistency requer cuidado (ver ADR-006) |
| Complexidade Operacional  | ⭐⭐⭐     | Múltiplos serviços para monitorar                 |

## 🔄 Padrões Aplicados

- ✅ **Domain-Driven Design (DDD)** - Bounded Contexts claros
- ✅ **API Gateway Pattern** - Entrypoint único
- ✅ **Service Discovery Pattern** - Eureka
- ✅ **Circuit Breaker Pattern** - Resilience4j
- ✅ **Event Sourcing** - Kafka para eventos de domínio
- ✅ **Saga Pattern** - Para transações distribuídas (Order Flow)

## 🚀 Próximos Passos

1. **Implementar Service Mesh** (Istio) para K8s produção
2. **Melhorar Observabilidade** com OpenTelemetry
3. **Documentar Contracts** com AsyncAPI para Kafka topics
4. **Implementar Canary Deployments** para rolling updates
5. **Estabelecer SLOs** por serviço

---

**Relacionado:** ADR-002, ADR-003, ADR-006  
**Referências:**

- [Microservices Patterns - Chris Richardson](https://microservices.io/patterns/index.html)
- [Building Microservices - Sam Newman](https://samnewman.io/books/building_microservices/)
