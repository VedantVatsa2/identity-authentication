# ADR-003: Authorization Model & Policy Evaluation Architecture

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant
* **Depends On:** [ADR-011: Auth Session/Token Architecture](./ADR-011-Auth-Session-Token-Architecture.md)

---

## 1. Context
While Identity & Authentication establishes *who* a user is (`user_id`), Authorization determines *what* an authenticated user or service principal is allowed to do. We must decide how authorization policies (Roles, Permissions, RBAC) are represented and evaluated across consuming products.

---

## 2. Problem
Should Authorization be a remote HTTP service called on every API request, or an embedded shared library / framework module?

---

## 3. Options Considered

### Option A: Centralized Remote Authorization Service
Every incoming API request to consuming applications triggers a synchronous HTTP/gRPC call to a central Authorization Service (`/v1/authz/check`).
* **Advantages:** Central management of all permissions and roles in one database.
* **Disadvantages:** High latency penalty (+20–50ms per request); single point of failure; massive network overhead.

### Option B: Shared Embedded Authorization Library (Recommended)
Package authorization evaluation logic as a shared library (`platform-authz-lib`). Roles/permissions are embedded in validated access token claims (short-lived JWTs per ADR-011) and evaluated locally in-process.
* **Advantages:** Sub-millisecond policy evaluation; zero network overhead; operates offline even if central Auth is temporarily down.
* **Disadvantages:** Token claim size grows if a user has hundreds of fine-grained permissions (mitigated by role-based claim scopes).

---

## 4. Decision Recommendation
Propose **Option B: Shared Embedded Authorization Library (`platform-authz-lib`)**. 

1. Auth includes `roles` and `scopes` inside short-lived signed JWT access tokens (RS256, 15-min TTL per ADR-011).
2. Consuming products import `platform-authz-lib` to evaluate permissions in-process using standard RBAC.
3. Fine-grained resource ownership checks (e.g., "Does user 123 own document 456?") remain owned by consuming application domain code.

---

## 5. Consequences
* High performance with zero extra network hops per API call.
* If a role is revoked in `auth_db`, the user's access is updated when their 15-minute JWT access token expires and is refreshed.
