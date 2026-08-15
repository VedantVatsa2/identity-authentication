# ADR-002: Internal Component Architecture & Deployment Model for Identity & Authentication

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context

Diagram #1 (System Context Diagram) established that Identity & Authentication is a shared platform capability deployed independently of consuming applications. 

We now need to decide the internal deployment boundary and component structure inside the Auth platform itself (Diagram #2: Container / Component Diagram).

We must decide whether to structure Auth internally as:
1. A **Modular Monolith Deployment Unit** (single deployable container containing clean internal domain modules and a single logical PostgreSQL database).
2. A **Microservices Architecture** (decomposing Auth into multiple independent services: Identity Service, Token Service, Verification Worker Service, Client Registry Service, each with its own database/runtime).

---

## Problem

What internal deployment boundary minimizes operational complexity and system friction while maintaining clean domain boundaries, high security, testability, and future extensibility?

---

## Options Considered

### Option A: Decomposed Microservices within Auth Platform
Split Auth into 4-5 microservices (e.g., `User-Identity-Service`, `Authentication-Token-Service`, `OTP-Verification-Service`, `Notification-Worker-Service`), each running in its own container with independent databases.

* **Advantages:**
  * Sub-components can be deployed and autoscaled independently.
  * Fault isolation: a crash in the OTP verification component does not take down token validation.
* **Disadvantages:**
  * **Severe Over-Engineering:** Introduces distributed transactions (Sagas / 2PC) for simple workflows like registration (which requires identity creation + challenge generation atomically).
  * **High Operational Complexity:** Requires multi-container orchestration, inter-service gRPC/REST overhead, distributed tracing, and complex CI/CD matrices.
  * **Violates Project Guidelines:** Violates the directive to avoid microservices and complex infrastructure unless a concrete architectural requirement demands it.

### Option B: Modular Monolith Container for Auth Platform (Recommended)
Package all Auth sub-components (`Auth API Controller Layer`, `Identity Management Module`, `Credential & Password Module`, `Session & Token Module`, `Verification & Challenge Module`, `Client Registry Module`, `Transactional Outbox Worker`) into a **single deployable container** backed by a **single dedicated PostgreSQL database**.

* **Advantages:**
  * **Simplicity & High Reliability:** Single deployment unit, simple CI/CD pipeline, straightforward local debugging, zero distributed system overhead.
  * **ACID Transaction Boundaries:** User registration and verification challenge generation execute inside standard database transactions, guaranteeing atomic consistency.
  * **Strict Modular Boundaries:** Logical code encapsulation (Java packages / Python modules) enforces low coupling and high cohesion internally, allowing clean future extraction into separate services *if and only if* scaling metrics demand it.
  * **Stateless Horizontal Scalability:** The Auth process is completely stateless (state resides in PostgreSQL/Cache), allowing multiple identical container instances to run behind a load balancer.
* **Disadvantages:**
  * Redeploying any internal module requires redeploying the container instance.

---

## Decision

We propose **Option B: Modular Monolith Container for Auth Platform**.

1. **Deployment Container:** The Identity & Authentication platform will be deployed as a single, stateless application container (e.g., Spring Boot / FastAPI).
2. **Database Isolation:** Auth will use a single dedicated PostgreSQL database instance/schema (`auth_db`).
3. **Internal Domain Boundaries:** Clear module boundaries will be maintained in code:
   * `api`: HTTP/REST controllers, request DTO validation, response formatting, rate limiting interceptors.
   * `identity`: User registration, identity lifecycle management, account status enforcement.
   * `credential`: Secure password hashing, password validation, password reset logic.
   * `session`: Token/Session issuance, renewal, revocation, and validation logic.
   * `verification`: OTP challenge generation, attempt throttling, verification challenge validation.
   * `client_registry`: Consuming application registration, API key/secret verification, client scoping.
   * `outbox_worker`: Internal asynchronous thread pool/worker reading pending outbox notification events and invoking external notification delivery.

---

## Consequences

* The platform avoids unnecessary distributed system failures during early application development.
* All internal module communication occurs via in-process method calls with zero network latency.
* If a specific sub-component (e.g., Token Validation) eventually experiences extreme load, its stateless nature allows scaling out the container instances horizontally before considering service decomposition.

---

## Related Deliverables

* **Diagram 2:** Container / Component Diagram (Proposed)
* **ADR-001:** Auth Platform Boundary & Data Ownership (Accepted)
