# 🔍 Análise Crítica de Qualidade - Micro-Commerce

**Avaliação Adversarial Realizada por: Carla QA**  
**Data: 2026-07-23**  
**Nível de Severidade: CRÍTICO**

---

## Executive Summary

O projeto Micro-Commerce apresenta uma **postura de qualidade significativamente abaixo dos padrões esperados** para uma arquitetura de microsserviços em produção. Apesar de haver infraestrutura de testes básica (Testcontainers, REST Assured, WireMock), a cobertura é **mínima (~5% estimado)**, não há automação de métricas de qualidade, e faltam testes críticos para garantir confiabilidade.

### Situação Crítica
- ⚠️ **9 testes totais** para uma arquitetura com 5 microsserviços
- ⚠️ **Sem medição de cobertura** de código
- ⚠️ **Testes desativados** (Kafka, serviços intermediários)
- ⚠️ **Sem testes de segurança** (OAuth2, CORS, validação de entrada)
- ⚠️ **Sem testes de performance**
- ⚠️ **Pipeline CI/CD incompleto** (sem gates de qualidade)

---

## 1. Test Strategy Assessment

### 1.1 Unit Tests
**Status: INADEQUADO** ❌

#### Achados
- Nenhum teste unitário identificado
- Apenas testes de integração (E2E-like) com SpringBootTest
- Lógica de negócio não isolada do Spring Framework
  - `OrderServiceImpl.placeOrder()`: lógica crítica sem testes unitários
  - `HaveStockUseCase`, `CreateOrderUseCase`: sem testes de unidade
  - `OrderController`: sem testes de validação de endpoint
  
#### Problemas Específicos
```java
// OrderServiceImpl.placeOrder() - 8 linhas, SEM testes unitários
public Order placeOrder(@NotNull PlaceOrderDTO order) {
    var isAvailable = haveStock.execute(...);
    if (!isAvailable) throw new InsufficientStockException(order.getSkuCode());
    var orderPlaced = saveOrder.execute(createOrder.execute(order));
    produceOrderPlacedEvent.execute(...);
    return orderPlaced;
}
```

**Cenários de Teste Faltantes:**
- ❌ Null checks (PlaceOrderDTO, response handling)
- ❌ Exception propagation (InsufficientStockException)
- ❌ Event production failure scenarios
- ❌ Transaction rollback behavior

#### Recomendação
```
Implementar testes unitários com Mockito:
- Mock HaveStockUseCase, CreateOrderUseCase, SaveOrderUseCase, ProduceOrderPlacedEventUseCase
- Testar 3 cenários: happy path, sem estoque, falha na produção de evento
- Target: 80% cobertura de unit tests
```

### 1.2 Integration Tests
**Status: INCOMPLETO** ⚠️

#### Achados Positivos ✅
- Testcontainers bem configurado para PostgreSQL, MySQL, MongoDB
- REST Assured para testes de API
- WireMock para stubbing de dependências externas

#### Problemas Críticos ❌
1. **Kafka Integration Desativada**
   ```java
   // NotificationServiceApplicationTests.java
   @TestConfiguration(proxyBeanMethods = false)
   class TestcontainersConfiguration {
       //@Bean @ServiceConnection
       //KafkaContainer kafkaContainer() { ... }  // ← DESATIVADA
   }
   ```
   - Notification Service depende de Kafka
   - Nenhum teste de consumo de eventos
   - Risco: falhas silenciosas em produção

2. **Código Comentado Indicando Testes Incompletos**
   ```java
   // OrderServiceApplicationTests - 20+ linhas comentadas
   // inventoryService = new GenericContainer<>("samuelrcabral/inventory-service:test")
   // .withExposedPorts(8082)
   // .dependsOn(mysql)
   // Serviço intermediário não está sendo testado
   ```

3. **Testes de Banco de Dados Incompletos**
   - Sem testes de transação rollback
   - Sem testes de deadlock
   - Sem testes de data consistency
   - Sem testes de Flyway migrations (Order Service)

#### Recomendação
```
1. Reativar Kafka Container nos testes de Notification Service
2. Expandir testes de integração:
   - Cenário: Order placed → Event published → Email sent (end-to-end)
   - Cenário: Inventory service down → Circuit breaker behavior
   - Cenário: Database connection timeout
3. Testar database migrations com testcontainers
```

### 1.3 E2E Tests
**Status: NÃO EXISTEM** ❌

#### Problemas
- Nenhum teste que valida o fluxo completo:
  1. Criar produto
  2. Verificar estoque
  3. Fazer pedido
  4. Receber notificação

- Nenhum teste do API Gateway (apenas contextLoads)

#### Recomendação
```
Criar teste E2E que simule usuário real:
@Test
void shouldCompleteFullOrderFlow() {
    // 1. Create product
    Product product = createProduct("iPhone 15");
    
    // 2. Add inventory
    addInventory(product.getSkuCode(), 10);
    
    // 3. Place order
    Order order = placeOrder(product.getSkuCode(), 5);
    
    // 4. Verify order created
    assertThat(order.getStatus()).isEqualTo("CONFIRMED");
    
    // 5. Verify notification sent (mock mail service)
    // 6. Verify event published to Kafka
}
```

### 1.4 Contract Tests
**Status: DEPENDÊNCIA SEM USO** ❌

#### Achados
```xml
<!-- pom.xml detectado -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
    <version>4.2.0</version>
</dependency>
```
- Dependência de Contract Testing instalada **mas não utilizada**
- Interfaces entre serviços não validadas por contratos
- Risco: mudanças incompatíveis em APIs

#### Problemas
1. Order Service → Inventory Service
   - Contrato não definido
   - Mudanças futuras podem quebrar integração
   
2. Order Service → Notification Service (via Kafka)
   - `OrderPlacedEvent` não tem contrato validado
   - Consumidor pode estar fora de sincronismo

#### Recomendação
```
Implementar Spring Cloud Contract:
1. Definir contratos em order-service/src/test/resources/contracts/
2. Gerar stubs automaticamente
3. Validar compatibilidade com inventory-service
4. Adicionar verificação de contrato no CI/CD
```

### 1.5 Performance Tests
**Status: NÃO EXISTEM** ❌

#### Problemas Críticos
- Sem baseline de performance
- Sem detecção de memory leaks
- Sem teste de timeout handling

#### Cenários Críticos Faltantes
```
1. Load test:
   - 100 concurrent orders
   - Verificar degradação de performance
   - Medir latência P95, P99

2. Stress test:
   - 1000 concurrent requests
   - Identificar breaking point
   
3. Soak test:
   - Rodar por 24h
   - Verificar memory leaks (heap dump)
```

#### Recomendação
```
Usar JMeter/Gatling:
- Target: <200ms P99 latency para POST /orders
- Target: <100ms P99 latency para GET /inventory
- Load test como parte do CI/CD (on merge)
```

### 1.6 Security Tests
**Status: NÃO EXISTEM** ❌

#### Problemas Críticos (OWASP Top 10)

| Risco | Status | Teste |
|-------|--------|-------|
| A01: Injection | ❌ Untested | SQL injection em query params |
| A02: Authentication | ❌ Untested | Bypass de JWT validation |
| A03: Broken Access Control | ❌ Untested | Acesso não autorizado a recursos |
| A04: XML External Entity | ⚠️ N/A (REST) | Não aplica a APIs REST |
| A05: Broken Access Control | ❌ Untested | Rate limiting bypass |
| A06: Vulnerable Components | ⚠️ Não monitorado | Spring Boot 3.4.1 vulnerabilities |
| A07: Identification Failure | ❌ Untested | JWT expiration handling |
| A08: Data Integrity Failures | ❌ Untested | Kafka message tampering |
| A09: Logging Failures | ⚠️ Logs estruturados | Mas não testados |
| A10: SSRF | ⚠️ Possível em inventoryServiceUrl | Sem validação |

#### Exemplos de Testes Faltantes

```java
// ❌ Faltando: Teste de SQL Injection em InventoryService
@Test
void shouldNotAllowSqlInjectionInSkuCode() {
    String maliciousSkuCode = "iphone'; DROP TABLE inventory; --";
    
    RestAssured
        .when()
        .get("/api/v1/inventory?skuCode=" + maliciousSkuCode + "&quantity=1")
        .then()
        .statusCode(400) // ou sanitizado
        .body("error", containsString("Invalid input"));
}

// ❌ Faltando: Teste de JWT expiration
@Test
void shouldRejectExpiredJwtToken() {
    String expiredToken = generateExpiredJwt();
    
    RestAssured
        .given()
        .header("Authorization", "Bearer " + expiredToken)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(401);
}

// ❌ Faltando: Teste de CORS bypass
@Test
void shouldRejectUnauthorizedOrigin() {
    RestAssured
        .given()
        .header("Origin", "https://malicious.com")
        .when()
        .options("/api/v1/orders")
        .then()
        .statusCode(403);
}
```

#### API Gateway Security Concerns
```java
// SecurityConfig.java - Problema detectado
config.setAllowedOrigins(List.of(ALLOWED_ORIGINS)); // ← Configurável
config.setAllowedHeaders(List.of("*")); // ← MUITO PERMISSIVO
config.setAllowCredentials(false); // ✅ Bom, nega wildcard + credentials
```

#### Recomendação
```
1. Implementar OWASP dependency check no Maven:
   <plugin>
       <groupId>org.owasp</groupId>
       <artifactId>dependency-check-maven</artifactId>
   </plugin>

2. Adicionar testes de segurança (OwaspZap, Burp):
   - Input validation
   - Authentication/Authorization
   - Rate limiting
   - CORS validation

3. Executar testes de segurança no CI/CD:
   - Falhar build se CRITICAL vulnerabilities encontradas
```

---

## 2. Test Infrastructure Analysis

### 2.1 Testcontainers Usage
**Status: BEM IMPLEMENTADO MAS INCOMPLETO** ⚠️

#### O que funciona ✅
```java
@ServiceConnection
static PostgreSQLContainer postgre = new PostgreSQLContainer("postgres");

@ServiceConnection
static MongoDBContainer mongo = new MongoDBContainer("mongo:latest");

@ServiceConnection
static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:latest"));
```
- Containers iniciados corretamente
- @ServiceConnection simplifica configuração
- Múltiplos bancos suportados

#### O que falta ❌
1. **Kafka Container desativado**
   ```java
   // NotificationServiceApplicationTests
   //@Bean
   //@ServiceConnection
   //KafkaContainer kafkaContainer() { ... } // ← COMENTADO
   ```

2. **Sem cleanup entre testes**
   - Possível dados persistindo entre execuções
   - Risco: testes flaky

3. **Sem reutilização de containers**
   - Cada teste inicia novos containers
   - Lento (30-60s por serviço)

#### Recomendação
```java
// Usar static containers reutilizáveis
public abstract class AbstractIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}

// Ganho: testes 60% mais rápidos
```

### 2.2 Mock/Stub Strategies
**Status: BÁSICO** ⚠️

#### Implementação Atual
```java
// WireMock para stubbing de Inventory Service
public class InventoryClientStub {
    public static void stubInventoryCall(String skuCode, Integer quantity) {
        if (quantity <= 100) {
            stubFor(get(urlEqualTo("/api/v1/inventory?skuCode=" + skuCode + "&quantity=" + quantity))
                .willReturn(aResponse().withStatus(200).withBody("true")));
        } else {
            stubFor(...).withBody("false"));
        }
    }
}
```

#### Problemas
1. **Lógica hardcoded no stub**
   - Stub simula comportamento (quantity <= 100 = true)
   - Desincronizado com implementação real
   
2. **Sem testes de erro**
   - Sem stub para status 500
   - Sem stub para timeout
   - Sem stub para malformed response

#### Recomendação
```java
// Usar Mockito para maior controle
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private HaveStockUseCase haveStockUseCase;
    
    @Mock
    private SaveOrderUseCase saveOrderUseCase;
    
    @InjectMocks
    private OrderServiceImpl orderService;
    
    @Test
    void shouldThrowExceptionWhenStockCheckFails() {
        when(haveStockUseCase.execute(any()))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        assertThrows(RuntimeException.class, 
            () -> orderService.placeOrder(validOrder));
    }
}
```

### 2.3 Test Data Management
**Status: NÃO EXISTE** ❌

#### Problemas
- Sem fixtures (dados pré-configurados)
- Sem factories (geração de objetos de teste)
- Sem builders para DTOs complexos

#### Recomendação
```java
// Criar TestDataBuilder
public class PlaceOrderDTOBuilder {
    private String skuCode = "iphone_15";
    private Integer quantity = 10;
    private String email = "test@example.com";
    
    public PlaceOrderDTO build() {
        return new PlaceOrderDTO(
            "ORD-" + UUID.randomUUID(),
            skuCode,
            100.0,
            quantity,
            new UserDetailsDTO(email, "Test", "User")
        );
    }
    
    public PlaceOrderDTOBuilder withSkuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }
}

// Uso
@Test
void shouldCreateProduct() {
    PlaceOrderDTO order = new PlaceOrderDTOBuilder()
        .withSkuCode("invalid_sku")
        .build();
    // ...
}
```

### 2.4 Test Isolation
**Status: PARCIAL** ⚠️

#### Problemas Detectados
1. **Sem rollback entre testes**
   - Dados criados em um teste podem afetar outro
   - Ordem de execução pode impactar resultados

2. **Shared containers entre testes**
   - Sem limpeza de dados
   - Teste T2 pode ver dados de T1

#### Recomendação
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @BeforeEach
    void setUp() {
        // Limpar dados antes de cada teste
        orderRepository.deleteAll();
    }
    
    @Transactional
    @Test
    void shouldPlaceOrder() {
        // Teste com transação garantida de rollback
    }
}
```

### 2.5 CI/CD Integration
**Status: MINIMAL** ⚠️

#### Pipeline Atual (ci_order.yml)
```yaml
- name: Run Tests
  run: ./mvnw test
  
- name: Build Project
  run: ./mvnw clean package -DskipTests
```

#### Problemas
1. ❌ Nenhuma medição de cobertura
2. ❌ Nenhum relatório de testes
3. ❌ Nenhum quality gate (falhar se cobertura < 80%)
4. ❌ Spotless/formatting apenas check, não apply
5. ❌ Nenhuma análise de segurança (OWASP, SonarQube)

#### Recomendação
```yaml
- name: Run Tests with Coverage
  run: ./mvnw test jacoco:report

- name: Upload Coverage to Codecov
  uses: codecov/codecov-action@v3

- name: SonarQube Analysis
  run: ./mvnw sonar:sonar -Dsonar.host.url=${{ secrets.SONARQUBE_HOST }}

- name: Check Security Vulnerabilities
  run: ./mvnw org.owasp:dependency-check-maven:check
```

---

## 3. Quality Metrics Assessment

### 3.1 Code Coverage
**Status: DESCONHECIDO (não medido)** ❌

#### Análise Manual
Com apenas 9 testes para ~50 classes Java:

```
Estimativa de Cobertura:
- OrderService logic: ~30% (2 testes para 5 use cases)
- ProductService logic: ~20% (2 testes para 5 use cases)
- InventoryService logic: ~30% (2 testes para 3 use cases)
- API Gateway: ~5% (contextLoads apenas)
- Notification Service: ~5% (contextLoads apenas)
- ERROR HANDLING: ~0% (sem testes de erro)
- SECURITY: ~0% (sem testes de segurança)

COBERTURA ESTIMADA: 5-10%
TARGET: >80%
```

#### Recomendação
```xml
<!-- pom.xml -->
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

### 3.2 Test Execution Time
**Status: ACEITÁVEL** ✅

#### Métricas (Estimado)
```
Total test time: ~45-60 segundos
- Container startup: ~30s (PostgreSQL, MySQL, MongoDB, WireMock)
- Test execution: ~15-30s
- Maven overhead: ~5s

Breakdown por serviço:
- Order Service: ~15s (2 testes + 1 container)
- Product Service: ~15s (2 testes + 1 container)
- Inventory Service: ~15s (2 testes + 1 container)
- Notification Service: ~5s (contextLoads)
- API Gateway: <1s (contextLoads)
```

#### Recomendação
```
Para otimização futura:
1. Reutilizar containers (Testcontainers singleton pattern)
2. Paralelizar execução de testes
3. Target: < 30 segundos total
```

### 3.3 Flaky Test Detection
**Status: NENHUMA DETECÇÃO** ❌

#### Problemas
- Sem re-execução automática de testes flacos
- Sem logging de intermittent failures
- Sem histórico de execução

#### Recomendação
```
1. Implementar retry policy:
   @ExtendWith(RetryingTestExtension.class)
   @RepeatedTest(3)
   void shouldPlaceOrder() { }

2. Monitorar testes flaky:
   - CI/CD gerar relatório de testes que falharam 1-2 vezes
   - Investigar cause em tests flacos (timeout, race conditions)
```

### 3.4 Code Maintainability
**Status: BOM COM EXCEÇÕES** ⚠️

#### Positivos ✅
- Uso de Lombok reduz boilerplate
- DDD patterns aplicados (UseCase, Gateway, DTOs)
- Separação de concerns (Controller → Service → UseCase)

#### Problemas ❌
1. **Cyclomatic Complexity não medido**
   ```java
   // OrderServiceImpl.placeOrder() - complexidade moderada
   // Mas sem testes unitários, difícil validar todos os paths
   ```

2. **Sem SonarQube analysis**
   - Sem detecção de code smells
   - Sem detecção de duplicação

#### Recomendação
```xml
<!-- SonarQube Maven Plugin -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.9.1.2184</version>
</plugin>

<!-- CI/CD -->
./mvnw sonar:sonar -Dsonar.projectKey=micro-commerce
```

### 3.5 Bug Density
**Status: DESCONHECIDO (não medido)** ❌

#### Análise Potencial
```
Com cobertura ~5%, estimativa de bugs não detectados:
- OrderService: ~3-5 bugs potenciais (null handling, error cases)
- ProductService: ~2-3 bugs
- InventoryService: ~2 bugs
- NotificationService: ~3-5 bugs (Kafka untested)
- API Gateway: ~2-3 bugs (security untested)

Total Estimado: 12-19 bugs nãodetectados
Risco: ALTO
```

#### Recomendação
```
1. Implementar SonarQube bug detection
2. Usar Spotbugs para análise estática
3. Implementar testes sugeridos acima
```

---

## 4. API Testing Analysis

### 4.1 REST Assured Usage
**Status: BEM IMPLEMENTADO MAS LIMITADO** ⚠️

#### Implementação Atual
```java
RestAssured.given()
    .contentType("application/json")
    .body(requestBody)
    .when()
    .post("/api/v1/orders")
    .then()
    .statusCode(201)
    .body(containsString("has been placed with success"));
```

#### Problemas
1. ❌ Sem validação de schema JSON
2. ❌ Sem teste de headers de resposta
3. ❌ Sem teste de performance (response time)
4. ❌ Sem teste de error responses (422, 500, etc)

#### Recomendação
```java
@Test
void shouldCreateOrderWithValidation() {
    String requestBody = """
        {
            "orderNumber": "ORD-12345",
            "skuCode": "iphone_15",
            "price": 19.99,
            "quantity": 100,
            "userDetails": {
                "email": "test@example.com",
                "firstName": "Test",
                "lastName": "User"
            }
        }
    """;
    
    RestAssured.given()
        .contentType("application/json")
        .body(requestBody)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(201)
        .time(lessThan(500L)) // Validar performance
        .body("orderNumber", notNullValue())
        .body("status", equalTo("CONFIRMED"))
        .header("Content-Type", containsString("application/json"));
}

@Test
void shouldReturnValidationErrorForInvalidEmail() {
    String invalidRequest = """
        {
            "userDetails": {
                "email": "invalid-email",
                "firstName": "Test",
                "lastName": "User"
            }
        }
    """;
    
    RestAssured.given()
        .contentType("application/json")
        .body(invalidRequest)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(400)
        .body("errors.size()", greaterThan(0));
}
```

### 4.2 OpenAPI/Swagger Validation
**Status: DOCUMENTADO MAS NÃO TESTADO** ⚠️

#### Achados
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.3</version>
</dependency>
```

- Swagger/OpenAPI disponível em `/swagger-ui.html`
- Documentação gerada automaticamente
- ✅ API Gateway expõe swagger docs de todos os serviços

#### Problemas
1. ❌ Sem validação de resposta contra schema OpenAPI
2. ❌ Sem detecção de mudanças incompatíveis em API

#### Recomendação
```java
// Validar resposta contra schema
@Test
void shouldFollowOpenApiSchema() {
    RestAssured.given()
        .contentType("application/json")
        .body(validOrder)
        .when()
        .post("/api/v1/orders")
        .then()
        .assertThat()
        .body(matchesJsonSchemaFromResource("schemas/create-order-response.json"));
}
```

### 4.3 Error Response Handling
**Status: PARCIALMENTE TESTADO** ⚠️

#### O que é testado ✅
```java
@Test
void shouldNotPlaceOrderIfInventoryIsNotAvailable() {
    // ...
    .statusCode(422)
    .body("statusCode", equalTo("UNPROCESSABLE_ENTITY"))
}
```

#### O que falta ❌
```java
// ❌ Teste 400 Bad Request
@Test
void shouldReturn400ForMissingEmail() {
    RestAssured.given()
        .body("{\"firstName\": \"Test\"}") // Sem email
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(400)
        .body("message", containsString("email is required"));
}

// ❌ Teste 401 Unauthorized (autenticação)
@Test
void shouldReturn401WithoutAuthToken() {
    RestAssured.given()
        // Sem Authorization header
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(401);
}

// ❌ Teste 500 Server Error
@Test
void shouldHandleServiceUnavailable() {
    // Simular inventory-service down
    InventoryClientStub.stubServerError(500);
    
    RestAssured.given()
        .body(validOrder)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(503); // Ou retry com circuit breaker
}
```

### 4.4 Rate Limiting & Throttling
**Status: NÃO IMPLEMENTADO** ❌

#### Problemas
- Sem rate limiting visível no código
- Sem teste de throttling
- Risco: DDoS attacks

#### Recomendação
```xml
<!-- Adicionar rate limiting -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

```java
// Implementar rate limiter
@RateLimiter(name = "orderService", fallbackMethod = "rateLimitFallback")
@PostMapping("/orders")
public String placeOrder(@RequestBody @Valid PlaceOrderDTO order) {
    return service.placeOrder(order).getOrderNumber();
}

public String rateLimitFallback(PlaceOrderDTO order, Exception e) {
    throw new RateLimitExceededException("Too many requests");
}

// Teste
@Test
void shouldEnforceRateLimiting() {
    for (int i = 0; i < 101; i++) {
        RestAssured.given()
            .body(validOrder)
            .when()
            .post("/api/v1/orders")
            .then()
            .statusCode(i < 100 ? 201 : 429); // Too Many Requests
    }
}
```

---

## 5. Data Validation Testing

### 5.1 Input Validation (Bean Validation)
**Status: NÃO TESTADO** ❌

#### Validações Implementadas ✅
```java
// UserDetailsDTO
data class UserDetailsDTO(
    @NotBlank
    @Email
    val email: String,
    @NotBlank
    val firstName: String,
    @NotBlank
    val lastName: String
)

// IsInStockDTO
data class IsInStockDTO(
    @NotNull
    val skuCode: SkuCode,
    @NotNull
    val quantity: Int
)
```

#### Testes Faltantes ❌
```java
@Test
void shouldRejectInvalidEmail() {
    String request = """
        {
            "email": "invalid-email",
            "firstName": "Test",
            "lastName": "User"
        }
    """;
    
    RestAssured.given()
        .body(request)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(400);
}

@Test
void shouldRejectEmptyFields() {
    String request = """
        {
            "email": "",
            "firstName": "",
            "lastName": ""
        }
    """;
    
    RestAssured.given()
        .body(request)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(400);
}

@Test
void shouldRejectNullFields() {
    String request = """
        {
            "email": null,
            "firstName": "Test",
            "lastName": "User"
        }
    """;
    
    RestAssured.given()
        .body(request)
        .when()
        .post("/api/v1/orders")
        .then()
        .statusCode(400);
}

@Test
void shouldValidateQuantityBoundaries() {
    RestAssured
        .when()
        .get("/api/v1/inventory?skuCode=iphone_15&quantity=0") // Inválido
        .then()
        .statusCode(400);
    
    RestAssured
        .when()
        .get("/api/v1/inventory?skuCode=iphone_15&quantity=-1") // Inválido
        .then()
        .statusCode(400);
    
    RestAssured
        .when()
        .get("/api/v1/inventory?skuCode=iphone_15&quantity=999999") // Possível overflow
        .then()
        .statusCode(200); // Ou 400 dependendo da política
}
```

### 5.2 Data Integrity Testing
**Status: MÍNIMO** ❌

#### Problemas
- Sem testes de foreign key constraints
- Sem testes de unique constraints
- Sem testes de data type validation

#### Recomendação
```java
@Test
void shouldEnforceUniqueOrderNumber() {
    // Criar primeira ordem
    Order order1 = createOrder("ORD-123");
    
    // Tentar criar segunda com mesmo orderNumber
    assertThrows(ConstraintViolationException.class, 
        () -> createOrder("ORD-123"));
}

@Test
void shouldMaintainInventoryIntegrity() {
    // Criar inventário com quantidade 10
    Inventory inv = createInventory("iphone_15", 10);
    
    // Reduzir para -5 (inválido)
    assertThrows(ValidationException.class,
        () -> inv.setQuantity(-5));
}

@Test
void shouldCascadeDeleteOnProductDeletion() {
    Product product = createProduct("iPhone");
    Inventory inventory = createInventory(product.getId(), 10);
    
    // Deletar produto
    productRepository.delete(product);
    
    // Verificar que inventário também foi deletado
    assertThat(inventoryRepository.findByProductId(product.getId()))
        .isEmpty();
}
```

### 5.3 Transaction Rollback Scenarios
**Status: NÃO TESTADO** ❌

#### Problemas
- Sem validação de transação ACID
- Sem testes de rollback em caso de erro
- Risco: dados inconsistentes

#### Recomendação
```java
@Test
void shouldRollbackOrderIfEventPublicationFails() {
    // Mock para falhar na publicação de evento
    when(produceOrderPlacedEvent.execute(any()))
        .thenThrow(new KafkaException("Broker unavailable"));
    
    // Tentar criar ordem
    assertThrows(KafkaException.class, 
        () -> orderService.placeOrder(validOrder));
    
    // Verificar que ordem NÃO foi salva (rollback)
    assertThat(orderRepository.count()).isZero();
}

@Test
void shouldRollbackIfInventoryDecrementFails() {
    // Mock inventory failure
    when(saveOrderUseCase.execute(any()))
        .thenThrow(new DatabaseException("Connection failed"));
    
    // Operação deveria falhar e rollback
    assertThrows(DatabaseException.class,
        () -> orderService.placeOrder(validOrder));
}
```

### 5.4 Eventual Consistency Testing
**Status: NÃO TESTADO** ❌

#### Problemas (Arquitetura Assíncrona)
```
Order Service → Publica OrderPlacedEvent → Kafka →Notification Service
Notification Service → Envia Email

Cenários não testados:
1. Event publicado mas consumer não processou ainda
2. Event processado mas email não enviado
3. Retry após falha
```

#### Recomendação
```java
@Test
@Transactional
void shouldEventuallyPublishNotification() throws InterruptedException {
    // 1. Criar ordem
    Order order = orderService.placeOrder(validOrder);
    
    // 2. Verificar que evento foi enfileirado
    assertThat(kafkaTemplate.send(ORDER_PLACED_TOPIC, ...).get())
        .isNotNull();
    
    // 3. Aguardar consumer processar (com timeout)
    awaitility().atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(() -> {
            Optional<Notification> notification = 
                notificationRepository.findByOrderNumber(order.getOrderNumber());
            assertThat(notification).isPresent();
            assertThat(notification.get().getStatus())
                .isEqualTo(NotificationStatus.SENT);
        });
}

@Test
void shouldRetryOnFailedNotification() {
    // Simular falha de email na primeira tentativa
    when(emailService.send(any()))
        .thenThrow(new EmailException())
        .thenReturn(true); // Segunda tentativa sucede
    
    // Publicar evento
    kafkaOrderConsumer.listenOrderPlacedEvent(orderPlacedEvent);
    
    // Verificar que houve retry
    verify(emailService, times(2)).send(any());
}
```

---

## 6. Observability Testing

### 6.1 Logging Assertion Tests
**Status: NÃO TESTADO** ❌

#### Infraestrutura ✅
```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>1.3.2</version>
</dependency>
```
- Loki4j configurado para enviar logs
- Estrutura de logging via Slf4j

#### Testes Faltantes ❌
```java
@Test
void shouldLogOrderCreation() {
    // Capturar logs
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    ((LoggerContext) LoggerFactory.getILoggerFactory())
        .getLogger(Logger.ROOT_LOGGER_NAME)
        .addAppender(appender);
    
    // Criar ordem
    orderService.placeOrder(validOrder);
    
    // Verificar logs
    List<ILoggingEvent> logs = appender.list;
    assertThat(logs)
        .extracting(ILoggingEvent::getMessage)
        .anyMatch(msg -> msg.contains("Order placed"));
}

@Test
void shouldLogErrorWithStackTrace() {
    // Forçar erro
    when(inventoryGateway.checkStock(any()))
        .thenThrow(new InventoryServiceException("Connection failed"));
    
    // Verificar que erro foi logado
    assertThat(logs)
        .anyMatch(log -> log.getLevel() == Level.ERROR &&
                        log.getThrowableProxy() != null);
}
```

### 6.2 Metrics Validation
**Status: BEM IMPLEMENTADO MAS NÃO TESTADO** ✅⚠️

#### Implementação ✅
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```
- Prometheus métricas configuradas
- Micrometer para observability

#### Testes Faltantes ❌
```java
@Test
void shouldIncrementOrderCounterOnPlacement() {
    // Obter counter antes
    MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry.class);
    double countBefore = meterRegistry.find("order.placed")
        .counter()
        .count();
    
    // Criar ordem
    orderService.placeOrder(validOrder);
    
    // Verificar que counter incrementou
    double countAfter = meterRegistry.find("order.placed")
        .counter()
        .count();
    assertThat(countAfter).isGreaterThan(countBefore);
}

@Test
void shouldTrackOrderProcessingTime() {
    MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry.class);
    
    orderService.placeOrder(validOrder);
    
    // Verificar timer
    Timer timer = meterRegistry.find("order.processing.time")
        .timer();
    assertThat(timer.mean(TimeUnit.MILLISECONDS))
        .isLessThan(500); // P99 < 500ms
}
```

### 6.3 Distributed Tracing Validation
**Status: CONFIGURADO MAS NÃO TESTADO** ✅⚠️

#### Implementação ✅
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```
- Brave tracing configurado
- Zipkin reporter para observability

#### Testes Faltantes ❌
```java
@Test
void shouldPropagateTraceIdAcrossServices() {
    // Extrair trace ID do header
    String traceId = "4e68f7f8-8a87-4e42-9c2e-10e67e8d7c42";
    
    RestAssured.given()
        .header("X-Trace-Id", traceId)
        .body(validOrder)
        .when()
        .post("/api/v1/orders")
        .then()
        .header("X-Trace-Id", traceId); // Mesmo trace ID na resposta
}

@Test
void shouldCreateSpansForEachOperation() {
    // Usar Testcontainers para Jaeger/Zipkin
    orderService.placeOrder(validOrder);
    
    // Verificar spans no Jaeger
    List<Span> spans = jaegerClient.getTraces("order-service");
    assertThat(spans)
        .extracting(Span::getOperationName)
        .contains(
            "OrderService.placeOrder",
            "HaveStockUseCase.execute",
            "SaveOrderUseCase.execute",
            "KafkaOrderProducer.produceOrderPlacedEvent"
        );
}
```

### 6.4 Alert Condition Testing
**Status: NÃO EXISTE** ❌

#### Recomendação
```java
@Test
void shouldTriggerAlertOnHighErrorRate() {
    // Simular 50% de falhas
    for (int i = 0; i < 10; i++) {
        if (i % 2 == 0) {
            simulateOrderFailure();
        } else {
            orderService.placeOrder(validOrder);
        }
    }
    
    // Verificar que alert foi acionado
    Assert.that(alertService.getLastAlert())
        .hasTitle("High error rate detected in order-service")
        .hasLevel(AlertLevel.CRITICAL);
}
```

---

## 7. Known Issues & Limitations

### 7.1 Critical Issues (Bloqueantes)

| ID | Issue | Impact | Status |
|---|-------|--------|--------|
| QA-001 | Nenhuma medição de cobertura de código | Impossível validar qualidade | 🔴 CRÍTICO |
| QA-002 | Kafka integration tests desativados | Notification Service untestado | 🔴 CRÍTICO |
| QA-003 | API Gateway com apenas contextLoads | Routing não validado | 🔴 CRÍTICO |
| QA-004 | Sem testes de segurança | Vulnerabilidades OWASP não detectadas | 🔴 CRÍTICO |
| QA-005 | Sem contract testing | Mudanças incompatíveis entre serviços | 🔴 CRÍTICO |

### 7.2 High Priority Issues

| ID | Issue | Impact | Status |
|---|-------|--------|--------|
| QA-006 | Sem testes unitários | Lógica de negócio não isolada | 🟠 ALTA |
| QA-007 | Sem E2E tests | Fluxo completo de usuário não validado | 🟠 ALTA |
| QA-008 | Sem testes de performance | Risco de degradação em produção | 🟠 ALTA |
| QA-009 | Sem teste de validação de entrada | SQL Injection e XSS possíveis | 🟠 ALTA |
| QA-010 | Sem teste de tratamento de erro | Falhas silenciosas | 🟠 ALTA |

### 7.3 Medium Priority Issues

| ID | Issue | Impact | Status |
|---|-------|--------|--------|
| QA-011 | Sem cobertura de código no CI/CD | Qualidade não enforçada | 🟡 MÉDIA |
| QA-012 | Sem análise de segurança automática | Vulnerabilidades passam despercebidas | 🟡 MÉDIA |
| QA-013 | Testes sem dados factories | Código de teste complexo e quebrado | 🟡 MÉDIA |
| QA-014 | Sem teste de event eventual consistency | Dados inconsistentes possíveis | 🟡 MÉDIA |
| QA-015 | Código comentado nos testes | Teste code smell | 🟡 MÉDIA |

---

## 8. Test Automation Roadmap

### Phase 1: Foundation (Week 1-2)
```
[ ] 1.1 Implementar JaCoCo para medição de cobertura
[ ] 1.2 Configurar quality gate (>80% coverage)
[ ] 1.3 Reativar Kafka tests na Notification Service
[ ] 1.4 Criar TestDataBuilder para DTOs
```

### Phase 2: Unit Tests (Week 3-4)
```
[ ] 2.1 Implementar testes unitários para OrderService (10+ testes)
[ ] 2.2 Implementar testes unitários para ProductService (8+ testes)
[ ] 2.3 Implementar testes unitários para InventoryService (6+ testes)
[ ] 2.4 Implementar testes unitários para Notification Service (6+ testes)
[ ] 2.5 Cobertura target: 80%
```

### Phase 3: Integration & E2E Tests (Week 5-6)
```
[ ] 3.1 Expandir testes de integração (30+ testes)
[ ] 3.2 Implementar E2E test para full order flow
[ ] 3.3 Implementar contract tests (Spring Cloud Contract)
[ ] 3.4 Adicionar testes de API Gateway
```

### Phase 4: Security & Performance (Week 7-8)
```
[ ] 4.1 Implementar testes de segurança (OWASP Top 10)
[ ] 4.2 Implementar load tests com JMeter/Gatling
[ ] 4.3 Implementar OWASP dependency check
[ ] 4.4 Adicionar SonarQube analysis
```

### Phase 5: CI/CD & Observability (Week 9-10)
```
[ ] 5.1 Atualizar pipeline CI/CD com quality gates
[ ] 5.2 Adicionar relatórios de cobertura (Codecov)
[ ] 5.3 Implementar testes de logging
[ ] 5.4 Implementar testes de metrics/tracing
```

---

## 9. Test Pyramid Assessment

### Situação Atual ❌
```
              /\
             /  \
            /____\      E2E: 0 testes
           /      \
          /        \    Integration: 9 testes
         /          \
        /____________\
       /              \
      /________________\ Unit: 0 testes (estimado)
      
Proporção: 0% Unit : 100% Integration : 0% E2E ← INVERTIDA
```

### Situação Desejada ✅
```
              /\
             /  \
            /____\      E2E: ~10 testes (10%)
           /      \
          /        \    Integration: ~40 testes (40%)
         /          \
        /____________\
       /              \
      /________________\ Unit: ~50 testes (50%)
      
Proporção: 50% Unit : 40% Integration : 10% E2E ← IDEAL
```

### Roadmap de Implementação
1. **Semanas 1-2**: Adicionar 50 testes unitários
2. **Semanas 3-4**: Adicionar 30 testes de integração
3. **Semanas 5-6**: Adicionar 10 testes E2E

---

## 10. Recommendations Summary

### Immediate Actions (Do Now)
1. **Implementar JaCoCo** - Medir cobertura atual (estimado 5-10%)
2. **Reativar Kafka tests** - 30 min de trabalho
3. **Criar quality gate no CI/CD** - Falhar build se cobertura < 80%
4. **Adicionar @Test em API Gateway** - Minimal routing tests

### Short Term (1-2 weeks)
1. **Implementar 20+ testes unitários** - OrderService, ProductService
2. **Adicionar dados factories** - TestDataBuilder para DTOs
3. **Implementar testes de validação** - Input validation, error handling
4. **Adicionar SonarQube analysis** - Detecção de code smells

### Medium Term (2-4 weeks)
1. **Contract testing** - Spring Cloud Contract entre serviços
2. **E2E tests** - Full order flow end-to-end
3. **Security tests** - OWASP Top 10, SQL Injection, XSS, CORS
4. **Performance tests** - JMeter/Gatling com baselines

### Long Term (4+ weeks)
1. **Observability tests** - Logs, metrics, tracing
2. **Chaos engineering** - Teste resiliência sob falhas
3. **Flaky test detection** - Monitorar testes intermitentes
4. **Test analytics** - Dashboard de trends de qualidade

---

## 11. Quality Gates

### Recomendado para CI/CD

```yaml
# Coverage Requirements
- Line Coverage: >= 80%
- Branch Coverage: >= 75%
- Cyclomatic Complexity: <= 10 per method

# Test Requirements
- Unit Tests: >= 50 (current: 0)
- Integration Tests: >= 40 (current: 9)
- E2E Tests: >= 10 (current: 0)
- Security Tests: >= 20 (current: 0)

# Code Quality
- Bugs Found: < 5
- Code Smells: < 20
- Vulnerabilities: 0 CRITICAL/HIGH

# Performance
- Test Execution Time: < 5 minutes
- Coverage Report Generation: < 30 seconds
- Build Time: < 10 minutes
```

---

## Conclusão

O projeto Micro-Commerce possui uma **fundação de infraestrutura de testes adequada** (Testcontainers, REST Assured, Spring Boot Test), mas sofre de **execução extremamente deficiente**. Com apenas 9 testes para uma arquitetura de 5 microsserviços, a cobertura estimada de **5-10% é INACEITÁVEL** para um sistema em produção.

A implementação das recomendações acima levará o projeto a uma **postura de qualidade profissional** (80%+ cobertura, segurança validada, performance testada).

### Priority: 🔴 CRÍTICO
**Começar implementação de testes unitários imediatamente.**

---

**Carla QA - Quality Assurance Engineer**  
**Avanade Method v2.1**  
**Data: 2026-07-23**

