# Learning Resources

**Document Status:** Draft  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Microservices Architecture](#microservices-architecture)
2. [Spring Boot & Spring Cloud](#spring-boot--spring-cloud)
3. [Database Design](#database-design)
4. [Kubernetes & DevOps](#kubernetes--devops)
5. [Testing & Quality](#testing--quality)
6. [Security Best Practices](#security-best-practices)
7. [Project-Specific Resources](#project-specific-resources)

---

## Microservices Architecture

### Foundational Concepts

#### Books
- **Building Microservices** by Sam Newman
  - Practical guide to designing microservices
  - Domain-driven design principles
  - Service boundaries and communication patterns

- **Microservices Patterns** by Chris Richardson
  - Comprehensive pattern catalog
  - Real-world examples and tradeoffs
  - Essential for architecture decisions

#### Articles & Papers
- [Martin Fowler - Microservices](https://martinfowler.com/articles/microservices.html)
- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [The Twelve-Factor App](https://12factor.net/)

#### Key Patterns Used in Micro-Commerce

```
1. Database per Service
   - Each service owns its database
   - Enables independent scaling
   - Trade-off: Distributed transactions

2. API Gateway Pattern
   - Single entry point
   - Request routing and authentication
   - Rate limiting and logging

3. Service Discovery
   - Dynamic service registration
   - Health checks and load balancing
   - Eureka in our architecture

4. Event-Driven Communication
   - Asynchronous messaging via Kafka
   - Loose coupling between services
   - Eventual consistency model

5. Circuit Breaker Pattern
   - Prevent cascading failures
   - Graceful degradation
   - Automatic recovery
```

---

## Spring Boot & Spring Cloud

### Spring Boot Essentials

#### Official Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Guide](https://spring.io/projects/spring-data-jpa)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

#### Key Concepts

##### 1. Dependency Injection
```java
// Constructor injection (preferred)
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    
    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}

// Field injection (avoid for final fields)
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
}
```

##### 2. Application Properties
```yaml
# application.yml
spring:
  application:
    name: product-service
  datasource:
    url: jdbc:mysql://localhost:3306/micro_commerce
    username: root
    password: root123
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

##### 3. Rest Controllers
```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getProducts(
        @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(productService.getProducts(page));
    }
}
```

### Spring Cloud

#### Components

| Component | Purpose | In Micro-Commerce |
|-----------|---------|------------------|
| **Service Discovery** | Dynamic service location | Eureka |
| **API Gateway** | Request routing | Spring Cloud Gateway |
| **Config Server** | Centralized configuration | Planned |
| **Circuit Breaker** | Fault tolerance | Resilience4j (planned) |
| **Distributed Tracing** | Request tracking | Planned |
| **Message Queue** | Event streaming | Kafka |

#### Resources
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Cloud Gateway Guide](https://spring.cloud.io/spring-cloud-gateway)
- [Eureka Documentation](https://github.com/Netflix/eureka/wiki)

---

## Database Design

### SQL Best Practices

#### Entity Relationship Design

```sql
-- Good: Clear relationships with proper foreign keys
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id)
);
```

#### Indexing Strategy

```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- Composite indexes for common filter combinations
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
```

#### Query Optimization

```java
// ❌ BAD: N+1 Query Problem
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    for (OrderItem item : order.getOrderItems()) {  // ❌ Triggers query per order
        Product product = item.getProduct();
    }
}

// ✅ GOOD: Eager Loading with JOIN
@Query("SELECT DISTINCT o FROM Order o " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.product")
List<Order> findAllWithProducts();
```

#### Resources
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Database Normalization](https://en.wikipedia.org/wiki/Database_normalization)
- [Hibernate Performance Tuning](https://hibernate.org/orm/documentation/)

---

## Kubernetes & DevOps

### Kubernetes Concepts

#### Core Objects

```yaml
# Pod: Smallest deployable unit
apiVersion: v1
kind: Pod
metadata:
  name: product-service-pod
spec:
  containers:
    - name: product-service
      image: product-service:1.0.0

# Deployment: Manages Pod replicas
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: product-service

# Service: Network abstraction
apiVersion: v1
kind: Service
metadata:
  name: product-service
spec:
  selector:
    app: product-service
  ports:
    - port: 8080
```

#### Key Resources
- [Kubernetes Official Documentation](https://kubernetes.io/docs/)
- [Kubernetes Patterns](https://kubernetes.io/docs/concepts/workloads/)
- [Helm Package Manager](https://helm.sh/)

### Docker Best Practices

```dockerfile
# ✅ GOOD: Multi-stage build with small base image
FROM maven:3.9 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-slim
RUN useradd -m appuser
USER appuser
COPY --from=builder /build/target/*.jar app.jar
HEALTHCHECK CMD curl -f http://localhost:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

# ❌ BAD: Large image, root user, no health check
FROM openjdk:21
COPY . /app
WORKDIR /app
RUN javac src/main/java/App.java
CMD ["java", "-cp", "src/main/java", "App"]
```

---

## Testing & Quality

### Testing Frameworks & Practices

#### JUnit 5 (Jupiter)

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductRepository repository;
    
    @InjectMocks
    private ProductService service;
    
    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductById() {
        // Arrange
        Long productId = 1L;
        Product product = new Product(productId, "Test", 99.99);
        when(repository.findById(productId)).thenReturn(Optional.of(product));
        
        // Act
        Product result = service.getProductById(productId);
        
        // Assert
        assertThat(result).isNotNull().extracting("id").isEqualTo(productId);
        verify(repository).findById(productId);
    }
}
```

#### Integration Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testFullOrderFlow() {
        // Test with real database in container
    }
}
```

#### Test Coverage Analysis

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html

# Enforce coverage threshold
mvn jacoco:check
```

#### Resources
- [JUnit 5 Documentation](https://junit.org/junit5/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core)
- [TestContainers](https://www.testcontainers.org/)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)

---

## Security Best Practices

### Authentication & Authorization

#### OAuth2 / OpenID Connect
- [OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Specification](https://openid.net/connect/)
- [Keycloak Documentation](https://www.keycloak.org/documentation.html)

#### Spring Security
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/api/public/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer().jwt();
        return http.build();
    }
}
```

### OWASP Top 10

| # | Vulnerability | Prevention |
|---|---|---|
| 1 | Broken Access Control | RBAC, authorization checks |
| 2 | Cryptographic Failures | TLS, encryption, hashing |
| 3 | Injection | Parameterized queries, input validation |
| 4 | Insecure Design | Threat modeling, secure defaults |
| 5 | Security Misconfiguration | Security headers, principle of least privilege |
| 6 | Vulnerable Components | Dependency scanning, updates |
| 7 | Auth Failures | Strong auth, session management |
| 8 | Data Integrity Failures | Digital signatures, checksums |
| 9 | Logging Failures | Comprehensive logging, monitoring |
| 10 | SSRF | URL validation, network policies |

#### Resources
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Cheat Sheets](https://cheatsheetseries.owasp.org/)
- [Spring Security Best Practices](https://spring.io/guides/topicals/spring-security-architecture/)

---

## Project-Specific Resources

### Internal Documentation
- [Architecture.md](./ARCHITECTURE.md) — System design and C4 models
- [Implementation-Guide.md](./IMPLEMENTATION-GUIDE.md) — Development setup
- [Testing-Strategy.md](./TESTING-STRATEGY.md) — Quality assurance approach
- [API-Reference.md](./API-REFERENCE.md) — API endpoints

### External References

#### Design Patterns
- [Java Design Patterns](https://refactoring.guru/design-patterns/java)
- [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/)

#### Clean Code
- **Clean Code** by Robert C. Martin
- **The Pragmatic Programmer** by David Thomas & Andrew Hunt
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

#### Event-Driven Architecture
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Kafka Documentation](https://kafka.apache.org/documentation/)

---

## Recommended Learning Path

### Week 1-2: Microservices Fundamentals
1. Read: Martin Fowler's Microservices article
2. Study: Project architecture and design decisions
3. Practice: Deploy local services with Docker Compose

### Week 3-4: Spring Boot & Spring Cloud
1. Complete: Spring Boot official tutorial
2. Study: Eureka service discovery setup
3. Practice: Add new service endpoint

### Week 5-6: Database & SQL
1. Study: Database schema and relationships
2. Practice: Write optimized SQL queries
3. Implement: Fix N+1 query problems

### Week 7-8: Testing & Quality
1. Study: Testing pyramid and frameworks
2. Practice: Write unit and integration tests
3. Implement: Achieve >80% code coverage

### Week 9-10: Security & DevOps
1. Study: OAuth2 and Spring Security
2. Practice: Kubernetes deployment
3. Implement: Security improvements

---

## Community & Support

### Online Communities
- [Stack Overflow - Java Spring](https://stackoverflow.com/questions/tagged/spring)
- [Spring Community Forum](https://spring.io/community)
- [GitHub Discussions](https://github.com/samuel-ricardo/micro-commerce/discussions)

### Blogs & Podcasts
- [Spring Blog](https://spring.io/blog)
- [InfoQ Java](https://www.infoq.com/java/)
- [Talking Kotlin Podcast](https://talkingkotlin.com/)

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** Learning Team
- **Audience:** All team members
- **Last Updated:** 2026-07-23

