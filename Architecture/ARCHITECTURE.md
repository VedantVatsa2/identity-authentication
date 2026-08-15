# Platform Architecture — Primary Source of Truth

* **Architecture Status:** FINAL ARCHITECTURE BASELINE — FROZEN
* **Implementation Status:** READY TO BEGIN (Phase 1)
* **Document Version:** 3.0.0 (Consolidated Primary Reference)
* **Last Updated:** 2026-08-15

---

## 1. System Overview

We are building a **reusable application platform** designed to serve multiple independent user-facing products (e.g. Document Vault, Research Platform, Financial Applications). The platform supplies common capabilities—Identity & Authentication, Authorization, Notifications, Background Jobs, Events, Scheduler, File Storage, Audit, and Observability—allowing consuming applications to focus exclusively on their core business domains.

The platform is designed for a multi-month development lifecycle with zero paid service dependencies. All components are Docker-compatible, open-source, and runnable locally at zero infrastructure cost.

---

## 2. Architectural Principles

1. **Clean Boundaries & Low Coupling:** Every capability owns its data and domain contracts. Direct database access across capability boundaries is strictly prohibited.
2. **Minimal Infrastructure Complexity (Anti-Over-Engineering):** Do not introduce microservices, Redis, Kafka, Kubernetes, or service meshes prematurely. Use the simplest architecture that satisfies security, reliability, and scaling needs.
3. **Fail-Closed Security & Fail-Open Observability:** Security controls MUST fail closed (denying access on error). Observability tools MUST fail open (business operations continue even if telemetry fails).
4. **Data Ownership Isolation:** Logical database/schema isolation enforces strict boundaries. Consuming applications reference user identities via immutable `user_id` tokens, never raw user tables.
5. **Asynchronous Non-Blocking Processing:** Heavy work (emails, job queues, event routing, malware scanning, audit ingestion) MUST execute asynchronously out-of-band without delaying HTTP API responses.
6. **Zero Paid Infrastructure Constraint:** 100% of the platform stack (PostgreSQL, MinIO, OpenTelemetry, Prometheus, Jaeger, Grafana, Mailpit) runs locally via Docker at zero cost.

---

## 3. Platform Architecture

```mermaid
graph TB
    subgraph Clients ["Client Layer"]
        WebUser["End User Browser / Mobile App"]
        AdminPortal["Platform Admin Console"]
    end

    subgraph Products ["Consuming Product Applications"]
        DocVault["Document Vault App"]
        ResearchApp["Research Platform App"]
    end

    subgraph PlatformBoundary ["Reusable Platform Capability Layer"]
        AuthService["Identity & Authentication Service
(Modular Monolith Container / auth_db)"]
        AuthzLib["platform-authz-lib
(Embedded Shared Library)"]
        NotifyService["Notifications Service
(Service & Dispatchers / notifications_db)"]
        JobsService["Background Jobs Service & Workers
(Service & Workers / jobs_db)"]
        EventRouter["Events Platform Router
(Event Router & Ingestor API)"]
        SchedulerService["Scheduler Service
(Leader-Elected Engine / scheduler_db)"]
        FileService["File Storage Service
(Service & Presigned URLs / files_db)"]
        AuditService["Audit Ingestion Service
(Service & Append-Only DB / audit_db)"]
        ObsStack["Observability Engine
(OpenTelemetry Collector + Grafana/Prometheus)"]
    end

    subgraph StorageLayer ["Physical Storage Layer"]
        PostgreSQL[(Physical PostgreSQL Cluster
Logical Schemas: auth_db, jobs_db, files_db, etc.)]
        ObjectStore[(MinIO / S3 Object Storage
Quarantine & Clean Buckets)]
    end

    %% Flow links
    WebUser -- "HTTPS / REST" --> DocVault & ResearchApp
    AdminPortal -- "HTTPS / REST" --> AuthService & AuditService & JobsService

    DocVault & ResearchApp -- "Local JWT Claim Check" --> AuthzLib
    DocVault & ResearchApp -- "Sync Auth & Token Refresh" --> AuthService
    DocVault & ResearchApp -- "Submit Jobs" --> JobsService
    DocVault & ResearchApp -- "Request Presigned Upload/Download" --> FileService
    DocVault & ResearchApp -- "Async Event Outbox" --> EventRouter

    AuthService -- "Outbox Dispatch (User Registered)" --> NotifyService
    AuthService -- "Security Audit Logs" --> AuditService

    SchedulerService -- "Trigger Cron Execution" --> JobsService
    FileService -- "Direct Presigned I/O" --> ObjectStore
    WebUser -- "Direct Upload / Download Stream" --> ObjectStore

    ObsStack -.- "Out-of-Band Telemetry" -.-> AuthService & JobsService & FileService & NotifyService & AuditService
    PostgreSQL -.- "Schema Isolation" -.-> AuthService & JobsService & FileService & NotifyService & AuditService & SchedulerService & EventRouter
```

---

## 4. Capability Boundaries

| Capability | Final Implementation Form | Deployment Boundary | Storage Ownership | Justification |
| :--- | :--- | :--- | :--- | :--- |
| **1. Identity & Auth** | **Modular Monolith Service** | Standalone Container | PostgreSQL `auth_db` | Credential security isolation, independent deployment across consuming products. |
| **2. Authorization** | **Shared Embedded Library** | Embedded in Services (`platform-authz-lib`) | Local Token Claims / In-Process | Sub-millisecond policy check; zero network latency per API request. |
| **3. Notifications** | **Standalone Service + Dispatcher** | Standalone Container | PostgreSQL `notifications_db` | Isolates third-party vendor failures (SendGrid/SMTP), manages retries and templates. |
| **4. Background Jobs** | **Service + Worker Pool** | Standalone Container | PostgreSQL `jobs_db` | Isolates heavy async task execution from web HTTP threads using `SKIP LOCKED`. |
| **5. Events** | **Outbox Library + Router** | Outbox in Producers + Router Container | Producer DB Outbox Tables | Guarantees zero dual-write event loss; routes domain events to subscribers. |
| **6. Scheduler** | **Leader-Elected Service** | Standalone Container (Single Active) | PostgreSQL `scheduler_db` | Evaluates cron timers via DB row locks (`NOWAIT`) and hands off tasks to Jobs API. |
| **7. File Storage** | **Service + Presigned URLs** | Standalone Container + MinIO | PostgreSQL `files_db` + S3 Bucket | Bypasses application bandwidth limits; segregates binary file bytes from PostgreSQL. |
| **8. Audit** | **Ingestion Service + Append DB** | Standalone Container | PostgreSQL `audit_db` (Append-Only) | Immutable compliance log; DB triggers reject `UPDATE` and `DELETE` queries. |
| **9. Observability** | **Shared Infrastructure Stack** | OTel Collector Container Stack | Local Disk / Time-Series | Fail-open out-of-band telemetry collection; 100% self-hosted zero-cost stack. |

---

## 5. Identity & Authentication — Final Architecture

### 5.1 Responsibility & Boundaries
Auth owns user identity records, salted Argon2id password hashes, verification challenges, client application secrets, and server-side refresh tokens. Auth explicitly does **not** own business domain data or product authorization rules.

### 5.2 Token & Session Architecture (ADR-011 Finalized)
1. **Short-Lived Signed Access Tokens (JWT):** Issued as RS256 signed JWTs with a strict **15-minute TTL**. Contains `user_id`, `roles`, `scopes`, `session_id`, `iss`, and `exp`. Consuming products validate JWTs locally using Auth's public key.
2. **Refresh Tokens:** Issued as cryptographically secure 256-bit random strings stored in an **HTTP-only, Secure, SameSite=Strict cookie**.
3. **Server-Side Session State:** Auth stores SHA-256 hashes of active refresh tokens in `auth_sessions` (`session_id`, `user_id`, `client_id`, `refresh_token_hash`, `expires_at`, `revoked_at`). Plaintext refresh tokens are **never** stored in the database.
4. **Refresh Token Rotation & Reuse Detection:** Every refresh attempt invalidates the old refresh token and issues a new refresh token pair. If an already-invalidated refresh token is presented (indicating theft), Auth immediately revokes all active sessions for that `user_id`.
5. **Session Revocation & Logout-All:** "Logout Everywhere" or password resets set `revoked_at = NOW()` on all active sessions in `auth_sessions`. Stolen access tokens expire within 15 minutes max, and refresh tokens are blocked immediately.

---

## 6. Authorization — Final Architecture (ADR-003 Finalized)

Authorization is implemented as an embedded shared library (`platform-authz-lib`) imported by consuming services.

```
                    IN-PROCESS AUTHORIZATION CHECK
┌──────────────────────────────────────────────────────────────────┐
│ Consuming Application Process                                    │
│                                                                  │
│  HTTP Request ──► REST Controller                                │
│                       │                                          │
│                       ▼                                          │
│              platform-authz-lib                                  │
│                       │                                          │
│                       ├─► Validate JWT RS256 Signature           │
│                       ├─► Check Expiration (TTL < 15m)           │
│                       └─► Evaluate Roles & Scopes In-Process     │
│                       │                                          │
│                       ▼                                          │
│              [ ALLOW / DENY (< 1ms) ]                            │
└──────────────────────────────────────────────────────────────────┘
```

1. **No Network Hop:** Permission checks execute in-process without contacting Auth during request handling.
2. **Stale Claim Mitigation:** Short 15-minute JWT access token lifetimes ensure permission role updates take effect within 15 minutes. For immediate revocation on critical endpoints, services invoke Auth's introspection API.
3. **Scope:** RBAC is the initial model (`ADMIN`, `USER`, `GUEST`). Fine-grained resource ownership checks (e.g. "Does user X own document Y?") remain in business application domain code.

---

## 7. Auth Internal Outbox Boundary

Auth utilizes a Transactional Outbox table (`auth_outbox_events`) to guarantee that domain events (e.g. `USER_REGISTERED`, `PASSWORD_RESET_REQUESTED`) are persisted atomically within the same database transaction as user registration.

* **Boundary Guardrail:** The Auth outbox worker is **not** a notification delivery engine or a background job queue. Its sole responsibility is to poll `auth_outbox_events` and dispatch messages to the **Notifications Platform API** or **Event Router**.

---

## 8. Notifications — Final Architecture (ADR-004 Finalized)

Notifications Platform is a dedicated service responsible for multi-channel message delivery (Email, SMS, Push).

1. **Vendor Abstraction:** Business services call `POST /v1/notifications/send`. Notifications handles template rendering, delivery status tracking, retries, and vendor failover.
2. **Pluggable Adapters:** Provider adapters for SendGrid, AWS SES, Twilio, SMTP, and a zero-cost local Mock/Log provider for development.
3. **No Business SDK Vendor Coupling:** Business applications never import vendor SDKs directly.

---

## 9. Background Jobs — Final Architecture (ADR-005 Finalized)

Background Jobs handles asynchronous, heavy task execution using PostgreSQL `jobs_db`.

1. **Worker Claiming:** Workers claim queued jobs using SQL `SELECT * FROM jobs WHERE status='QUEUED' AND next_retry_at <= NOW() ORDER BY priority DESC FOR UPDATE SKIP LOCKED LIMIT 1`.
2. **At-Least-Once Execution:** Jobs are guaranteed to execute at least once. Job handlers **must be idempotent**. Exactly-once execution is enforced at the business handler layer via unique `job_key` checks.
3. **Crash Recovery & DLQ:** Watchdog process resets stale `RUNNING` jobs (> 15m heartbeat loss) back to `QUEUED`. Jobs exceeding `max_retries` transition to `DEAD_LETTER`.

---

## 10. Events — Final Architecture (ADR-006 Finalized)

Events Platform provides decoupled domain event distribution via Transactional Outbox + Producer Outbox Dispatcher + Events Platform API.

```
                      EVENT DISTRIBUTION ARCHITECTURE

  Producer Service (e.g. Auth)
            │
            ▼ (Single DB Transaction)
  Producer-owned Transactional Outbox Table (e.g. auth_outbox_events)
            │
            ▼ (Local Polling / SKIP LOCKED)
  Producer-owned Outbox Dispatcher
            │
            ▼ (HTTP POST /v1/events/publish)
  Events Platform API
            │
            ▼ (HTTP Push Delivery)
  Subscribers (e.g. Notifications / Audit / Consuming Products)
```

1. **Data Ownership Compliance:** Producer-owned outbox tables (e.g., `auth_outbox_events`) remain owned by each producer service. The Events Platform **never** directly accesses or reads producer databases or producer outbox tables. Producers own their local outbox tables and use a producer-owned background dispatcher to push events to the Events Platform API (`POST /v1/events/publish`).
2. **Delivery Guarantees:** At-least-once delivery; subscribers MUST enforce idempotency using `event_id`.
3. **No Central Event Database:** A redundant central `events_db` is excluded from the finalized architecture. Events are routed directly to active subscriber endpoints.

---

## 11. Scheduler — Final Architecture (ADR-007 Finalized)

Scheduler manages cron triggers, one-time delayed tasks, and timezone calculations.

1. **Leader Election:** PostgreSQL row lock (`SELECT * FROM scheduler_leader WHERE id=1 FOR UPDATE NOWAIT`) ensures a single active Scheduler instance evaluates triggers across multi-container deployments.
2. **Task Handoff:** Scheduler **does not** execute task code. When a schedule is due, it submits a job to the **Background Jobs API** (`POST /v1/jobs/submit`) with an idempotency key (`sched_{id}_{timestamp}`).

---

## 12. File Storage — Final Architecture (ADR-008 Finalized)

File Storage Service manages file metadata in `files_db` and object storage transfers via S3-compatible backends (MinIO locally, S3 in prod).

1. **Presigned URL Transfers:** Clients request presigned upload/download URLs and transfer binary bytes directly to MinIO/S3, bypassing application web servers.
2. **Server-Controlled Keys:** Object keys are generated by the server (`/v1/{tenant_id}/{file_id}`). Client MIME types and extensions are strictly validated on the server.

---

## 13. Malware Scanning — Final Architecture

1. **Asynchronous Quarantine Scanning:** Direct uploads land in an isolated `quarantine` object storage bucket with metadata `status = QUARANTINED`.
2. **Background Scanner:** A background worker invokes ClamAV container to scan quarantine files.
3. **Status Promotion:** Clean files are moved to the `clean` bucket and marked `status = ACTIVE`. Infected files are deleted and marked `status = REJECTED`. Files are **never downloadable** until marked `ACTIVE`.

```
                    MALWARE SCANNING LIFECYCLE
  Client Upload ──► Quarantine Bucket (MinIO)
                           │
                           ▼
                    ClamAV Worker Scan
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
       [ CLEAN ]                  [ INFECTED ]
             │                           │
  Move to Clean Bucket             Delete Object
  Set status = ACTIVE              Set status = REJECTED
  (Download Allowed)               (Blocked Permanently)
```

---

## 14. Audit — Final Architecture (ADR-009 Finalized)

Audit Platform records an immutable, append-only audit trail answering: *Who did what, when, to what resource, with what result?*

1. **Immutability:** PostgreSQL database triggers on `audit_logs` reject any `UPDATE` or `DELETE` SQL operations.
2. **Asynchronous Ingestion:** Services submit structured audit records out-of-band. Audit ingestion failures **must not** crash business transactions.

---

## 15. Observability — Final Architecture (ADR-010 Finalized)

Observability uses an out-of-band self-hosted stack: OpenTelemetry Collector + Prometheus + Jaeger + Grafana.

1. **Fail-Open Isolation:** Telemetry client SDKs drop buffers silently if the collector fails. Core business operations continue uninterrupted.
2. **Zero Sensitive Data:** Passwords, raw OTPs, authorization tokens, and PII are redacted prior to telemetry emission.
3. **Correlation Headers:** Mandatory propagation of `X-Request-ID` and `X-Correlation-ID` headers across all API calls.

---

## 16. Data Ownership & Storage Architecture

1. **Single PostgreSQL Deployment:** One physical PostgreSQL server hosting logically isolated databases/schemas (`auth_db`, `jobs_db`, `files_db`, `notifications_db`, `scheduler_db`, `audit_db`).
2. **No Cross-Schema SQL JOINs / FKs:** Direct cross-database SQL queries or foreign keys are strictly prohibited.
3. **No Binary Blobs in PostgreSQL:** Binary file bytes live exclusively in object storage (MinIO/S3).

---

## 17. Transient State & Redis Strategy

1. **No Initial Redis Dependency:** All transient state (sessions, refresh token hashes, OTP challenges, rate-limiting sliding windows) is stored in indexed PostgreSQL tables with automatic expiration timestamps and scheduled cleanup.
2. **Future Optimization Path:** Redis is documented as an optional future caching layer to be introduced only if PostgreSQL IOPS limits are exceeded under measured production load.

---

## 18. API Standards

1. **REST Semantics:** All platform endpoints use standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`) under `/v1/...`.
2. **Error Responses (RFC 7807):** Standardized JSON error response format (`type`, `title`, `status`, `detail`, `instance`, `code`, `timestamp`).
3. **Idempotency:** Mutating endpoints accept `Idempotency-Key` headers to guarantee safe client retries.

---

## 19. Service-to-Service Authentication

Service-to-service internal calls use short-lived, cryptographically signed **Service JWT Access Tokens** issued by Auth (`client_id` + `client_secret` authentication). Static shared secrets across services are strictly prohibited.

---

## 20. Security Baseline

* **Password Security:** Argon2id salted memory-hardened hashing.
* **Transport Security:** TLS 1.3 in all deployed environments.
* **Secrets Management:** Injected exclusively via environment variables; zero plaintext secrets in Git.
* **Sanitized Logging:** Passwords, tokens, OTPs, and PII stripped from logs and telemetry.
* **Abuse Protection:** Tiered sliding-window rate limiting on login, registration, OTP, and reset routes.
* **Web Protections:** HTTP-only SameSite=Strict cookies, CSP headers, CORS restrictions, input sanitization against SQLi/XSS/SSRF/IDOR.

---

## 21. Performance & Database Strategy

* **Local Token Validation:** Consuming services validate short-lived RS256 JWTs locally (< 1ms). Auth is NOT called on every API request.
* **Direct Binary Transfers:** Presigned URLs bypass application web servers for file transfers.
* **Database Optimization:** Indexed lookup queries, HikariCP connection pooling, bounded pagination, and async worker polling.

---

## 22. Deployment Architecture

```
                    DEPLOYMENT CONTAINER TOPOLOGY
┌─────────────────────────────────────────────────────────────────────────┐
│ Docker Host / Local Stack                                               │
│                                                                         │
│  ┌────────────────┐    ┌────────────────┐    ┌────────────────┐         │
│  │ Nginx Ingress  │    │  Auth Container│    │ Notify Container│        │
│  └───────┬────────┘    └───────┬────────┘    └───────┬────────┘         │
│          │                     │                     │                  │
│          ▼                     ▼                     ▼                  │
│  ┌────────────────┐    ┌────────────────┐    ┌────────────────┐         │
│  │ Jobs Worker    │    │ Scheduler      │    │ File Service   │         │
│  └───────┬────────┘    └───────┬────────┘    └───────┬────────┘         │
│          │                     │                     │                  │
│          └─────────────────────┼─────────────────────┘                  │
│                                │                                        │
│                                ▼                                        │
│               ┌─────────────────────────────────┐                       │
│               │ PostgreSQL Single Instance      │                       │
│               │ (auth_db, jobs_db, files_db...) │                       │
│               └─────────────────────────────────┘                       │
│                                │                                        │
│                                ▼                                        │
│               ┌─────────────────────────────────┐                       │
│               │ MinIO Container (S3 Object Store)│                      │
│               └─────────────────────────────────┘                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 23. Testing Strategy

1. **Unit Testing:** 100% logic coverage for password hashing, token validation, permission checks, and cron calculations.
2. **Integration Testing:** Service integration testing against local PostgreSQL and MinIO containers.
3. **Contract Testing:** OpenAPI schema contract validation between platform services and consuming apps.

---

## 24. Final Architecture Decisions (ADR Index)

* **[ADR-001 (Accepted)](./Architecture%20Decisions/ADR-001-Auth-Platform-Boundary.md):** Shared Auth platform boundary & prohibited direct database access.
* **[ADR-002 (Finalized)](./Architecture%20Decisions/ADR-002-Auth-Internal-Component-Structure.md):** Stateless Modular Monolith Container for Auth Platform.
* **[ADR-003 (Finalized)](./Architecture%20Decisions/ADR-003-Authorization-RBAC-Permission-Model.md):** Shared Embedded Authorization Library (`platform-authz-lib`).
* **[ADR-004 (Finalized)](./Architecture%20Decisions/ADR-004-Notification-Provider-Abstraction-Outbox.md):** Notifications Service with Pluggable Vendor Adapters.
* **[ADR-005 (Finalized)](./Architecture%20Decisions/ADR-005-Background-Jobs-Execution-Queue-Architecture.md):** PostgreSQL `SKIP LOCKED` Background Job Queue.
* **[ADR-006 (Finalized)](./Architecture%20Decisions/ADR-006-Event-Platform-Broker-And-Outbox.md):** Transactional Outbox Pattern + Event Router.
* **[ADR-007 (Finalized)](./Architecture%20Decisions/ADR-007-Scheduler-Distributed-Locking-And-Job-Handoff.md):** Leader-Elected Scheduler Service via PostgreSQL Row Locking.
* **[ADR-008 (Finalized)](./Architecture%20Decisions/ADR-008-File-Storage-Presigned-URL-Object-Abstraction.md):** Presigned S3/MinIO Object Storage Abstraction.
* **[ADR-009 (Finalized)](./Architecture%20Decisions/ADR-009-Audit-Platform-Immutability-And-Ingestion.md):** Immutable Append-Only Audit Logging via DB Triggers.
* **[ADR-010 (Finalized)](./Architecture%20Decisions/ADR-010-Observability-OpenTelemetry-Self-Hosted.md):** OpenTelemetry Collector Standard & Self-Hosted Telemetry Stack.
* **[ADR-011 (Finalized)](./Architecture%20Decisions/ADR-011-Auth-Session-Token-Architecture.md):** Hybrid Short-Lived JWT + Refresh Token Architecture.

---

## 25. Implementation Rules

1. **Follow `IMPLEMENTATION-GUIDE.md`:** Developers must adhere strictly to the rules in `IMPLEMENTATION-GUIDE.md`.
2. **No Direct DB Cross-Access:** Never query another capability's database schema.
3. **No Production Code in Architecture Phase:** Architecture is frozen; code implementation proceeds in Phase 1 order.
