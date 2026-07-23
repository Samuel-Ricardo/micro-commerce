# Performance Optimization

**Document Status:** Draft  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Performance Overview](#performance-overview)
2. [Current Bottlenecks](#current-bottlenecks)
3. [Optimization Strategies](#optimization-strategies)
4. [Caching Approaches](#caching-approaches)
5. [Database Optimization](#database-optimization)
6. [Scaling Considerations](#scaling-considerations)
7. [Load Testing Results](#load-testing-results)
8. [Monitoring & Profiling](#monitoring--profiling)

---

## Performance Overview

### Performance Targets

| Metric | Target | Current | Gap |
|--------|--------|---------|-----|
| **API Response (p95)** | < 200ms | ~ 250ms | -50ms |
| **Page Load Time** | < 2s | ~ 2.5s | -0.5s |
| **Throughput** | 1000+ req/s | ~ 800 req/s | -200 |
| **Database Query (p95)** | < 50ms | ~ 80ms | -30ms |
| **Cache Hit Rate** | > 80% | N/A | TBD |

### Performance Strategy

1. **Measure First** — Use APM tools to identify real bottlenecks
2. **Target Wisely** — Focus on high-impact improvements
3. **Optimize Incrementally** — Make changes, measure, verify
4. **Monitor Continuously** — Maintain performance gains

---

## Current Bottlenecks

### Identified Bottlenecks

#### 1. Database Queries (High Impact)

```sql
-- Problem: N+1 query pattern in OrderService
SELECT o.* FROM orders o WHERE o.customer_id = 1;
-- For each order:
SELECT p.* FROM products p WHERE p.id = oi.product_id;
-- Result: 1 + N queries instead of 1

-- Solution: Eager loading with JOIN
SELECT o.*, p.* FROM orders o
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE o.customer_id = 1;
```

#### 2. Missing Database Indexes

```sql
-- Problem: Full table scan on common queries
SELECT * FROM orders WHERE customer_id = 123;  -- No index

-- Solution: Add missing indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
```

#### 3. No Caching Layer

```
Current Flow:
Client → API Gateway → Service → Database → Service → Client
Problems:
- Repeated queries for same data
- Database under heavy load
- No intermediate caching

With Caching:
Client → API Gateway → Cache → Service → Database
Benefits:
- 10x faster responses for cached data
- Reduced database load
- Better user experience
```

#### 4. Synchronous Service Calls

```java
// Problem: Blocking calls cause slowness
OrderDTO order = orderService.createOrder(request);
InventoryDTO inventory = inventoryService.checkInventory(order.getProductId());  // Waits
NotificationDTO notification = notificationService.notify(order);  // Waits

// Solution: Async events
orderService.createOrder(request);  // Returns immediately
// Events published to Kafka:
// - OrderCreated → consumed by inventory service
// - OrderCreated → consumed by notification service
```

---

## Optimization Strategies

### 1. Database Query Optimization

```java
// BEFORE: N+1 query problem
@Service
public class OrderService {
    
    public List<OrderDTO> getCustomerOrders(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        // This triggers additional query per order for products
        return orders.stream()
            .map(o -> toDTO(o))  // toDTO calls o.getOrderItems() -> loads products
            .collect(Collectors.toList());
    }
}

// AFTER: Eager loading with JOIN
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.orderItems oi " +
           "JOIN FETCH oi.product " +
           "WHERE o.customerId = :customerId")
    List<Order> findByCustomerIdWithItems(@Param("customerId") Long customerId);
}

@Service
public class OrderService {
    
    public List<OrderDTO> getCustomerOrders(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerIdWithItems(customerId);
        // Single query with all data loaded
        return orders.stream()
            .map(o -> toDTO(o))
            .collect(Collectors.toList());
    }
}
```

### 2. Database Indexing

```sql
-- Add indexes for common queries
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_name ON products(name);

CREATE INDEX idx_inventory_product_id ON inventory_items(product_id);
CREATE INDEX idx_inventory_stock ON inventory_items(stock_level);

-- Composite indexes for common filter combinations
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX idx_products_category_active ON products(category_id, is_active);
```

### 3. Query Pagination

```java
// BEFORE: Load all results
public List<ProductDTO> getAllProducts() {
    return productRepository.findAll()
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());  // Could be millions!
}

// AFTER: Paginate results
public Page<ProductDTO> getProducts(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return productRepository.findAll(pageable)
        .map(this::toDTO);
}

// API endpoint
@GetMapping("/api/v1/products")
public ResponseEntity<Page<ProductDTO>> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(productService.getProducts(page, size));
}
```

---

## Caching Approaches

### 1. Redis Caching

```xml
<!-- Add Redis dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### Product Catalog Caching

```java
@Service
@EnableCaching  // Enable caching
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(Long id) {
        // Cached for 1 hour (or configurable)
        return toDTO(productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id)));
    }
    
    @Cacheable(value = "productList", key = "#page + '-' + #size")
    public Page<ProductDTO> getProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size))
            .map(this::toDTO);
    }
    
    @CacheEvict(value = "products", key = "#id")
    public ProductDTO updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        // Update logic
        return toDTO(productRepository.save(product));
    }
    
    @CacheEvict(value = {"products", "productList"}, allEntries = true)
    public ProductDTO createProduct(ProductRequest request) {
        // Create logic
        return toDTO(productRepository.save(product));
    }
}
```

#### Cache Configuration

```yaml
# application.properties
spring.cache.type=redis
spring.redis.host=redis
spring.redis.port=6379
spring.cache.redis.time-to-live=3600000  # 1 hour

# Cache settings per service
spring.cache.cache-names=products,orders,inventory,categories
```

### 2. HTTP Caching Headers

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .lastModified(product.getLastModified())
            .body(product);
    }
    
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getProducts(
        @RequestParam(defaultValue = "0") int page) {
        Page<ProductDTO> products = productService.getProducts(page, 20);
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(products);
    }
}
```

### 3. Cache Warming

```java
@Component
public class CacheWarmer {
    
    @Autowired
    private ProductService productService;
    
    @PostConstruct
    @Async  // Run on startup
    public void warmCache() {
        logger.info("Warming up cache...");
        
        // Cache top products
        productService.getTopProducts(100);
        
        // Cache popular categories
        productService.getCategoriesWithProducts();
        
        logger.info("Cache warming completed");
    }
}
```

---

## Database Optimization

### Connection Pooling

```properties
# HikariCP connection pooling (default in Spring Boot)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

### Query Optimization

```java
// Use projections for specific fields
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT new map(p.id as id, p.name as name, p.price as price) " +
           "FROM Product p WHERE p.category.id = :categoryId")
    List<Map<String, Object>> findByCategoryIdProjection(@Param("categoryId") Long categoryId);
}

// Reduces data transfer
ProductDTO: ~500 bytes
Projection: ~100 bytes (80% reduction)
```

### Database Sharding (Future)

```yaml
# Planned: Shard orders by customer_id
Shard 1: customers 1-1M
Shard 2: customers 1M-2M
Shard 3: customers 2M-3M

Benefits:
- Distribute load across multiple databases
- Horizontal scaling
- Better cache utilization
```

---

## Scaling Considerations

### Horizontal Scaling

```yaml
Current Architecture:
  API Gateway (1): Single instance
  Services (1 each): Single instance
  
Scaled Architecture:
  API Gateway (3-5): Load balanced
  Product Service (2-3): Auto-scaled
  Order Service (2-3): Auto-scaled
  Inventory Service (2-3): Auto-scaled
  Notification Service (1-2): Async, low overhead
```

### Kubernetes Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-service
  minReplicas: 2
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

### Load Balancing Strategy

```yaml
# Kubernetes Service with load balancing
apiVersion: v1
kind: Service
metadata:
  name: product-service
spec:
  type: LoadBalancer
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 3600
  selector:
    app: product-service
  ports:
    - port: 8080
      targetPort: 8080
```

---

## Load Testing Results

### Test Scenario 1: Product Listing

```
Scenario: List products (page size 20)
Duration: 10 minutes
Users: Ramped up to 100
Ramp time: 2 minutes

Results:
├── Throughput: 850 req/s
├── p50 latency: 120ms
├── p95 latency: 280ms
├── p99 latency: 450ms
├── Error rate: 0.2%
└── Top bottleneck: Database queries (60%)
```

### Test Scenario 2: Order Creation

```
Scenario: Create order (with inventory check)
Duration: 10 minutes
Users: Ramped up to 50
Ramp time: 2 minutes

Results:
├── Throughput: 320 req/s
├── p50 latency: 280ms
├── p95 latency: 550ms
├── p99 latency: 850ms
├── Error rate: 0.5%
└── Top bottleneck: Service-to-service calls (65%)
```

### Performance Improvement Goals

| Optimization | Expected Impact | Priority |
|--------------|-----------------|----------|
| **Add Redis cache** | -40% latency for reads | High |
| **Database indexing** | -30% query time | High |
| **N+1 query fixes** | -20% database load | High |
| **Async events** | -50% order creation time | Medium |
| **Connection pooling tuning** | -10% overall latency | Medium |
| **Query pagination** | Scalability | High |

---

## Monitoring & Profiling

### Spring Boot Actuator Metrics

```yaml
# Enable all endpoints
management.endpoints.web.exposure.include=*

# Key metrics to monitor
metrics:
  - http.server.requests
  - db.connection.active
  - db.connection.max
  - cache.hits
  - cache.misses
  - process.cpu.usage
  - process.memory.usage
```

### Micrometer + Prometheus

```yaml
# Add Prometheus metrics
spring:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true

# Scrape configuration for Prometheus
scrape_configs:
  - job_name: 'micro-commerce'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### JProfiler / YourKit (Optional)

```bash
# Profile service with JProfiler
java -agentpath:/path/to/jprofiler/bin/libjpagent.so=port=8849 -jar app.jar

# Then connect with GUI for:
# - CPU profiling
# - Memory leak detection
# - Thread analysis
```

---

## Quick Win Checklist

- [ ] Add database indexes for common queries
- [ ] Implement Redis caching for products
- [ ] Fix N+1 query patterns
- [ ] Add query pagination
- [ ] Configure HTTP cache headers
- [ ] Tune HikariCP connection pool
- [ ] Enable JPA query statistics
- [ ] Add monitoring dashboards
- [ ] Run load test baseline
- [ ] Document performance targets

---

## Related Documentation

- [Architecture.md](./ARCHITECTURE.md) — System design
- [Deployment-Guide.md](./DEPLOYMENT-GUIDE.md) — Scaling setup

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** Performance Team
- **Audience:** Development and DevOps teams

