# 🧪 Test Strategy & Implementation Guide - Micro-Commerce

**Data: 2026-07-23**  
**Preparado por: Carla QA**  
**Confidencialidade: Team Internal**

---

## 1. Order Service Testing Strategy

### 1.1 Unit Tests to Implement

#### OrderServiceImpl Tests (6 testes)

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    
    @Mock
    private HaveStockUseCase haveStockUseCase;
    
    @Mock
    private CreateOrderUseCase createOrderUseCase;
    
    @Mock
    private SaveOrderUseCase saveOrderUseCase;
    
    @Mock
    private ProduceOrderPlacedEventUseCase produceOrderPlacedEventUseCase;
    
    @InjectMocks
    private OrderServiceImpl orderService;
    
    @Test
    void placeOrder_shouldReturnOrder_whenStockIsAvailable() {
        // GIVEN
        PlaceOrderDTO orderDTO = new PlaceOrderDTOBuilder().build();
        Order expectedOrder = new OrderBuilder().build();
        
        when(haveStockUseCase.execute(any())).thenReturn(true);
        when(createOrderUseCase.execute(orderDTO)).thenReturn(expectedOrder);
        when(saveOrderUseCase.execute(expectedOrder)).thenReturn(expectedOrder);
        
        // WHEN
        Order result = orderService.placeOrder(orderDTO);
        
        // THEN
        assertThat(result).isEqualTo(expectedOrder);
        verify(produceOrderPlacedEventUseCase, times(1)).execute(any());
    }
    
    @Test
    void placeOrder_shouldThrowException_whenStockIsUnavailable() {
        // GIVEN
        PlaceOrderDTO orderDTO = new PlaceOrderDTOBuilder().build();
        when(haveStockUseCase.execute(any())).thenReturn(false);
        
        // WHEN & THEN
        assertThrows(InsufficientStockException.class,
            () -> orderService.placeOrder(orderDTO));
        
        // Verificar que order NÃO foi salva
        verify(saveOrderUseCase, never()).execute(any());
    }
    
    @Test
    void placeOrder_shouldThrowException_whenCreateOrderFails() {
        // GIVEN
        PlaceOrderDTO orderDTO = new PlaceOrderDTOBuilder().build();
        
        when(haveStockUseCase.execute(any())).thenReturn(true);
        when(createOrderUseCase.execute(orderDTO))
            .thenThrow(new OrderCreationException("Invalid data"));
        
        // WHEN & THEN
        assertThrows(OrderCreationException.class,
            () -> orderService.placeOrder(orderDTO));
    }
    
    @Test
    void placeOrder_shouldThrowException_whenEventPublicationFails() {
        // GIVEN
        PlaceOrderDTO orderDTO = new PlaceOrderDTOBuilder().build();
        Order createdOrder = new OrderBuilder().build();
        
        when(haveStockUseCase.execute(any())).thenReturn(true);
        when(createOrderUseCase.execute(orderDTO)).thenReturn(createdOrder);
        when(saveOrderUseCase.execute(createdOrder)).thenReturn(createdOrder);
        when(produceOrderPlacedEventUseCase.execute(any()))
            .thenThrow(new KafkaException("Broker unavailable"));
        
        // WHEN & THEN
        assertThrows(KafkaException.class,
            () -> orderService.placeOrder(orderDTO));
    }
    
    @Test
    void placeOrder_shouldThrowException_whenInputIsNull() {
        // WHEN & THEN
        assertThrows(NullPointerException.class,
            () -> orderService.placeOrder(null));
    }
    
    @Test
    void placeOrder_shouldCallAllUseCasesInCorrectOrder() {
        // GIVEN
        PlaceOrderDTO orderDTO = new PlaceOrderDTOBuilder().build();
        Order order = new OrderBuilder().build();
        
        when(haveStockUseCase.execute(any())).thenReturn(true);
        when(createOrderUseCase.execute(orderDTO)).thenReturn(order);
        when(saveOrderUseCase.execute(order)).thenReturn(order);
        
        // WHEN
        orderService.placeOrder(orderDTO);
        
        // THEN - Verificar ordem de chamadas
        InOrder inOrder = inOrder(
            haveStockUseCase,
            createOrderUseCase,
            saveOrderUseCase,
            produceOrderPlacedEventUseCase
        );
        
        inOrder.verify(haveStockUseCase).execute(any());
        inOrder.verify(createOrderUseCase).execute(any());
        inOrder.verify(saveOrderUseCase).execute(any());
        inOrder.verify(produceOrderPlacedEventUseCase).execute(any());
    }
}
```

#### OrderController Tests (6 testes)

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    private PlaceOrderDTOBuilder orderBuilder = new PlaceOrderDTOBuilder();
    
    @Test
    void createOrder_shouldReturn201_whenOrderIsValid() throws Exception {
        // GIVEN
        PlaceOrderDTO orderDTO = orderBuilder.build();
        Order createdOrder = new OrderBuilder().build();
        
        when(orderService.placeOrder(any())).thenReturn(createdOrder);
        
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderNumber")
                .value(createdOrder.getOrderNumber()))
            .andExpect(content()
                .string(containsString("has been placed with success")));
    }
    
    @Test
    void createOrder_shouldReturn400_whenEmailIsInvalid() throws Exception {
        // GIVEN
        PlaceOrderDTO orderDTO = orderBuilder
            .withEmail("invalid-email")
            .build();
        
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void createOrder_shouldReturn400_whenEmailIsNull() throws Exception {
        // GIVEN
        String requestBody = """
            {
                "orderNumber": "ORD-123",
                "skuCode": "iphone_15",
                "price": 100.0,
                "quantity": 1,
                "userDetails": {
                    "email": null,
                    "firstName": "Test",
                    "lastName": "User"
                }
            }
        """;
        
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void createOrder_shouldReturn400_whenFieldsAreEmpty() throws Exception {
        // GIVEN
        PlaceOrderDTO orderDTO = orderBuilder
            .withFirstName("")
            .withLastName("")
            .build();
        
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void createOrder_shouldReturn422_whenStockIsInsufficient() throws Exception {
        // GIVEN
        PlaceOrderDTO orderDTO = orderBuilder.build();
        
        when(orderService.placeOrder(any()))
            .thenThrow(new InsufficientStockException("iphone_15"));
        
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void createOrder_shouldReturn500_whenServiceFails() throws Exception {
        // GIVEN
        PlaceOrderDTO orderDTO = orderBuilder.build();
        
        when(orderService.placeOrder(any()))
            .thenThrow(new RuntimeException("Service error"));
        
        // WHEN & THEN
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
            .andExpect(status().isInternalServerError());
    }
}
```

#### Error Handling Tests (3 testes)

```java
class AppErrorHandlerTest {
    
    private AppErrorHandler errorHandler;
    
    @BeforeEach
    void setUp() {
        errorHandler = new AppErrorHandler();
    }
    
    @Test
    void handleInsufficientStockException_shouldReturn422() {
        // GIVEN
        InsufficientStockException exception = 
            new InsufficientStockException("iphone_15");
        
        // WHEN
        ResponseEntity<AppError> response = 
            errorHandler.handleInsufficientStockException(exception);
        
        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getStatusCode())
            .isEqualTo("UNPROCESSABLE_ENTITY");
        assertThat(response.getBody().getMessage())
            .contains("iphone_15");
    }
    
    @Test
    void handleValidationException_shouldReturn400() {
        // ...
    }
    
    @Test
    void handleGeneralException_shouldReturn500() {
        // ...
    }
}
```

### 1.2 Integration Tests to Implement

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class OrderServiceIntegrationTest {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    @LocalServerPort
    private Integer port;
    
    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
    
    @Test
    void shouldCreateOrderAndPublishEvent() {
        // GIVEN
        InventoryClientStub.stubInventoryCall("iphone_15", 10);
        String validRequest = """
            {
                "orderNumber": "ORD-123",
                "skuCode": "iphone_15",
                "price": 999.99,
                "quantity": 10,
                "userDetails": {
                    "email": "customer@example.com",
                    "firstName": "John",
                    "lastName": "Doe"
                }
            }
        """;
        
        // WHEN
        RestAssured.given()
            .contentType("application/json")
            .body(validRequest)
            .when()
            .post("/api/v1/orders")
            .then()
            .statusCode(201)
            .body(containsString("has been placed with success"));
        
        // THEN - Verificar que ordem foi salva
        assertThat(orderRepository.count()).isEqualTo(1);
        
        // Verificar que evento foi publicado
        // (usar Kafka testcontainer para validar)
    }
    
    @Test
    void shouldNotCreateOrderIfInventoryServiceIsDown() {
        // GIVEN
        InventoryClientStub.stubServerError(500, "Connection refused");
        String validRequest = """
            {
                "orderNumber": "ORD-124",
                "skuCode": "iphone_15",
                "price": 999.99,
                "quantity": 1,
                "userDetails": {
                    "email": "customer@example.com",
                    "firstName": "John",
                    "lastName": "Doe"
                }
            }
        """;
        
        // WHEN & THEN
        RestAssured.given()
            .contentType("application/json")
            .body(validRequest)
            .when()
            .post("/api/v1/orders")
            .then()
            .statusCode(503);
        
        // Verificar que ordem NÃO foi criada
        assertThat(orderRepository.count()).isZero();
    }
}
```

---

## 2. Product Service Testing Strategy

### 2.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private CreateProductUseCaseImpl createProductUseCase;
    
    @Test
    void execute_shouldCreateProduct_whenInputIsValid() {
        // GIVEN
        CreateProductDTO input = new CreateProductDTOBuilder().build();
        Product expectedProduct = new ProductBuilder().build();
        
        when(productRepository.save(any())).thenReturn(expectedProduct);
        
        // WHEN
        Product result = createProductUseCase.execute(input);
        
        // THEN
        assertThat(result).isEqualTo(expectedProduct);
        verify(productRepository, times(1)).save(any());
    }
    
    @Test
    void execute_shouldThrowException_whenSkuCodeAlreadyExists() {
        // GIVEN
        CreateProductDTO input = new CreateProductDTOBuilder()
            .withSkuCode("EXISTING_SKU")
            .build();
        
        when(productRepository.existsBySkuCode("EXISTING_SKU"))
            .thenReturn(true);
        
        // WHEN & THEN
        assertThrows(DuplicateSkuCodeException.class,
            () -> createProductUseCase.execute(input));
        
        verify(productRepository, never()).save(any());
    }
}
```

---

## 3. Notification Service Testing Strategy

### 3.1 Kafka Consumer Tests

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class KafkaOrderConsumerTest {
    
    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @MockBean
    private EmailService emailService;
    
    @Test
    @Timeout(10)
    void shouldConsumerOrderPlacedEvent() throws InterruptedException {
        // GIVEN
        OrderPlacedEvent event = new OrderPlacedEventBuilder()
            .withOrderNumber("ORD-123")
            .withEmail("customer@example.com")
            .build();
        
        when(emailService.send(any())).thenReturn(true);
        
        // WHEN
        kafkaTemplate.send("order.placed", event).get();
        
        // THEN - Aguardar processamento
        awaitility().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                assertThat(notificationRepository.count()).isGreaterThan(0);
            });
        
        verify(emailService, times(1)).send(any());
    }
    
    @Test
    void shouldHandleMalformedEvent() throws Exception {
        // GIVEN
        String malformedEvent = "{ invalid json }";
        
        // WHEN
        kafkaTemplate.send("order.placed", malformedEvent).get();
        
        // THEN
        awaitility().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                // Verificar que erro foi logado
                // Verificar que retry foi agendado
            });
    }
}
```

---

## 4. API Gateway Testing Strategy

```java
@WebMvcTest(Router.class)
class ApiGatewayRoutingTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldRouteProductServiceRequests() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk());
    }
    
    @Test
    void shouldRouteOrderServiceRequests() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest()); // Bad request from service
    }
    
    @Test
    void shouldReturn404ForUnknownRoute() throws Exception {
        mockMvc.perform(get("/unknown-path"))
            .andExpect(status().isNotFound());
    }
}
```

---

## 5. Security Tests

### 5.1 Input Validation Tests

```java
class InputValidationTest {
    
    @Test
    void shouldRejectSqlInjectionInSkuCode() {
        String maliciousSku = "iphone'; DROP TABLE inventory; --";
        
        RestAssured
            .when()
            .get("/api/v1/inventory?skuCode=" + 
                URLEncoder.encode(maliciousSku, StandardCharsets.UTF_8) + 
                "&quantity=1")
            .then()
            .statusCode(400); // Ou sanitizado
    }
    
    @Test
    void shouldRejectXssInEmail() {
        String xssEmail = "test<script>alert('xss')</script>@example.com";
        
        RestAssured.given()
            .contentType("application/json")
            .body("""
                {
                    "email": "%s",
                    "firstName": "Test",
                    "lastName": "User"
                }
            """.formatted(xssEmail))
            .when()
            .post("/api/v1/orders")
            .then()
            .statusCode(400);
    }
}
```

### 5.2 Authentication/Authorization Tests

```java
class SecurityTest {
    
    @Test
    void shouldRejectRequestWithoutJwtToken() {
        RestAssured
            .when()
            .post("/api/v1/orders") // Token requerido
            .then()
            .statusCode(401);
    }
    
    @Test
    void shouldRejectExpiredJwtToken() {
        String expiredToken = generateExpiredJwt();
        
        RestAssured.given()
            .header("Authorization", "Bearer " + expiredToken)
            .when()
            .post("/api/v1/orders")
            .then()
            .statusCode(401);
    }
}
```

---

## 6. Test Data Builders

### OrderDTOBuilder

```java
public class PlaceOrderDTOBuilder {
    private String orderNumber = "ORD-" + UUID.randomUUID();
    private String skuCode = "iphone_15";
    private Double price = 999.99;
    private Integer quantity = 10;
    private String email = "customer@example.com";
    private String firstName = "John";
    private String lastName = "Doe";
    
    public PlaceOrderDTO build() {
        UserDetailsDTO userDetails = new UserDetailsDTO(
            email, firstName, lastName
        );
        return new PlaceOrderDTO(
            orderNumber, skuCode, price, quantity, userDetails
        );
    }
    
    public PlaceOrderDTOBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public PlaceOrderDTOBuilder withSkuCode(String skuCode) {
        this.skuCode = skuCode;
        return this;
    }
    
    // ... outros builders
}
```

---

## Summary & Implementation Checklist

```
Phase 1: Unit Tests (Week 1-2)
[ ] OrderService unit tests (6 testes)
[ ] OrderController unit tests (6 testes)
[ ] OrderController validation tests (3 testes)
[ ] ProductService unit tests (8 testes)
[ ] InventoryService unit tests (6 testes)
[ ] Test data builders implementation

Phase 2: Integration Tests (Week 3-4)
[ ] Order service integration tests (8 testes)
[ ] Product service integration tests (5 testes)
[ ] Notification Kafka consumer tests (6 testes)
[ ] Database integrity tests (6 testes)

Phase 3: Security Tests (Week 5)
[ ] Input validation tests (8 testes)
[ ] Authentication/Authorization tests (6 testes)
[ ] CORS validation tests (3 testes)

Phase 4: E2E & Performance (Week 6+)
[ ] End-to-end order flow test (5 testes)
[ ] API Gateway routing tests (4 testes)
[ ] Performance baseline tests (5 testes)
```

**Total Effort: 200+ horas / 50+ testes novos**

---

**Carla QA - Quality Assurance Engineer**  
**Avanade Method v2.1**
