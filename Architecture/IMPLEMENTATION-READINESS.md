# Implementation Readiness Gate & Phase Execution Order

* **Architecture:** FINAL
* **Major Decisions:** FINALIZED
* **Documentation:** CONSOLIDATED
* **Implementation:** READY TO BEGIN
* **Document Version:** 1.0.0
* **Date:** 2026-08-15

---

## 1. Implementation Readiness Gate Statement

The architecture documentation phase is **COMPLETE, CONSOLIDATED, and FROZEN**. All fundamental architectural decisions across all nine capabilities have been resolved, recorded in ADRs 001–011, consolidated into `ARCHITECTURE.md`, and translated into developer rules in `IMPLEMENTATION-GUIDE.md`.

The repository is officially **READY TO BEGIN IMPLEMENTATION**.

---

## 2. Sequential Implementation Phase Order

To ensure clean dependency resolution and avoid circular development bottlenecks, implementation MUST proceed strictly in the following 10 phases:

### Phase 1: Repository Structure & Foundation
* Establish repository directory tree (`platform-services/`, `platform-libraries/`, `consuming-products/`).
* Setup shared build tools, Docker Compose local infrastructure (PostgreSQL, MinIO, Mailpit, OTel Collector), and CI linting pipelines.
* Configure cross-cutting security headers, logging formatters, and correlation ID interceptors (`X-Correlation-ID`).

### Phase 2: Identity & Authentication (`auth-service`)
* Implement database migrations for `auth_db` (`auth_users`, `auth_credentials`, `auth_sessions`, `auth_outbox_events`).
* Implement Argon2id password hashing, user registration, email verification OTP challenges, and login REST endpoints.
* Implement RS256 JWT access token minting (15-min TTL) and HTTP-only cookie refresh token rotation (`auth_sessions`).

### Phase 3: Authorization (`platform-authz-lib`)
* Implement embedded Java/Python shared library `platform-authz-lib`.
* Implement RS256 JWT public key signature verification and in-process RBAC permission claim evaluation.
* Integrate authorization middleware into consuming product sample controllers.

### Phase 4: Notifications (`notifications-service`)
* Implement database migrations for `notifications_db` (`notification_templates`, `notifications`).
* Implement notification API (`POST /v1/notifications/send`), template rendering engine, and pluggable vendor adapters (SMTP, Mailpit, SendGrid).
* Connect Auth Outbox Dispatcher to invoke Notifications API for registration/reset emails.

### Phase 5: Background Jobs (`jobs-service`)
* Implement database migrations for `jobs_db` (`jobs`, `job_execution_logs`).
* Implement job queueing API (`POST /v1/jobs/submit`) and worker execution engine using SQL `FOR UPDATE SKIP LOCKED`.
* Implement exponential retry backoff, watchdog worker crash recovery, and Dead-Letter Queue handling.

### Phase 6: Events (`events-router`)
* Implement Transactional Outbox library for producer domain event emission.
* Implement producer-owned outbox dispatchers pushing events to Events Platform API (`POST /v1/events/publish`).
* Implement Events Platform router pushing CloudEvents v1.0 payloads to subscriber HTTP endpoints with consumer idempotency tracking.

### Phase 7: Scheduler (`scheduler-service`)
* Implement database migrations for `scheduler_db` (`schedules`, `schedule_executions`).
* Implement leader election via PostgreSQL row locking (`SELECT FOR UPDATE NOWAIT`).
* Implement cron expression evaluation engine and task handoff to Background Jobs API.

### Phase 8: File Storage (`file-service`)
* Implement database migrations for `files_db` (`files`).
* Implement presigned S3/MinIO upload/download URL generation API (`POST /v1/files/upload-url`).
* Connect MinIO object storage container and integrate ClamAV background quarantine malware scanner worker.

### Phase 9: Audit (`audit-service`)
* Implement database migrations for `audit_db` (`audit_logs`) with PostgreSQL append-only triggers rejecting `UPDATE` and `DELETE` SQL queries.
* Implement Audit Ingestion API (`POST /v1/audit/events`) and admin query search route.

### Phase 10: Observability Hardening
* Finalize OpenTelemetry Collector container routing to Prometheus, Jaeger, and Grafana.
* Verify fail-open client SDK isolation and metric dashboards for latency, HTTP error rates, and job queues.

---

## 3. Strict Development Rule

> **No code implementation may skip ahead of the sequential phase order defined above.**
