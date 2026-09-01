# Micro-Commerce documentation

[Back to the project README](../README.md)

**Source baseline:** branch `feature/kubernetes`, HEAD `b3dafa4416ca12a90ae7cfd5249b166c42321799` (`b3dafa4`), dated 2025-01-17.  
**Documentation state:** `README.md` and `docs/**` are current uncommitted working-tree changes from this pass.  
**Analysis date:** 2026-09-01.  
**Evidence mode:** static repository inspection only; no commands, builds, tests, containers, or network calls were run for this documentation pass. No pre-existing project-local docs directory was found before this pass; the current `docs/**` pages are uncommitted changes from this pass.

## Start here

Micro-Commerce is a brownfield learning project for a small shop flow. It contains five backend Spring Boot applications in total: the gateway plus four domain services (product, order, inventory, and notification). It also contains two Angular applications, local Compose definitions, Kubernetes manifests, Keycloak configuration, Kafka/Avro messaging, and observability configuration.

The pre-pass/baseline README claimed Eureka as a service-discovery component. No Eureka implementation was evidenced in the source, configuration, routes, or manifests inspected for this snapshot; fixed service URLs and Kubernetes DNS names appear instead.

The repository is **not documented as production-ready**. Read the [limitations and maturity notice](../README.md#status-and-maturity) before treating any path as an executable runbook.

## Reading paths

### New contributors

1. [README](../README.md): scope, status, architecture at a glance, and navigation.
2. [Development guide](development.md): layout, toolchains, evidenced commands, and setup blockers.
3. [Configuration reference](configuration.md): configuration ownership, variable names, and drift.
4. [Services and API reference](services-and-api.md): controllers, payloads, gateway paths, and event contract.

### Backend contributors

1. [Architecture](architecture.md): synchronous and asynchronous boundaries.
2. [Services and API](services-and-api.md): source-level contracts.
3. [Testing and quality](testing-and-quality.md): what the tests cover and what they do not.
4. [Security](security.md): controls, exposure, and remediation priorities.

### Platform and operations contributors

1. [Operations](operations.md): Compose/Kubernetes topology and operational gaps.
2. [Configuration](configuration.md): environment and manifest ownership.
3. [Security](security.md): secrets and direct-port concerns.
4. [Roadmap](roadmap.md): evidence-derived work items and readiness criteria.

### Reviewers and maintainers

1. [Evidence appendix](evidence.md): claim provenance and confidence rules.
2. [Testing and quality](testing-and-quality.md): historical report interpretation.
3. [Roadmap](roadmap.md): prioritized gaps; these are not approved requirements.

## Documentation status legend

- **[Verified]**: directly visible in a source, configuration, test, or generated report inspected in the snapshot. This label does not mean runtime success.
- **[Inferred]**: a reasonable interpretation of multiple verified artifacts; not an explicit product requirement.
- **[Unverified]**: represented in repository artifacts but not validated at runtime.
- **[Blocked]**: evidence or a prerequisite prevents a reliable conclusion or execution claim.

Static evidence can show what files declare or implement. It cannot prove that a deployment starts, that a dependency is reachable, that a route returns a response, or that a control withstands attack.

## Generated and secondary documentation

`HELP.md` files are Spring Initializr-style generated references. The instances observed in the working tree are ignored/untracked and optional; they are absent from a clean checkout unless regenerated. They are not required for this documentation set, and the pages in this `docs/` directory are the canonical project documentation.

Observed generated paths (plain code paths, not required links):

- `api-gateway/HELP.md`
- `product-service/product-service/HELP.md`
- `order-service/HELP.md`
- `inventory-service/inventory-service/HELP.md`
- `notification-service/HELP.md`

The following frontend-owned READMEs remain separate and untouched:

- [`frontend/java-shop/README.md`](../frontend/java-shop/README.md)
- [`frontend/java-shop-website/README.md`](../frontend/java-shop-website/README.md)

These secondary files contain useful generator or frontend references, but they are not the canonical project architecture or operations guide.

## External references

These links explain version-sensitive framework behavior. They are not evidence that the project has been executed successfully:

- [Spring Boot 3.4.1 reference documentation](https://docs.spring.io/spring-boot/3.4.1/reference/html/)
- [Spring Cloud Gateway Server MVC reference](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html)
- [Angular documentation](https://angular.dev/)
- [Angular 17.3.12 API reference](https://v17.angular.io/)
- [CommonMark specification](https://commonmark.org/)
- [Mermaid documentation](https://mermaid.js.org/)
