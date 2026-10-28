# Architecture Decision Records (ADR) — BankOps

## ADR-001: Optimistic Locking for Exception Case State Concurrency
* **Context**: Multiple operational analysts may simultaneously view or triage the same operational exception queue. Without concurrency control, a slower analyst's update could silently overwrite a faster analyst's triage notes or state resolution (lost updates).
* **Decision**: Implement JPA `@Version` on `ExceptionCase` entity, incrementing an integer version on every state or assignment mutation. Expose an `expectedVersion` parameter on REST transition endpoints.
* **Consequences**: Concurrent conflicts are rejected immediately with HTTP 409 Conflict and a clear, non-leaking error response requiring the analyst to refresh the latest state.

---

## ADR-002: Append-Only Audit Trail Architecture
* **Context**: Financial operations platforms require auditable accountability for regulatory and internal review. Traditional CRUD logs allow records to be mutated or deleted.
* **Decision**: Implement an append-only `AuditEvent` entity with Spring Security actor attribution and propagation of a distributed `X-Correlation-ID`. Only `INSERT` and `SELECT` query paths are exposed in the service and API layers.
* **Security Limitations**: While application-level access control guarantees that non-admin and admin users cannot mutate audit events through the API, it does not guarantee cryptographic tamper-proofing against privileged database administrators (DBAs) with direct SQL access. Future phases will integrate write-once-read-many (WORM) storage or cryptographic hash chaining.

---

## ADR-003: Decoupled Operational Validation Rules Engine
* **Context**: In financial transaction monitoring, rule matches (such as amount thresholds, channel anomalies, or gateway timeout errors) indicate *operational exceptions* requiring human investigation, not definitive proof of fraud or system failure.
* **Decision**: Separate the transaction ingestion pipeline from exception generation. Transactions are always persisted with their true clearing state (`COMPLETED`, `PENDING`, `FAILED`). Active rules are evaluated against the record and generate corresponding `ExceptionCase` records with defined severities without aborting ingestion.
* **Consequences**: Clean separation of transaction ledger persistence from asynchronous operational triage workflows.

---

## ADR-004: Stateless JWT Authentication with Role Hierarchy
* **Context**: Need a secure, stateless session mechanism across distributed horizontal nodes without server-side session stickiness.
* **Decision**: Issue short-lived HMAC-SHA256 signed JSON Web Tokens (JJWT) with claims including user UUID, username, and assigned authorities (`ROLE_ADMIN`, `ROLE_ANALYST`). Enforce method-level security with Spring Security `@PreAuthorize`.
