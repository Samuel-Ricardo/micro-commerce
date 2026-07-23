# ⚡ QUICKSTART: Micro-Commerce em 5 Minutos

> Guia rápido para entender o que é Micro-Commerce e começar a trabalhar nele

---

## 🎯 O que você precisa saber em 30 segundos

**Micro-Commerce** é um sistema de e-commerce moderno baseado em **7 microserviços** em Java Spring Boot, com observabilidade completa, segurança enterprise e pronto para Kubernetes.

```
┌─────────────────────────────────────────────────────────────┐
│                    MICRO-COMMERCE STACK                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Frontend (Angular)                                         │
│       ↓                                                      │
│  API Gateway (Spring Boot 3.4.1)                            │
│       ↓                                                      │
│  ┌─────────────┬──────────────┬──────────────────────┐     │
│  │             │              │                      │     │
│  ↓             ↓              ↓                      ↓     │
│ Product     Order        Inventory           Notification  │
│ Service    Service      Service             Service       │
│ (MongoDB)  (PostgreSQL) (PostgreSQL)       (Kafka)        │
│  │             │              │                      │     │
│  └──────────────┴──────────────┴──────────────────────┘     │
│                                                             │
│  Observability: Prometheus + Grafana + Loki + Tempo        │
│  Auth: Keycloak + OAuth2                                    │
│  Message: Apache Kafka                                      │
│  Orchestration: Docker + Kubernetes                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Como Começar (5 min)

### 1️⃣ Clone o Repositório
```bash
git clone https://github.com/Samuel-Ricardo/micro-commerce.git
cd micro-commerce
```

### 2️⃣ Suba a Infraestrutura
```bash
# Start all services (Docker Compose)
docker-compose up -d

# Aguarde ~30s para tudo inicializar
```

### 3️⃣ Acesse a Aplicação
```
Frontend:         http://localhost:4200
API Gateway:      http://localhost:9000
Product Service:  http://localhost:8080
Order Service:    http://localhost:8081
Keycloak:         http://localhost:8181
Grafana:          http://localhost:3000
Prometheus:       http://localhost:9090
```

### 4️⃣ Faça Seu Primeiro Teste
```bash
# Listar produtos
curl http://localhost:9000/api/products

# Resposta esperada:
# [{ "id": "1", "name": "Product 1", ... }]
```

### 5️⃣ Veja as Métricas
Abra [http://localhost:3000](http://localhost:3000) (Grafana)
- Username: `admin`
- Password: `admin`

---

## 📚 Leia Isso Depois (por ordem de prioridade)

1. **[architecture/OVERVIEW.md](./architecture/OVERVIEW.md)** (10 min)
   → Entender design do sistema

2. **[guides/GETTING-STARTED.md](./guides/GETTING-STARTED.md)** (15 min)
   → Setup de desenvolvimento local

3. **[business/OVERVIEW.md](./business/OVERVIEW.md)** (10 min)
   → Entender requisitos de negócio

4. **[technical/OVERVIEW.md](./technical/OVERVIEW.md)** (20 min)
   → Deep dive na implementação

5. **[quality/OVERVIEW.md](./quality/OVERVIEW.md)** (10 min)
   → Entender postura de testes (critica!)

---

## 🗂️ Estrutura do Projeto

```
micro-commerce/
├── docker/                    # Docker configs (Keycloak, Grafana, etc)
├── api-gateway/               # Gateway (Spring Cloud Gateway)
├── product-service/           # Catálogo de produtos (MongoDB)
├── order-service/             # Gerenciamento de pedidos (PostgreSQL)
├── inventory-service/         # Controle de estoque (PostgreSQL)
├── notification-service/      # Notificações (Kafka consumer)
├── frontend/                  # Angular app (Java Shop)
├── docker-compose.yaml        # Orquestração de tudo
└── docs/                      # Você está aqui! 📖
```

---

## 🔧 Comandos Essenciais

### Desenvolvimento
```bash
# Build um serviço específico
mvn clean package -f api-gateway/pom.xml

# Run um serviço localmente (sem Docker)
mvn spring-boot:run -f product-service/pom.xml

# Ver logs em tempo real
docker-compose logs -f product-service

# Parar tudo
docker-compose down
```

### Testes
```bash
# Rodar testes de um serviço
mvn test -f order-service/pom.xml

# Com cobertura
mvn test jacoco:report -f order-service/pom.xml

# Ver relatório
open order-service/target/site/jacoco/index.html
```

### Database
```bash
# Executar migrations (Flyway)
mvn flyway:migrate -f order-service/pom.xml

# Reset do banco (apenas dev!)
docker-compose exec order-db psql -U user -d micro_order -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

---

## 🎓 Conceitos-Chave (Resumido)

### Microserviços
- **Product Service**: Catálogo de produtos (busca, filtros, CRUD)
- **Order Service**: Processamento de pedidos (criação, acompanhamento, cancelamento)
- **Inventory Service**: Controle de estoque (reservas, sincronização)
- **Notification Service**: Notificações (email, SMS, push via Kafka)
- **API Gateway**: Entrada única (roteamento, segurança, rate limiting)

### Bancos de Dados
- **MongoDB**: Products (documentos flexíveis)
- **PostgreSQL**: Orders, Inventory (dados estruturados com transações)
- **MySQL**: Keycloak (autenticação)

### Comunicação
- **REST**: Síncrono (requests entre serviços)
- **Kafka**: Assíncrono (eventos de negócio)

### Segurança
- **OAuth2**: Autenticação distribuída (Keycloak)
- **JWT**: Tokens stateless para APIs
- **Spring Security**: Autorização (roles, permissions)

### Observabilidade
- **Prometheus**: Métricas (latência, RPS, erros)
- **Grafana**: Dashboards (visualização)
- **Loki**: Logs (busca e análise)
- **Tempo**: Distributed Tracing (rastreamento de requisições)

---

## ⚠️ Coisas Críticas a Saber

### 🔴 Cobertura de Testes: 5-10% (CRÍTICO!)
- **Status**: Inaceitável para produção
- **Target**: 80%+
- **Plano**: 100+ novos testes em 6-8 semanas
- **Ação**: Veja [quality/IMPLEMENTATION-PLAN.md](./quality/IMPLEMENTATION-PLAN.md)

### 🟡 Débito Técnico
1. Sem Global Exception Handler (erros inconsistentes)
2. Sem Rate Limiting (segurança)
3. Sem caching Redis (performance)
4. Sem OpenTelemetry unificado

Mais detalhes: [technical/TECH-DEBT.md](./technical/TECH-DEBT.md)

### 🟠 Roadmap P1 (Next 2-4 weeks)
- [ ] OpenTelemetry unificado
- [ ] Global Exception Handler
- [ ] Rate Limiting
- [ ] Test Coverage → 30%

Veja [NAVIGATION.md](./NAVIGATION.md) para roadmap completo.

---

## 🆘 Troubleshooting Rápido

**P: Docker não está rodando?**  
R: Certifique-se que Docker Desktop está ligado. No Windows, ative WSL 2.

**P: Porta 8080 já está em uso?**  
R: Ou outro serviço está rodando, ou tente: `docker-compose down -v`

**P: Keycloak não está respondendo?**  
R: Aguarde ~10s para inicializar. Check: `docker ps | grep keycloak`

**P: Erro de conexão ao banco?**  
R: Verifique `.env` files e `docker-compose.yaml` - credenciais devem bater.

**P: Como debugar um serviço?**  
R: Adicione `JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"` no docker-compose.yaml

Mais troubleshooting: [guides/TROUBLESHOOTING.md](./guides/TROUBLESHOOTING.md)

---

## 📊 Métricas Resumidas

| Aspecto | Score | Status |
|---------|-------|--------|
| Arquitetura | 7.8/10 | ✅ Muito Bom |
| Implementação | 6.2/10 | ⚠️ Precisa Melhorias |
| Testes | 2.0/10 | 🔴 CRÍTICO |
| Segurança | 8.0/10 | ✅ Bom |
| Observabilidade | 7.5/10 | ✅ Bom |

---

## 🎯 Próximos Passos

- [ ] Leia [architecture/OVERVIEW.md](./architecture/OVERVIEW.md)
- [ ] Setup local com [guides/GETTING-STARTED.md](./guides/GETTING-STARTED.md)
- [ ] Explore um serviço: `cd product-service && grep -r "RestController" src/`
- [ ] Faça um teste: `mvn test -f product-service/pom.xml`
- [ ] Veja o plano de testes: [quality/IMPLEMENTATION-PLAN.md](./quality/IMPLEMENTATION-PLAN.md)

---

## 📞 Precisa de Ajuda?

- 📖 Docs completos: [README.md](./README.md)
- 🗺️ Mapa de navegação: [NAVIGATION.md](./NAVIGATION.md)
- 🐛 Issues: [GitHub Issues](https://github.com/Samuel-Ricardo/micro-commerce/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/Samuel-Ricardo/micro-commerce/discussions)

---

**⏱️ Tempo gasto: 5 min ✅**  
**⏭️ Próximo: [guides/GETTING-STARTED.md](./guides/GETTING-STARTED.md) (15 min)**
