# Evidence-derived roadmap

[Back to the documentation hub](index.md) · [Back to the README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Status:** proposed backlog derived from repository evidence; not an approved product roadmap or set of requirements. No pre-existing project-local docs directory was found before this pass.

## Prioritization model

- **P0:** blocks safe execution, security, data integrity, or reliable deployment.
- **P1:** blocks a defensible end-to-end feature or release gate.
- **P2:** improves maintainability, product completeness, or operator experience.

Acceptance criteria below are proposed readiness checks. Confirm ownership and scope before implementation.

## Severity versus sequencing

The security review uses severity labels such as **High** or **Critical** to describe the potential impact and urgency of a risk. This roadmap uses **P0/P1/P2** to describe dependency and readiness sequencing. They are different dimensions: a P0 is work that must unblock safe validation or a prerequisite, while a P1 follows when the prerequisite is available. A High/Critical finding can therefore be sequenced as P1 when it depends on P0 deployment or identity reconciliation; the sequencing label does not reduce the finding's severity.

| Dimension | Meaning | Examples in this documentation |
|---|---|---|
| Risk severity | Potential security or business impact of a finding | High/Critical risk labels in [Security](security.md) |
| Roadmap priority | Proposed order based on safety, dependency, and release readiness | P0 before P1 before P2 |

## P0 — make the system safe to validate

### Epic: Remove exposed/default secrets

**Evidence:** root/service environment artifacts, hardcoded order datasource credentials, Kubernetes Secret manifests, Keycloak and Grafana configuration.  
**Acceptance criteria:**

- No live/default credentials remain in tracked deployable artifacts.
- All secrets are injected through an approved mechanism.
- Previously exposed values are rotated and repository history is assessed.
- Secret scanning is part of CI and its result is retained.

### Epic: Reconcile the deployment contract

**Evidence:** Compose/Kubernetes/app port, image, database, realm, ConfigMap, and Secret drift documented in [Configuration](configuration.md) and [Operations](operations.md).  
**Acceptance criteria:**

- One canonical local topology and one canonical Kubernetes preview path are named.
- Every service has one authoritative image, container port, service port, database URL, and required variable set.
- Kubernetes manifests pass schema/lint validation.
- Compose dependencies use readiness checks where needed.
- Images use immutable release identifiers.

### Epic: Close the authentication boundary

**Evidence:** gateway JWT validation, direct backend port mappings, missing downstream security declarations, realm/issuer name mismatch, and a frontend interceptor whose asynchronous token subscription does not return the token-bearing request.  
**Acceptance criteria:**

- Untrusted callers cannot bypass gateway policy through backend host ports.
- Issuer, audience, redirect URI, and CORS values agree across frontend, Keycloak, gateway, Compose, and Kubernetes.
- Authorization decisions are explicit for catalog mutation and order placement.
- Invalid/expired/wrong-issuer/wrong-role token tests pass.

### Epic: Protect order data integrity

**Evidence:** client-supplied price/user details, missing quantity/price positivity constraints, read-only stock check, `PlaceOrderDTO.userDetails` nested constraints without evidenced `@Valid` cascade, and database save before unchecked Kafka send.  
**Acceptance criteria:**

- Server derives price from authoritative catalog data.
- Server derives user identity from token claims after an explicitly tested authorization boundary.
- Quantity and price constraints reject zero, negative, excessive, and malformed values.
- Stock reservation/decrement semantics are defined and concurrency-tested.
- Database/event publication has a documented atomicity and recovery strategy.

## P1 — prove the intended vertical slice

### Epic: Define and test the public API contract

**Evidence:** controllers expose a minimal create/list/catalog, create/order, and availability surface; API-doc support is evidenced for gateway/product/order/inventory but not notification; error contract is incomplete.  
**Acceptance criteria:**

- OpenAPI documents all public routes, fields, requiredness, status codes, and error shapes.
- Gateway aggregate paths match service docs.
- Contract tests cover validation, authentication, dependency failure, and success responses.
- API versioning and backward-compatibility rules are documented.

### Epic: Make event processing reliable

**Evidence:** duplicated Avro schema files, async producer with ignored result, notification consumer without visible idempotency/DLQ/retry persistence.  
**Acceptance criteria:**

- One governed schema source and compatibility policy exists.
- Producer failures are observable and recoverable.
- Consumer retries, dead-letter handling, replay, and idempotency are defined.
- Duplicate delivery produces at most one intended notification per order.
- SMTP failure does not silently lose the business notification.

### Epic: Build meaningful end-to-end coverage

**Evidence:** narrow backend happy paths, context-load tests, generated Angular tests, no numeric coverage evidence.  
**Acceptance criteria:**

- Isolated tests cover login, catalog, create product authorization, order success, insufficient stock, dependency failure, event delivery, and mail failure.
- Browser tests cover the functional Angular journey and keyboard/accessibility behavior.
- Kubernetes/Compose smoke tests validate service DNS, ports, secrets, and health.
- CI publishes coverage and enforces an agreed threshold.

### Epic: Harden notification rendering

**Evidence:** HTML mail template interpolates event/user values directly.  
**Acceptance criteria:**

- Dynamic values are safely escaped.
- Email content has a maintained template and text alternative where required.
- Tests cover markup characters, long values, missing values, and replay.

## P2 — improve product and operations maturity

### Epic: Expand shop capabilities

**Evidence:** minimal catalog create/list, single-SKU order, read-only inventory, and asynchronous confirmation email.  
**Acceptance criteria:**

- Product lifecycle requirements are agreed before adding update/delete/search.
- Order retrieval/status/cancellation and customer history requirements are agreed.
- Inventory adjustment/reservation/release operations have authorization and audit rules.
- Payment, fulfillment, and delivery scope is explicitly accepted or excluded.

### Epic: Establish operations practice

**Evidence:** no probes, alerts, SLOs, rollback, backup/restore runbook, capacity policy, or incident guide found.  
**Acceptance criteria:**

- Readiness/liveness probes and resource limits exist for supported workloads.
- Metrics, logs, and traces have dashboards and actionable alerts.
- Backup/restore is tested and documented.
- Rollback and schema migration procedures are rehearsed.
- SLOs and ownership are recorded.

### Epic: Reduce repository drift

**Evidence:** duplicate Compose files, backup files with `~`, generated artifacts, partial CI coverage, malformed inventory workflow structure, standalone workflow directory changes, and the baseline README's Eureka claim not found in implementation.  
**Acceptance criteria:**

- Duplicate deployment definitions are either removed or explicitly assigned a purpose.
- Generated artifacts have a documented generation workflow.
- CI covers all supported services and frontend applications.
- Documentation/implementation drift is checked during changes.

## Dependencies and assumptions

- The business owner must decide whether the repository is a learning reference, a local demo, or a product foundation.
- Security and data-integrity work precede public exposure.
- Deployment reconciliation precedes meaningful runtime testing.
- Runtime evidence requires an approved environment; this documentation pass did not execute commands.
- No PRD, product brief, ADR set, formal backlog, or approved acceptance criteria was found in the inspected snapshot. Items here are analysis outputs, not commitments.

## Bounded conclusion about AI/ML

No material AI, machine-learning, large-language-model, retrieval-augmented-generation, or model-serving component was evidenced in the inspected source, configuration, build files, or deployment manifests. This is a static absence finding for the snapshot, not a statement about external systems or future plans.
