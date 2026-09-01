# Operations and deployment

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Operational status:** declarations reviewed; no deployment or runtime operation was performed. No pre-existing project-local docs directory was found before this pass.

## Topology options

The repository contains two main topology declarations. Together they describe five backend Spring Boot applications total: gateway plus product, order, inventory, and notification.

1. **Root Compose:** `docker-compose.yaml` declares frontend, Keycloak and its MySQL database, gateway, four domain services (product, order, inventory, and notification), Kafka/Zookeeper/Schema Registry/Kafka UI, Loki, Prometheus, Tempo, Grafana, MongoDB/Mongo Express, PostgreSQL/pgAdmin, and MySQL/phpMyAdmin.
2. **Kind/Kubernetes preview:** `k8s/kind/` creates a Kind cluster and recursively applies `k8s/manifest/`.

The service-level Compose files are additional, overlapping topologies. They do not form a single canonical deployment contract and contain inconsistent ports, images, environment files, and dependency sets. Do not combine a mapping or environment assumption from one topology with a runbook for another.

## Declared local ports

Port notation below is `host:container`. It describes forwarding, not the port on which the application process necessarily listens.

The following table summarizes root Compose mappings. It is a declaration inventory, not proof that all listeners start.

| Component | Host port | Container port | Source |
|---|---:|---:|---|
| Functional frontend | 4200 | 80 | `docker-compose.yaml` |
| Keycloak | 8181 | 8080 | `docker-compose.yaml` |
| API gateway | 9000 | 8080 | `docker-compose.yaml` |
| Product service | 8080 | 8080 | `docker-compose.yaml` |
| Order service | 8081 | 8081 | `docker-compose.yaml` |
| Inventory service | 8082 | 8080 | `docker-compose.yaml` |
| Notification service | 8083 | 8083 | `docker-compose.yaml` |
| Zookeeper | 2181 | 2181 | `docker-compose.yaml` |
| Kafka broker | 9092, 29092 | same | `docker-compose.yaml` |
| Schema Registry | 9191 | 8081 | `docker-compose.yaml` |
| Kafka UI | 7070 | 8080 | `docker-compose.yaml` |
| Loki | 3100 | 3100 | `docker-compose.yaml` |
| Prometheus | 9090 | 9090 | `docker-compose.yaml` |
| Tempo | 3110, 9411 | 3100, 9411 | `docker-compose.yaml` |
| Grafana | 3000 | 3000 | `docker-compose.yaml` |
| MongoDB | 27017 | 27017 | `docker-compose.yaml` |
| Mongo Express | 9081 | 8081 | `docker-compose.yaml` |
| PostgreSQL | 5432 | 5432 | `docker-compose.yaml` |
| pgAdmin | 5050 | 80 | `docker-compose.yaml` |
| MySQL | 3306 | 3306 | `docker-compose.yaml` |
| phpMyAdmin | 9080 | 80 | `docker-compose.yaml` |

**Port risks:** the application `server.port` values, environment overrides, Dockerfile `EXPOSE` values, root Compose mappings, service-level Compose mappings, and Kubernetes targets do not agree consistently. Root Compose maps gateway host `9000` to container `8080`, while the gateway app/Dockerfile declare `9000`; it maps inventory host `8082` to container `8080`, while the inventory app/Dockerfile and Kubernetes target use `8082`. Order is declared as `8081` by the app/Dockerfile and root mapping, but `.env.order` and `.env.order.example` set `SERVER_PORT=8080`, and `8081:8080` appears in other Compose files. Reconcile before using any port as a runbook guarantee.

## Images and provenance

Backend Dockerfiles use Maven 3.9.5 with Eclipse Temurin 21 builder stages and Temurin 21 JRE runtime stages. Frontend Dockerfiles use Node 20 Alpine and Nginx or Node SSR runtime stages.

Root Compose uses `samuelrcabral/*:latest` images. Kubernetes application manifests use `saiupadhyayula007/*:latest` image names. CI workflows build/push mutable `latest` tags for at least order and inventory. This makes it impossible to infer an exact artifact from a tag alone.

**Required before shared deployment:** immutable image digests or release tags, SBOM/provenance, vulnerability scanning, and a documented promotion path.

## Dependency and startup ordering

Compose uses `depends_on` for several databases, gateway dependencies, and Kafka components. `depends_on` expresses startup ordering but does not prove application readiness. The inspected Compose files contain no healthcheck definitions.

Kubernetes manifests define Deployments and Services for application and infrastructure components, but the inspected manifests contain no liveness or readiness probes. A pod can be considered started before its database, broker, Schema Registry, identity provider, or SMTP dependency is usable.

## Observability

### Metrics

Backend POMs include Actuator and Prometheus registry dependencies. Prometheus configurations scrape `/actuator/prometheus` for the gateway and four services using static targets. Kubernetes uses service DNS names in `k8s/manifest/infraestructure/observability/prometheus/configmap.yml`.

### Logs

Each backend `logback-spring.xml` configures a Loki4j appender using `loki.url`. Root Compose supplies Loki and the service examples declare host-based Loki URLs. The Kubernetes manifests deploy Loki but do not visibly mount a Loki configuration file even though the deployment passes `/etc/loki/local-config.yaml` as an argument.

### Traces

The services declare Micrometer tracing, Brave, and Zipkin reporting. Tempo is configured with a Zipkin receiver in `docker/tempo/tempo.yml` and has Kubernetes ports for Tempo and Zipkin. Runtime trace delivery and correlation are not verified.

### Dashboards

Grafana provisions Prometheus, Tempo, and Loki datasource definitions and includes a dashboard JSON. The root observability environment example enables anonymous access with an administrative role. Treat this as a local-only declaration until secured.

## Kubernetes evidence and blockers

The Kind script's apply path is:

```text
kubectl apply -f ../manifest --recursive
```

The following blockers are visible in manifests and must be resolved before claiming a working Kubernetes deployment:

- Application images differ from root Compose images and use mutable `latest` tags.
- Gateway ConfigMap realm name differs from the functional local realm name.
- Order Kubernetes ConfigMap points to a MySQL JDBC URL while the application declares PostgreSQL and uses PostgreSQL Flyway dependencies.
- Root Compose inventory mapping (`8082:8080`) differs from the inventory app/Dockerfile and Kubernetes Service target (`8082`); this is cross-topology drift, not a single Kubernetes conflict.
- Product, order, inventory, and notification Deployments inject only subsets of the environment variables required by their `application.yml` files.
- Some Kubernetes env names use hyphens in keys such as `SPRING_KAFKA_BOOTSTRAP-SERVERS`; reconcile with actual Spring relaxed binding and ConfigMap key references.
- Secret manifests use inconsistent key names, and some values are encoded/default data.
- PostgreSQL service has `metada` instead of `metadata`.
- MongoDB deployment uses nonstandard field spellings such as `mounthostPath` and `persitentVolumeClaim`.
- MongoDB service declares `apiVersion: 1` rather than the usual core API version form used elsewhere.
- PostgreSQL deployment indentation places the pod `spec` outside the template structure.
- Tempo, Loki, and persistence declarations require manifest validation and storage review.
- No probes, resource requests/limits, network policies, pod disruption policies, or documented namespace strategy were found in the inspected application manifests.

These are static findings. No `kubectl` validation was run.

## Safe operator checklist

Before any approved local or shared deployment:

- [ ] Confirm the exact commit and image digest/tag.
- [ ] Supply secrets through an approved mechanism; do not use checked-in defaults.
- [ ] Confirm every service port across application config, image, Compose, and Service manifests.
- [ ] Confirm database schema and database name for each service.
- [ ] Confirm the Keycloak realm, issuer, client redirect URI, and allowed origins.
- [ ] Confirm Kafka listeners, bootstrap servers, topic, serializers, and Schema Registry URL.
- [ ] Validate manifests and ConfigMap/Secret key references.
- [ ] Confirm persistent volumes, ownership, capacity, backup, and restore procedure.
- [ ] Confirm readiness/liveness probes and dependency startup behavior.
- [ ] Restrict management, database, administrative UI, and broker ports.
- [ ] Verify metrics, logs, traces, dashboards, alerts, and retention.
- [ ] Exercise a safe catalog/order/notification smoke test in an isolated environment.
- [ ] Record rollback and incident contacts before exposing the system.

## Startup, shutdown, rollback, and recovery gaps

No repository runbook defines:

- A canonical startup command and expected readiness conditions.
- Graceful shutdown or event-drain behavior.
- Rollback procedure for application images, schemas, or Kubernetes manifests.
- Database backup schedule, encryption, retention, restore test, or data-loss objectives.
- Kafka topic retention, replication, partition strategy, replay, or dead-letter handling.
- SMTP outage handling or notification reconciliation.
- Alert thresholds, service-level objectives (SLOs), or on-call ownership.
- Capacity limits, autoscaling, resource budgets, or load-test results.

These gaps block a production-readiness claim.

## Runtime limit

This page deliberately does not say that Compose, Kind, Kubernetes, Docker images, health endpoints, observability, or email delivery work. Those statements require execution evidence from a controlled environment and should be added with date, commit, command, environment, and result. The root Compose, service-level Compose files, and Kubernetes manifests are separate declarations, not interchangeable runbooks.
