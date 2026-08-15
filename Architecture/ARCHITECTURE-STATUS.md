# Central Architecture Status Tracker

* **Architecture Documentation:** FINAL
* **Architecture Decision Status:** ACCEPTED / FINAL
* **Implementation:** NOT STARTED
* **Architecture Freeze:** ACTIVE
* **Human Review:** COMPLETED BY CURRENT ARCHITECTURE DECISION
* **Document Version:** 3.0.0 (FINAL FROZEN BASELINE)
* **Last Updated:** 2026-08-15

---

## 1. Final Platform Capability Design Status Summary

| Capability | Design Status | Decision Reference | Architecture Boundary Form | Implementation Status |
| :--- | :--- | :--- | :--- | :--- |
| **Identity & Authentication** | **FINAL** | ADR-001, ADR-002, ADR-011 | Modular Monolith Container (`auth-service`) | NOT STARTED |
| **Authorization** | **FINAL** | ADR-003 | Shared Embedded Library (`platform-authz-lib`) | NOT STARTED |
| **Notifications** | **FINAL** | ADR-004 | Standalone Service + Pluggable Adapters | NOT STARTED |
| **Background Jobs** | **FINAL** | ADR-005 | PostgreSQL `SKIP LOCKED` Service & Worker Pool | NOT STARTED |
| **Events** | **FINAL** | ADR-006 | Transactional Outbox + Event Router | NOT STARTED |
| **Scheduler** | **FINAL** | ADR-007 | Leader-Elected Service (Jobs Handoff) | NOT STARTED |
| **File Storage** | **FINAL** | ADR-008 | Presigned S3/MinIO Object Storage Service | NOT STARTED |
| **Audit** | **FINAL** | ADR-009 | Ingestion Service + Append-Only DB | NOT STARTED |
| **Observability** | **FINAL** | ADR-010 | Self-Hosted OpenTelemetry Telemetry Stack | NOT STARTED |

---

## 2. Final Architecture Decision Log

* **ADR-001 (Accepted / Finalized):** Auth Shared Platform Boundary & Prohibited Direct DB Access.
* **ADR-002 (Accepted / Finalized):** Modular Monolith Container for Auth Platform.
* **ADR-003 (Accepted / Finalized):** Shared Embedded Authorization Library (`platform-authz-lib`).
* **ADR-004 (Accepted / Finalized):** Notifications Service & Provider Abstraction.
* **ADR-005 (Accepted / Finalized):** PostgreSQL-Backed Job Queue with `SKIP LOCKED` Concurrency.
* **ADR-006 (Accepted / Finalized):** Transactional Outbox Pattern + Event Router.
* **ADR-007 (Accepted / Finalized):** Leader-Elected Scheduler Service with Background Job Handoff.
* **ADR-008 (Accepted / Finalized):** Presigned URL Object Storage Abstraction (MinIO / S3).
* **ADR-009 (Accepted / Finalized):** Dedicated Audit Ingestion Service & Append-Only Database Storage.
* **ADR-010 (Accepted / Finalized):** OpenTelemetry Collector Standard & Self-Hosted Telemetry Stack.
* **ADR-011 (Accepted / Finalized):** Hybrid Short-Lived JWT + Refresh Token Architecture.

---

## 3. Future Enhancements (Non-Blocking Future Roadmap)

* **[FUTURE-01] NATS JetStream Migration:** Upgrade Event Router / Job Queue to NATS JetStream if PostgreSQL IOPS limits are exceeded under high volume (>5,000 req/sec).
* **[FUTURE-02] Redis Caching Layer:** Introduce Redis caching for session tokens and rate limits if database query latency metrics demand it under measured load.
