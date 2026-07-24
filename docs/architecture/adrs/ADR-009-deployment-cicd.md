# ADR-009: Deployment Strategy (Docker + Kubernetes + CI/CD)

**Status:** ✅ Implementado  
**Data:** 2026-07-23  
**Decisores:** DevOps & Platform Team  
**Tech Stack:** Docker, Kubernetes, GitHub Actions, Helm

---

## 📋 Contexto

Projeto com 5+ microserviços necessita:
- 🐳 Containerização consistente
- ☸️ Orquestração em prod
- 🔄 CI/CD automático
- 📦 Versionamento de releases
- 🚀 Blue-Green/Canary deployments

## 🎯 Decisão

**Docker + Kubernetes em Produção, Docker Compose em Dev:**

```
┌────────────────────────────────────────┐
│         Development Local              │
│     (Docker Compose - docker-compose.yaml)
├────────────────────────────────────────┤
│ ✓ Single command: docker-compose up   │
│ ✓ All services + dependencies         │
│ ✓ Fast feedback loop                  │
│ ✓ Mirrors prod architecture           │
└────────────────────────────────────────┘
         ↓ (git push)
┌────────────────────────────────────────┐
│      Continuous Integration            │
│   (GitHub Actions - .github/workflows) │
├────────────────────────────────────────┤
│ 1. Build Docker images                 │
│ 2. Run tests (unit + integration)     │
│ 3. Security scan (SAST)                │
│ 4. Push to registry (DockerHub)        │
│ 5. Create release notes                │
└────────────────────────────────────────┘
         ↓ (manual approval)
┌────────────────────────────────────────┐
│      Continuous Deployment             │
│  (Kubernetes - staging → production)   │
├────────────────────────────────────────┤
│ 1. Deploy to staging (k8s)            │
│ 2. Smoke tests (automated)             │
│ 3. Canary deployment (5% traffic)      │
│ 4. Monitor metrics (Prometheus)        │
│ 5. Gradual rollout (95% traffic)       │
│ 6. Rollback if issues (automated)      │
└────────────────────────────────────────┘
```

## 🔧 Implementação Técnica

### 1️⃣ **Dockerfile Strategy**

**Multi-stage build para otimizar tamanho:**

```dockerfile
# Stage 1: Builder
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy source code
COPY pom.xml .
COPY src ./src

# Build
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime (minimal)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only compiled JAR
COPY --from=builder /build/target/product-service-*.jar app.jar

# Security: Non-root user
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run
CMD ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

**Otimizações:**
```dockerfile
# Layer caching: dependências mudam menos
FROM maven:3.9-eclipse-temurin-21 AS builder
COPY pom.xml .
RUN mvn dependency:resolve  # Cache layer

COPY src ./src
RUN mvn clean package  # Only rebuilds on src changes

# Minimal runtime image (Alpine)
FROM eclipse-temurin:21-jre-alpine  # ~190MB vs 700MB

# Security best practices
RUN addgroup -g 1001 appuser && adduser -D -u 1001 -G appuser appuser
USER appuser  # Non-root

# Health check para Kubernetes readiness
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --quiet --spider http://localhost:8080/actuator/health
```

### 2️⃣ **Docker Compose (Development)**

```yaml
# docker-compose.yaml
version: '3.9'

services:
  # ===== Frontend =====
  frontend-app:
    container_name: frontend-app
    build:
      context: ./frontend/java-shop-website
      dockerfile: Dockerfile
    image: micro-commerce/frontend:latest
    ports:
      - "4200:80"
    depends_on:
      - api-gateway
    environment:
      - API_URL=http://localhost:9000
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:80/"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== API Gateway =====
  api-gateway:
    container_name: api-gateway
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    image: micro-commerce/api-gateway:${VERSION:-latest}
    ports:
      - "9000:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka/
      - KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/master
      - KEYCLOAK_CLIENT_SECRET=${KEYCLOAK_CLIENT_SECRET}
    depends_on:
      - product-service
      - order-service
      - inventory-service
      - keycloak
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Product Service =====
  product-service:
    container_name: product-service
    build:
      context: ./product-service/product-service
      dockerfile: Dockerfile
    image: micro-commerce/product-service:${VERSION:-latest}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/products
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka/
      - MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
    depends_on:
      - mongodb
      - eureka
      - kafka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Order Service =====
  order-service:
    container_name: order-service
    build:
      context: ./order-service
      dockerfile: Dockerfile
    image: micro-commerce/order-service:${VERSION:-latest}
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/orders_db
      - SPRING_DATASOURCE_USERNAME=orders_user
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka/
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
    depends_on:
      - postgres
      - eureka
      - kafka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Inventory Service =====
  inventory-service:
    container_name: inventory-service
    build:
      context: ./inventory-service/inventory-service
      dockerfile: Dockerfile
    image: micro-commerce/inventory-service:${VERSION:-latest}
    ports:
      - "8082:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/inventory_db
      - SPRING_DATASOURCE_USERNAME=inventory_user
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka/
    depends_on:
      - mysql
      - eureka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Notification Service =====
  notification-service:
    container_name: notification-service
    build:
      context: ./notification-service
      dockerfile: Dockerfile
    image: micro-commerce/notification-service:${VERSION:-latest}
    ports:
      - "8083:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
      - MAIL_HOST=${MAIL_HOST:-smtp.gmail.com}
      - MAIL_PORT=${MAIL_PORT:-587}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
    depends_on:
      - kafka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Service Discovery (Eureka) =====
  eureka:
    container_name: eureka-server
    image: springcloud/eureka-server:latest
    ports:
      - "8761:8761"
    environment:
      - EUREKA_INSTANCE_HOSTNAME=eureka
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Databases =====
  mongodb:
    container_name: mongodb
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    environment:
      - MONGO_INITDB_ROOT_USERNAME=root
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
    networks:
      - micro-commerce
    restart: unless-stopped

  postgres:
    container_name: postgres
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=orders_db
      - POSTGRES_USER=orders_user
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    networks:
      - micro-commerce
    restart: unless-stopped

  mysql:
    container_name: mysql
    image: mysql:8-alpine
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    environment:
      - MYSQL_DATABASE=inventory_db
      - MYSQL_USER=inventory_user
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Message Broker =====
  kafka:
    container_name: kafka
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper
    networks:
      - micro-commerce
    restart: unless-stopped

  zookeeper:
    container_name: zookeeper
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Observability =====
  prometheus:
    container_name: prometheus
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - micro-commerce
    restart: unless-stopped

  grafana:
    container_name: grafana
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - micro-commerce
    restart: unless-stopped

  zipkin:
    container_name: zipkin
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
    networks:
      - micro-commerce
    restart: unless-stopped

  # ===== Authentication =====
  keycloak:
    container_name: keycloak
    image: quay.io/keycloak/keycloak:24.0.1
    command: ["start-dev", "--import-realm"]
    ports:
      - "8181:8080"
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}
      - KC_DB=mysql
      - KC_DB_URL=jdbc:mysql://keycloak-mysql:3306/keycloak
      - KC_DB_USERNAME=keycloak
      - KC_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD}
    volumes:
      - ./docker/keycloak/realms:/opt/keycloak/data/import/
    depends_on:
      - keycloak-mysql
    networks:
      - micro-commerce
    restart: unless-stopped

  keycloak-mysql:
    container_name: keycloak-mysql
    image: mysql:8-alpine
    ports:
      - "3307:3306"
    volumes:
      - keycloak_mysql_data:/var/lib/mysql
    environment:
      - MYSQL_DATABASE=keycloak
      - MYSQL_USER=keycloak
      - MYSQL_PASSWORD=${KEYCLOAK_DB_PASSWORD}
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
    networks:
      - micro-commerce
    restart: unless-stopped

volumes:
  mongo_data:
  postgres_data:
  mysql_data:
  keycloak_mysql_data:
  prometheus_data:
  grafana_data:

networks:
  micro-commerce:
    driver: bridge
```

### 3️⃣ **GitHub Actions CI/CD Pipeline**

```yaml
# .github/workflows/build-and-deploy.yml
name: Build & Deploy Microservices

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ===== Build & Test =====
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [api-gateway, product-service, order-service, 
                 inventory-service, notification-service]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Build with Maven
        run: |
          cd ${{ matrix.service }}
          mvn clean package -DskipTests
      
      - name: Run Unit Tests
        run: |
          cd ${{ matrix.service }}
          mvn test
      
      - name: Run Integration Tests
        run: |
          cd ${{ matrix.service }}
          mvn verify -P integration-tests
      
      - name: SonarQube Scan
        uses: sonarsource/sonarcloud-github-action@master
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      
      - name: Build Docker Image
        run: |
          docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}:${{ github.sha }} \
                       -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}:latest \
                       ./${{ matrix.service }}
      
      - name: Security Scan (Trivy)
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload Trivy Results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
      
      - name: Push Docker Image
        if: github.event_name == 'push'
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ${{ env.REGISTRY }} -u ${{ github.actor }} --password-stdin
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}:${{ github.sha }}
          docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}:latest

  # ===== Deploy to Staging =====
  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure Kubernetes
        uses: azure/setup-kubectl@v3
        with:
          version: 'v1.27.0'
      
      - name: Set Kubeconfig
        run: |
          mkdir -p $HOME/.kube
          echo "${{ secrets.KUBE_CONFIG_STAGING }}" | base64 -d > $HOME/.kube/config
      
      - name: Deploy with Helm
        run: |
          helm upgrade --install micro-commerce ./helm/micro-commerce \
            --namespace staging \
            --values ./helm/values-staging.yaml \
            --set image.tag=${{ github.sha }}
      
      - name: Wait for Rollout
        run: |
          kubectl rollout status deployment/product-service -n staging --timeout=5m
          kubectl rollout status deployment/order-service -n staging --timeout=5m
          kubectl rollout status deployment/inventory-service -n staging --timeout=5m
      
      - name: Smoke Tests
        run: |
          bash ./scripts/smoke-tests-staging.sh

  # ===== Deploy to Production =====
  deploy-production:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production  # Manual approval required
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure Kubernetes
        uses: azure/setup-kubectl@v3
      
      - name: Set Kubeconfig
        run: |
          mkdir -p $HOME/.kube
          echo "${{ secrets.KUBE_CONFIG_PROD }}" | base64 -d > $HOME/.kube/config
      
      - name: Canary Deployment
        run: |
          # Deploy to 5% of traffic first
          helm upgrade micro-commerce ./helm/micro-commerce \
            --namespace production \
            --values ./helm/values-prod.yaml \
            --set image.tag=${{ github.sha }} \
            --set canary.enabled=true \
            --set canary.weight=5
      
      - name: Monitor Canary Metrics
        run: |
          sleep 120  # Monitor for 2 minutes
          bash ./scripts/check-canary-health.sh
      
      - name: Rollout to 100%
        run: |
          helm upgrade micro-commerce ./helm/micro-commerce \
            --namespace production \
            --values ./helm/values-prod.yaml \
            --set image.tag=${{ github.sha }} \
            --set canary.enabled=false
      
      - name: Smoke Tests
        run: |
          bash ./scripts/smoke-tests-prod.sh
      
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: v${{ github.run_number }}
          body_path: RELEASE_NOTES.md
```

### 4️⃣ **Kubernetes Manifests**

```yaml
# helm/micro-commerce/values-prod.yaml
replicaCount: 3

image:
  repository: ghcr.io/micro-commerce
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: LoadBalancer
  port: 9000

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: api.micro-commerce.com
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5

canary:
  enabled: false
  weight: 100  # 0-100 for traffic split
```

**Deployment manifest:**
```yaml
# helm/micro-commerce/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "micro-commerce.fullname" . }}-{{ .Chart.Name }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app: product-service
  template:
    metadata:
      labels:
        app: product-service
        version: {{ .Chart.AppVersion }}
    spec:
      containers:
      - name: product-service
        image: "{{ .Values.image.repository }}/product-service:{{ .Values.image.tag }}"
        ports:
        - containerPort: 8080
        livenessProbe:
          {{ toYaml .Values.livenessProbe | indent 10 }}
        readinessProbe:
          {{ toYaml .Values.readinessProbe | indent 10 }}
        resources:
          {{ toYaml .Values.resources | indent 10 }}
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: product-service-secrets
              key: database-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: product-service-secrets
              key: database-user
```

## 🚀 Deployment Strategies

### **Blue-Green Deployment:**
```
Blue (running):   v1.0.0 (100% traffic)
Green (new):      v1.1.0 (0% traffic)

Test v1.1.0 ... OK
Switch router:    v1.1.0 (100% traffic)
Keep Blue ready:  v1.0.0 (hot standby)
```

### **Canary Deployment:**
```
Stable:   v1.0.0 (95% traffic)
Canary:   v1.1.0 (5% traffic)

Monitor metrics for 30 min:
  - Error rate < 1%
  - Latency p99 < 500ms
  - CPU < 80%

If OK: Rollout v1.1.0 to 100%
If BAD: Automatic rollback to v1.0.0
```

## 📊 Deployment Readiness Checklist

```
┌─ Pre-Deployment
│  ✅ Code reviewed
│  ✅ All tests passing (>80% coverage)
│  ✅ Security scan clean (no critical vulns)
│  ✅ Performance baseline established
│  ✅ Documentation updated
│
├─ During Deployment
│  ✅ Canary deployed (5% traffic)
│  ✅ Metrics monitored (2 minutes)
│  ✅ Error rate stable
│  ✅ Rollback plan ready
│
└─ Post-Deployment
   ✅ All services healthy
   ✅ No increased error rate
   ✅ Latency within SLOs
   ✅ Audit log updated
```

---

**Relacionado:** ADR-001, ADR-003, ADR-004  
**Referências:**
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Chart Best Practices](https://helm.sh/docs/chart_best_practices/)
- [GitHub Actions CI/CD](https://docs.github.com/en/actions)
- [Continuous Delivery - Jez Humble](https://continuousdelivery.com/)
