# Known Issues

**Document Status:** Draft  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Critical Issues](#critical-issues)
2. [High Priority Issues](#high-priority-issues)
3. [Medium Priority Issues](#medium-priority-issues)
4. [Low Priority Issues](#low-priority-issues)
5. [Technical Debt](#technical-debt)
6. [Performance Issues](#performance-issues)
7. [Security Concerns](#security-concerns)

---

## Critical Issues

### 🔴 CRITICAL-001: Database Connection Pool Exhaustion

**Status**: Open  
**Severity**: Critical  
**Impact**: Application becomes unresponsive under load  

**Description**:
Under peak load (>1000 concurrent users), the HikariCP connection pool exhausts available connections, causing all database requests to timeout.

**Root Cause**:
- Current pool size: 10 (too small for expected load)
- Service-to-service calls don't properly release connections
- No connection monitoring in place

**Workaround**:
Temporarily increase pool size to 30 in `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=30
```

**Permanent Solution**:
1. Implement connection monitoring with Micrometer
2. Implement async non-blocking database driver (R2DBC)
3. Add circuit breaker with fallback responses

**Ticket**: [SHOP-001](https://github.com/samuel-ricardo/micro-commerce/issues/1)

---

### 🔴 CRITICAL-002: Inventory Synchronization Race Condition

**Status**: Open  
**Severity**: Critical  
**Impact**: Inventory inconsistency, overselling  

**Description**:
When multiple orders are placed simultaneously for the same product, the inventory can be oversold due to race condition in the reservation process.

**Root Cause**:
```java
// Race condition: read-modify-write not atomic
InventoryItem item = inventoryRepository.findById(productId);
if (item.getStockLevel() > quantity) {
    item.setStockLevel(item.getStockLevel() - quantity);  // ❌ Lost update
    inventoryRepository.save(item);
}
```

**Example Scenario**:
```
Initial stock: 100
Request 1: Check stock (100 > 50) ✓
Request 2: Check stock (100 > 50) ✓
Request 1: Deduct (100 - 50 = 50)
Request 2: Deduct (100 - 50 = 50)  // ❌ Should be (50 - 50 = 0)
Final stock: 50 (should be 0!)
```

**Workaround**:
Enable pessimistic locking in repository:
```java
@Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);
```

**Permanent Solution**:
1. Implement optimistic locking with version field
2. Move to event-sourcing pattern for inventory
3. Add integration tests for concurrent scenarios

**Ticket**: [SHOP-002](https://github.com/samuel-ricardo/micro-commerce/issues/2)

---

## High Priority Issues

### 🟠 HIGH-001: N+1 Query Problem in Order Retrieval

**Status**: Open  
**Severity**: High  
**Impact**: Database performance degradation, slow page load  

**Description**:
Retrieving orders with associated products causes N+1 database queries.

**Code**:
```java
List<Order> orders = orderRepository.findByCustomerId(customerId);
// Iterating triggers additional query per order for products
orders.forEach(order -> {
    order.getOrderItems().forEach(item -> {
        Product product = item.getProduct();  // ❌ Triggers query
    });
});
```

**Performance Impact**:
- 1 order: 1 query
- 10 orders: 11 queries (1 + 10 for products)
- 100 orders: 101 queries

**Solution**: Implement eager loading

```java
@Query("SELECT DISTINCT o FROM Order o " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.product " +
       "WHERE o.customerId = :customerId")
List<Order> findByCustomerIdWithProducts(@Param("customerId") Long customerId);
```

**Ticket**: [SHOP-003](https://github.com/samuel-ricardo/micro-commerce/issues/3)

---

### 🟠 HIGH-002: No Caching Layer

**Status**: Open  
**Severity**: High  
**Impact**: High database load, slow product searches  

**Description**:
Product catalog is queried directly from database on every request without caching, causing excessive load during peak traffic.

**Metrics**:
- Current: 1000 queries/sec for 800 req/sec
- With cache: Expected ~200 queries/sec

**Solution**:
Implement Redis caching:
```yaml
# docker-compose.yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
```

```java
@Service
@EnableCaching
public class ProductService {
    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(Long id) { ... }
}
```

**Ticket**: [SHOP-004](https://github.com/samuel-ricardo/micro-commerce/issues/4)

---

### 🟠 HIGH-003: Missing Database Indexes

**Status**: Open  
**Severity**: High  
**Impact**: Slow queries, full table scans  

**Description**:
Common queries don't have supporting indexes, causing full table scans.

**Affected Queries**:
```sql
-- Slow: Full table scan
SELECT * FROM orders WHERE customer_id = 123;
SELECT * FROM products WHERE category_id = 5;
SELECT * FROM orders WHERE status = 'PENDING';
```

**Missing Indexes**:
```sql
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
```

**Ticket**: [SHOP-005](https://github.com/samuel-ricardo/micro-commerce/issues/5)

---

## Medium Priority Issues

### 🟡 MEDIUM-001: Service-to-Service Communication Not Encrypted

**Status**: Open  
**Severity**: Medium  
**Impact**: Data transmitted in plaintext between services  

**Description**:
Service-to-service calls use HTTP instead of HTTPS/mTLS.

**Solution**:
Implement mTLS with Istio service mesh.

**Ticket**: [SHOP-006](https://github.com/samuel-ricardo/micro-commerce/issues/6)

---

### 🟡 MEDIUM-002: Insufficient Error Handling

**Status**: Open  
**Severity**: Medium  
**Impact**: Stack traces exposed to clients  

**Description**:
Some endpoints return full stack traces in error responses instead of user-friendly messages.

**Solution**:
Implement global exception handler:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("Product not found", "PRODUCT_NOT_FOUND"));
    }
}
```

**Ticket**: [SHOP-007](https://github.com/samuel-ricardo/micro-commerce/issues/7)

---

### 🟡 MEDIUM-003: No Request Timeout Configuration

**Status**: Open  
**Severity**: Medium  
**Impact**: Hung requests block threads  

**Description**:
Services don't have request timeout configuration, causing indefinite blocking on slow operations.

**Solution**:
```properties
# application.properties
server.tomcat.connection-timeout=30000  # 30 seconds
server.tomcat.keep-alive-timeout=60000  # 60 seconds
```

**Ticket**: [SHOP-008](https://github.com/samuel-ricardo/micro-commerce/issues/8)

---

## Low Priority Issues

### 🟢 LOW-001: Inconsistent Logging Levels

**Status**: Open  
**Severity**: Low  
**Impact**: Difficulty debugging in production  

**Description**:
Different services use different logging levels, making it hard to troubleshoot issues.

**Solution**:
Standardize logging configuration across all services.

**Ticket**: [SHOP-009](https://github.com/samuel-ricardo/micro-commerce/issues/9)

---

### 🟢 LOW-002: Missing API Documentation

**Status**: Open  
**Severity**: Low  
**Impact**: Developers unclear on API contracts  

**Description**:
API endpoints lack comprehensive documentation.

**Solution**:
Generate OpenAPI/Swagger documentation automatically.

---

## Technical Debt

### Outdated Spring Cloud Version

```yaml
Current: Spring Cloud 2024.0.0
Latest: Spring Cloud 2024.0.1

Action: Plan upgrade in next sprint
```

### Custom Configuration Serialization

```java
// Current: Manual JSON parsing
JSONObject json = new JSONObject(config);

// Should use: Jackson ObjectMapper
ObjectMapper mapper = new ObjectMapper();
Config config = mapper.readValue(json, Config.class);
```

---

## Performance Issues

### Slow Product Search

**Symptom**: Search takes >1 second for typical queries  
**Root Cause**: No full-text search index  
**Solution**: Implement Elasticsearch indexing

### Memory Leaks in Notification Service

**Symptom**: Memory usage grows over time  
**Root Cause**: Kafka consumer not properly releasing memory  
**Solution**: Profile with JProfiler and fix memory leaks

---

## Security Concerns

### Secrets in Environment Variables

**Status**: Partial implementation  
**Issue**: Some services still have hardcoded API keys  
**Solution**: Migrate to HashiCorp Vault

### OWASP Top 10 - Incomplete Coverage

| Vulnerability | Status | Priority |
|---|---|---|
| Broken Access Control | ✅ Implemented | - |
| Cryptographic Failures | ⚠️ Partial | High |
| Injection | ✅ Implemented | - |
| Insecure Design | ⚠️ Partial | Medium |
| Security Misconfiguration | ✅ Implemented | - |
| Vulnerable Components | ⚠️ Scanning | Medium |
| Identification Failures | ✅ Implemented | - |
| Data Integrity Failures | ❌ Not implemented | High |
| Logging Failures | ⚠️ Partial | Medium |
| SSRF | ⚠️ Partial | Medium |

---

## Recommendations for Improvement

### Quick Wins (1-2 sprints)

1. Add missing database indexes
2. Implement Redis caching for products
3. Fix N+1 query problems
4. Add request timeout configuration
5. Implement global exception handler

### Medium-term (3-6 months)

1. Implement mTLS for service-to-service communication
2. Add comprehensive API documentation
3. Set up Elasticsearch for product search
4. Implement distributed tracing with Jaeger
5. Complete OWASP Top 10 coverage

### Long-term (6-12 months)

1. Migrate to event-sourcing for inventory
2. Implement CQRS pattern
3. Add machine learning for recommendations
4. Implement marketplace features
5. Support multiple payment providers

---

## Related Documentation

- [Architecture.md](./ARCHITECTURE.md) — System design
- [Performance-Optimization.md](./PERFORMANCE-OPTIMIZATION.md) — Optimization strategies
- [Security-Analysis.md](./SECURITY-ANALYSIS.md) — Security improvements

---

## Issue Tracking

All issues are tracked in GitHub Issues with labels:

- `critical` — Stop-the-line issues
- `performance` — Performance-related
- `security` — Security concerns
- `tech-debt` — Technical debt
- `bug` — Bugs
- `enhancement` — New features

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** Development Team
- **Review Cycle:** Weekly
- **Audience:** Development team, stakeholders

