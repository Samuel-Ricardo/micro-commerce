# Risk Matrix - Micro-Commerce
## Análise Estruturada de Riscos

**Data:** 2026-07-23  
**Versão:** 1.0  
**Responsável:** Maria Analyst (Risk Owner: Tech Lead)

---

## 📊 Matriz de Risco Geral

```
Impacto →
Probabilidade ↓    BAIXO          MÉDIO          ALTO           CRÍTICO
─────────────────────────────────────────────────────────────────────
ALTA       | (Verde)       | (Amarelo)     | (Laranja)     | (Vermelho)
           | Monitor      | Mitigar       | Planejar      | Mitigar
─────────────────────────────────────────────────────────────────────
MÉDIA      | (Verde)       | (Amarelo)     | (Laranja)     | (Laranja)
           | Monitor      | Mitigar       | Planejar      | Escalar
─────────────────────────────────────────────────────────────────────
BAIXA      | (Verde)       | (Verde)       | (Amarelo)     | (Laranja)
           | Aceitar      | Monitor       | Mitigar       | Planejar
─────────────────────────────────────────────────────────────────────
MUITO BAIXA| (Verde)       | (Verde)       | (Verde)       | (Amarelo)
           | Aceitar      | Aceitar       | Monitor       | Planejar
```

---

## 🎯 Matriz de Priorização

| ID | Risco | Categoria | Probabilidade | Impacto | Score | Status | Proprietário |
|----|-------|-----------|---------------|---------|-------|--------|--------------|
| **RT-001** | Microservices Complexity | Técnico | MÉDIA | ALTO | **6** | 🟠 Ativo | Tech Lead |
| **RT-002** | Keycloak SPOF | Técnico | BAIXA | CRÍTICO | **6** | 🟠 Ativo | Infra |
| **RT-003** | Cache Inconsistency | Técnico | MÉDIA | MÉDIO | **4** | 🟡 Monitor | Backend |
| **RT-004** | Cascade Failures | Técnico | MÉDIA | ALTO | **6** | 🟠 Ativo | DevOps |
| **RB-001** | Market Competition | Negócio | ALTA | ALTO | **9** | 🔴 Crítico | Product |
| **RB-002** | Customer Churn | Negócio | MÉDIA | ALTO | **6** | 🟠 Ativo | Product |
| **RB-003** | Fraud & Chargeback | Negócio | BAIXA | MÉDIO | **3** | 🟡 Monitor | Finance |
| **RO-001** | Bad Deployment | Operacional | BAIXA | CRÍTICO | **6** | 🟠 Ativo | DevOps |
| **RO-002** | Observability Gap | Operacional | MÉDIA | ALTO | **6** | 🟠 Ativo | DevOps |
| **RO-003** | Backup Failure | Operacional | BAIXA | CRÍTICO | **6** | 🟠 Ativo | DBA |

---

## 🔴 RISCOS CRÍTICOS (Score 9)

### RB-001: Competição no Mercado
**Categoria:** Negócio | **Probabilidade:** ALTA | **Impacto:** ALTO | **Score:** 9  
**Status:** 🔴 CRÍTICO - Requer plano de ação imediato

#### Descrição
Competidores mais estabelecidos (Amazon, Mercado Livre, Shopee) podem oferecer:
- Preços mais competitivos (volume de compra)
- Melhor UX/design
- Frete mais barato ou frete grátis
- Programa de fidelidade mais robusto
- Reputação maior (reviews, confiança)

#### Consequências
- Baixa conversion rate (<1%)
- Alto CAC (Customer Acquisition Cost)
- Churn rápido de novos usuários
- Dificuldade em atingir rentabilidade

#### Cenário Crítico
```
Lançamento dia 1 com:
- 100 usuários iniciais
- Conversion rate 1% (vs target 3.5%)
- AOV R$ 50 (vs target R$ 200)
- Burn rate: R$ 50k/mês
- Runway: 3-6 meses

Após 3 meses, produto descontinuado por falta de tração
```

#### Mitigação

**Curto Prazo (0-1 mês):**
- ✅ **Diferenciação clara:** Nichos (ex: eco-friendly products, local sellers)
- ✅ **Promoção agressiva:** Desconto 20% first purchase, free shipping >R$100
- ✅ **Programa Beta:** Convidar 100 early adopters, coletar feedback
- ✅ **Content Marketing:** Blog sobre marketplace tendências, SEO
- ✅ **Social Proof:** Instagram, TikTok com user-generated content

**Médio Prazo (1-3 meses):**
- 📊 **Analytics:** Monitorar CAC, LTV, cohort retention
- 🎁 **Loyalty Program:** Pontos, cashback, tier-based benefits
- 📱 **Mobile App:** Melhor UX, push notifications
- 🤖 **Recomendações:** ML para personalized browsing
- 🔄 **Seller Onboarding:** Crescimento de catálogo via terceiros

**Longo Prazo (3-12 meses):**
- 💰 **Dinamic Pricing:** Algoritmo de preço competitivo
- 🌍 **Expansão Regional:** Multi-city logistics
- 🏪 **Integração com lojas:** Plugin para sellers
- 🎯 **Marketplace B2B:** Wholesale para pequenas lojas

#### Métricas de Monitoramento
| Métrica | Baseline | Target | Frequency |
|---------|----------|--------|-----------|
| CAC (R$) | 150 | 80 | Semanal |
| LTV (R$) | 200 | 500 | Mensal |
| Conversion Rate | 1% | 3.5% | Diário |
| Repeat Rate | 10% | 40% | Semanal |
| NPS | 20 | 45 | Mensal |

#### Dono do Risco
- **Proprietário:** Head of Product
- **Revisor:** CEO
- **Escalação:** Board se NPS < 20 ou CAC > R$ 200

---

## 🟠 RISCOS ALTOS (Score 6)

### RT-001: Complexidade de Microserviços
**Categoria:** Técnico | **Probabilidade:** MÉDIA | **Impacto:** ALTO | **Score:** 6  
**Status:** 🟠 Ativo - Em mitigação

#### Descrição
Arquitetura de microserviços com múltiplos bancos de dados (PostgreSQL, MySQL, MongoDB) e serviços independentes dificulta:
- Garantir transações ACID distribuídas
- Manter consistência de dados
- Debugar problemas em produção (debugging distribuído)
- Escalar sem cascata de falhas

#### Cenário Crítico 1: Inconsistência de Stock
```
Timeline:
- 14:00:00 Customer A tenta comprar 50 units de SKU-123
- 14:00:01 Order Service chama Inventory Service: "reserve 50"
- 14:00:02 Inventory Service bloqueia linha (lock acquired)
- 14:00:03 Network timeout entre Order e Inventory
- 14:00:04 Order Service retorna erro ao cliente
- 14:00:05 Order Service NÃO faz rollback do DB
- 14:00:06 Inventory Service commit: reservado 50 units
- 14:00:07 Order nunca foi criado, mas stock foi reservado
- Result: "ghost reservation" de 50 units
```

#### Cenário Crítico 2: Cascata de Falhas
```
Timeline:
- Inventory Service fica lento (query lenta)
- Order Service timeout aguardando resposta (30s)
- API Gateway acumula requests (timeout 30s cada)
- Frontend timeout aguardando gateway (60s browser timeout)
- Customer experience: "página congelada", recarrega
- Recarregamento cria novo request (sem idempotency)
- Duplicação: 2 pedidos criados em vez de 1
- Inventory discrepância: 2x -50 units
```

#### Mitigação

**Implementadas (✅):**
- ✅ Cada serviço tem BD separado (não compartilha schema)
- ✅ Keycloak para identidade centralizada
- ✅ API Gateway como ponto de entrada único

**Planejadas (⏳):**
- 🔄 **Saga Pattern:** Implementar coreografia com Kafka
  - Order Service: cria pedido (PENDING)
  - Publica: OrderCreatedEvent
  - Inventory Service: consome, tenta reservar
  - Se falha: publica OrderFailedEvent
  - Order Service: reverte para CANCELLED

- 🔄 **Event Sourcing:** Todas as mudanças são eventos imutáveis
  - Benefit: auditoria completa, temporal queries
  - Store: event log em banco append-only

- 🔄 **Idempotency Keys:** Cliente envia idempotency-key UUID
  - Servidor armazena (request_id, response)
  - Requisição duplicada retorna mesma resposta (não executa 2x)

- 🔄 **Compensating Transactions:** Se passo N falha, desfazer passos 1..N-1
  - PlaceOrder falha em step 5? Chamar cancelOrder (reverse)

- 🧪 **Chaos Engineering:** Simular falhas de rede
  - Kill serviço aleatoriamente
  - Latência artificial (+ timeout)
  - Validar que sistema recupera

- 📊 **Distributed Tracing:** Tempo de debug reduzido
  - Jaeger com traceId único por request
  - Ver cascata de chamadas entre serviços

#### Métricas de Monitoramento
| Métrica | Target | Frequency |
|---------|--------|-----------|
| Stock Discrepância | < 1% | Diário |
| Saga Success Rate | > 99.5% | Real-time |
| E2E Transaction Time | < 2s | Real-time |
| Deadlock Incidents | 0 | Diário |

#### Dono do Risco
- **Proprietário:** Tech Lead / Arquiteto
- **Revisor:** CTO

---

### RT-002: Keycloak Single Point of Failure
**Categoria:** Técnico | **Probabilidade:** BAIXA | **Impacto:** CRÍTICO | **Score:** 6  
**Status:** 🟠 Ativo - Mitigação parcial

#### Descrição
Keycloak é única fonte de verdade para autenticação. Se ficar down:
- Ninguém consegue fazer login
- Sessões ativas viram inválidas
- API rejeita todos os requests (sem token válido)
- RTO: Quanto tempo leva pra derrubar e subir Keycloak? 5-10 min?

#### Impacto de Negócio
- **RPO:** 0 (perda zero de dados)
- **RTO:** 5-10 minutos (ambição: < 1 min)
- **Negócio:** 5-10 min sem vendas = R$ 10-50k loss (em picos)

#### Mitigação

**Curto Prazo (implementar em 2 sprints):**
- ✅ **Replicação:** Master-slave MySQL para Keycloak
- ✅ **JWT Local Validation:** 
  - Token contains: {sub, exp, iat, roles}
  - API valida signature localmente (sem chamar Keycloak)
  - Se Keycloak down, JWT de 15 min ainda válido
  - Benefit: API continua operacional
  - Risco: Role revoke leva até 15 min para propagar

- 🔄 **Token Caching:** Redis com TTL 5 min
  - Se Keycloak down, usar token em cache
  - Grace period: 5 min de "stale" authentication

**Médio Prazo (3-6 meses):**
- 🔄 **Keycloak Cluster:** 2+ instâncias em diferentes zonas
  - Load balancer na frente
  - Heartbeat entre nós
  - Failover automático

- 🔄 **Multi-Region:** Keycloak em 2+ regiões
  - Geo-distributed (ex: São Paulo + Rio)
  - Replicação cross-region
  - Ativo-ativo (não ativo-passivo)

#### Dono do Risco
- **Proprietário:** Infra Lead
- **Revisor:** CTO

---

### RT-004: Cascata de Falhas
**Categoria:** Técnico | **Probabilidade:** MÉDIA | **Impacto:** ALTO | **Score:** 6  
**Status:** 🟠 Ativo

#### Descrição
Falha em um serviço propaga para dependentes, causando outage em cascata:
- Inventory Service lento → Order Service aguarda → API Gateway congestiona
- Uma falha: todo o sistema fica indisponível

#### Cenário Crítico
```
13:00:00 Inventory Service: query lenta (1000s de SKUs)
13:00:05 Order Service timeout aguardando Inventory (30s timeout)
13:00:06 10 requisições de Order chegam (cada aguardando 30s)
13:00:10 API Gateway recebe 100 requisições (10 de Order congestionadas)
13:00:11 API Gateway aguardando Inventory respostas
13:00:12 Frontend timeout na chamada pra Gateway (60s)
13:00:13 Usuários veem "página congelada"
13:00:14 Usuários recarregam página (novo request)
13:00:15 Todos os pools de conexão esgotados
13:01:00 Inventory Service retorna (query finaliza)
13:01:01 Order Service retorna, mas API Gateway ainda congestionada
13:05:00 Gateway com 1000 de requisições acumuladas
13:10:00 Sistema volta ao normal

Total downtime: 10 minutos
Impacto: Zero vendas, clientes frustrados
```

#### Mitigação

**Implementadas (✅):**
- ✅ Timeout agressivo em chamadas HTTP (30s padrão)
- ✅ Retry com backoff (1s, 2s, 4s, 8s, 16s max)

**Planejadas (⏳):**
- 🔄 **Circuit Breaker:** Hystrix / Resilience4j
  - Order Service para Inventory: após 5 falhas consecutivas, retorna erro FAST
  - Não fica aguardando 30s se serviço está down
  - Auto-recovery: tenta chamar a cada 30s

- 🔄 **Bulkhead Pattern:** Connection pools separados
  - Pool A: chamadas críticas (5 connections)
  - Pool B: chamadas não-críticas (2 connections)
  - Se Pool B saturado, não afeta Pool A

- 🔄 **Rate Limiting:** Quota por client
  - Anônimo: 100 req/min
  - Logado: 1000 req/min
  - Evita um cliente ficar enviando 10k req/min

- 🔄 **Timeout Diversificado:**
  - Inventory: 10s (crítico para checkout)
  - Product: 30s (menos crítico)
  - Notification: 60s (async, não bloqueia)

- 🔄 **Graceful Degradation:**
  - Se Inventory down: retornar cache stale (últimos 30s)
  - Frontend: "Stock pode estar desatualizado"
  - Customer: pode continuar navegando (não pode comprar)

#### Dono do Risco
- **Proprietário:** DevOps Lead
- **Revisor:** Tech Lead

---

### RB-002: Customer Churn
**Categoria:** Negócio | **Probabilidade:** MÉDIA | **Impacto:** ALTO | **Score:** 6  
**Status:** 🟠 Ativo

#### Descrição
Clientes abandonam plataforma após primeira experiência ruim:
- Page load lento (> 5s)
- Erro de pagamento não explicado
- Suporte indisponível
- Estoque inconsistente (vê em estoque, mas não consegue comprar)

#### Baseline Esperado
- **Cohort Retention (dia 1):** 10% (vs 30% target)
- **Cohort Retention (dia 7):** 2% (vs 10% target)
- **Cohort Retention (dia 30):** 0.5% (vs 3% target)

#### Mitigação

**Onboarding (0-1 dia):**
- ✅ Welcome email com guia de como comprar
- ✅ First-purchase discount (10%) com cupom
- ✅ Tutorial interativo na home

**User Engagement (semana 1):**
- ✅ Email: "Confira produtos novos" (dia 3)
- ✅ SMS: "Apenas para você" discount (dia 5)
- ✅ Push: product recommendation (dia 7)

**Retention (mês 1+):**
- ✅ Loyalty program: acumular points a cada compra
- ✅ Birthday discount: -10% próximo ao aniversário
- ✅ Win-back campaign: desconto especial se 30+ dias sem compra

**Support:**
- ✅ Live chat durante business hours
- ✅ FAQ integrado
- ✅ SLA: responder em < 24h

#### Métricas
| Métrica | Target | Frequency |
|---------|--------|-----------|
| Day-1 Retention | 30% | Diário |
| Day-7 Retention | 10% | Semanal |
| Day-30 Retention | 3% | Semanal |
| Repeat Purchase Rate | 40% | Semanal |
| NPS Score | > 40 | Mensal |

---

### RO-001: Bad Deployment
**Categoria:** Operacional | **Probabilidade:** BAIXA | **Impacto:** CRÍTICO | **Score:** 6  
**Status:** 🟠 Ativo

#### Descrição
Deploy de código com bug causa downtime ou data loss:
- Deploy novo, aplicação não inicia (bootloader error)
- Migration de DB falha, dados corrompidos
- Feature flag ativada por engano, cascata de erros

#### Cenário Crítico
```
Sexta-feira 17h:
- Dev A faz deploy de "nova checkout feature"
- Test automation passou
- Código live
- 18h: Clientes reportam "Checkout não funciona"
- 18:05: On-call tenta fazer rollback
- 18:10: Rollback falha (database migration)
- 18:30: Restaurar de backup, data loss (últimas 30 min de pedidos)
- Total: 90 minutos de downtime
```

#### Mitigação

**CI/CD Pipeline:**
- ✅ Unit tests (80%+ coverage)
- ✅ Integration tests (API endpoints)
- ✅ Build + push image
- ✅ Deploy to staging (smoke tests)

**Deployment Strategy:**
- ✅ Blue-Green deploy (0-downtime)
  - Blue: versão atual (recebendo traffic)
  - Green: versão nova (testando)
  - Switch: instant se tudo OK
  - Rollback: instant se problemas

- ✅ Canary Release (5% → 25% → 100%)
  - 5% dos usuários: versão nova (monitora errors)
  - Se error rate < 0.1%: 25%
  - Se error rate < 0.1%: 100%
  - Se error rate > 1%: automatic rollback

- ✅ Feature Flags:
  - Nova feature: disabled por padrão
  - Habilita em staging → canary → 100%
  - Roll back: desabilitar (sem deploy)

**Database Migrations:**
- ✅ Backward-compatible migrations
  - Add column (nullable ou com default)
  - Remove column em segundo deploy
  - Never alter existing columns

- ✅ Zero-downtime migrations
  - Flyway versioning
  - Dry-run antes de apply
  - Fallback script testado

**Monitoring & Alerts:**
- ✅ Post-deploy checks:
  - Pod health check (liveness probe)
  - Dependency checks (DB, cache)
  - Sanity test (basic API call)

- ✅ Automated rollback:
  - Se error rate > 1% nos 5 min após deploy
  - Se latência p95 > 2x baseline
  - Se pod restart loop

#### Dono do Risco
- **Proprietário:** DevOps Lead
- **Revisor:** Tech Lead

---

### RO-002: Observability Gap
**Categoria:** Operacional | **Probabilidade:** MÉDIA | **Impacto:** ALTO | **Score:** 6  
**Status:** 🟠 Ativo

#### Descrição
Falta de observabilidade completa (metrics, logs, traces) dificulta:
- Debugar problemas em produção rapidamente
- Detectar anomalias de performance
- Root-cause analysis de incidentes

#### Cenário Crítico
```
Sábado 14h: Clientes reportam "Checkout lento"
Team não tem alertas (ninguém sabia)

Debugging:
- 14:05: Verificar metrics do Prometheus
  - CPU: normal
  - Memory: normal
  - Request rate: normal
  - Latência: 500ms (normal é 200ms)

- 14:10: Procurar em logs (qual serviço está lento?)
  - Logs em 3 lugares: Elasticsearch, Papertrail, Loki
  - Busca confusa, sem correlation

- 14:20: Verificar traces em Jaeger
  - Jaeger não está rodando (ninguém soube)
  - Feature nunca foi configurado

- 14:30: Database query log
  - Query SELECT * FROM ORDERS
  - Full table scan: 1M de linhas
  - Sem índice!

- 14:45: Aplicar índice, latência volta a 150ms
- Total: 45 minutos de degradação

Impacto: ~100 clientes frustrados, perda de R$ 5k em vendas
```

#### Mitigação

**Metrics (Prometheus):**
- ✅ Business metrics:
  - orders_total (counter)
  - order_value_sum (gauge)
  - checkout_time_histogram

- ✅ System metrics:
  - http_requests_duration_seconds
  - database_query_duration_seconds
  - cache_hit_ratio

- ✅ Alerts:
  - Error rate > 1% (5 min window)
  - Latência p95 > 1s (10 min window)
  - Pod restart loop

**Logs (ELK / Loki):**
- ✅ Structured logging (JSON):
  ```json
  {
    "timestamp": "2026-07-23T14:05:00Z",
    "level": "ERROR",
    "service": "order-service",
    "traceId": "abc123",
    "userId": "user-456",
    "message": "Failed to reserve stock",
    "skuCode": "SKU-789",
    "error": "timeout after 10s",
    "duration_ms": 10000
  }
  ```

- ✅ Log sampling:
  - 100% para errors
  - 10% para info
  - 1% para debug

- ✅ Retention: 7 dias hot, 90 dias cold (Glacier)

**Traces (Jaeger / Tempo):**
- ✅ Distributed tracing:
  - traceId: rastreado através de todos os serviços
  - spanId: cada chamada HTTP/DB é um span
  - Visualizar cascata de chamadas

- ✅ Sampling:
  - 100% para erro
  - 1% para sucesso (reduz overhead)

- ✅ Retention: 7 dias

**Dashboard (Grafana):**
- ✅ Real-time dashboard com:
  - Uptime (%)
  - Error rate (%)
  - Latência (p50, p95, p99)
  - Throughput (req/s)
  - Traffic distribution entre serviços

- ✅ Customizable per team:
  - Product team: conversion, AOV, retention
  - Backend team: API latency, database queries
  - DevOps: pod health, deployment status

#### Dono do Risco
- **Proprietário:** DevOps Lead
- **Revisor:** CTO

---

## 🟡 RISCOS MÉDIOS (Score 4)

### RT-003: Cache Inconsistency
**Categoria:** Técnico | **Probabilidade:** MÉDIA | **Impacto:** MÉDIO | **Score:** 4  
**Status:** 🟡 Monitor

#### Descrição
Cache desincronizado causa dados stale para customers:
- Vê "em estoque", mas ao comprar recebe "indisponível"
- Vê preço antigo
- Reviews não aparecem

#### Mitigação
- ✅ TTL curto (30s para inventory, 5 min para produtos)
- ✅ Pub/Sub real-time invalidation
- ⏳ Read-your-write consistency (sticky sessions)

#### Dono
- **Proprietário:** Backend Lead

---

## 📋 Plano de Ação Consolidado

### Fase 1: Imediato (0-2 semanas)
**Prioridade:** Riscos com Score 6+ (RT-001, RT-002, RT-004, RB-001, RB-002, RO-001, RO-002)

**Ações:**
1. ✅ Criar runbooks para each critical risk
2. ✅ Setup alerting para production (Prometheus + PagerDuty)
3. ✅ Teste de rollback (blue-green)
4. ✅ Teste de failover de banco de dados
5. ✅ Documentação de RTO/RPO
6. ✅ On-call schedule definido

**Responsável:** Tech Lead + DevOps  
**Deadline:** 2 semanas

---

### Fase 2: Curto Prazo (2-8 semanas)
**Prioridade:** Implementação de mitigações-chave

**Ações:**
1. Implementar Saga pattern com Kafka
2. Implementar circuit breaker (Resilience4j)
3. Setup Keycloak replicado
4. Implementar token caching em Redis
5. Setup distributed tracing (Jaeger)
6. Chaos engineering: primeiros testes

**Responsável:** Backend + DevOps  
**Deadline:** 8 semanas

---

### Fase 3: Médio Prazo (2-6 meses)
**Prioridade:** Resiliência e escalabilidade

**Ações:**
1. Keycloak cluster (multi-node)
2. Event sourcing para audit trail
3. Idempotency key pattern
4. Full chaos engineering suite
5. Multi-region setup (ativo-passivo)

**Responsável:** Tech Lead + Arquiteto  
**Deadline:** 6 meses

---

## 📊 Métricas de Risco

| Métrica | Baseline | Target | Frequency |
|---------|----------|--------|-----------|
| **Incident Count** | - | < 2/semana | Semanal |
| **MTTR (Mean Time to Resolve)** | - | < 30 min | Por incident |
| **MTTD (Mean Time to Detect)** | - | < 5 min | Por incident |
| **Error Rate** | - | < 0.1% | Real-time |
| **Stock Discrepância** | - | < 1% | Diário |
| **Saga Success Rate** | - | > 99.5% | Real-time |
| **Churn Rate** | - | < 2% | Mensal |

---

## 🎯 Governance

### Comitê de Risco
- **Frequência:** Semanal (terças-feiras, 15h)
- **Duração:** 30 minutos
- **Participantes:** Tech Lead, Product Lead, DevOps Lead, CFO (opcional)
- **Agenda:**
  1. Status de riscos ativos (2 min)
  2. Novos riscos (5 min)
  3. Métricas de health (3 min)
  4. Decisões (15 min)
  5. Next steps (5 min)

### Escalação
| Score | Ação | Comunicação |
|-------|------|-------------|
| 9-10 | Imediato | CEO, Board |
| 6-8 | 24h | Tech Lead, CTO |
| 4-5 | 1 semana | Product Lead |
| 1-3 | Planejar | Team lead |

---

## 📝 Documento de Referência

- **Criado:** 2026-07-23
- **Próxima revisão:** 2026-08-06
- **Versão:** 1.0
- **Dono:** Maria Analyst / Tech Lead

**Próximos Passos:**
1. ⏳ Revisão com stakeholders (semana 1)
2. ⏳ Priorização em sprint planning
3. ⏳ Criação de user stories para mitigação
4. ⏳ Estimativas de esforço
5. ⏳ Roadmap de implementação

