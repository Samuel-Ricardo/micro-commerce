# Services and API reference

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Status:** source-level reference; endpoints were not exercised during this pass. No pre-existing project-local docs directory was found before this pass.

## API conventions

The application controllers use `/api/v1` prefixes. The gateway forwards product, order, and inventory requests to configured target URLs. The gateway also exposes aggregate OpenAPI document routes. Springdoc/API-doc support is evidenced for the gateway, product, order, and inventory applications only; notification has no REST/Springdoc evidence. Exact runtime status behavior can differ when validation, security, serialization, dependency failures, or gateway filters intervene.

## Gateway routes

`api-gateway/src/main/java/com/js/gateway/infra/Router.java` defines the following route predicates. The route rows below identify circuit-breaker filters; no gateway retry filter is evidenced. Retry configuration elsewhere is not treated as proof that a route retries:

| Incoming path | Target configuration | Gateway behavior |
|---|---|---|
| `/api/v1/products` | `services.url.product` | HTTP forwarding with product circuit-breaker filter; no route retry evidenced |
| `/api/v1/orders` | `services.url.order` | HTTP forwarding with order circuit-breaker filter; no route retry evidenced |
| `/api/v1/inventory` | `services.url.inventory` | HTTP forwarding with inventory circuit-breaker filter; no route retry evidenced |
| `/aggregate/product-service/v3/api-docs` | `services.url.product` | HTTP forwarding with a `setPath("/api-docs")` filter; no route retry evidenced |
| `/aggregate/order-service/v3/api-docs` | `services.url.order` | HTTP forwarding with a `setPath("/api-docs")` filter; no route retry evidenced |
| `/aggregate/inventory-service/v3/api-docs` | `services.url.inventory` | HTTP forwarding with a `setPath("/api-docs")` filter; no route retry evidenced |
| `GET /fallback` | Gateway-local | Source-declared `503 Service Unavailable` response with a plain-text body; runtime behavior is unverified |

`SecurityConfig` permits the documentation and aggregate paths, plus `/actuator/prometheus`, without authentication. Its source configuration declares that other gateway requests require a valid JWT according to the configured issuer URI. The repository does not prove whether every downstream service is unreachable directly in a deployed environment.

The gateway circuit-breaker filters point to `forward:/fallback`, while the local fallback route is declared only for `GET /fallback`. This page does not infer fallback handling for every original request method; fallback invocation and response behavior remain runtime-unverified.

Springdoc/API-doc support is evidenced only where dependencies, controllers, and configuration support it: gateway, product, order, and inventory expose or aggregate the documented `/api-docs` paths. Notification has no REST controller, Springdoc dependency, or Springdoc configuration in the inspected evidence; describe it as event-driven rather than as an API-doc service.

## Product service

**Controller:** `product-service/product-service/src/main/java/com/js/microservices/product_service/application/controller/ProductController.java`  
**Base path:** `/api/v1/products`

### `POST /api/v1/products`

Creates a product. The controller applies `@Valid` to `CreateProductDTO` and returns HTTP `201 Created`.

**Request body:**

| Field | Type | Required by source | Notes |
|---|---|---|---|
| `name` | string | Yes | `@NotBlank` |
| `description` | string | Yes | `@NotBlank` |
| `skuCode` | string | Yes | `@NotBlank` |
| `price` | decimal | Yes | `@NotNull`; no positive/minimum constraint found |

**Response shape:** `ProductCreatedDTO` contains `id`, `name`, `description`, `skuCode`, and `price`.

### `GET /api/v1/products`

Returns HTTP `200 OK` and a list of `ProductCreatedDTO` values. The repository call is `ProductRepository.findAll()`; pagination, sorting, filtering, and a maximum result size were not found.

**Persistence:** `ProductModel` is a MongoDB document with collection name `products` and fields `id`, `name`, `description`, `skuCode`, and `price`.

**Missing/unknown behavior:** no controller-level exception handler was found in the inspected product service source. Validation error serialization is therefore not documented as a stable public contract.

## Order service

**Controller:** `order-service/src/main/java/com/js/microservices/order_service/application/controller/OrderController.java`  
**Base path:** `/api/v1/orders`

### `POST /api/v1/orders`

Places one order and returns HTTP `201 Created` with text in the form `Order {orderNumber} has been placed with success`.

**Request body:**

| Field | Type | Required by source | Notes |
|---|---|---|---|
| `skuCode` | string | Yes | `@NotBlank` |
| `price` | decimal | Yes | `@NotNull`; supplied by client and used in total calculation |
| `quantity` | integer | Yes | `@NotNull`; no positive/minimum constraint found |
| `userDetails` | object | Yes | Nested object |

`userDetails` is `UserDetailsDTO`:

The outer `PlaceOrderDTO.userDetails` property has `@NotNull`. The nested `UserDetailsDTO` fields declare `@NotBlank`/`@Email`, but no `@Valid` cascade annotation was evidenced on the nested property. Treat those nested constraints as declarations, not as proven request-enforcement behavior.

| Field | Type | Required by source | Notes |
|---|---|---|---|
| `email` | string | Yes | `@NotBlank` and `@Email` |
| `firstName` | string | Yes | `@NotBlank` |
| `lastName` | string | Yes | `@NotBlank` |

**Static processing order:**

1. `HaveStockUseCaseImpl` calls the inventory gateway.
2. A false result raises `InsufficientStockException` with HTTP `422 Unprocessable Entity`.
3. `CreateOrderUseCaseImpl` generates an ID and order number and calculates `price × quantity`.
4. `SaveOrderUseCaseImpl` persists the order.
5. `ProduceOrderPlacedEventUseCaseImpl` builds and sends an Avro event.

**Order-to-inventory resilience:** `InventoryGateway` declares a circuit breaker and a separate `@Retry` annotation on the inventory call, with a fallback that returns `false`. These declarations belong to the order-service inventory client. They are separate from the gateway's circuit-breaker filters; no gateway retry filter is evidenced, and gateway retry configuration alone is not treated as route behavior.

**Error response:** `AppErrorHandler` handles `AppError` and uses Spring `ErrorResponse`. The inspected code does not establish a versioned error JSON schema. Validation errors and infrastructure exceptions are not fully handled by this advice.

**Persistence:** `OrderModel` maps to PostgreSQL table `t_orders` with `id`, `orderNumber`, `skuCode`, `price`, and `quantity`. The migration declares `price` and `quantity` as non-null but leaves `order_number` and `sku_code` nullable.

## Inventory service

**Controller:** `inventory-service/inventory-service/src/main/java/com/js/microservices/inventory_service/application/controller/InventoryController.java`  
**Base path:** `/api/v1/inventory`

### `GET /api/v1/inventory?skuCode={skuCode}&quantity={quantity}`

Returns HTTP `200 OK` with a JSON boolean. `true` means that `existsBySkuCodeAndQuantityIsGreaterThanEqual` found a matching row. `false` means no such row was found. The order service has a separate inventory-client fallback that returns `false` on its declared dependency-failure path; that fallback is distinct from the gateway's `GET /fallback` route. Runtime behavior for either path is unverified.

**Query parameters:**

| Parameter | Type | Required by binding | Notes |
|---|---|---|---|
| `skuCode` | string | Yes | Wrapped in `SkuCode` |
| `quantity` | integer | Yes | `@NotNull` exists on the DTO, but controller parameter validation is not declared |

**Persistence:** `InventoryModel` maps to `t_inventory` with `id`, `skuCode`, and `quantity`. The source contains no endpoint for reservation, decrement, release, or adjustment.

## Notification service and event contract

**Consumer:** `notification-service/src/main/java/com/js/microservices/notification/application/consumer/kafka/KafkaOrderConsumer.java`  
**Topic:** `${kafka.topic.order.placed}`  
**Listener:** `@KafkaListener(topics = "${kafka.topic.order.placed}")`

The event schema is duplicated in:

- `order-service/src/main/resources/avro/order-placed.avsc`
- `notification-service/src/main/resources/avro/order-placed.avsc`

Both declare the Avro record `OrderPlacedEvent` in namespace `com.js.microservices.order.event`:

| Field | Avro type | Consumer use |
|---|---|---|
| `orderNumber` | string | Email subject and body |
| `email` | string | Mail recipient |
| `firstName` | string | Mail body |
| `lastName` | string | Mail body |

The generated Java class under each service's source tree is marked autogenerated and must not be edited directly. The Maven Avro plugin generates sources from the schema during `generate-sources`; the repository currently contains generated classes as well.

### Mail behavior

`SendOrderPlacedMailNotificationUseCaseImpl` creates a subject and delegates an HTML body to `MailGatewayImpl`. `FormatMailBodyUseCaseImpl` inserts first name, last name, and order number into an HTML template. The body is sent with `MimeMessageHelper` as HTML.

**Static security note:** the inspected formatter does not HTML-escape event values before interpolation. This is documented as a code risk in [Security](security.md), not as evidence of exploitation.

## OpenAPI paths

Springdoc/API-doc support is evidenced for the gateway, product, order, and inventory applications through their dependencies, controller/configuration paths, and gateway aggregation. The gateway aggregates the three HTTP API services under `/aggregate/.../v3/api-docs`. Notification has no REST controller, Springdoc dependency, or Springdoc configuration in the inspected evidence; it has no gateway API-doc route and is event-driven here.

## Frontend request models

`frontend/java-shop/src/app/application/service/product-service.service.ts` calls the configured product URL for `GET` and `POST`. `order-service.service.ts` posts an `Order` and expects text. `environment.development.ts` and `environment.prod.ts` route all three service URLs through the gateway in their respective declared environments.

The primary UI builds an order from the selected product's `skuCode` and `price`, a user profile from OIDC claims, and a numeric quantity from an input. The backend therefore receives a client-supplied price and client-supplied user details in the source-level request flow; the server-side source does not replace them with authoritative product or token values. The UI flow was not exercised.

## API inventory gaps

- No product update/delete endpoint was found.
- No order retrieval, cancellation, status, payment, or shipment endpoint was found.
- No inventory mutation endpoint was found.
- No notification management endpoint was found.
- Pagination, rate limiting, request correlation IDs, a stable problem-details schema, and API version lifecycle policy are not established by the inspected source.
