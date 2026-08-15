# ADR-001: Identity & Authentication Platform Boundary & Data Ownership

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context

We are building a reusable application platform intended to serve multiple independent user-facing applications (e.g., Document Vault, Research Platform, Financial Applications). A foundational capability required by all consuming products is user identity management and authentication.

Without a centralized platform capability, every product team would re-implement user registration, credential hashing, session management, and authentication APIs, leading to duplicated effort, fragmented user identity, and inconsistent security practices.

---

## Problem

How should user identity and authentication capabilities be structured across the platform and consuming applications?

1. Should Auth be an embedded library inside each application, or a shared platform capability?
2. Who owns user identity versus application business data?
3. How should consuming applications access Auth data?

---

## Options Considered

### Option A: Embedded Auth Library / Framework Module
Auth logic is packaged as a shared library (e.g., JAR/npm package) compiled directly into each consuming application, sharing the application's process and database.

* **Advantages:**
  * Zero network overhead; fast local execution.
  * Simple initial project setup.
* **Disadvantages:**
  * Security patches or authentication logic updates require rebuilding and redeploying every consuming application.
  * Security-sensitive credential handling code exists inside every application runtime.
  * Product applications share database tables with Auth, creating tight data coupling.

### Option B: Shared Platform Service with Strict Data Ownership (Selected)
Identity & Authentication is deployed as an independent platform capability with an explicit service boundary. Auth owns user identity and credentials; consuming products own domain-specific business data and reference Auth users via immutable `user_id` identifiers.

* **Advantages:**
  * **Centralized Security & Maintenance:** Security updates, password hashing upgrades, and MFA policies can be updated independently without forcing immediate redeployment of consuming apps.
  * **Strong Decoupling:** Business domain applications (e.g., Document Vault) have zero access to raw passwords or credential hashes.
  * **Multi-Product Reusability:** Multiple independent applications consume the same platform Auth service via defined API contracts.
* **Disadvantages:**
  * Requires network communication between consuming applications and Auth.
  * Requires explicit versioning of API contracts.

---

## Decision

We accept **Option B**. 

1. **Shared Platform Capability:** Identity & Authentication will be implemented as an independently deployable shared platform capability.
2. **Domain Boundary:** Auth owns identity, credentials, sessions, and verification challenges. Consuming applications own domain data and reference Auth users exclusively via immutable `user_id` tokens.
3. **Database Isolation:** Direct database access to Auth tables by consuming applications is strictly prohibited. All interactions must proceed through defined platform API contracts.
4. **Communication Model Deferred:** The exact HTTP/browser flow (e.g., browser-to-Auth vs. backend-to-Auth) and session token format (JWT vs. Opaque vs. Stateful Session) remain **OPEN** for evaluation in subsequent sequence and token flow deliverables.

---

## Consequences

* Consuming applications cannot write custom SQL queries against user credentials or sessions.
* Integration between products and Auth must occur via HTTP/REST (or gRPC) API contracts or client SDKs.
* The platform team can evolve credential security, hashing algorithms, and session revocation logic without breaking consuming applications.

---

## Related Deliverables

* **Diagram 1:** System Context Diagram (v1 Accepted)
* **Diagram 2:** Container / Component Diagram (Proposed)
