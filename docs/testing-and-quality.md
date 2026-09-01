# Testing and quality

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Execution status:** no tests were run during this documentation pass. No pre-existing project-local docs directory was found before this pass.

## Test inventory

### Backend tests

| Application | Test artifacts | Declared style | Source-declared scenarios (not coverage) |
|---|---|---|---|
| Gateway | `ApiGatewayApplicationTests`: 1 executable JUnit test class with 1 test method | `@SpringBootTest` | Context-load scenario only |
| Product | `ProductServiceApplicationTests`: 1 executable JUnit test class with 2 test methods; plus `TestProductServiceApplication` launcher and `TestcontainersConfiguration` support/configuration files | Random-port Spring Boot test, Rest Assured, MongoDB Testcontainer | Product creation and list scenarios are declared; this is not a coverage claim |
| Order | `OrderServiceApplicationTests`: 1 executable JUnit test class with 2 test methods; plus `TestOrderServiceApplication` launcher, `TestcontainersConfiguration` support file, and `InventoryClientStub` WireMock helper | Random-port Spring Boot test, Rest Assured, PostgreSQL Testcontainer, WireMock stub | Successful order and insufficient-stock scenarios are declared |
| Inventory | `InventoryServiceApplicationTests`: 1 executable JUnit test class with 2 test methods; plus `TestInventoryServiceApplication` launcher and `TestcontainersConfiguration` support file | Random-port Spring Boot test, Rest Assured, MySQL Testcontainer | In-stock and not-in-stock scenarios are declared |
| Notification | `NotificationServiceApplicationTests`: 1 executable JUnit test class with 1 test method; plus `TestNotificationServiceApplication` launcher and `TestcontainersConfiguration` support/configuration file | Spring Boot context test; Kafka container configuration is commented | Context-load scenario only in the visible test |

For Product specifically, the executable JUnit surface is the single `ProductServiceApplicationTests` class and its two test methods. The launcher and `TestcontainersConfiguration` are support/configuration files, not additional executable JUnit test classes.

Representative paths:

- `api-gateway/src/test/java/com/js/gateway/ApiGatewayApplicationTests.java`
- `product-service/product-service/src/test/java/com/js/microservices/product_service/ProductServiceApplicationTests.java`
- `order-service/src/test/java/com/js/microservices/order_service/OrderServiceApplicationTests.java`
- `inventory-service/inventory-service/src/test/java/com/js/microservices/inventory_service/InventoryServiceApplicationTests.java`
- `notification-service/src/test/java/com/js/microservices/notification/NotificationServiceApplicationTests.java`

### Frontend tests

The primary Angular app contains generated or narrow tests for:

- Root component.
- Product service construction.
- Order service construction.
- Auth interceptor construction.
- Home page construction.
- Add-product component construction.
- Header component construction.

The SSR scaffold contains an Angular-generated root-component test that checks construction, title, and placeholder heading. No meaningful browser journey, authenticated order flow, accessibility suite, or SSR integration test was found in the inspected files.

## What the visible tests prove

The tests demonstrate intended examples, not current runtime health. A test name or assertion is source evidence of an intended scenario, not proof that the scenario passed in the current environment:

- The two Product test methods intend to verify HTTP `201` creation and `200` list behavior using a MongoDB Testcontainer.
- Inventory tests intend to verify `true` for a seeded quantity and `false` above that quantity using MySQL.
- Order tests intend to verify `201` success and `422` insufficient stock using a PostgreSQL Testcontainer and a WireMock inventory stub.
- Gateway and notification tests check application context loading.
- Angular tests mostly verify that dependency injection can instantiate classes/components.

They do not establish:

- Full browser-to-Keycloak-to-gateway-to-service behavior.
- Direct-port exposure policy.
- Kafka delivery, schema compatibility, consumer retries, or email delivery.
- Concurrency behavior or stock overselling.
- Price authority, user-claim authority, or authorization roles.
- Kubernetes startup, readiness, rolling update, rollback, persistence, or backup behavior.
- Accessibility, performance, resilience, security, or disaster recovery requirements.

## Historical generated report evidence

The ignored/untracked report files under `target/surefire-reports/` were inspected where present in the working tree as historical/generated artifacts. They are not durable evidence in a clean clone or a CI artifact, and their values are not current execution results. No current execution is claimed.

| Report | Historical recorded result | Interpretation |
|---|---|---|
| Inventory | 1 test run, 1 error | Test initialization failed because Testcontainers could not find a valid Docker environment. |
| Product | 2 tests run, 2 errors | Application context failed because `LOKI_URL` could not be resolved while loading Logback. |
| Order | 2 tests run, 2 errors | Application context failed while initializing Logback because the Loki URI had an undefined scheme. |
| Notification | 1 test run, 0 failures/errors | The historical report records a context test without an error; it does not prove Kafka, SMTP, or end-to-end behavior. |

Do not summarize these historical artifacts as a green project-wide build.

## Coverage status

**[Verified] Numeric coverage is unavailable.** No JaCoCo report, coverage directory, coverage threshold, or project-wide 80% gate was found in the inspected artifacts. The presence of `karma-coverage` in frontend dependencies does not provide a coverage result or threshold.

## CI quality signals

The repository contains:

- `.github/workflows/inventory-service/ci.yml`, which declares formatting, lint, tests, and a build artifact upload.
- `.github/workflows/inventory-service/cd.yml`, which builds and pushes an inventory image after the CI workflow.
- `.github/workflows/ci_order.yml`, which declares order formatting, lint, tests, build, and Docker push steps.
- `.github/dependabot.yml`, which covers only the product Maven directory.

The inventory workflow at `.github/workflows/inventory-service/ci.yml` has malformed job/steps structure: its `steps` block is at the workflow `jobs` level rather than nested under `ci_pipeline`. It also uses a standalone `cd ./inventory-service/inventory-service` step, which does not persist to later steps. The order workflow has the analogous standalone `cd ./order-service` risk. The workflow files also contain risks such as broad package upgrades, Docker login with command-line password syntax, mutable `latest` image tags, and incomplete service coverage. These are static workflow observations, not claims about a particular GitHub run; the workflows are declarations, not proof of a functioning quality gate.

## Risk-based test matrix

| Risk | Needed evidence | Current status |
|---|---|---|
| Authentication and authorization | OIDC login, expired/invalid JWT, route authorization, issuer alignment | Not verified; gateway-only security is a concern |
| Price integrity | Server-side catalog lookup and tamper-resistant total calculation | Not covered; client price is accepted |
| Quantity boundaries | Zero, negative, decimal coercion, very large values, missing nested data | Not covered; no positive/minimum constraints found |
| Stock consistency | Concurrent orders, reservation/decrement, release after failure | Not covered; endpoint is read-only |
| Order/event atomicity | Database failure, Kafka failure, retry, duplicate delivery, outbox recovery | Not covered |
| Notification safety | HTML escaping, SMTP failure, replay, dead-letter handling | Not covered |
| API contracts | Validation error schema, 401/403/404/409/422/5xx behavior | Narrow happy-path coverage only |
| Kubernetes | Manifest validation, startup/readiness, service DNS, secrets, persistence | Not covered |
| Frontend journey | Login, catalog, create product, order success/failure, keyboard/accessibility | Not covered |
| Observability | Scrape, logs, traces, dashboards, alerts | Not covered |

## Release gate status

**[Blocked] No defensible release gate is satisfied by repository evidence.** A future gate should require at least:

- Reproducible dependency installation and builds for all five backend applications and the supported frontend.
- Unit, integration, contract, and end-to-end tests for the risk matrix.
- Numeric coverage generated by CI with an agreed threshold.
- Security and secret scanning with actionable findings resolved or accepted.
- Validated Compose and Kubernetes manifests.
- Image provenance and immutable version tags.
- Operational evidence for health, metrics, logs, traces, rollback, and restore.

## Test execution limits

This documentation pass deliberately did not run Maven, npm, Angular CLI, Docker, Kind, Kubernetes, network calls, or external services. Any maintainer following the commands in [Development](development.md) must record the environment, commit, command, result, and artifact location rather than converting a local result into a general production claim.
