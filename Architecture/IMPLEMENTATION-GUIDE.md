# Developer Implementation Guide

* **Target Audience:** All Software Engineers & AI Pair Programmers
* **Document Purpose:** Practical, enforceable rules for building platform capabilities and consuming applications.
* **Document Version:** 1.0.0 (Frozen Baseline)
* **Last Updated:** 2026-08-15

---

## 1. Repository Structure Standards

```
/
├── platform-services/
│   ├── auth-service/              # Identity & Authentication (Spring Boot / FastAPI)
│   ├── notifications-service/     # Notifications Platform
│   ├── jobs-service/              # Background Jobs API & Worker Pool
│   ├── events-router/             # Events Platform Router
│   ├── scheduler-service/         # Scheduler Service
│   ├── file-service/              # File Storage Service
│   └── audit-service/             # Audit Ingestion Service
├── platform-libraries/
│   └── platform-authz-lib/        # Shared Embedded Authorization Library
├── consuming-products/
│   ├── document-vault/            # Document Vault Product Application
│   └── research-platform/         # Research Platform Product Application
├── docker-compose.yml             # Local zero-cost infrastructure stack
└── README.md
```

---

## 2. Capability & Boundary Rules

1. **Strict Logical Separation:** Every capability owns its code folder and PostgreSQL database schema (`auth_db`, `jobs_db`, `files_db`, etc.).
2. **Prohibited DB Cross-Queries:** Never write SQL JOINs across database schemas. Never create foreign keys targeting another service's tables.
3. **Library vs Service:**
   * **Authorization** is imported as a shared library (`platform-authz-lib`).
   * **All other capabilities** run as containerized services communicating via REST APIs or async outbox events.

---

## 3. API Conventions & Standards

* **Base Path:** `/v1/{capability}/{resource}` (e.g. `/v1/auth/login`, `/v1/files/upload-url`).
* **HTTP Methods:** `GET` (read), `POST` (create/action), `PUT`/`PATCH` (update), `DELETE` (remove).
* **Timestamps:** ISO-8601 UTC strings (`2026-08-15T16:00:00Z`).
* **IDs:** Standard UUID v4 strings (`usr_1a2b3c4d...`).
* **Pagination:** Query parameters `?page=1&limit=20`. Response body contains:
  ```json
  { "data": [...], "pagination": { "page": 1, "limit": 20, "total_items": 100, "total_pages": 5 } }
  ```
* **Error Response Format (RFC 7807):**
  ```json
  {
    "type": "https://platform.example.com/errors/invalid-input",
    "title": "Invalid Parameter",
    "status": 400,
    "detail": "Password does not meet complexity requirements.",
    "instance": "/v1/auth/register",
    "code": "INVALID_PASSWORD_COMPLEXITY",
    "timestamp": "2026-08-15T16:00:00Z"
  }
  ```

---

## 4. Authentication Usage Rules

* **Validating Requests in Product Services:**
  1. Extract JWT access token from `Authorization: Bearer <token>` header.
  2. Validate RS256 signature locally using Auth's public key (fetched at service boot and cached).
  3. Reject request with 401 if token is expired or signature invalid.
* **Introspection:** Call `POST /v1/auth/introspect` ONLY on high-security operations (e.g., password change, financial transfers).

---

## 5. Authorization Usage Rules (`platform-authz-lib`)

* Import `platform-authz-lib` into your REST controller or middleware layer.
* Evaluate permissions in-process:
  ```java
  if (!authzLibrary.hasPermission(principal, "document:delete")) {
      throw new ForbiddenException("Required permission: document:delete");
  }
  ```

---

## 6. Event Usage & Outbox Rules

* **Event Flow:** `Producer -> Producer-owned transactional outbox -> Producer-owned dispatcher -> Events Platform API -> Subscribers`
* **Event Emission:** To emit a domain event, insert the event payload into your local service's `outbox_events` table **inside the same SQL database transaction** as your business data update.
* **Producer Dispatching:** A local producer-owned background worker polls the service's `outbox_events` table (`SKIP LOCKED`) and posts events to the Events Platform API (`POST /v1/events/publish`). The Events Platform **never** directly reads producer database tables.
* **Payload Format:** CloudEvents v1.0 JSON format containing `specversion`, `type`, `source`, `id`, `time`, and `data`.
* **Idempotency:** Event consumers MUST track processed `event_id` values to ignore duplicate event deliveries.

---

## 7. Background Jobs Usage Rules

* **Submission:** Call `POST /v1/jobs/submit` with `job_type`, JSON `payload`, and a unique `job_key`.
* **Handler Idempotency:** Job handlers MUST check `job_key` before executing work to ensure idempotency.
* **Worker Execution:** Workers claim jobs using `FOR UPDATE SKIP LOCKED`. Never block job workers indefinitely; set timeouts on worker handlers.

---

## 8. File Storage & Presigned URL Rules

* **Upload Flow:**
  1. Call `POST /v1/files/upload-url` to receive a presigned S3/MinIO upload URL and `file_id`.
  2. Client uploads file bytes directly to object storage via HTTP `PUT`.
  3. Call `POST /v1/files/{file_id}/confirm` to initiate background ClamAV malware scanning.
* **Download Flow:** Call `GET /v1/files/{file_id}/download-url`. Direct file streaming is permitted ONLY if metadata `status == ACTIVE` (clean scan).

---

## 9. Audit Logging Rules

* Emit audit events out-of-band for security-sensitive operations (login, password change, user creation, privilege modification, resource deletion).
* Format:
  ```json
  { "actor_id": "usr_123", "action": "USER_ROLE_ASSIGNED", "resource_type": "USER", "resource_id": "usr_456", "ip_address": "1.2.3.4" }
  ```

---

## 10. Observability, Logging & Correlation IDs

* **Headers:** Every HTTP request MUST propagate `X-Request-ID` and `X-Correlation-ID`.
* **Structured Logs:** Write JSON logs to `stdout`. Include `timestamp`, `level`, `trace_id`, `service_name`, `user_id`.
* **Secret Masking:** NEVER log passwords, raw OTPs, JWT tokens, credit cards, or refresh tokens. Use regex sanitization filters in logging setup.
* **Fail-Open Isolation:** OpenTelemetry SDKs MUST be configured to drop telemetry buffers silently if the OTel Collector is unreachable.

---

## 11. Git Workflow & Branching Strategy

```
main (Protected — No direct pushes)
  │
  ├── feature/auth-jwt-validation
  ├── feature/notifications-smtp-adapter
  ├── bugfix/jobs-skip-locked-fix
  └── refactor/file-presigned-urls
```

* **Branch Naming:** `feature/<capability>-<short-description>`, `bugfix/...`, `security/...`.
* **Commit Messages:** Meaningful commit messages (e.g. `feat(auth): add Argon2id password hashing policy`).
* **PR Process:** Small, focused PRs requiring clean CI build and test execution before merge.

---

## 12. Security Baseline Enforcement

1. **Passwords:** Argon2id salted memory-hardened hashing.
2. **Secrets:** Injected via environment variables. Zero hardcoded secrets in source code or Git.
3. **Rate Limiting:** IP + Account sliding window throttling on login, register, OTP, and reset routes.
4. **CORS:** Whitelist explicit client origins; never use `Access-Control-Allow-Origin: *` on authenticated routes.
5. **CSRF:** SameSite=Strict, HTTP-only cookies for refresh tokens.
