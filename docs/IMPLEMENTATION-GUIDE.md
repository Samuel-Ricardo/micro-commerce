# Implementation Guide

**Document Status:** In Progress (Awaiting Tiago Dev Review)  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Building Services](#building-services)
4. [Running Services](#running-services)
5. [Database Setup](#database-setup)
6. [Configuration Management](#configuration-management)
7. [API Endpoints Reference](#api-endpoints-reference)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

- **OS**: Windows 10+, macOS 10.14+, or Linux (Ubuntu 20.04+)
- **Memory**: 16GB RAM minimum (8GB per Docker container group)
- **Storage**: 50GB free space for containers and databases
- **CPU**: 4+ cores recommended

### Required Software

```bash
# Core tools
- Git (2.37+)
- Docker (20.10+)
- Docker Compose (2.0+)
- Java Development Kit (JDK 21+)
- Maven (3.8+)
- Node.js (18+) - for frontend development

# Optional but recommended
- IDE: IntelliJ IDEA Ultimate or VS Code
- Postman or Insomnia for API testing
- kubectl (for Kubernetes interaction)
- kubectx for context switching
```

### Verify Installation

```bash
# Check all installations
java -version
mvn --version
docker --version
docker-compose --version
node --version
git --version
```

---

## Local Development Setup

### 1. Clone Repository

```bash
# Clone the project
git clone https://github.com/Samuel-Ricardo/micro-commerce.git
cd micro-commerce

# Verify structure
ls -la
```

Expected directories:
```
api-gateway/
inventory-service/
notification-service/
order-service/
product-service/
frontend/
docker/
.github/
.env.*.example
docker-compose.yaml
```

### 2. Environment Configuration

Copy environment files and customize as needed:

```bash
# Copy environment templates
cp .env.gateway.example .env.gateway
cp .env.keycloak.example .env.keycloak
cp .env.notification.example .env.notification
cp .env.order.example .env.order
cp .env.kafka.example .env.kafka
cp .env.observability.example .env.observability
```

#### Gateway Configuration (`.env.gateway`)

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Service URLs
PRODUCT_SERVICE_URL=http://product-service:8080
ORDER_SERVICE_URL=http://order-service:8080
INVENTORY_SERVICE_URL=http://inventory-service:8080

# Keycloak Configuration
KEYCLOAK_SERVER_URL=http://keycloak:8080
KEYCLOAK_REALM=micro-commerce
KEYCLOAK_CLIENT_ID=api-gateway

# Logging
logging.level.root=INFO
logging.level.com.js=DEBUG
```

#### Keycloak Configuration (`.env.keycloak`)

```properties
# Admin Credentials
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123

# Database Configuration
KC_DB=mysql
KC_DB_URL=jdbc:mysql://keycloak-mysql:3306/keycloak
KC_DB_USERNAME=keycloak
KC_DB_PASSWORD=keycloak123

# JDBC Pool
KC_DB_POOL_INITIAL_SIZE=10
KC_DB_POOL_MAX_SIZE=20
```

#### Service Configuration Template

```properties
# Each service follows this pattern:
server.port=8080
spring.application.name=product-service

# Database Configuration
spring.datasource.url=jdbc:mysql://mysql:3306/micro_commerce
spring.datasource.username=root
spring.datasource.password=root123
spring.jpa.hibernate.ddl-auto=validate

# Eureka Configuration
eureka.client.service-url.defaultZone=http://eureka:8761/eureka/
eureka.instance.hostname=product-service

# Kafka Configuration
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Keycloak/OAuth2
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/micro-commerce

# Logging
logging.level.root=INFO
logging.level.com.js=DEBUG
```

### 3. Start Infrastructure

```bash
# Start all services with Docker Compose
docker-compose up -d

# Verify all containers are running
docker-compose ps

# Expected output:
# NAME                    STATUS
# keycloak-mysql          Up
# keycloak                Up
# api-gateway             Up
# product-service         Up
# order-service           Up
# inventory-service       Up
# notification-service    Up
# mysql                   Up
# kafka                   Up
```

### 4. Verify Services

```bash
# Check API Gateway
curl -i http://localhost:9000/api/health

# Check Keycloak
curl -i http://localhost:8181/auth/admin

# Check individual services
curl -i http://localhost:8081/api/v1/products       # Product Service
curl -i http://localhost:8082/api/v1/orders         # Order Service
curl -i http://localhost:8083/api/v1/inventory      # Inventory Service
```

---

## Building Services

### Build All Services

```bash
# Build all backend services
mvn clean package

# Skip tests for faster build
mvn clean package -DskipTests

# Build with specific profiles
mvn clean package -P production
```

### Build Individual Services

```bash
# API Gateway
cd api-gateway
mvn clean package -DskipTests
cd ..

# Product Service
cd product-service/product-service
mvn clean package -DskipTests
cd ../..

# Order Service
cd order-service/order-service
mvn clean package -DskipTests
cd ../..

# Inventory Service
cd inventory-service/inventory-service
mvn clean package -DskipTests
cd ../..

# Notification Service
cd notification-service
mvn clean package -DskipTests
cd ..
```

### Build Frontend

```bash
# Navigate to frontend directory
cd frontend/java-shop

# Install dependencies
npm install

# Build for production
npm run build

# Output is in dist/ directory
```

---

## Running Services

### Option 1: Docker Compose (Recommended for Development)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f api-gateway

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Option 2: Local Development (IDE)

#### IntelliJ IDEA

1. Open project as Maven project
2. Configure JDK 21 in Project Settings
3. Run configurations:
   - API Gateway: `com.js.ApiGatewayApplication`
   - Product Service: `com.js.ProductServiceApplication`
   - Order Service: `com.js.OrderServiceApplication`
   - Inventory Service: `com.js.InventoryServiceApplication`

#### VS Code

1. Install Java Extension Pack
2. Open folder
3. Use terminal to run: `mvn spring-boot:run`

### Verify All Services

```bash
# Create a simple test script
#!/bin/bash

echo "Testing API Gateway..."
curl -s http://localhost:9000/api/health | jq .

echo "Testing Product Service..."
curl -s http://localhost:8081/api/v1/products | jq .

echo "Testing Order Service..."
curl -s http://localhost:8082/api/v1/orders | jq .

echo "Testing Inventory Service..."
curl -s http://localhost:8083/api/v1/inventory | jq .

echo "Testing Keycloak..."
curl -s http://localhost:8181/admin | head -20
```

---

## Database Setup

### Initial Database Creation

Databases are automatically created by Docker Compose:

```yaml
Services:
  mysql:
    Database: micro_commerce
    User: root
    Password: root123
  
  keycloak-mysql:
    Database: keycloak
    User: keycloak
    Password: keycloak123
```

### Database Access

```bash
# Connect to MySQL
docker exec -it mysql mysql -uroot -proot123 micro_commerce

# View tables
SHOW TABLES;

# View schema
DESCRIBE products;
DESCRIBE orders;
DESCRIBE order_items;
DESCRIBE inventory_items;
```

### Running Migrations

Each service includes automated schema creation:

```bash
# Via Spring Boot (automatic on startup)
spring.jpa.hibernate.ddl-auto=validate  # Verify only
spring.jpa.hibernate.ddl-auto=update    # Auto-update

# Manual SQL scripts
# Scripts are in: service-name/src/main/resources/db/migration/
```

### Database Backup

```bash
# Backup MySQL database
docker exec mysql mysqldump -uroot -proot123 micro_commerce > backup.sql

# Restore from backup
docker exec -i mysql mysql -uroot -proot123 micro_commerce < backup.sql
```

---

## Configuration Management

### Configuration Hierarchy

1. **application.properties** - Default configuration
2. **application-{profile}.properties** - Profile-specific
3. **.env files** - Docker environment overrides
4. **Environment variables** - Runtime overrides

### Profiles

```bash
# Development (default)
spring.profiles.active=dev

# Testing
spring.profiles.active=test

# Production
spring.profiles.active=prod
```

### Feature Toggles

```properties
# Enable/disable features
feature.products.search.enabled=true
feature.orders.payment.enabled=false
feature.inventory.auto-reorder.enabled=false
feature.notifications.email.enabled=true
```

---

## API Endpoints Reference

### Product Service (Port 8081)

```http
GET     /api/v1/products              # List all products
GET     /api/v1/products/{id}         # Get product by ID
POST    /api/v1/products              # Create new product
PUT     /api/v1/products/{id}         # Update product
DELETE  /api/v1/products/{id}         # Delete product
GET     /api/v1/products/search?q=    # Search products
```

### Order Service (Port 8082)

```http
GET     /api/v1/orders                # List all orders
GET     /api/v1/orders/{id}           # Get order by ID
POST    /api/v1/orders                # Create new order
PUT     /api/v1/orders/{id}           # Update order
PUT     /api/v1/orders/{id}/cancel    # Cancel order
GET     /api/v1/orders/user/{userId}  # Get user orders
```

### Inventory Service (Port 8083)

```http
GET     /api/v1/inventory             # List inventory
GET     /api/v1/inventory/{productId} # Get product inventory
POST    /api/v1/inventory/reserve     # Reserve inventory
DELETE  /api/v1/inventory/reserve/{id}# Release reservation
PUT     /api/v1/inventory/adjust      # Adjust inventory level
```

### API Gateway (Port 9000)

```http
# All above endpoints accessible through gateway
GET     /api/v1/products              # Routed to Product Service
GET     /api/v1/orders                # Routed to Order Service
GET     /api/v1/inventory             # Routed to Inventory Service
```

---

## Troubleshooting

### Common Issues and Solutions

#### Issue: Containers won't start

```bash
# Check logs
docker-compose logs

# Verify ports are not in use
netstat -an | grep 9000
netstat -an | grep 8181

# Kill processes on specific ports (macOS/Linux)
lsof -ti:9000 | xargs kill -9

# Remove old containers
docker-compose down -v
docker system prune -a
```

#### Issue: Database connection errors

```bash
# Verify MySQL is running
docker-compose ps mysql

# Check MySQL logs
docker-compose logs mysql

# Manually test connection
docker exec -it mysql mysql -uroot -proot123 -e "SELECT 1"
```

#### Issue: Service discovery not working

```bash
# Check Eureka dashboard
curl http://localhost:8761

# Verify service registration
curl http://localhost:8761/eureka/apps

# Check service logs for Eureka errors
docker-compose logs product-service | grep -i eureka
```

#### Issue: Keycloak won't start

```bash
# Check Keycloak logs
docker-compose logs keycloak

# Verify database
docker-compose logs keycloak-mysql

# Reset Keycloak (removes data)
docker-compose down -v
docker volume rm micro-commerce_keycloak-mysql-data
docker-compose up -d keycloak-mysql
docker-compose up -d keycloak
```

### Health Checks

```bash
#!/bin/bash
# Health check script

echo "=== System Health Check ==="
echo "API Gateway (9000):"
curl -s http://localhost:9000/api/health | jq . || echo "DOWN"

echo "\nProduct Service:"
curl -s http://localhost:8081/actuator/health | jq . || echo "DOWN"

echo "\nOrder Service:"
curl -s http://localhost:8082/actuator/health | jq . || echo "DOWN"

echo "\nInventory Service:"
curl -s http://localhost:8083/actuator/health | jq . || echo "DOWN"

echo "\nKeycloak:"
curl -s http://localhost:8181/auth/health | jq . || echo "DOWN"

echo "\nMySQL:"
docker exec mysql mysql -uroot -proot123 -e "SELECT 1" &>/dev/null && echo "UP" || echo "DOWN"

echo "\nDocker Containers:"
docker-compose ps
```

---

## Related Documentation

- [Architecture.md](./ARCHITECTURE.md) — System architecture
- [Testing-Strategy.md](./TESTING-STRATEGY.md) — Test execution
- [Deployment-Guide.md](./DEPLOYMENT-GUIDE.md) — Production deployment

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** In Progress
- **Owner:** Tiago Developer (pending)
- **Audience:** Development team

