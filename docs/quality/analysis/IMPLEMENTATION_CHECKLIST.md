# ✅ Implementation Checklist - Micro-Commerce QA Improvements

**Responsável:** Development Team  
**Timeline:** 6-8 semanas  
**Prioridade:** 🔴 CRÍTICO

---

## Phase 1: Foundation (Week 1-2) - IMMEDIATE

### Week 1

#### Maven Configuration
- [ ] Add JaCoCo plugin to order-service/pom.xml
- [ ] Add JaCoCo plugin to product-service/pom.xml
- [ ] Add JaCoCo plugin to inventory-service/pom.xml
- [ ] Add JaCoCo plugin to notification-service/pom.xml
- [ ] Configure coverage thresholds (80% minimum)
- [ ] Verify local build with `mvn clean test jacoco:report`

#### Test Data Setup
- [ ] Create PlaceOrderDTOBuilder class
- [ ] Create OrderBuilder class
- [ ] Create ProductBuilder class
- [ ] Create InventoryBuilder class
- [ ] Create base TestDataFactory class

#### Kafka Tests Reactivation
- [ ] Uncomment @Bean @ServiceConnection KafkaContainer in NotificationServiceApplicationTests
- [ ] Create TestcontainersConfiguration for Kafka
- [ ] Verify Kafka container starts in local tests

#### CI/CD Initial Setup
- [ ] Update ci_order.yml to generate coverage reports
- [ ] Add codecov step to GitHub Actions
- [ ] Configure SonarQube connection parameters
- [ ] Create maven settings for SonarQube token

**Effort: ~40 horas | Expected Coverage: 10-15%**

---

### Week 2

#### OrderService Unit Tests (15 testes)
- [ ] Create OrderServiceImplTest class
  - [ ] `placeOrder_shouldReturnOrder_whenStockIsAvailable()`
  - [ ] `placeOrder_shouldThrowException_whenStockIsUnavailable()`
  - [ ] `placeOrder_shouldThrowException_whenCreateOrderFails()`
  - [ ] `placeOrder_shouldThrowException_whenEventPublicationFails()`
  - [ ] `placeOrder_shouldThrowException_whenInputIsNull()`
  - [ ] `placeOrder_shouldCallAllUseCasesInCorrectOrder()`

- [ ] Create OrderControllerTest class (@WebMvcTest)
  - [ ] `createOrder_shouldReturn201_whenOrderIsValid()`
  - [ ] `createOrder_shouldReturn400_whenEmailIsInvalid()`
  - [ ] `createOrder_shouldReturn400_whenEmailIsNull()`
  - [ ] `createOrder_shouldReturn400_whenFieldsAreEmpty()`
  - [ ] `createOrder_shouldReturn422_whenStockIsInsufficient()`
  - [ ] `createOrder_shouldReturn500_whenServiceFails()`

- [ ] Create AppErrorHandlerTest class
  - [ ] `handleInsufficientStockException_shouldReturn422()`
  - [ ] `handleValidationException_shouldReturn400()`
  - [ ] `handleGeneralException_shouldReturn500()`

#### ProductService Unit Tests (12 testes)
- [ ] Create CreateProductUseCaseTest
  - [ ] `execute_shouldCreateProduct_whenInputIsValid()`
  - [ ] `execute_shouldThrowException_whenSkuCodeAlreadyExists()`
  - [ ] `execute_shouldThrowException_whenInputIsInvalid()`
  - [ ] `execute_shouldValidateAllFields()`
  - [ ] `execute_shouldSetDefaultValues()`
  - [ ] `execute_shouldPersistToDatabase()`

- [ ] Create ProductControllerTest
  - [ ] `createProduct_shouldReturn201()`
  - [ ] `createProduct_shouldReturn400_forInvalidInput()`
  - [ ] `getAllProducts_shouldReturn200()`
  - [ ] `getAllProducts_shouldReturnEmpty_whenNoProducts()`
  - [ ] `getProduct_shouldReturn200()`
  - [ ] `getProduct_shouldReturn404_whenNotFound()`

#### InventoryService Unit Tests (10 testes)
- [ ] Create IsInStockUseCaseTest
  - [ ] `execute_shouldReturnTrue_whenQuantityAvailable()`
  - [ ] `execute_shouldReturnFalse_whenQuantityUnavailable()`
  - [ ] `execute_shouldThrowException_whenSkuCodeInvalid()`
  - [ ] `execute_shouldThrowException_whenQuantityNegative()`
  - [ ] `execute_shouldThrowException_whenDatabaseFails()`

- [ ] Create InventoryControllerTest
  - [ ] `checkInventory_shouldReturn200_whenAvailable()`
  - [ ] `checkInventory_shouldReturn200_whenUnavailable()`
  - [ ] `checkInventory_shouldReturn400_forInvalidSkuCode()`
  - [ ] `checkInventory_shouldReturn400_forNegativeQuantity()`
  - [ ] `checkInventory_shouldReturn500_whenDatabaseFails()`

**Effort: ~40 horas | Expected Coverage: 25-35%**

---

## Phase 2: Integration & E2E Tests (Week 3-4)

### Week 3

#### Order Service Integration Tests (8 testes)
- [ ] Create OrderServiceIntegrationTest class (@SpringBootTest)
  - [ ] `shouldCreateOrderAndPublishEvent()`
  - [ ] `shouldNotCreateOrderIfInventoryServiceIsDown()`
  - [ ] `shouldRetryOnTransientInventoryServiceFailure()`
  - [ ] `shouldHandleCircuitBreakerTriggered()`
  - [ ] `shouldRollbackOrderIfEventPublicationFails()`
  - [ ] `shouldPersistOrderInDatabase()`
  - [ ] `shouldValidateOrderData()`
  - [ ] `shouldHandleConcurrentOrders()`

#### Product Service Integration Tests (5 testes)
- [ ] Create ProductServiceIntegrationTest
  - [ ] `shouldCreateProductAndPersist()`
  - [ ] `shouldRejectDuplicateSkuCode()`
  - [ ] `shouldFetchAllProducts()`
  - [ ] `shouldUpdateProduct()`
  - [ ] `shouldDeleteProduct()`

#### Inventory Service Integration Tests (4 testes)
- [ ] Create InventoryServiceIntegrationTest
  - [ ] `shouldCheckInventory()`
  - [ ] `shouldDecrementInventory()`
  - [ ] `shouldValidateDatabaseConstraints()`
  - [ ] `shouldHandleConcurrentInventoryChecks()`

#### Database Integrity Tests (6 testes)
- [ ] Create DatabaseIntegrityTest
  - [ ] `shouldEnforceUniqueOrderNumber()`
  - [ ] `shouldEnforceSkuCodeUniqueness()`
  - [ ] `shouldEnforceForeignKeyConstraints()`
  - [ ] `shouldMaintainInventoryConstraints()`
  - [ ] `shouldValidateTransactionRollback()`
  - [ ] `shouldValidateMigrationConsistency()`

**Effort: ~35 horas | Expected Coverage: 40-50%**

### Week 4

#### Kafka Integration Tests (6 testes)
- [ ] Create KafkaOrderConsumerTest
  - [ ] `shouldConsumeOrderPlacedEvent()`
  - [ ] `shouldHandleMalformedEvent()`
  - [ ] `shouldRetryOnFailedEmailSend()`
  - [ ] `shouldEnsureIdempotency()`
  - [ ] `shouldTrackConsumerLag()`
  - [ ] `shouldPersistNotification()`

#### Cross-Service E2E Tests (5 testes)
- [ ] Create EndToEndOrderFlowTest
  - [ ] `shouldCompleteFullOrderFlow()`
  - [ ] `shouldTrackOrderStatus()`
  - [ ] `shouldHandleMultipleConcurrentOrders()`
  - [ ] `shouldValidateEventChaining()`
  - [ ] `shouldPropagateErrorsThroughServices()`

**Effort: ~25 horas | Expected Coverage: 55-65%**

---

## Phase 3: Security Tests (Week 5)

### Input Validation Tests (8 testes)
- [ ] Create InputValidationTest
  - [ ] `shouldRejectSqlInjectionInSkuCode()`
  - [ ] `shouldRejectXssInEmail()`
  - [ ] `shouldRejectExcessivelyLongInput()`
  - [ ] `shouldValidateEmailFormat()`
  - [ ] `shouldRejectNullFields()`
  - [ ] `shouldRejectNegativePrices()`
  - [ ] `shouldRejectInvalidCurrencyCodes()`
  - [ ] `shouldSanitizeSpecialCharacters()`

### Authentication/Authorization Tests (6 testes)
- [ ] Create SecurityTest
  - [ ] `shouldRejectRequestWithoutJwtToken()`
  - [ ] `shouldRejectExpiredJwtToken()`
  - [ ] `shouldRejectMalformedJwtToken()`
  - [ ] `shouldValidateTokenScopes()`
  - [ ] `shouldRejectUnauthorizedUsers()`
  - [ ] `shouldValidateRoleBasedAccess()`

### CORS & Gateway Security Tests (3 testes)
- [ ] Create CorsSecurityTest
  - [ ] `shouldRejectUnauthorizedOrigins()`
  - [ ] `shouldAllowAuthenticatedOrigins()`
  - [ ] `shouldValidatePreflight()`

### Rate Limiting Tests (2 testes)
- [ ] Create RateLimitingTest
  - [ ] `shouldEnforceRateLimiting()`
  - [ ] `shouldReturn429_whenLimitExceeded()`

**Effort: ~30 horas | Expected Coverage: 70-75%**

---

## Phase 4: Performance & API Tests (Week 6)

### API Contract Tests (4 testes)
- [ ] Create ApiContractTest
  - [ ] `shouldValidateOrderCreationResponseSchema()`
  - [ ] `shouldValidateProductFetchResponseSchema()`
  - [ ] `shouldValidateInventoryCheckResponseSchema()`
  - [ ] `shouldValidateErrorResponseFormat()`

### Performance Baseline Tests (5 testes)
- [ ] Create PerformanceTest
  - [ ] `shouldCreateOrderWithin500ms_P99()`
  - [ ] `shouldFetchProductsWithin200ms_P99()`
  - [ ] `shouldCheckInventoryWithin100ms_P99()`
  - [ ] `shouldHandleLoadWithoutMemoryLeak()`
  - [ ] `shouldMaintainConsistencyUnderLoad()`

### API Gateway Routing Tests (4 testes)
- [ ] Create ApiGatewayRoutingTest (@WebMvcTest)
  - [ ] `shouldRouteProductServiceRequests()`
  - [ ] `shouldRouteOrderServiceRequests()`
  - [ ] `shouldRouteInventoryServiceRequests()`
  - [ ] `shouldReturn404ForUnknownRoutes()`

**Effort: ~25 horas | Expected Coverage: 75-80%**

---

## Phase 5: Final Quality Checks (Week 7-8)

### Code Quality Tools Setup
- [ ] Install SonarQube Scanner Maven plugin
- [ ] Configure project key & host in maven
- [ ] Add OWASP Dependency Check plugin
- [ ] Install Spotbugs Maven plugin
- [ ] Configure CI/CD to run all checks

### Quality Gates Configuration
- [ ] Set Line Coverage minimum to 80%
- [ ] Set Branch Coverage minimum to 75%
- [ ] Configure Sonar Quality Gate
- [ ] Setup Codecov badge
- [ ] Configure build failure on coverage regression

### Documentation
- [ ] Update README with coverage badge
- [ ] Document testing best practices
- [ ] Create test execution guide
- [ ] Document CI/CD pipeline

### Final Validation
- [ ] Run full test suite locally
- [ ] Verify all tests pass
- [ ] Confirm coverage reports generated
- [ ] Validate CI/CD pipeline
- [ ] Review SonarQube dashboard

**Effort: ~20 horas**

---

## Test Count & Coverage Progression

```
Week 1-2 (Foundation + Unit Tests)
├── Unit Tests: 37
├── Coverage: 25-35%
└── Status: Build foundation ✓

Week 3-4 (Integration & E2E)
├── Unit Tests: 37
├── Integration Tests: 23
├── E2E Tests: 5
├── Coverage: 55-65%
└── Status: Core flows validated ✓

Week 5 (Security)
├── Security Tests: 19
├── Total Tests: 84
├── Coverage: 70-75%
└── Status: Security validated ✓

Week 6 (Performance & API)
├── Performance Tests: 5
├── API Tests: 8
├── Total Tests: 97
├── Coverage: 75-80%
└── Status: Performance validated ✓

Week 7-8 (Quality Gates)
├── Final Coverage: 80%+
├── Total Tests: 100+
├── Tools: JaCoCo + SonarQube + Codecov
└── Status: Production Ready ✓
```

---

## Success Metrics

### Coverage Targets
- [ ] Line Coverage: ≥ 80% (Target: 85%)
- [ ] Branch Coverage: ≥ 75% (Target: 80%)
- [ ] Method Coverage: ≥ 90% (Target: 95%)

### Test Metrics
- [ ] Total Tests: ≥ 100 (Target: 120)
- [ ] Unit Tests: ≥ 50 (Target: 60)
- [ ] Integration Tests: ≥ 40 (Target: 50)
- [ ] E2E Tests: ≥ 10 (Target: 10)

### Quality Metrics
- [ ] Test Execution Time: < 5 minutes
- [ ] Build Time: < 10 minutes
- [ ] No CRITICAL/HIGH vulnerabilities
- [ ] Code Smells: < 20
- [ ] Bugs Found: < 5

### Process Metrics
- [ ] CI/CD pipeline passes: 100%
- [ ] Test reliability: 98%+ (< 2% flaky)
- [ ] Code review coverage: 100%
- [ ] Documentation coverage: 100%

---

## Risk Mitigation

### Risks & Mitigations
```
RISK: Tests break existing functionality
├── Mitigation: Run full test suite before commit
├── Mitigation: Use feature branches
└── Mitigation: Code review required

RISK: Test execution time becomes unmanageable
├── Mitigation: Parallelize test execution
├── Mitigation: Separate unit/integration tests
└── Mitigation: Cache container startup

RISK: Flaky tests reduce confidence
├── Mitigation: Implement retry logic
├── Mitigation: Monitor test stability
└── Mitigation: Fix flaky tests immediately

RISK: Coverage metrics become unreliable
├── Mitigation: Use mutation testing
├── Mitigation: Monitor code quality trends
└── Mitigation: Regular coverage audits
```

---

## Sign-Off & Approval

```
Development Lead: _________________________ Date: ____
QA Manager:       _________________________ Date: ____
Product Owner:    _________________________ Date: ____
```

---

## Contact & Support

For questions or issues during implementation, contact:

**Carla QA - Quality Assurance Engineer**  
**Email:** carla.qa@avanade.com  
**Slack:** @carla-qa  
**Office Hours:** Daily 10:00-11:00 AM (sync with Scrum Master)

---

**Document Version:** 1.0  
**Last Updated:** 2026-07-23  
**Status:** 🔴 PENDING IMPLEMENTATION
