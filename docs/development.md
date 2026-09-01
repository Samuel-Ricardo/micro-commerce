# Development guide

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Execution status:** commands below are repository-evidenced commands, but they were not run during this documentation pass. No pre-existing project-local docs directory was found before this pass.

## Before you start

The repository declares or uses the following tooling. The backend count is five Spring Boot applications total: gateway, product, order, inventory, and notification.

- Java 21 in every backend POM.
- Spring Boot 3.4.1 in every backend parent POM.
- Kotlin 2.1.0 in product, order, inventory, and notification services.
- Maven Wrapper files in each backend service directory.
- Angular CLI 17.3.11 declarations in both frontend `package.json` files.
- TypeScript 5.4.2 declarations in both frontend `package.json` files.
- Node 20 Alpine base images in both frontend Dockerfiles.
- Docker for Compose, Dockerfiles, Testcontainers, and Kind workflows.
- A Docker-capable environment is required for the repository's Testcontainers integration tests.

**[Blocked] Runtime readiness:** no clean-machine setup, build, test, or container execution was performed for this guide. Existing generated reports record failures for some historical test runs; see [Testing and quality](testing-and-quality.md).

## Repository layout

```text
.
├── api-gateway/                         Spring Cloud Gateway Server MVC app
├── product-service/product-service/     Product catalog and MongoDB app
├── order-service/                       Order, PostgreSQL, Kafka/Avro app
├── inventory-service/inventory-service/ Inventory availability and MySQL app
├── notification-service/                Kafka consumer and SMTP app
├── frontend/java-shop/                  Functional Angular browser app
├── frontend/java-shop-website/          Angular SSR scaffold/placeholder
├── docker-compose.yaml                  Root local topology declaration
├── docker/                              Local database and telemetry configuration
├── k8s/                                 Kind scripts and Kubernetes manifests
├── .env.*.example                       Example variable declarations
├── .github/                             Partial CI/CD and Dependabot configuration
└── README.md, docs/                     Project documentation
```

Generated or secondary files remain in place:

- `HELP.md` files are Spring Initializr-style generated references; they are ignored/untracked and optional, and absent from a clean clone unless regenerated.
- Avro Java classes are generated and marked `DO NOT EDIT DIRECTLY`.
- `target/` contains ignored/untracked historical build/test artifacts where present in the inspected working tree; they are not durable source or CI evidence of a current successful build.
- The frontend READMEs remain frontend-owned and are not replaced by this guide.

## Backend commands evidenced by repository artifacts

Each backend has a Maven Wrapper. The workflows and generated help files evidence these command forms:

```text
./mvnw test
./mvnw clean package -DskipTests
./mvnw spotless:apply
./mvnw spotless:check
```

Use the wrapper from the service directory that contains the intended `pom.xml`. The inventory workflow references `./inventory-service/inventory-service`; the order workflow references `./order-service`. Their workflow files place `cd` in standalone steps, which does not persist to later steps; this is a static workflow defect/risk. Other service-specific working-directory behavior is not consistently configured in CI, so do not assume a root multi-module build or a functioning quality gate exists.

The Dockerfiles independently evidence:

```text
mvn clean package -DskipTests
java -jar app.jar
```

These are image build/start instructions, not proof that the images build or start successfully.

## Frontend commands evidenced by `package.json`

### Functional UI: `frontend/java-shop`

```text
npm install
npm start
npm run build
npm run build:dev
npm run build:prod
npm run watch
npm test
```

`npm start` maps to `ng serve`; `npm test` maps to `ng test`. `angular.json` sets development as the serve default and production as the build default.

### SSR scaffold: `frontend/java-shop-website`

```text
npm install
npm start
npm run start:ssr
npm run build
npm run watch
npm test
npm run serve:ssr:java-shop-website
```

`start:ssr` is declared as a build followed by `serve:ssr:java-shop-website`; the latter runs `node dist/java-shop-website/server/server.mjs`. The scaffold's `angular.json` enables server output and prerendering. Its routes array is empty and its root component remains Angular's generated placeholder.

## Local Compose paths

The root `docker-compose.yaml` declares a broad topology including both frontend/API services and supporting infrastructure. It uses environment files such as `.env.gateway`, `.env.order`, `.env.notification`, `.env.kafka`, `.env.keycloak`, and `.env.observability`. The files are ignored or contain defaults in example form; do not copy values into tickets or documentation. Its host/container port mappings are not interchangeable with service-level Compose or Kubernetes.

A safe preview workflow is:

1. Review the root Compose file and the relevant `.env.*.example` files.
2. Provide local values through ignored environment files or an external secret mechanism.
3. Reconcile service ports and URLs against [Configuration](configuration.md).
4. Treat the topology as **[Unverified]** until a maintainer runs it in an approved environment.

No exact root Compose start command is documented in a pre-existing project README. Therefore this documentation does not claim that a particular Compose invocation is the canonical supported command.

## Kubernetes/Kind path

The repository contains these scripts under `k8s/kind/`:

- `create-kind-cluster.sh` runs `kind create cluster --name java-shop --config kind-config.yaml`, then invokes `apply_all.sh`.
- `apply_all.sh` runs `kubectl apply -f ../manifest --recursive`.
- `delete-kind-cluster.sh` exists as a cleanup script; its behavior should be reviewed before use.
- `kind-load.sh` exists, while image-loading lines in `create-kind-cluster.sh` are commented out.

The manifests are therefore a declared preview path, not a verified deployment. Several manifests use images, environment keys, ports, and fields inconsistent with application configuration. See [Operations](operations.md) and [Configuration](configuration.md).

## Configuration files to inspect first

| Concern | Primary files |
|---|---|
| Gateway routes/security | `api-gateway/src/main/java/com/js/gateway/infra/Router.java`, `SecurityConfig.java`, `src/main/resources/application.yml` |
| Product service | `product-service/product-service/pom.xml`, `src/main/resources/application.yml`, `src/main/.../ProductController.java` |
| Order service | `order-service/pom.xml`, `src/main/resources/application.yml`, `src/main/resources/avro/order-placed.avsc` |
| Inventory | `inventory-service/inventory-service/pom.xml`, `src/main/resources/application.yml`, Flyway migrations |
| Notification | `notification-service/pom.xml`, `src/main/resources/application.yml`, Kafka consumer and mail gateway |
| Functional frontend | `frontend/java-shop/package.json`, `angular.json`, `src/environments/`, `src/app/` |
| SSR scaffold | `frontend/java-shop-website/package.json`, `angular.json`, `server.ts`, `src/app/` |
| Local topology | `docker-compose.yaml`, `docker/`, `.env.*.example` |
| Kubernetes preview | `k8s/kind/`, `k8s/manifest/` |

## Troubleshooting pointers

- **Missing environment placeholder:** inspect the service `application.yml`, its `logback-spring.xml`, and the matching `.env.*.example`. Historical product and order reports show failures caused by missing or malformed Loki configuration.
- **Docker unavailable:** product, inventory, and order tests use Testcontainers or Docker-backed infrastructure. Historical inventory output explicitly reports that no valid Docker environment was found.
- **Wrong internal port:** compare the Dockerfile `EXPOSE`, Spring `server.port`, environment overrides, Compose port mapping, and Kubernetes `targetPort`. In particular, root Compose maps inventory host `8082` to container `8080`, while the inventory app/Dockerfile/Kubernetes target use `8082`; gateway and order also have `9000`/`8080` and `8081`/`8080` alternatives. These values are not uniform, and the topologies are not interchangeable runbooks.
- **OIDC redirect/issuer mismatch:** compare `frontend/java-shop/src/environments/`, gateway issuer configuration, Keycloak realm name, and frontend client settings. The SSR scaffold still contains placeholder OIDC values.
- **Kafka connection or Avro mismatch:** compare topic, bootstrap server, serializer/deserializer, Schema Registry URL, and the two schema files.
- **Kubernetes startup issue:** inspect ConfigMap keys, Secret keys, image names, manifest field spelling, and dependency order before attempting a deployment.

## Contribution rules for this repository

- Keep changes scoped to the intended service or frontend application.
- Treat environment and realm exports as sensitive configuration; never add live credentials.
- Update the relevant documentation page when a route, environment variable, event schema, port, or deployment declaration changes.
- Do not edit generated Avro classes directly.
- Preserve the distinction between source evidence and runtime verification in pull requests.
- Treat `.github/workflows/inventory-service/ci.yml` as a declaration only: its `steps` block is not nested under the job, and its standalone directory-change step does not persist. The analogous order workflow has the same directory-scope risk.
