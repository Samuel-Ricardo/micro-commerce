# Evidence appendix

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Analysis date:** 2026-09-01.  
**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Method:** source, configuration, tests, generated reports, deployment declarations, and existing documentation were inspected without executing commands, builds, tests, containers, or network calls.

## Evidence rules

- **[Verified]** means the claim is directly visible in an inspected artifact.
- **[Inferred]** means the claim combines verified facts into an interpretation.
- **[Unverified]** means a declaration exists but runtime behavior was not checked.
- **[Blocked]** means the available evidence prevents a reliable execution or readiness conclusion.

A source declaration is not runtime proof. A generated test report is historical evidence, not a new test run. The ignored/untracked `target/` report files were inspected where present in the working tree; they are not durable evidence in a clean clone or a CI artifact, and their values are not current execution results. A dependency declaration does not prove that the dependency is configured correctly or reachable.

## Repository and documentation inventory

| Claim | Evidence |
|---|---|
| Repository is on `feature/kubernetes` | `.git/HEAD` |
| Inspected commit is `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`) | `.git/refs/heads/feature/kubernetes` |
| Existing root README contains author links, badges, Java Devicon, and architecture image | `README.md` |
| Existing frontend READMEs remain separate | `frontend/java-shop/README.md`, `frontend/java-shop-website/README.md` |
| Generated Spring help files were present where inspected | `HELP.md` files are generated, ignored/untracked, optional, and absent from a clean clone unless regenerated |
| No project-local `.avanade-method/` was found during the initial inventory | Initial repository inspection; global standards were used |
| No pre-existing project-local docs directory was found before this pass | Initial repository inventory; the current `docs/**` pages are uncommitted documentation changes from this pass |

## Technology evidence

| Claim | Evidence |
|---|---|
| Five backend Spring Boot applications exist in total: gateway plus four domain services | Five `pom.xml` files under the backend application directories |
| Spring Boot version is 3.4.1 | Parent version in each backend POM |
| Java version is 21 | `java.version` in each backend POM |
| Kotlin 2.1.0 is declared in product/order/inventory/notification | `kotlin.version` in those POMs |
| Gateway is servlet Spring Cloud Gateway Server MVC | `spring-cloud-starter-gateway-mvc` in `api-gateway/pom.xml`; MVC handler imports in `Router.java` |
| Springdoc/API-doc support is evidenced for gateway, product, order, and inventory only | Springdoc dependencies and paths in the gateway/product/order/inventory POMs and application configuration; gateway aggregate routes and the three domain controllers; no notification REST controller, Springdoc dependency, or Springdoc configuration was found |
| Functional Angular app uses Angular 17.3 declarations and TypeScript 5.4.2 | `frontend/java-shop/package.json` |
| SSR scaffold uses Angular SSR and Express | `frontend/java-shop-website/package.json`, `angular.json`, `server.ts` |

## Architecture evidence

| Claim | Evidence |
|---|---|
| Gateway forwards product/order/inventory paths | `api-gateway/src/main/java/com/js/gateway/infra/Router.java` |
| Gateway route code declares circuit-breaker filters; route retries are not evidenced | `Router.java` uses `CircuitBreakerFilterFunctions`; no gateway retry filter was found, and retry configuration alone is not treated as route behavior |
| Order inventory client separately declares circuit breaker and retry behavior | `order-service/src/main/java/com/js/microservices/order_service/domain/gateway/InventoryGateway.kt` uses `@CircuitBreaker` and `@Retry` on the inventory call |
| Gateway has JWT resource-server configuration | `SecurityConfig.java`, gateway `application.yml` |
| Product uses MongoDB | Product POM, `ProductModel.java`, product `application.yml` |
| Order uses PostgreSQL and Flyway | Order POM, order `application.yml`, `V1__init.sql` |
| Inventory uses MySQL and Flyway | Inventory POM, inventory `application.yml`, migrations |
| Order checks inventory before save | `OrderServiceImpl.java`, `HaveStockUseCaseImpl.java`, `InventoryGateway.kt` |
| Order saves then sends Kafka event | `OrderServiceImpl.java`, `SaveOrderUseCaseImpl.java`, `KafkaOrderProducer.java` |
| Event is Avro with four string fields | Both `order-placed.avsc` files and generated event class |
| Notification consumes event and sends SMTP HTML mail | `KafkaOrderConsumer.java`, notification use cases, `MailGatewayImpl.java` |
| The pre-pass/baseline README claimed Eureka, but no Eureka implementation was evidenced | No Eureka dependency, application, route, or registry manifest was found during inspection; fixed service URLs and Kubernetes DNS names are present |

## Frontend evidence

| Claim | Evidence |
|---|---|
| Primary UI has home and add-product routes | `frontend/java-shop/src/app/app.routes.ts` |
| Primary UI configures OIDC; its interceptor has a static token-return defect | `frontend/java-shop/src/app/app.config.ts`, `frontend/java-shop/src/app/auth/auth.config.ts`, `frontend/java-shop/src/app/interceptor/auth.interceptor.ts`; the callback subscribes asynchronously, the outer function immediately calls `next(req)`, and the callback calls `next(...)` again without returning the token-bearing request |
| Primary UI reads catalog and posts orders | `frontend/java-shop/src/app/application/service/product-service.service.ts`, `frontend/java-shop/src/app/application/service/order-service.service.ts` |
| Primary UI sends selected product price and OIDC profile data in order payload | `frontend/java-shop/src/app/pages/home-page/home-page.component.ts` |
| SSR app has no application routes and generated placeholder root | `frontend/java-shop-website/src/app/app.routes.ts`, `frontend/java-shop-website/src/app/app.component.html` |

## Deployment evidence

| Claim | Evidence |
|---|---|
| Root Compose declares application and infrastructure topology | Root `docker-compose.yaml` |
| Backend Dockerfiles skip tests during image build | Each backend `Dockerfile` contains `mvn clean package -DskipTests` |
| Kind creates `java-shop` and recursively applies manifests | `k8s/kind/create-kind-cluster.sh`, `apply_all.sh` |
| Kubernetes uses application Deployments/Services and supporting infrastructure | `k8s/manifest/application/`, `k8s/manifest/infraestructure/` |
| Kubernetes contains manifest/configuration inconsistencies | Direct inspection of PostgreSQL, MongoDB, app, auth, Kafka, and observability manifests |
| Kubernetes inventory Service `targetPort: 8082` matches the inventory app and Dockerfile declarations | `inventory-service/inventory-service/src/main/resources/application.yml`, `inventory-service/inventory-service/Dockerfile`, `k8s/manifest/application/services/inventory/service.yml`; root Compose separately maps `8082:8080` |
| CI is partial and not evidence of a functioning quality gate | `.github/workflows/inventory-service/ci.yml` has `steps` at the wrong job scope; both inventory and order workflows use standalone `cd` steps that do not persist to later steps |

## Test evidence

| Claim | Evidence |
|---|---|
| Backend test shapes are mostly context-load or narrow HTTP happy paths | Test source files under each service `src/test/` |
| Frontend tests are mostly construction/generated checks | `*.spec.ts` files under both frontend applications |
| Numeric coverage is unavailable | No JaCoCo/coverage report or threshold found during inventory |
| Historical inventory report recorded an initialization error due to unavailable Docker | `inventory-service/.../target/surefire-reports/...txt` |
| Historical product report recorded context-load errors due to unresolved `LOKI_URL` | `product-service/.../target/surefire-reports/...txt` |
| Historical order report recorded context-load errors due to an undefined Loki URI scheme | `order-service/.../target/surefire-reports/...txt` |
| Historical notification context report recorded no errors | `notification-service/.../target/surefire-reports/...txt` |

## Security and quality evidence

| Claim | Evidence |
|---|---|
| Configuration contains defaults or sensitive values | `.env*`, service application files, Kubernetes Secret files, realm/configuration artifacts |
| Direct backend ports are published in local topology | Root and service Compose files |
| Root Compose and Kubernetes use different inventory container-port declarations | Root `docker-compose.yaml` maps host `8082` to container `8080`; inventory app/Dockerfile and Kubernetes Service target `8082` |
| Gateway permits selected management/documentation paths | `SecurityConfig.java` |
| Gateway/order port values can be overridden by environment files | Gateway app/Dockerfile declare `9000` while root Compose maps `9000:8080` and `.env.gateway` and its example supply `SERVER_PORT=8080`; order app/Dockerfile declare `8081`, `.env.order` and `.env.order.example` supply `SERVER_PORT=8080`, and service-level Compose files map `8081:8080` |
| Product price and order identity details are request-supplied | Frontend order model/component and `PlaceOrderDTO` |
| No stock decrement/reservation is present | Inventory controller/service/repository and migrations |
| `PlaceOrderDTO.userDetails` is non-null, but nested validation cascade is not evidenced | `PlaceOrderDTO.kt` has `@NotNull` on `userDetails`; `UserDetailsDTO.kt` declares nested constraints; no `@Valid` on the nested property was found |
| No outbox/idempotency implementation was found | Order producer/use-case and notification consumer inspection |
| HTML values are interpolated in a mail template | `FormatMailBodyUseCaseImpl.java` |
| Mutable `latest` images are used | Root/service Compose, Testcontainers declarations, Kubernetes manifests, CI workflows |

## Asset preservation record

Existing Markdown/media references inventoried before writing included:

- Java Devicon SVG URL in the root README:
  `https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/java/java-original-wordmark.svg`
- GitHub author badge URL in the root README.
- Instagram author badge URL in the root README.
- LinkedIn author badge URL in the root README.
- Existing architecture image URL in the root README:
  `https://github.com/user-attachments/assets/c1a527cd-ecc3-492d-9882-c35dbd379d4f`

These references are preserved in the rewritten root README. No existing image/media/asset file was modified, renamed, duplicated, or overwritten.

## Known unknowns

- No runtime success claim is supported by this static pass.
- No deployment health, endpoint reachability, email delivery, Kafka delivery, or browser journey was verified.
- No numeric code coverage is available.
- No formal PRD, brief, ADR, approved backlog, SLO, runbook, or production readiness sign-off was found.
- No material AI/ML/LLM/RAG/model component was evidenced in the inspected snapshot.
