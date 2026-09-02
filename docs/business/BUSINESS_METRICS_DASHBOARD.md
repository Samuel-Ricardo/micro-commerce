# Business Metrics & Dashboard Specification
## Micro-Commerce Observability

**Data:** 2026-07-23  
**Versão:** 1.0  
**Owner:** Maria Analyst & Product Lead

---

## 📊 Visão Geral de Métricas

```
┌─────────────────────────────────────────────────────┐
│         BUSINESS METRICS ARCHITECTURE               │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Data Sources                  Aggregation          │
│  ──────────────────────────────────────────        │
│  • PostgreSQL (Orders)      ──→  Prometheus        │
│  • MySQL (Inventory)        ──→  (metrics server)  │
│  • MongoDB (Products)       ──→  ↓                 │
│  • Keycloak (Auth)          ──→  Grafana           │
│  • Application logs         ──→  (visualization)   │
│  • Event stream (Kafka)     ──→  ↓                 │
│                                  Alertas           │
│                                  (PagerDuty)       │
│                                                     │
│  Retention:                                         │
│  • Real-time: 7 dias em Prometheus                 │
│  • Historical: 12 meses em S3/Glacier             │
│  • Ad-hoc analytics: BigQuery                      │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 🎯 KPIs de Negócio (Top-Level)

### Tier 1: Revenue & Growth

| KPI | Descrição | Baseline | Target 6m | Target 12m | Fórmula | Freq |
|-----|-----------|----------|-----------|-----------|---------|------|
| **GMV** | Gross Merchandise Value | R$ 0 | R$ 100k | R$ 1M | SUM(order_value) | Diário |
| **Revenue** | Platform revenue (após comissão) | R$ 0 | R$ 30k | R$ 300k | SUM(order_value × commission%) | Diário |
| **AOV** | Average Order Value | R$ 0 | R$ 200 | R$ 250 | SUM(order_value) / order_count | Semanal |
| **Orders** | Total de pedidos | 0 | 500 | 4,000 | COUNT(orders) | Diário |
| **Active Users** | Usuários com 1+ compra | 0 | 100 | 1,000 | COUNT(DISTINCT user_id) | Semanal |
| **New Users** | Novos usuários registrados | 0 | 500 | 2,000 | COUNT(users where created_at in period) | Semanal |

---

### Tier 2: Conversion & Engagement

| KPI | Descrição | Baseline | Target 6m | Target 12m | Fórmula | Freq |
|-----|-----------|----------|-----------|-----------|---------|------|
| **Conversion Rate** | % visitantes → compradores | 2.5% | 3.5% | 4.5% | orders / sessions | Diário |
| **Cart Conversion** | % carrinho → compra | 60% | 70% | 80% | orders / carts_created | Diário |
| **Cart Abandonment** | % carrinho não comprado | 70% | 60% | 50% | (carts_created - orders) / carts_created | Diário |
| **Session Duration** | Tempo médio na plataforma | 5 min | 10 min | 15 min | AVG(session_length_minutes) | Semanal |
| **Pages per Session** | Páginas visitadas por sessão | 8 | 15 | 20 | AVG(page_count) | Semanal |
| **Search Success Rate** | % buscas com resultado | 75% | 90% | 95% | searches_with_results / total_searches | Semanal |

---

### Tier 3: Retention & Loyalty

| KPI | Descrição | Baseline | Target 6m | Target 12m | Fórmula | Freq |
|-----|-----------|----------|-----------|-----------|---------|------|
| **Day-1 Retention** | % ativos dia 1 após 1ª compra | 10% | 20% | 30% | COUNT(active_day1) / cohort_size | Semanal |
| **Day-7 Retention** | % ativos dia 7 após 1ª compra | 2% | 5% | 10% | COUNT(active_day7) / cohort_size | Semanal |
| **Day-30 Retention** | % ativos dia 30 | 0.5% | 2% | 3% | COUNT(active_day30) / cohort_size | Semanal |
| **Repeat Rate** | % clientes com 2+ compras | 5% | 20% | 40% | COUNT(users with 2+ orders) / total_users | Semanal |
| **Churn Rate** | % usuários inativos 30+ dias | 95% | 80% | 60% | COUNT(no_activity_30d) / total_users | Semanal |
| **Lifetime Value** | Valor total esperado por cliente | R$ 150 | R$ 350 | R$ 600 | AVG(total_spend_per_user) | Mensal |
| **NPS Score** | Net Promoter Score | 20 | 35 | 45 | (Promoters - Detractors) / respondents | Mensal |

---

### Tier 4: Customer Experience

| KPI | Descrição | Baseline | Target | SLO | Fórmula | Freq |
|-----|-----------|----------|--------|-----|---------|------|
| **Page Load Time** | Tempo carregamento (p95) | 4s | <3s | 99% | HISTOGRAM_QUANTILE(0.95, load_time) | Real-time |
| **Search Response Time** | Tempo resposta busca (p95) | 300ms | <200ms | 99% | HISTOGRAM_QUANTILE(0.95, search_time) | Real-time |
| **Checkout Time** | Tempo pra completar checkout | 3 min | <2 min | 99% | AVG(checkout_duration) | Semanal |
| **Shipping Accuracy** | % pedidos entregues no prazo | 85% | 95% | 99% | COUNT(delivered_ontime) / total_delivered | Semanal |
| **Support Resolution Time** | Tempo médio resolução ticket | 24h | <12h | 99% | AVG(ticket_resolution_time) | Semanal |
| **CSAT Score** | Customer Satisfaction | 75% | 85% | 90% | (satisfied / total) × 100 | Mensal |

---

## 🔧 Métricas Técnicas (Health)

### Performance

| Métrica | Target | SLO | Alert |
|---------|--------|-----|-------|
| **API Latência (p95)** | 200-500ms | 99% | > 1s (10 min) |
| **API Latência (p99)** | 500ms-1s | 99% | > 2s (5 min) |
| **Database Query Time (p95)** | < 100ms | 99.5% | > 500ms (5 min) |
| **Cache Hit Ratio** | 80%+ | - | < 60% (alert) |
| **Throughput (req/s)** | 5,000 RPS | 99% | > 6,000 RPS |

### Availability

| Métrica | Target | Window | Alert |
|---------|--------|--------|-------|
| **API Uptime** | 99.5% | 30 dias | < 99.5% (daily) |
| **Database Uptime** | 99.9% | 30 dias | < 99.9% (daily) |
| **Error Rate (5xx)** | < 0.1% | 5 min | > 1% (escalate) |
| **Error Rate (4xx)** | < 1% | 5 min | > 5% (monitor) |

### Infrastructure

| Métrica | Target | Alert |
|---------|--------|-------|
| **Pod Restarts** | 0/dia | > 2 restarts/10 min |
| **CPU Usage** | < 70% | > 80% sustained |
| **Memory Usage** | < 70% | > 80% sustained |
| **Disk Usage** | < 80% | > 90% sustained |
| **Network I/O** | < 60% bandwidth | > 80% sustained |

---

## 📊 Grupos de Métricas para Collection

### 1. Orders Metrics
```prometheus
# Counter: total pedidos
orders_total{status="completed",currency="BRL"} 1234

# Counter: valor total (revenue)
order_value_total{currency="BRL"} 245678.50

# Gauge: AOV
order_value_avg{currency="BRL"} 199.30

# Histogram: tempo para criar pedido
order_creation_duration_seconds
  bucket[+Inf] = 10000
  bucket[1.0] = 8500
  bucket[0.5] = 7200
  bucket[0.1] = 5000

# Histogram: status distribution
orders_by_status{status="pending"} 234
orders_by_status{status="shipped"} 1000
orders_by_status{status="delivered"} 9000
```

### 2. Cart Metrics
```prometheus
# Counter: carrinhos criados
carts_total 15000

# Counter: carrinhos abandonados
carts_abandoned_total 10500

# Gauge: carts ativos agora
carts_active_gauge 450

# Histogram: items por carrinho
cart_items_count
  avg = 3.2
  min = 1
  max = 50
  p95 = 8

# Gauge: cart abandonment rate
cart_abandonment_rate 0.70
```

### 3. Conversion Metrics
```prometheus
# Gauge: conversion rate (orders / sessions)
conversion_rate_gauge 0.035

# Gauge: cart conversion (orders / carts)
cart_conversion_rate_gauge 0.45

# Counter: conversions por origem
conversions_by_source{source="organic"} 50
conversions_by_source{source="paid_ad"} 150
conversions_by_source{source="email"} 30
conversions_by_source{source="direct"} 20
```

### 4. User Metrics
```prometheus
# Gauge: usuários ativos hoje
active_users_today_gauge 250

# Gauge: usuários novos hoje
new_users_today_gauge 50

# Counter: total registros
users_registered_total 2000

# Histogram: usuários por cohort
users_retention_day1{cohort="week_28"} 30
users_retention_day7{cohort="week_28"} 6
users_retention_day30{cohort="week_28"} 1

# Gauge: repeat purchase rate
repeat_purchase_rate_gauge 0.18
```

### 5. Inventory Metrics
```prometheus
# Gauge: stock por SKU
inventory_quantity_gauge{sku="SKU-001"} 500
inventory_quantity_gauge{sku="SKU-002"} 2

# Gauge: estoque baixo count
low_stock_count_gauge 15

# Counter: reservas criadas
reservations_created_total 1234

# Gauge: discrepâncias de estoque
inventory_discrepancy_gauge{sku="SKU-003"} 5
```

### 6. Search Metrics
```prometheus
# Counter: total de buscas
searches_total 10000

# Counter: buscas com resultado
searches_with_results_total 7500

# Gauge: search success rate
search_success_rate_gauge 0.75

# Histogram: search response time
search_duration_seconds
  p50 = 0.050
  p95 = 0.200
  p99 = 0.500
```

### 7. API Performance
```prometheus
# Histogram: request latency por endpoint
http_request_duration_seconds{method="GET",endpoint="/products"}
  p50 = 0.100
  p95 = 0.200
  p99 = 0.500

# Counter: requests por status
http_requests_total{status="200"} 95000
http_requests_total{status="404"} 500
http_requests_total{status="500"} 100

# Gauge: error rate
http_error_rate_gauge 0.0006
```

### 8. Database Metrics
```prometheus
# Histogram: query duration
db_query_duration_seconds{query="SELECT_orders"}
  p50 = 0.010
  p95 = 0.050
  p99 = 0.100

# Counter: queries por tipo
db_queries_total{type="SELECT"} 1000000
db_queries_total{type="INSERT"} 100000
db_queries_total{type="UPDATE"} 50000

# Gauge: connection pool usage
db_connection_pool_usage_gauge{pool="orders"} 0.75
```

### 9. Cache Metrics
```prometheus
# Counter: cache hits
cache_hits_total 500000

# Counter: cache misses
cache_misses_total 100000

# Gauge: hit ratio
cache_hit_ratio_gauge 0.833

# Gauge: cache size
cache_size_bytes{cache="products"} 52428800
cache_size_bytes{cache="sessions"} 1048576
```

### 10. Business Events
```prometheus
# Counter: eventos por tipo
events_total{type="order_placed"} 1234
events_total{type="payment_received"} 1200
events_total{type="shipment_created"} 1195
events_total{type="delivery_completed"} 1180
events_total{type="refund_processed"} 15

# Histogram: time between events
order_to_payment_duration_seconds
  p50 = 30
  p95 = 60
  p99 = 120
```

---

## 📈 Dashboard Specification

### Dashboard 1: Executive Overview
**Audience:** CEO, Board  
**Refresh:** 5 minutos  
**Size:** 1920x1080

```
┌──────────────────────────────────────────────┐
│  MICRO-COMMERCE EXECUTIVE DASHBOARD          │
├──────────────────────────────────────────────┤
│                                              │
│  [GMV: R$ 245.6k]  [Revenue: R$ 73.7k]     │
│  [Orders: 1,234]   [AOV: R$ 199.30]        │
│  [Users: 1,250]    [Conversion: 3.5%]      │
│                                              │
│  ┌─────────────────────┬─────────────────┐  │
│  │  Revenue by Day     │  Orders Trend   │  │
│  │  (line chart)       │  (bar chart)    │  │
│  │                     │                 │  │
│  │  R$ 50k ▅▆▇▅▆▅     │ 300 ▁▃▄▅▆▇▆▅   │  │
│  │         Jul 20-26   │     Jul 20-26   │  │
│  └─────────────────────┴─────────────────┘  │
│                                              │
│  ┌─────────────────────┬─────────────────┐  │
│  │ Conversion Rate     │ NPS Score       │  │
│  │ 3.5% (↑ 0.5 vs ago) │ 35 (Solid)      │  │
│  │ Target: 3.5%        │ Target: 45      │  │
│  └─────────────────────┴─────────────────┘  │
│                                              │
│  ┌─────────────────────┬─────────────────┐  │
│  │ System Health       │ KPI Status      │  │
│  │ Uptime: 99.8% ✅   │ 🟢 6/7 KPIs OK  │  │
│  │ Errors: 0.03% ✅   │ 🟡 1 at risk    │  │
│  └─────────────────────┴─────────────────┘  │
│                                              │
└──────────────────────────────────────────────┘
```

### Dashboard 2: Product Team
**Audience:** Product Manager, Growth  
**Refresh:** 15 minutos

```
┌─────────────────────────────────────────────┐
│ CONVERSION & ENGAGEMENT DASHBOARD           │
├─────────────────────────────────────────────┤
│                                             │
│  Conversion Funnel                          │
│  ─────────────────────────────────────      │
│  Sessions: 7,150                            │
│    ↓ 46% drop                               │
│  Visitors: 3,289                            │
│    ↓ 18% drop                               │
│  Product Views: 2,697                       │
│    ↓ 35% drop                               │
│  Add to Cart: 1,753                         │
│    ↓ 70% drop                               │
│  Orders: 525 (3.5% conversion)              │
│                                             │
│  Cart Abandonment: 70% (↓ 3% vs ago)        │
│  Target: 50%                                │
│                                             │
│  ┌──────────────────┬──────────────────┐   │
│  │ Cohort Retention │ Top Pages        │   │
│  │                  │                  │   │
│  │ Day-1: 20% ✅    │ 1. Home: 45%     │   │
│  │ Day-7: 5%  🟡    │ 2. Search: 35%   │   │
│  │ Day-30: 2% 🔴    │ 3. Product: 20%  │   │
│  │ Target: 3%, 10%, │                  │   │
│  │          5%      │ Mobile: 55% ✅   │   │
│  └──────────────────┴──────────────────┘   │
│                                             │
│  Search Success: 90% ✅ | AOV: R$ 199 ✅   │
│                                             │
└─────────────────────────────────────────────┘
```

### Dashboard 3: Operations/DevOps
**Audience:** DevOps, Tech Lead  
**Refresh:** Real-time (10s)

```
┌──────────────────────────────────────────────┐
│  SYSTEM HEALTH & PERFORMANCE DASHBOARD       │
├──────────────────────────────────────────────┤
│                                              │
│  [Uptime: 99.8%] [Errors: 0.03%] [OK: ✅]  │
│                                              │
│  ┌──────────────────┬──────────────────┐    │
│  │ API Latency      │ Throughput       │    │
│  │ p95: 215ms ✅    │ 4,200 req/s ✅   │    │
│  │ p99: 580ms ✅    │ Peak: 5,200      │    │
│  │ Target: <500ms   │ Capacity: 6,000  │    │
│  └──────────────────┴──────────────────┘    │
│                                              │
│  Pod Status: 24/24 running ✅                │
│  • order-service: 4 pods (4 ready)          │
│  • product-service: 4 pods (4 ready)        │
│  • inventory-service: 3 pods (3 ready)      │
│  • api-gateway: 3 pods (3 ready)            │
│  • notification-service: 2 pods (2 ready)   │
│                                              │
│  Resource Usage:                             │
│  • CPU: 54% (target: <70%)                  │
│  • Memory: 62% (target: <70%)               │
│  • Disk: 45% (target: <80%)                 │
│                                              │
│  Database:                                   │
│  • PostgreSQL: 99.99% uptime ✅             │
│  • MySQL: 99.99% uptime ✅                  │
│  • MongoDB: 99.99% uptime ✅                │
│                                              │
│  Network:                                    │
│  • Latency to DB: 2.3ms ✅                  │
│  • Latency to Cache: 0.8ms ✅               │
│  • Error rate: 0.001% ✅                    │
│                                              │
└──────────────────────────────────────────────┘
```

### Dashboard 4: Customer Support
**Audience:** Support Team, Customer Service  
**Refresh:** 5 minutos

```
┌──────────────────────────────────────────────┐
│  CUSTOMER SUPPORT DASHBOARD                  │
├──────────────────────────────────────────────┤
│                                              │
│  Open Tickets: 23 (↓ 5 vs yesterday)        │
│  • High Priority: 2 (SLA: 1h)               │
│  • Medium: 8 (SLA: 4h)                      │
│  • Low: 13 (SLA: 24h)                       │
│                                              │
│  Top Issues (Last 24h):                      │
│  1. Order Status Questions: 8               │
│  2. Payment Issues: 5                       │
│  3. Shipping Delays: 4                      │
│  4. Product Questions: 3                    │
│  5. Login Problems: 2                       │
│                                              │
│  ┌──────────────────┬──────────────────┐    │
│  │ Resolution Time  │ Customer Health  │    │
│  │ Avg: 8h 30m      │ CSAT: 82% ✅     │    │
│  │ Median: 4h       │ NPS: 35 (↑ 5)    │    │
│  │ 95th %ile: 18h   │ Churn: 15% 🟡    │    │
│  └──────────────────┴──────────────────┘    │
│                                              │
│  Recent Compliments:                         │
│  ✅ "Fast delivery and good quality!"       │
│  ✅ "Great customer service!"               │
│  ✅ "Easy to use platform"                  │
│                                              │
│  Recent Complaints:                          │
│  🔴 "Shipping took 7 days" (2x)             │
│  🔴 "Product different from photos" (1x)    │
│                                              │
└──────────────────────────────────────────────┘
```

---

## 🔔 Alert Configuration

### Severidade de Alertas

```
Level 1 - CRITICAL (Page on-call imediatamente)
  └─ Conditions:
     - API Uptime < 99%
     - Error Rate > 1%
     - Database down
     - Payment gateway down
     - Stock negative (inconsistency detected)

Level 2 - HIGH (Notificar team em 15 min)
  └─ Conditions:
     - API Latency p95 > 1s (sustained 10 min)
     - CPU > 85%
     - Memory > 85%
     - Cache hit ratio < 50%
     - Conversion rate drop > 30%

Level 3 - MEDIUM (Notificar lead em 1 hora)
  └─ Conditions:
     - Cart abandonment > 75%
     - Search success < 70%
     - AOV drop > 20%
     - Churn rate > 20%
     - NPS < 30

Level 4 - LOW (Slack notification)
  └─ Conditions:
     - Disk usage > 80%
     - Log volume spike
     - New issues in error tracking
     - Minor SLA breaches
```

### Exemplo: Alert Rule (Prometheus)
```yaml
groups:
  - name: micro-commerce-alerts
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: |
          (
            sum(rate(http_requests_total{status=~"5.."}[5m]))
            /
            sum(rate(http_requests_total[5m]))
          ) > 0.01
        for: 5m
        annotations:
          severity: critical
          summary: "High error rate detected: {{ $value | humanizePercentage }}"
          runbook: "docs/runbooks/high-error-rate.md"

      - alert: APILatencyHigh
        expr: |
          histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 10m
        annotations:
          severity: high
          summary: "API latency p95 is {{ $value | humanizeDuration }}"
          dashboard: "https://grafana.company.com/d/api-performance"

      - alert: CartAbandonmentHigh
        expr: |
          (
            sum(rate(carts_abandoned_total[24h]))
            /
            sum(rate(carts_total[24h]))
          ) > 0.75
        for: 1h
        annotations:
          severity: medium
          summary: "Cart abandonment rate is {{ $value | humanizePercentage }}"
          action: "Review checkout UX, run A/B test"

      - alert: StockInconsistency
        expr: |
          inventory_discrepancy_gauge > 0
        for: 5m
        annotations:
          severity: critical
          summary: "Inventory discrepancy detected: {{ $value }} items"
          action: "Manual reconciliation required"
```

---

## 📱 Integração com ferramentas

### Data Collection
- **Prometheus:** Scrape metrics a cada 30s
- **Grafana Loki:** Aggregate logs from all services
- **Jaeger:** Distributed tracing (OpenTelemetry)
- **Kafka:** Event streaming para business metrics

### Alerting
- **PagerDuty:** Page on-call para L1 incidents
- **Slack:** Notificações em canais dedicados
- **Email:** Resumos semanais para stakeholders
- **SMS:** Alertas críticos para on-call

### Dashboards
- **Grafana:** Real-time dashboards
- **Kibana:** Log analysis (ELK)
- **Metabase:** BI & business analytics
- **Looker:** Dashboards executivos

---

## 📋 Próximos Passos

1. ✅ Definir SLOs para cada serviço
2. ✅ Implementar coleta de métricas
3. ✅ Criar dashboards base
4. ✅ Configurar alertas críticos
5. ✅ Treinar time em observability
6. ⏳ Análise de custos de observabilidade
7. ⏳ Otimizar sampling de logs/traces

---

**Documento de Referência**  
Criado: 2026-07-23  
Próxima revisão: 2026-08-06  
Versão: 1.0  
Dono: Maria Analyst / Product Lead

