# Reusable Application Platform

> **Architecture Documentation Status:** FINAL & FROZEN  
> **Architecture Decision Status:** ACCEPTED / FINAL  
> **Implementation Status:** READY TO BEGIN (Phase 1)  

---

## 1. Project Purpose

This repository houses a **reusable application platform** designed to serve multiple independent user-facing products (e.g. Document Vault, Research Platform, Financial Applications).

The platform abstracts foundational capabilities—identity, authentication, authorization, transactional notifications, background job processing, domain event routing, cron scheduling, file storage, audit logging, and observability—allowing consuming product applications to focus exclusively on their core business domains.

The system is engineered for zero-cost local development. All components run locally via Docker Compose using self-hosted open-source software (PostgreSQL, MinIO, Mailpit, OpenTelemetry, Prometheus, Jaeger, Grafana).

---

## 2. Platform Capabilities

| Capability | Implementation Boundary Form | Primary Storage / Backend | Description |
| :--- | :--- | :--- | :--- |
| **1. Identity & Authentication** | Stateless Modular Monolith Container (`auth-service`) | PostgreSQL `auth_db` | Owns user identities, Argon2id password hashing, email OTP verification, RS256 JWT access tokens (15-min TTL), and refresh token session rotation. |
| **2. Authorization** | Shared Embedded Library (`platform-authz-lib`) | In-Process JWT Claim Evaluation | Evaluates signed JWT access token claims locally for sub-millisecond RBAC permission checks without network latency. |
| **3. Notifications** | Service + Vendor Adapters (`notifications-service`) | PostgreSQL `notifications_db` | Multi-channel message delivery (Email, SMS, Push) with server-side templating and pluggable vendor adapters (SMTP/Mailpit/SendGrid). |
| **4. Background Jobs** | Service + Worker Pool (`jobs-service`) | PostgreSQL `jobs_db` | Asynchronous task execution engine using PostgreSQL `SKIP LOCKED` worker node claiming with at-least-once execution and retry backoff. |
| **5. Events** | Producer Outbox + Router (`events-router`) | Producer DB `outbox_events` | Decoupled domain event routing via Transactional Outbox Pattern; producer dispatchers push CloudEvents v1.0 payloads to subscriber APIs. |
| **6. Scheduler** | Leader-Elected Service (`scheduler-service`) | PostgreSQL `scheduler_db` | Evaluates cron schedules and delayed tasks using PostgreSQL row locking (`NOWAIT`) and submits execution jobs to Background Jobs API. |
| **7. File Storage** | Service + Object Storage (`file-service`) | PostgreSQL `files_db` + MinIO / S3 | Presigned S3/MinIO upload/download URLs for direct binary transfers; includes asynchronous ClamAV background quarantine malware scanning. |
| **8. Audit** | Ingestion Service (`audit-service`) | PostgreSQL `audit_db` (Append-Only) | Immutable compliance log answering *Who did what, when, and to what resource*; DB triggers reject `UPDATE` and `DELETE` SQL queries. |
| **9. Observability** | Shared Container Stack | OTel Collector + Prometheus / Grafana | Out-of-band telemetry collection (logs, metrics, traces, health checks) with fail-open client SDK isolation. |

---

## 3. Architecture & Implementation Documentation

All architectural decisions, developer guidelines, and implementation roadmaps are consolidated in the **`Architecture/`** directory:

* 📘 **Primary Architecture Source of Truth:** **[Architecture/ARCHITECTURE.md](./Architecture/ARCHITECTURE.md)**  
  *Contains the consolidated 25-section system design, capability boundaries, token flows, database schema isolation, and security baselines.*
* 🛠️ **Developer Implementation Guide:** **[Architecture/IMPLEMENTATION-GUIDE.md](./Architecture/IMPLEMENTATION-GUIDE.md)**  
  *Practical developer rules for repository structure, API standards (RFC 7807), error handling, logging, correlation IDs (`X-Correlation-ID`), Git workflow, and security rules.*
* 🚀 **Implementation Readiness & Phase Order:** **[Architecture/IMPLEMENTATION-READINESS.md](./Architecture/IMPLEMENTATION-READINESS.md)**  
  *Defines the 10 sequential implementation phases (starting with Phase 1: Repository Structure & Foundation).*
* 📜 **Architecture Decision Log (ADRs):** **[Architecture/Architecture Decisions/](./Architecture/Architecture%20Decisions/)**  
  *Historical records of finalized architectural decisions ([ADR-001](./Architecture/Architecture%20Decisions/ADR-001-Auth-Platform-Boundary.md) through [ADR-011](./Architecture/Architecture%20Decisions/ADR-011-Auth-Session-Token-Architecture.md)).*
* 📊 **Central Status Tracker:** **[Architecture/ARCHITECTURE-STATUS.md](./Architecture/ARCHITECTURE-STATUS.md)**  
  *Final status summary and capability matrix.*

---

## 4. Implementation Phase Order

Implementation must proceed strictly in the following 10 phases:

1. **Phase 1:** Repository Structure, Docker Compose Infrastructure, CI, Security Headers & Logging Foundation
2. **Phase 2:** Identity & Authentication (`auth-service`)
3. **Phase 3:** Authorization Shared Library (`platform-authz-lib`)
4. **Phase 4:** Notifications (`notifications-service`)
5. **Phase 5:** Background Jobs (`jobs-service`)
6. **Phase 6:** Events (`events-router`)
7. **Phase 7:** Scheduler (`scheduler-service`)
8. **Phase 8:** File Storage (`file-service` + MinIO + ClamAV)
9. **Phase 9:** Audit (`audit-service`)
10. **Phase 10:** Observability Hardening (OpenTelemetry + Prometheus + Jaeger + Grafana)
