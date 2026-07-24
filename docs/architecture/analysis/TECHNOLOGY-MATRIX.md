# Matriz de Decisões Tecnológicas - Micro-Commerce

## 📊 Análise Comparativa das Tecnologias Adotadas

### 1️⃣ Framework Web

| Critério | Spring Boot 3.4.1 | Quarkus | Micronaut |
|----------|-------------------|---------|-----------|
| **Startup Time** | 2-3s | 0.1-0.2s ⭐⭐⭐ | 0.05s ⭐⭐⭐⭐⭐ |
| **Memory Footprint** | 150-200MB | 50-80MB ⭐⭐⭐ | 30-50MB ⭐⭐⭐⭐ |
| **Ecosystem** | ⭐⭐⭐⭐⭐ Maior | ⭐⭐⭐ | ⭐⭐⭐ |
| **Community** | ⭐⭐⭐⭐⭐ Imenso | ⭐⭐⭐ Crescendo | ⭐⭐⭐ Pequeno |
| **Production Usage** | ⭐⭐⭐⭐⭐ Ubíquo | ⭐⭐⭐ | ⭐⭐ |
| **Kubernetes Ready** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ Native | ⭐⭐⭐⭐⭐ |
| **Learning Curve** | ⭐⭐⭐ Moderado | ⭐⭐⭐⭐ Íngreme | ⭐⭐⭐⭐ Íngreme |
| **Team Expertise** | ✅ Existe | ❌ Rara | ❌ Muito rara |

**Decisão: Spring Boot 3.4.1** ✅

**Justificativa:**
- ✅ Maior comunidade = melhor suporte
- ✅ Team já tem expertise
- ✅ Ecossistema robusto (Security, Cloud, etc)
- ⚠️ Startup time não é crítico (sempre running)
- ⚠️ Memory footprint OK com limite de 512MB-2GB

**Trade-off:** Sacrificamos startup time por robustez e suporte

---

### 2️⃣ Bancos de Dados

| Aspecto | MongoDB | PostgreSQL | MySQL |
|---------|---------|-----------|-------|
| **Use Case** | Product Catalog | Orders | Inventory |
| **Consistência** | Eventual ⭐⭐⭐ | ACID ⭐⭐⭐⭐⭐ | ACID ⭐⭐⭐⭐ |
| **Escalabilidade Horiz.** | ⭐⭐⭐⭐⭐ Sharding | ⭐⭐⭐ Replicação | ⭐⭐⭐ Replicação |
| **Schema Flexibilidade** | ⭐⭐⭐⭐⭐ Schema-less | ⭐⭐ Rigid | ⭐⭐ Rigid |
| **Performance Read** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Operação Complexas** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Managed Services** | AWS Atlas | AWS RDS | AWS RDS |
| **Cost (1000 req/s)** | ~$200-500/mês | ~$150-300/mês | ~$150-300/mês |

**Decisões:**

| Serviço | BD | Justificativa |
|---------|----|----|
| **Product** | MongoDB | Read-heavy (80/20), schema evolui (atributos dinâmicos), alta escala de leitura |
| **Order** | PostgreSQL | Transações ACID críticas, relacionamentos complexos, eventual consistency OK |
| **Inventory** | MySQL | Real-time consistency crucial, operações simples, HA via replicação |

**Trade-offs:**
```
✅ Otimização por domínio
❌ Mais infraestrutura para gerenciar
❌ Inconsistência temporária (mitigada por Saga)
```

---

### 3️⃣ Message Broker

| Critério | Apache Kafka | RabbitMQ | AWS SQS/SNS |
|----------|--------------|----------|-------------|
| **Throughput** | ⭐⭐⭐⭐⭐ 1M+ msg/s | ⭐⭐⭐⭐ 100k msg/s | ⭐⭐⭐⭐ Ilimitado |
| **Latência** | ⭐⭐⭐⭐ 100-500ms | ⭐⭐⭐⭐⭐ <10ms | ⭐⭐⭐ 100ms+ |
| **Durabilidade** | ⭐⭐⭐⭐⭐ Replicação | ⭐⭐⭐⭐ Clustering | ⭐⭐⭐⭐⭐ Gerenciado |
| **Event Replay** | ⭐⭐⭐⭐⭐ Nativo | ❌ Não suporta | ⚠️ DLQ apenas |
| **Operação** | ⭐⭐ Complexa | ⭐⭐⭐ Moderada | ⭐⭐⭐⭐⭐ Serverless |
| **Cost (self-hosted)** | ⭐⭐⭐ ~1000/mês | ⭐⭐⭐ ~500/mês | ⭐⭐⭐ ~800/mês |

**Decisão: Apache Kafka** ✅

**Justificativa:**
- ✅ Event Sourcing: histórico de eventos crucial para auditoria
- ✅ Event Replay: reprocessar eventos após bug fix
- ✅ High Throughput: 1000+ orders/segundo
- ✅ Partitioning: garantir ordering por orderId
- ❌ Operação mais complexa

**Trade-off:** Complexidade operacional vs. robustez + features

---

### 4️⃣ Service Discovery

| Critério | Eureka | Consul | Kubernetes DNS |
|----------|--------|--------|-----------------|
| **Spring Integration** | ⭐⭐⭐⭐⭐ Nativo | ⭐⭐⭐ | ⭐⭐⭐ |
| **Health Checking** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Performance** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ Nativo |
| **Self-healing** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Multi-cloud** | ✅ Sim | ✅ Sim | ❌ K8s only |
| **Setup Time** | ⭐⭐⭐ 1h | ⭐⭐ 4h | ⭐⭐⭐⭐ 10min |

**Decisão: Eureka (Dev/Staging) + Kubernetes DNS (Prod)** ✅

**Implementação:**
```
Local/Docker Compose:
  ├─ Eureka Server (port 8761)
  ├─ Services register automaticamente
  └─ Feign client descobreserviços

Kubernetes Production:
  ├─ Desabilitar Eureka
  ├─ Usar service.namespace.svc.cluster.local
  └─ K8s DNS faz descoberta automática
```

**Trade-off:** Stack duplo (Eureka + K8s) para compatibilidade

---

### 5️⃣ Circuit Breaker & Resilience

| Critério | Resilience4j | Hystrix | Istio |
|----------|--------------|---------|-------|
| **Spring Integration** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ (deprecated) | ❌ Service Mesh |
| **Circuit Breaker** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Retry/Timeout** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Bulkhead** | ⭐⭐⭐⭐ Thread pools | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ Automático |
| **Learning Curve** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ Íngreme |
| **Observability** | ⭐⭐⭐⭐ Prometheus | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ Nativa |
| **Operational Load** | ⭐⭐⭐ Low | ⭐⭐ Legacy | ⭐ Alto |

**Decisão: Resilience4j (App-level)** ✅

**Justificativa:**
- ✅ Spring Boot nativo
- ✅ Sem overhead de service mesh
- ✅ Controle fino por aplicação
- ⚠️ Istio como next step (complex)

**Configurações:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        minimumNumberOfCalls: 10
```

---

### 6️⃣ Observabilidade

| Stack | Prometheus | ELK (Elasticsearch) | Datadog |
|-------|-----------|--------------------|---------| 
| **Metrics** | ⭐⭐⭐⭐⭐ Nativo | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Logs** | ❌ Não | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Traces** | ❌ Não | ⚠️ APM add-on | ⭐⭐⭐⭐⭐ |
| **Dashboards** | Grafana ⭐⭐⭐⭐ | Kibana ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Cost (3 services)** | ~$200/mês (self) | ~$3000/mês | ~$2000+/mês |
| **Setup Time** | ⭐⭐⭐ 4h | ⭐⭐ 8h | ⭐⭐⭐⭐ 1h |

**Decisão: Three Pillars (Prometheus + Loki + Zipkin)** ✅

| Pilar | Ferramenta | Razão |
|-------|-----------|-------|
| **Métricas** | Prometheus | Open source, nativo em K8s |
| **Logs** | Loki | Log aggregation escalável |
| **Traces** | Zipkin | Distributed tracing |

**Próximo passo:** OpenTelemetry (padrão)

---

### 7️⃣ API Gateway

| Critério | Spring Cloud Gateway | Kong | NGINX |
|----------|-------------------|------|-------|
| **Spring Integration** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| **Routing** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Security** | ⭐⭐⭐⭐⭐ OAuth2 nativo | ⭐⭐⭐⭐ | ⚠️ Modules |
| **Rate Limiting** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Observability** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Learning Curve** | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **Dev Team Match** | ✅ 100% Java | ⚠️ Lua | ⚠️ Sysadmin |

**Decisão: Spring Cloud Gateway MVC** ✅

**Justificativa:**
- ✅ Java team, Spring ecosystem
- ✅ OAuth2 integrado
- ✅ Circuit breaker native
- ✅ Sem overhead de proxy externo

---

### 8️⃣ Authentication & Authorization

| Aspectos | Keycloak | Auth0 | AWS Cognito |
|----------|----------|--------|-----------|
| **Self-hosted** | ✅ Sim | ❌ SaaS only | ⚠️ Ambos |
| **OAuth2/OIDC** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **User Management** | ⭐⭐⭐⭐⭐ Completo | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **RBAC/ABAC** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Setup Time** | ⭐⭐ 6h | ⭐⭐⭐⭐ 30min | ⭐⭐⭐ 2h |
| **Cost (10k users)** | ~$500/mês (infra) | ~$2000+/mês | ~$800/mês |
| **Data Residency** | ✅ Controlado | ❌ SaaS | ⚠️ Regional |
| **Compliance** | ✅ GDPR ready | ✅ | ✅ |

**Decisão: Keycloak** ✅

**Justificativa:**
- ✅ Self-hosted = controle + GDPR
- ✅ Open source
- ✅ Multi-tenant ready
- ❌ Setup mais complexo

**Configuração:**
```yaml
keycloak:
  image: keycloak:24.0.1
  database: MySQL (HA)
  realm: "master"
  clients:
    - api-gateway (OAuth2 client)
    - product-service (service account)
  roles: [USER, ADMIN, INVENTORY_MANAGER]
```

---

## 🎯 Matriz de Decisão Consolidada

```
┌─────────────────────────────────────────────────────┐
│ TECNOLOGIA             DECISION  CONFIDENCE  RISK   │
├─────────────────────────────────────────────────────┤
│ Framework              Spring    ⭐⭐⭐⭐⭐    🟢 LOW │
│ Java Version           21        ⭐⭐⭐⭐⭐    🟢 LOW │
│ Product Database       MongoDB   ⭐⭐⭐⭐⭐    🟢 LOW │
│ Order Database         PostgreSQL⭐⭐⭐⭐⭐    🟢 LOW │
│ Inventory Database     MySQL     ⭐⭐⭐⭐     🟡 MED │
│ Message Broker         Kafka     ⭐⭐⭐⭐     🟡 MED │
│ Service Discovery      Eureka+K8s⭐⭐⭐⭐⭐   🟢 LOW │
│ Circuit Breaker        Resilience⭐⭐⭐⭐     🟡 MED │
│ API Gateway            Spring GW ⭐⭐⭐⭐⭐    🟢 LOW │
│ Auth                   Keycloak  ⭐⭐⭐⭐     🟡 MED │
│ Observability          3-Pillars ⭐⭐⭐⭐     🟡 MED │
│ Container              Docker    ⭐⭐⭐⭐⭐    🟢 LOW │
│ Orchestration          Kubernetes⭐⭐⭐⭐⭐    🟢 LOW │
│ CI/CD                  GH Actions⭐⭐⭐⭐     🟡 MED │
└─────────────────────────────────────────────────────┘

🟢 LOW: Proven, widely used, low switching cost
🟡 MED: Good choice, some operational overhead
🔴 HIGH: Cutting-edge, few experts, high risk
```

---

## 📊 Comparação de Cenários

### Scenario 1: High Read Volume (100k reads/min)
```
✅ MongoDB sharding for products
✅ Read replicas for order history
✅ Redis caching layer
```

### Scenario 2: High Write Volume (10k orders/min)
```
✅ Kafka partitions by orderId (ordering)
✅ PostgreSQL connection pooling
✅ Async processing with Saga
```

### Scenario 3: Multi-region Deployment
```
⚠️  Keycloak: requires careful replication
⚠️  Kafka: cross-region synchronization
✅ Kubernetes: multi-cluster ready
```

---

**Próximas seções:**
- [Padrões Identificados](./PATTERNS-ANALYSIS.md)
- [Recomendações de Melhorias](./RECOMMENDATIONS.md)
