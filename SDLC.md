# Software Development Life Cycle (SDLC) — BankOps

## 1. Overview
BankOps adheres to an enterprise-grade financial software engineering lifecycle emphasizing risk controls, auditability, data integrity, and deterministic testing.

## 2. SDLC Phases

```
Requirements Analysis ➔ Architecture & Threat Modeling ➔ Iterative Development ➔ Automated Testing & Verification ➔ Deployment & Observability ➔ Post-Release Audit
```

### Phase A: Requirements & Risk Classification
- Every operational feature is evaluated against financial operational risk categories (e.g. liquidity exposure, settlement delays, unauthorized access).
- Authorization boundaries are mapped (`ROLE_ADMIN` vs `ROLE_ANALYST`).

### Phase B: Design & Architecture
- Database schemas are designed as third-normal-form (3NF) relational tables with foreign keys and unique constraints.
- Flyway migrations are used exclusively for all DDL changes to maintain schema determinism across environments.
- Architectural decisions are formally documented in Architecture Decision Records (`ADR.md`).

### Phase C: Implementation Standards
- Clean layered architecture: Controllers handle HTTP transport and DTO mapping; Services encapsulate business rules and database transactions (`@Transactional`); Repositories execute optimized queries.
- Zero raw entity leakage: Data Transfer Objects (DTOs) are used for all external API contracts.
- Distributed tracing: Every request is tagged with an `X-Correlation-ID` propagated through Slf4j MDC.

### Phase D: Verification & Automated Testing
- Unit tests verify pure domain rules (RuleEngine, status transitions, CSV parsing, KPI calculations).
- Integration tests verify Spring Security filters, Flyway migrations, database constraints, and REST controllers.
- Pull requests require 100% passing tests on GitHub Actions CI before merging.

### Phase E: Release & Deployment
- Production deployments use Docker multi-stage containers running as unprivileged service users.
- Environment variables isolate secrets, database credentials, and CORS origins.
