# Configuration reference

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Secret policy:** this page names configuration keys and paths only. It intentionally omits all secret values. No pre-existing project-local docs directory was found before this pass.

## Configuration sources

| Source | Intended ownership | Notes |
|---|---|---|
| `src/main/resources/application.yml` | Service runtime defaults and placeholders | Spring property names are authoritative for the application process. |
| `api-gateway/src/main/resources/local_application.yml` | Gateway local preview | Contains fixed local URLs and a local issuer URI. |
| Root `.env.*.example` files | Compose variable names and example topology | Several examples use insecure development defaults; values are not repeated here. |
| Service `.env` files | Local service Compose environment | Ignored or sensitive; inspect paths locally without publishing values. |
| `k8s/manifest/**/configmap*.yml` | Kubernetes non-secret configuration | Some keys do not match Spring property binding or application expectations. |
| `k8s/manifest/**/secrets*.yml` | Kubernetes secret declarations | Some checked-in manifests contain encoded/default data and should be treated as exposed. |
| `frontend/*/src/environments/` | Compile-time browser endpoints and OIDC settings | These values are shipped to the browser and are not secret storage. |

## Backend ownership matrix

| Application | Port declaration | Data/integration variables | Telemetry variables |
|---|---|---|---|
| Gateway | `server.port: 9000` in `application.yml`; environment/topology mappings can differ | `PRODUCT_SERVICE_URL`, `ORDER_SERVICE_URL`, `INVENTORY_SERVICE_URL`, `KEYCLOAK_ISSUER_URI`, `CORS_ALLOWED_ORIGINS_FRONTEND` | `LOKI_URL`, `ZIPKIN_URL`, circuit-breaker and actuator variables |
| Product | `SERVER_PORT` placeholder; Dockerfile/root Compose use `8080` | `MONGODB_URI` | `ZIPKIN_URL`, `LOKI_URL` |
| Order | `server.port: 8081` in `application.yml`; `.env.order` and `.env.order.example` set `SERVER_PORT=8080`; service-level Compose files map `8081:8080` | `INVENTORY_SERVICE_URL`, Kafka producer/topic/Schema Registry variables, datasource settings | `ZIPKIN_URL`, `LOKI_URL`, order-specific resilience and actuator variables |
| Inventory | `server.port: 8082` in `application.yml`; root Compose maps host `8082` to container `8080`; `.env.gateway` and its example set `SERVER_PORT=8080` for shared Compose services | `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | `ZIPKIN_URL`, `LOKI_URL` |
| Notification | `NOTIFICATION_SERVICE_PORT`; Dockerfile/root Compose use `8083` | Kafka consumer/topic/Schema Registry variables, `NOTIFICATION_EMAIL`, `NOTIFICATION_MAIL_HOST`, `NOTIFICATION_MAIL_PORT`, `NOTIFICATION_MAIL_USERNAME`, `NOTIFICATION_MAIL_PASSWORD` | `ZIPKIN_URL`, `LOKI_URL` |

The application files use Spring placeholders such as `${MONGODB_URI}` and `${KAFKA_BOOTSTRAP_SERVERS}`. A placeholder being declared does not prove that the corresponding value is supplied in every topology. Environment overrides can change the effective port or endpoint; the root Compose, service-level Compose, and Kubernetes declarations are not interchangeable runbooks.

## Gateway variables

`api-gateway/src/main/resources/application.yml` consumes:

- `CORS_ALLOWED_ORIGINS_FRONTEND`
- `KEYCLOAK_ISSUER_URI`
- `PRODUCT_SERVICE_URL`
- `ORDER_SERVICE_URL`
- `INVENTORY_SERVICE_URL`
- `API_DOCS_PATH`
- `API_DOCS_UI_PATH`
- `API_DOCS_URLS_PRODUCT_SERVICE`
- `API_DOCS_URLS_ORDER_SERVICE`
- `API_DOCS_URLS_INVENTORY_SERVICE`
- `CIRCUIT_BREAKERS_ENABLED`
- `ENDPOINTS_WEB_EXPOSURE_INCLUDE`
- `ENDPOINT_HEALTH_SHOW_DETAILS`
- Resilience4j variables for gateway circuit-breaker and timeout configuration; retry properties are declarations only and do not prove that routes retry
- `ZIPKIN_URL` and `LOKI_URL`

The local gateway file hardcodes a Keycloak issuer and fixed service URLs. It also exposes all actuator endpoints and health details in its local profile. That is suitable only as a development declaration and is not a production security recommendation.

## Product, order, and inventory configuration

### Product

`product-service/product-service/src/main/resources/application.yml` expects `SERVER_PORT`, `MONGODB_URI`, `ZIPKIN_URL`, and `LOKI_URL`. The repository's root Compose file supplies the product container with `.env.gateway`, which is a cross-service ownership shortcut and a configuration-drift risk.

### Order

`order-service/src/main/resources/application.yml` expects:

- `INVENTORY_SERVICE_URL`
- `KAFKA_TOPIC_ORDER_PLACED`
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_TEMPLATE_DEFAULT_TOPIC`
- `KAFKA_PRODUCER_KEY_SERIALIZER`
- `KAFKA_PRODUCER_VALUE_SERIALIZER`
- `KAFKA_PRODUCER_SCHEMA_REGISTRY_URL`
- `ORDERS_CIRCUIT_BREAKERS_ENABLED`
- `ORDERS_ENDPOINTS_WEB_EXPOSURE_INCLUDE`
- `ORDERS_ENDPOINT_HEALTH_SHOW_DETAILS`
- The `ORDERS_*` resilience variables, including the repository's misspelled `ORDERS_NAX_ATTEMPTS` key.
- `ZIPKIN_URL` and `LOKI_URL`

The same file hardcodes a PostgreSQL JDBC URL and database credentials. This is a **[Verified] secret/configuration hygiene risk** and should be replaced by externally supplied values before any shared or production use.

The order service's `resilience4j.circuitbreaker.instances.inventory` and `resilience4j.retry.instances.inventory` settings apply to the annotated inventory client call. They are separate from the gateway's route-level circuit-breaker filters. Gateway retry properties are declarations only; no gateway retry filter is evidenced.

### Inventory

`inventory-service/inventory-service/src/main/resources/application.yml` expects `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `ZIPKIN_URL`, and `LOKI_URL`. Its database name must be reconciled with Compose and Kubernetes declarations, which use multiple spellings and names. The application and Dockerfile declare port `8082`, and Kubernetes inventory `targetPort: 8082` matches them; root Compose instead maps host `8082` to container `8080`, creating cross-topology drift.

### Notification

`notification-service/src/main/resources/application.yml` expects the Kafka consumer and mail variables listed in the matrix above. The mail username/password and SMTP host are operational secrets or sensitive connection settings; use an approved secret store or injected environment values.

## Messaging configuration

The root Kafka example declares:

- Zookeeper client settings.
- Kafka broker ID and listener/security protocol mapping.
- Internal and host advertised listeners.
- Schema Registry host/listener/bootstrap settings.
- Kafka UI cluster settings.
- The topic name is supplied to producer and consumer services through `KAFKA_TOPIC_ORDER_PLACED`.

Compose uses `broker:9092` for application services while the example also distinguishes `broker:29092` as an internal listener. Kubernetes `common-config` uses `broker.default.svc.cluster.local:29092`. These endpoints must be tested and reconciled as a single topology before deployment.

## Authentication configuration

The primary Angular app uses:

- OIDC authority from `environment.AUTH.AUTHORITY`.
- Client ID from `environment.AUTH.CLIENT.ID`.
- Authorization code flow.
- `openid profile offline_access` scope.
- Silent renew and refresh-token usage.

The `java-shop` development environment points the browser at the local Keycloak realm and gateway DNS name. The production environment example points services at `localhost:9000` and the issuer at `host.docker.internal`. These are environment-specific declarations, not production identity guidance.

The SSR scaffold's `auth.config.ts` still contains empty/placeholder authority, client ID, and scope values. It is not equivalent to the primary UI's authentication configuration.

## Observability configuration

All five backend applications configure a Loki logback appender using `LOKI_URL`. They declare Zipkin trace reporting through `ZIPKIN_URL` or equivalent Spring management properties. Prometheus scrapes `/actuator/prometheus` using static targets in `docker/prometheus/prometheus.yml` and Kubernetes DNS targets in the Prometheus ConfigMap.

The local examples expose actuator endpoints broadly and show health details. This increases diagnostic visibility but also increases exposure. Restrict management endpoints and health detail access outside isolated development environments.

## Compose/Kubernetes drift matrix

| Concern | Compose evidence | Kubernetes evidence | Impact |
|---|---|---|---|
| Image ownership | Root Compose uses `samuelrcabral/*:latest` | Application manifests use `saiupadhyayula007/new-*` and `latest` | Images may not represent the same build; rollback is ambiguous. |
| Gateway port | Root mapping is host `9000` → container `8080`; gateway app and Dockerfile declare `9000`; `.env.gateway` supplies `SERVER_PORT=8080` | Kubernetes deployment/service use `9000` | App listener, environment override, image declaration, and mapping conflict; reconcile before use. |
| Order port | Root mapping is `8081:8081`; app/Dockerfile declare `8081` | Kubernetes service uses `8081`; `.env.order` examples and other Compose files use `8080` inside the container | Effective port depends on the selected environment; declarations conflict. |
| Inventory port | Root mapping is host `8082` → container `8080` | App/Dockerfile and Kubernetes Service target `8082` | Kubernetes matches the app/Dockerfile; root Compose is the cross-topology drift. |
| Product database | MongoDB URI in `.env.gateway.example` | Kubernetes ConfigMap includes a different MongoDB credential pattern | Secret and endpoint ownership is inconsistent. |
| Order database | App YAML points to PostgreSQL; root Compose provides PostgreSQL | Kubernetes order ConfigMap points to MySQL | Kubernetes order path conflicts with application intent. |
| Inventory database | App YAML expects MySQL variables | Kubernetes ConfigMap declares a JDBC URL only | Required host/user/password bindings may be absent. |
| Keycloak realm | Local app uses `java-shop-microservices-security-realm` | Gateway ConfigMap uses `spring-microservices-security-realm` | Issuer mismatch can prevent JWT validation. |
| Secret keys | Manifests use different key names and data structures | Some consumers reference keys absent from Secrets | Startup or credential injection may fail. |
| Observability | Compose uses host mappings and local files | Kubernetes uses service DNS, but Loki config mount is not evident | Telemetry reachability is unverified. |

**Port interpretation:** `9000:8080` means host port `9000` forwards to container port `8080`; it does not establish that the gateway process listens on `8080`. Conversely, a Kubernetes `targetPort: 8082` selects a pod listener on `8082`. Environment overrides such as `SERVER_PORT=8080` can alter the process listener, so these topologies must be reconciled independently.

## Secret hygiene rules

- Never publish `.env`, service environment files, realm exports, Kubernetes Secret data, or copied credential values.
- Example files are not automatically safe: some contain default passwords, wildcard origins, administrative roles, or connection strings with credentials.
- Kubernetes `data` values are encoded, not encrypted merely because they are base64-looking. Replace checked-in defaults with externally managed secrets.
- Treat database, SMTP, Keycloak administrator, Grafana, and Kafka credentials as secrets.
- Avoid logging tokens, full user data, passwords, or mail addresses. Review the current logging statements before enabling verbose logging.
- Document variable names and paths, not values.
