# C4 Model - Micro-Commerce Architecture

## 🎯 System Context Diagram

```mermaid
graph TB
    User["👤 End User<br/>(Web Browser)"]
    Admin["👨‍💼 Admin<br/>(Dashboard)"]
    Mobile["📱 Mobile App"]
    ExtAPI["🔌 External APIs<br/>(Payment, Email)"]
    
    System["🏗️ Micro-Commerce<br/>E-Commerce Platform"]
    
    User -->|Browse Products<br/>Place Orders| System
    Admin -->|Manage Inventory<br/>View Orders| System
    Mobile -->|Mobile Shopping| System
    System -->|Process Payments<br/>Send Emails| ExtAPI
    
    style System fill:#4A90E2,stroke:#2E5C8A,color:#fff
    style User fill:#E8F4F8,stroke:#2E5C8A
    style Admin fill:#E8F4F8,stroke:#2E5C8A
    style Mobile fill:#E8F4F8,stroke:#2E5C8A
    style ExtAPI fill:#FFF4E6,stroke:#8B6914
```

## 🏢 Container Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        WEB["🌐 Web Browser<br/>Angular"]
        MOBILE["📱 Mobile App"]
    end
    
    subgraph "API Layer"
        GW["🚪 API Gateway<br/>Spring Cloud Gateway<br/>Port: 9000<br/><br/>- Route HTTP requests<br/>- OAuth2 validation<br/>- Rate limiting<br/>- Request logging"]
    end
    
    subgraph "Service Layer"
        PROD["📦 Product Service<br/>Spring Boot 3.4.1<br/>Port: 8080<br/><br/>- Product catalog<br/>- Category management<br/>- Search & filters"]
        
        ORDER["📋 Order Service<br/>Spring Boot 3.4.1<br/>Port: 8081<br/><br/>- Order creation<br/>- Order tracking<br/>- Order history"]
        
        INV["📊 Inventory Service<br/>Spring Boot 3.4.1<br/>Port: 8082<br/><br/>- Stock management<br/>- Reservation logic<br/>- Real-time updates"]
        
        NOTIF["📧 Notification Service<br/>Spring Boot 3.4.1<br/>Port: 8083<br/><br/>- Email sending<br/>- Event listening<br/>- Templates"]
    end
    
    subgraph "Data Layer"
        MONGO["🍃 MongoDB<br/>Port: 27017<br/><br/>Products collection<br/>(schema-less)"]
        
        PG["🐘 PostgreSQL<br/>Port: 5432<br/><br/>Orders & Order Items<br/>(ACID transactions)"]
        
        MYSQL["🐬 MySQL<br/>Port: 3306<br/><br/>Inventory & Stock<br/>(high availability)"]
    end
    
    subgraph "Infrastructure"
        KAFKA["📨 Apache Kafka<br/>Port: 9092<br/><br/>- order.created<br/>- inventory.reserved<br/>- order.confirmed<br/>- notifications"]
        
        EUREKA["🔍 Eureka Discovery<br/>Port: 8761<br/><br/>Service registration<br/>& discovery"]
        
        KEYCLOAK["🔐 Keycloak<br/>Port: 8181<br/><br/>OAuth2 provider<br/>Identity management"]
    end
    
    subgraph "Observability"
        PROM["📊 Prometheus<br/>Port: 9090<br/><br/>Metrics collection"]
        
        GRAF["📈 Grafana<br/>Port: 3000<br/><br/>Dashboards & Alerts"]
        
        ZIPKIN["🔗 Zipkin<br/>Port: 9411<br/><br/>Distributed tracing"]
    end
    
    WEB -->|HTTP/REST| GW
    MOBILE -->|HTTP/REST| GW
    
    GW -->|Routes to| PROD
    GW -->|Routes to| ORDER
    GW -->|Routes to| INV
    
    PROD -->|Reads/Writes| MONGO
    ORDER -->|Reads/Writes| PG
    INV -->|Reads/Writes| MYSQL
    
    PROD -->|Register| EUREKA
    ORDER -->|Register| EUREKA
    INV -->|Register| EUREKA
    
    ORDER -->|Lookup services| EUREKA
    INV -->|Lookup services| EUREKA
    
    PROD -->|Publish events| KAFKA
    ORDER -->|Publish/Listen events| KAFKA
    INV -->|Listen events| KAFKA
    NOTIF -->|Listen events| KAFKA
    
    GW -->|OAuth2 validation| KEYCLOAK
    ORDER -->|Feign call| INV
    
    PROD -->|Expose metrics| PROM
    ORDER -->|Expose metrics| PROM
    INV -->|Expose metrics| PROM
    
    PROM -->|Scrape metrics| GRAF
    
    PROD -->|Send traces| ZIPKIN
    ORDER -->|Send traces| ZIPKIN
    INV -->|Send traces| ZIPKIN
    
    style GW fill:#FF6B6B,stroke:#A44,color:#fff
    style PROD fill:#4ECDC4,stroke:#1A9A8D,color:#fff
    style ORDER fill:#4ECDC4,stroke:#1A9A8D,color:#fff
    style INV fill:#4ECDC4,stroke:#1A9A8D,color:#fff
    style NOTIF fill:#4ECDC4,stroke:#1A9A8D,color:#fff
    style MONGO fill:#13A538,stroke:#0A6B2B,color:#fff
    style PG fill:#336791,stroke:#1A3F5A,color:#fff
    style MYSQL fill:#00758F,stroke:#004D5C,color:#fff
    style KAFKA fill:#231F20,stroke:#0F0D0E,color:#fff
    style EUREKA fill:#FFB81C,stroke:#B8860D,color:#000
    style KEYCLOAK fill:#1496FF,stroke:#0C5BA8,color:#fff
    style PROM fill:#E6521E,stroke:#8B3210,color:#fff
    style GRAF fill:#F99F00,stroke:#C67C00,color:#000
    style ZIPKIN fill:#0B0E3B,stroke:#050612,color:#fff
```

## 🔧 Component Diagram (Product Service - Example)

```mermaid
graph TB
    subgraph "Product Service"
        direction TB
        
        CTRL["🌐 REST Controller<br/>@RestController<br/><br/>- GET /products<br/>- POST /products<br/>- GET /products/{id}"]
        
        SERVICE["⚙️ Product Service<br/>@Service<br/><br/>- Business logic<br/>- Validation<br/>- Transformation"]
        
        REPO["💾 Product Repository<br/>MongoRepository<br/><br/>- CRUD operations<br/>- Custom queries<br/>- Pagination"]
        
        MODEL["📦 Product Model<br/>@Document<br/><br/>- id: String<br/>- name: String<br/>- price: BigDecimal<br/>- categories: List<br/>- attributes: Map"]
        
        CONFIG["⚙️ Configuration<br/>- MongoDB config<br/>- OpenAPI/Swagger<br/>- Error handling"]
        
        ASPECT["🔍 AOP Aspects<br/>- Logging<br/>- Performance metrics<br/>- Caching"]
    end
    
    subgraph "External"
        MONGO_EXT["🍃 MongoDB"]
        KAFKA_EXT["📨 Kafka"]
    end
    
    CTRL -->|calls| SERVICE
    SERVICE -->|uses| REPO
    REPO -->|maps to| MODEL
    SERVICE -->|reads| CONFIG
    SERVICE -->|interception| ASPECT
    
    REPO -->|query/save| MONGO_EXT
    ASPECT -->|publish events| KAFKA_EXT
    
    style CTRL fill:#7FB3D5,stroke:#4A5D7F,color:#fff
    style SERVICE fill:#85C1E2,stroke:#4A5D7F,color:#fff
    style REPO fill:#A8D8F0,stroke:#4A5D7F,color:#fff
    style MODEL fill:#B8E0F0,stroke:#4A5D7F,color:#000
    style CONFIG fill:#CCCCCC,stroke:#666,color:#000
    style ASPECT fill:#FFE4B5,stroke:#8B6914,color:#000
    style MONGO_EXT fill:#13A538,stroke:#0A6B2B,color:#fff
    style KAFKA_EXT fill:#231F20,stroke:#0F0D0E,color:#fff
```

## 🔄 Data Flow: Order Creation Saga

```mermaid
sequenceDiagram
    actor User as User<br/>(Browser)
    participant Gateway as API Gateway<br/>(Port 9000)
    participant OrderSvc as Order Service<br/>(Port 8081)
    participant InventorySvc as Inventory Service<br/>(Port 8082)
    participant NotifSvc as Notification<br/>(Port 8083)
    participant Kafka as Kafka Broker
    participant PG as PostgreSQL
    participant MySQL as MySQL
    
    User->>Gateway: POST /api/v2/orders<br/>{items, customerId}
    
    Note over Gateway: Validate JWT<br/>RBAC check
    
    Gateway->>OrderSvc: Route request
    
    activate OrderSvc
    OrderSvc->>OrderSvc: Create Order entity<br/>(status=PENDING)
    OrderSvc->>PG: INSERT order<br/>(LOCAL TRANSACTION)
    PG-->>OrderSvc: ✓ Committed
    
    Note over OrderSvc: Publish event to Kafka
    OrderSvc->>Kafka: order.created event
    deactivate OrderSvc
    
    activate InventorySvc
    Kafka->>InventorySvc: Listen order.created
    
    InventorySvc->>InventorySvc: Reserve stock
    InventorySvc->>MySQL: UPDATE inventory SET<br/>quantity_reserved += qty
    MySQL-->>InventorySvc: ✓ Updated
    
    Note over InventorySvc: Publish success event
    InventorySvc->>Kafka: inventory.reserved
    deactivate InventorySvc
    
    activate OrderSvc
    Kafka->>OrderSvc: Listen inventory.reserved
    OrderSvc->>PG: UPDATE order<br/>status=CONFIRMED
    PG-->>OrderSvc: ✓ Updated
    
    OrderSvc->>Kafka: order.confirmed event
    deactivate OrderSvc
    
    activate NotifSvc
    Kafka->>NotifSvc: Listen order.confirmed
    NotifSvc->>NotifSvc: Compose email
    Note over NotifSvc: (External: Send via SMTP)
    deactivate NotifSvc
    
    OrderSvc->>Gateway: Return Order{id, status}
    Gateway->>User: 201 Created<br/>{orderId, totalAmount}
    
    Note over User: User receives confirmation<br/>Email sent to customer
```

## 🔒 Security Flow: OAuth2 Authentication

```mermaid
sequenceDiagram
    participant Client as Client<br/>(Browser)
    participant Gateway as API Gateway
    participant Keycloak as Keycloak<br/>(Port 8181)
    participant Service as Protected Service
    
    Client->>Client: Need data
    
    alt Token not available
        Client->>Gateway: GET /api/products
        Gateway-->>Client: 302 Redirect
        Client->>Keycloak: /protocol/openid-connect/auth
        
        Note over Keycloak: User enters credentials
        
        Keycloak-->>Client: Redirect with code
        
        Client->>Gateway: /login/oauth2/code/keycloak?code=ABC123
        
        activate Gateway
        Gateway->>Keycloak: POST /token<br/>(with code, secret)
        Keycloak-->>Gateway: {access_token, refresh_token}
        Gateway-->>Client: Store token (httpOnly cookie)
        deactivate Gateway
    end
    
    Client->>Gateway: GET /api/products<br/>Cookie: access_token
    
    activate Gateway
    Gateway->>Gateway: Extract JWT from cookie
    Gateway->>Keycloak: Validate JWT signature
    Keycloak-->>Gateway: ✓ Valid (claims: roles, sub)
    
    Note over Gateway: Add headers:<br/>X-User-ID: user123<br/>X-User-Roles: USER,PREMIUM
    
    Gateway->>Service: Route to service<br/>(with user context)
    Service-->>Gateway: Response
    Gateway-->>Client: 200 OK {products}
    deactivate Gateway
    
    alt Token expired
        Client->>Gateway: GET /api/orders<br/>Expired token
        Gateway-->>Client: 401 Unauthorized
        Client->>Gateway: POST /refresh<br/>with refresh_token
        Gateway->>Keycloak: POST /token<br/>(refresh_token grant)
        Keycloak-->>Gateway: New access_token
        Gateway-->>Client: New token
        Client->>Gateway: Retry with new token
    end
```

## 🎯 Deployment Architecture

```mermaid
graph TB
    subgraph "Development (Local)"
        DC["🐳 Docker Compose<br/><br/>docker-compose up<br/>- All services<br/>- All databases<br/>- Kafka, Keycloak<br/>- Prometheus, Grafana"]
    end
    
    subgraph "CI/CD Pipeline (GitHub Actions)"
        GH["🔄 GitHub Actions<br/><br/>On: git push<br/>1. Build (Maven)<br/>2. Test (JUnit)<br/>3. Security scan (Trivy)<br/>4. Build Docker image<br/>5. Push to registry"]
    end
    
    subgraph "Staging (Kubernetes)"
        STAGE["☸️ Kubernetes Cluster<br/>(staging namespace)<br/><br/>- 3 replicas per service<br/>- Auto-scaling: 3-5<br/>- Resource limits<br/>- Health checks"]
        
        STAGE_DB["📊 Managed Databases<br/>- MongoDB<br/>- PostgreSQL<br/>- MySQL"]
        
        STAGE_OBS["📈 Observability<br/>- Prometheus<br/>- Grafana<br/>- Zipkin"]
    end
    
    subgraph "Production (Kubernetes)"
        PROD["☸️ Kubernetes Cluster<br/>(production namespace)<br/><br/>- 5 replicas per service<br/>- Auto-scaling: 5-20<br/>- Pod disruption budgets<br/>- Network policies"]
        
        PROD_DB["📊 Managed Databases<br/>- MongoDB (replicated)<br/>- PostgreSQL (HA)<br/>- MySQL (replicated)"]
        
        PROD_OBS["📈 Observability<br/>- Prometheus (HA)<br/>- Grafana (HA)<br/>- Zipkin (persistent)"]
        
        LB["⚖️ Load Balancer<br/>nginx ingress<br/>API: api.micro-commerce.com"]
        
        CDN["🚀 CDN<br/>CloudFront/CloudFlare<br/>Static assets"]
    end
    
    DC -->|docker-compose up| GH
    GH -->|Build & Push| STAGE
    STAGE -->|Smoke tests| GH
    GH -->|Manual approval| PROD
    PROD -->|Load balancer| LB
    LB -->|Frontend| CDN
    
    style DC fill:#E8F4F8,stroke:#2E5C8A
    style GH fill:#FFF4E6,stroke:#8B6914
    style STAGE fill:#E8F5E9,stroke:#4CAF50
    style PROD fill:#FCE4EC,stroke:#E91E63
    style LB fill:#FFEBEE,stroke:#F44336
    style CDN fill:#FFF3E0,stroke:#FF9800
```

---

**Próximas seções:**
- [Matriz de Decisões Tecnológicas](./TECHNOLOGY-MATRIX.md)
- [Análise de Padrões](./PATTERNS-ANALYSIS.md)
- [Recomendações de Melhorias](./RECOMMENDATIONS.md)
