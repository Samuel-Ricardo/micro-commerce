# Deployment Guide

**Document Status:** Draft  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Deployment Overview](#deployment-overview)
2. [Docker Containerization](#docker-containerization)
3. [Kubernetes Manifests](#kubernetes-manifests)
4. [CI/CD Pipeline](#cicd-pipeline)
5. [Environment Setup](#environment-setup)
6. [Release Process](#release-process)
7. [Rollback Procedures](#rollback-procedures)

---

## Deployment Overview

### Deployment Architecture

```
Development → Staging → Production
    ↓
Docker Build
Docker Registry
Kubernetes Cluster
Monitor & Alert
```

### Deployment Tools

- **Containerization**: Docker
- **Orchestration**: Kubernetes
- **Registry**: Docker Hub / Private Registry
- **CI/CD**: GitHub Actions
- **Infrastructure**: Cloud-agnostic (AWS/Azure/GCP)

---

## Docker Containerization

### Dockerfile Best Practices

#### Base Service Dockerfile

```dockerfile
# Multi-stage build for minimal image size
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY . .

# Build application
RUN mvn clean package -DskipTests

# Runtime image
FROM eclipse-temurin:21-jre-slim

LABEL maintainer="Micro-Commerce Team"
LABEL version="1.0"

# Create non-root user
RUN useradd -m -u 1000 appuser

# Copy jar from builder
COPY --from=builder /build/target/*.jar /app/app.jar
CHOWN appuser:appuser /app/app.jar

# Switch to non-root user
USER appuser

WORKDIR /app

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Building Docker Images

```bash
# Build single service
docker build -t samuelrcabral/product-service:1.0.0 -f product-service/Dockerfile .

# Build all services
for service in api-gateway product-service order-service inventory-service notification-service; do
    docker build -t samuelrcabral/${service}:1.0.0 -f ${service}/Dockerfile .
done

# Tag for registry
docker tag samuelrcabral/product-service:1.0.0 registry.example.com/micro-commerce/product-service:1.0.0

# Push to registry
docker push registry.example.com/micro-commerce/product-service:1.0.0
```

### Docker Compose for Local Testing

```yaml
version: '3.9'

services:
  product-service:
    image: samuelrcabral/product-service:latest
    ports:
      - "8081:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/micro_commerce
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root123
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka/
    depends_on:
      - mysql
      - eureka
    networks:
      - micro-commerce

  order-service:
    image: samuelrcabral/order-service:latest
    ports:
      - "8082:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/micro_commerce
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root123
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka/
    depends_on:
      - mysql
      - kafka
      - eureka
    networks:
      - micro-commerce

  # ... other services ...

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: micro_commerce
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - micro-commerce

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    networks:
      - micro-commerce

  eureka:
    image: samuelrcabral/eureka:latest
    ports:
      - "8761:8761"
    networks:
      - micro-commerce

volumes:
  mysql-data:

networks:
  micro-commerce:
    driver: bridge
```

---

## Kubernetes Manifests

### Deployment Template

```yaml
# product-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
  namespace: default
  labels:
    app: product-service
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  
  selector:
    matchLabels:
      app: product-service
  
  template:
    metadata:
      labels:
        app: product-service
        version: v1
    
    spec:
      containers:
        - name: product-service
          image: registry.example.com/micro-commerce/product-service:1.0.0
          imagePullPolicy: IfNotPresent
          
          ports:
            - containerPort: 8080
              protocol: TCP
          
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                configMapKeyRef:
                  name: product-service-config
                  key: datasource.url
            
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: password
            
            - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
              value: "http://eureka:8761/eureka/"
          
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 500m
              memory: 1Gi
          
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 2
          
          lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 15"]
```

### Service Definition

```yaml
# product-service-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: product-service
  namespace: default
  labels:
    app: product-service
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
      protocol: TCP
      name: http
  selector:
    app: product-service
  sessionAffinity: None
```

### Horizontal Pod Autoscaler

```yaml
# product-service-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

env:
  REGISTRY: registry.example.com
  IMAGE_NAME: micro-commerce

jobs:
  build:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        service: [api-gateway, product-service, order-service, inventory-service, notification-service]
    
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
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      
      - name: Login to Docker Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
      
      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: ${{ matrix.service }}
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
  
  test:
    runs-on: ubuntu-latest
    needs: build
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          cache: maven
      
      - name: Run tests
        run: mvn clean verify
      
      - name: Generate coverage report
        run: mvn jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
  
  deploy:
    runs-on: ubuntu-latest
    needs: [build, test]
    if: github.ref == 'refs/heads/main' || startsWith(github.ref, 'refs/tags/v')
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure kubectl
        run: |
          mkdir -p $HOME/.kube
          echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > $HOME/.kube/config
          chmod 600 $HOME/.kube/config
      
      - name: Deploy to Kubernetes
        run: |
          kubectl apply -f k8s/
          kubectl rollout status deployment/product-service -n default
          kubectl rollout status deployment/order-service -n default
          kubectl rollout status deployment/inventory-service -n default
```

---

## Environment Setup

### Production Environment Variables

```bash
# .env.production
# Database
DB_HOST=prod-db.example.com
DB_PORT=3306
DB_NAME=micro_commerce
DB_USER=dbuser
DB_PASSWORD=${PROD_DB_PASSWORD}

# Keycloak
KEYCLOAK_REALM=micro-commerce
KEYCLOAK_SERVER_URL=https://keycloak.example.com
KEYCLOAK_CLIENT_ID=api-gateway
KEYCLOAK_CLIENT_SECRET=${KEYCLOAK_SECRET}

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092
KAFKA_SECURITY_PROTOCOL=SSL
KAFKA_SSL_TRUSTSTORE_PATH=/etc/ssl/kafka/truststore.jks
KAFKA_SSL_TRUSTSTORE_PASSWORD=${KAFKA_TRUSTSTORE_PASSWORD}

# Redis
REDIS_HOST=redis-cluster.example.com
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

# Logging
LOG_LEVEL=INFO
ELASTICSEARCH_HOST=logging.example.com
ELASTICSEARCH_PORT=9200

# Monitoring
PROMETHEUS_ENABLED=true
JAEGER_ENABLED=true
JAEGER_ENDPOINT=http://jaeger.example.com:14268/api/traces
```

### ConfigMap for Kubernetes

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: default
data:
  application.properties: |
    server.port=8080
    spring.application.name=micro-commerce
    logging.level.root=INFO
    management.endpoints.web.exposure.include=health,info,metrics,prometheus
  
  kafka.properties: |
    bootstrap.servers=kafka:9092
    client.id=product-service
    security.protocol=PLAINTEXT
```

---

## Release Process

### Version Numbering

```
MAJOR.MINOR.PATCH
1.0.0 → Initial release
1.1.0 → New feature
1.0.1 → Bug fix
```

### Release Steps

```bash
#!/bin/bash
# release.sh

VERSION=$1  # e.g., 1.0.0

echo "Preparing release $VERSION..."

# 1. Create release branch
git checkout -b release/$VERSION

# 2. Update version in pom.xml
mvn versions:set -DnewVersion=$VERSION
mvn versions:commit

# 3. Commit changes
git add -A
git commit -m "chore: bump version to $VERSION"

# 4. Create tag
git tag -a v$VERSION -m "Release $VERSION"

# 5. Push changes
git push origin release/$VERSION
git push origin v$VERSION

# 6. Create GitHub release
# (automated by GitHub Actions based on tag)

echo "Release $VERSION created successfully!"
```

---

## Rollback Procedures

### Kubernetes Rollback

```bash
# View rollout history
kubectl rollout history deployment/product-service

# Rollback to previous version
kubectl rollout undo deployment/product-service

# Rollback to specific revision
kubectl rollout undo deployment/product-service --to-revision=2

# Verify rollback
kubectl rollout status deployment/product-service
```

### Blue-Green Deployment

```yaml
# Maintain two versions simultaneously
apiVersion: v1
kind: Service
metadata:
  name: product-service
spec:
  selector:
    app: product-service
    version: blue  # Switch between blue and green
  ports:
    - port: 8080
```

---

## Related Documentation

- [Implementation-Guide.md](./IMPLEMENTATION-GUIDE.md) — Development setup
- [Performance-Optimization.md](./PERFORMANCE-OPTIMIZATION.md) — Scaling considerations

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** DevOps Team
- **Audience:** Operations and DevOps teams

