# Testing Strategy

**Document Status:** Draft (Awaiting Carla QA Review)  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Testing Philosophy](#testing-philosophy)
2. [Test Pyramid](#test-pyramid)
3. [Testing Frameworks](#testing-frameworks)
4. [Test Execution](#test-execution)
5. [Coverage Analysis](#coverage-analysis)
6. [Quality Gates](#quality-gates)
7. [Test Data Management](#test-data-management)
8. [Performance Testing](#performance-testing)

---

## Testing Philosophy

### Core Principles

- **Shift Left**: Test early and continuously throughout development
- **Automation First**: Maximize automated testing, minimize manual
- **Risk-Based**: Prioritize testing on high-risk areas
- **Quality Gates**: Enforce standards before release
- **Continuous Improvement**: Learn from failures and improve tests

### Quality Objectives

| Objective | Target | Measurement |
|-----------|--------|------------|
| **Defect Prevention** | > 95% | Defects caught before release |
| **Code Stability** | > 99% | Code without critical bugs |
| **User Impact** | < 0.1% | Bugs affecting customers |
| **Time to Market** | < 1 hour | Deployment lead time |

---

## Test Pyramid

The Micro-Commerce project follows the **Test Pyramid** pattern for optimal test distribution:

```
        /\
       /  \        E2E Tests (5%)
      /    \       - Critical user journeys
     /------\      - UI testing
    /        \     - Integration tests
   /          \
  /            \   Integration Tests (15%)
 /              \  - API contract tests
/                \ - Service integration
/------------------\- Database tests
/                    \
/                      \ Unit Tests (80%)
/        /\              \ - Individual functions
/       /  \              \ - Business logic
/------/    \------\       \ - Edge cases
/  Unit  \ Integration \ E2E \

```

### Test Distribution Target

| Test Type | Percentage | Count | Execution Time |
|-----------|-----------|-------|-----------------|
| **Unit Tests** | 80% | ~2000 tests | ~5 minutes |
| **Integration Tests** | 15% | ~375 tests | ~15 minutes |
| **E2E Tests** | 5% | ~125 tests | ~30 minutes |
| **Total** | 100% | ~2500 tests | ~50 minutes |

---

## Testing Frameworks

### Unit Testing

```xml
<!-- JUnit 5 (Jupiter) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.9.2</version>
    <scope>test</scope>
</dependency>

<!-- Mockito for mocking -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.3.1</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ for fluent assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.1</version>
    <scope>test</scope>
</dependency>
```

#### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private ProductService productService;
    
    @Test
    void testGetProductById_Success() {
        // Arrange
        Long productId = 1L;
        Product product = new Product(productId, "Test Product", 99.99);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        
        // Act
        Product result = productService.getProductById(productId);
        
        // Assert
        assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", productId)
            .hasFieldOrPropertyWithValue("name", "Test Product");
        
        verify(productRepository, times(1)).findById(productId);
    }
    
    @Test
    void testGetProductById_NotFound() {
        // Arrange
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(productId))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("Product not found: 999");
    }
}
```

### Integration Testing

```xml
<!-- Spring Boot Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- TestContainers for database isolation -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.18.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.18.0</version>
    <scope>test</scope>
</dependency>
```

#### Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductServiceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void testCreateProduct_Success() {
        // Arrange
        ProductDTO request = new ProductDTO("New Product", "Description", 49.99);
        
        // Act
        ResponseEntity<ProductDTO> response = restTemplate.postForEntity(
            "/api/v1/products",
            request,
            ProductDTO.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(productRepository.count()).isEqualTo(1);
    }
}
```

### End-to-End Testing

```xml
<!-- Selenium for UI testing -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.10.0</version>
    <scope>test</scope>
</dependency>

<!-- Rest Assured for API E2E -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.3.2</version>
    <scope>test</scope>
</dependency>
```

#### E2E Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderE2ETest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void testCompleteOrderFlow() {
        String baseUrl = "http://localhost:" + port;
        
        // Step 1: Authenticate
        String token = given()
            .contentType(ContentType.JSON)
            .body(new LoginRequest("user@example.com", "password"))
            .when()
            .post(baseUrl + "/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("token");
        
        // Step 2: Get products
        Product[] products = given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get(baseUrl + "/api/v1/products")
            .then()
            .statusCode(200)
            .extract()
            .as(Product[].class);
        
        // Step 3: Create order
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setProductId(products[0].getId());
        orderRequest.setQuantity(1);
        
        String orderId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(orderRequest)
            .when()
            .post(baseUrl + "/api/v1/orders")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");
        
        // Step 4: Verify order
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get(baseUrl + "/api/v1/orders/" + orderId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PENDING"));
    }
}
```

---

## Test Execution

### Run All Tests

```bash
# Run all tests
mvn clean test

# Run with coverage report
mvn clean test jacoco:report

# Run integration tests
mvn clean verify

# Run specific test class
mvn test -Dtest=ProductServiceTest

# Run specific test method
mvn test -Dtest=ProductServiceTest#testGetProductById_Success
```

### Continuous Testing

```bash
# Watch mode - runs tests on file changes (using Maven)
mvn test-compile exec:java@watch-tests

# Or use IDE features for continuous testing in background
```

### Test Reports

```bash
# Generate test report
mvn clean test site

# View in browser
open target/site/surefire-report.html

# Coverage report
open target/site/jacoco/index.html
```

---

## Coverage Analysis

### Coverage Targets

| Component | Target | Status |
|-----------|--------|--------|
| **Service Layer** | 90%+ | ✅ Critical |
| **Controller Layer** | 80%+ | ✅ Important |
| **Repository Layer** | 70%+ | ✅ Moderate |
| **Utility Layer** | 85%+ | ✅ Important |
| **Overall** | > 80% | ✅ Required |

### Code Coverage Tools

```xml
<!-- JaCoCo Maven Plugin -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <excludes>
                            <exclude>*Test</exclude>
                        </excludes>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Coverage Reports

```bash
# Generate coverage report
mvn clean test jacoco:report

# Check coverage percentage
mvn clean test jacoco:check

# Export coverage data
mvn clean test jacoco:dump
```

---

## Quality Gates

### Pre-Commit Checks

Before committing code:

```bash
#!/bin/bash
# pre-commit.sh

echo "Running code quality checks..."

# 1. Build project
mvn clean compile
if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

# 2. Run tests
mvn test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed"
    exit 1
fi

# 3. Check coverage
mvn jacoco:check
if [ $? -ne 0 ]; then
    echo "❌ Coverage threshold not met"
    exit 1
fi

# 4. Run static analysis
mvn sonar:sonar
if [ $? -ne 0 ]; then
    echo "❌ SonarQube analysis failed"
    exit 1
fi

echo "✅ All checks passed!"
```

### GitHub Actions CI/CD

```yaml
name: Quality Gate

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: 21
      
      - name: Run tests
        run: mvn clean test
      
      - name: Generate coverage report
        run: mvn jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
      
      - name: SonarQube analysis
        run: mvn sonar:sonar -Dsonar.projectKey=micro-commerce
```

---

## Test Data Management

### Test Data Strategy

```java
// BaseTest class with common test data
public class BaseTest {
    
    protected Product createTestProduct() {
        return Product.builder()
            .name("Test Product")
            .description("Test Description")
            .price(99.99)
            .stock(100)
            .build();
    }
    
    protected Order createTestOrder() {
        return Order.builder()
            .customerId(1L)
            .status(OrderStatus.PENDING)
            .total(99.99)
            .build();
    }
    
    protected User createTestUser() {
        return User.builder()
            .email("test@example.com")
            .password(encodePassword("password123"))
            .role(UserRole.CUSTOMER)
            .build();
    }
}
```

### Database Cleanup

```java
@SpringBootTest
@Transactional  // Rollback after each test
class DatabaseTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @AfterEach
    void cleanup() {
        entityManager.clear();
    }
}
```

---

## Performance Testing

### Load Testing with JMeter

```bash
# Run JMeter test
jmeter -n -t test-plan.jmx -l results.jtl -j jmeter.log

# Generate HTML report
jmeter -g results.jtl -o report/
```

### Performance Benchmarks

| Scenario | Target | Threshold |
|----------|--------|-----------|
| **Product List** | < 200ms p95 | < 300ms p99 |
| **Product Detail** | < 150ms p95 | < 250ms p99 |
| **Create Order** | < 300ms p95 | < 500ms p99 |
| **Search Products** | < 500ms p95 | < 800ms p99 |

---

## Related Documentation

- [Architecture.md](./ARCHITECTURE.md) — Technical design
- [Implementation-Guide.md](./IMPLEMENTATION-GUIDE.md) — Development setup
- [Quality-Gates.md](./QUALITY-GATES.md) — Release criteria

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** Carla QA (pending)
- **Audience:** Development and QA teams

