# ADR-011: Identity & Authentication Session / Token Architecture

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant
* **Resolves:** OPEN-002 (Token Strategy Decision)

---

## 1. Context & Problem
Once a user authenticates successfully (email + password + optional MFA), the platform must issue credentials representing the authenticated principal (`user_id`). Subsequent HTTP API requests to platform capabilities and consuming products must authenticate the principal.

We must decide the session/token architecture:
1. How are authenticated credentials represented?
2. How is session revocation ("logout everywhere", password reset revocation) handled?
3. How do consuming products validate incoming credentials without creating an HTTP bottleneck on central Auth?

---

## 2. Options Considered

### Option A: Pure Stateless Signed JWT Access Tokens
Auth issues cryptographically signed JWT access tokens (RS256) containing `user_id`, `roles`, `scopes`, and `exp`. Consuming services validate tokens using Auth's public key locally without calling Auth.
* **Advantages:** Zero network calls to Auth during API requests; sub-millisecond local validation; stateless horizontal scaling.
* **Disadvantages:** **Cannot be revoked immediately.** If a token is stolen or a user clicks "Logout Everywhere", the JWT remains valid until its `exp` timestamp expires.

### Option B: Opaque Access Tokens with Server-Side Introspection
Auth issues random 256-bit opaque strings (e.g. `tok_9f8a7b...`). Every consuming service must make an HTTP/gRPC introspection request (`POST /v1/auth/introspect`) on EVERY API request.
* **Advantages:** Immediate revocation; zero sensitive data in client-side tokens.
* **Disadvantages:** **Severe Performance Bottleneck.** Every API call across all products generates a synchronous network call to Auth, creating massive database pressure and single-point-of-failure coupling.

### Option C: Stateful Database Sessions (Session ID Cookies)
Auth sets HTTP-only `Set-Cookie: session_id=...` backed by a `auth_sessions` table in `auth_db`.
* **Advantages:** Immediate revocation; standard browser security against XSS.
* **Disadvantages:** Poor support for mobile/native clients; cross-domain CORS complexities for independent products; high DB lookup load.

### Option D: Hybrid Short-Lived JWT + Refresh Token with Revocation List (RECOMMENDED)
1. Auth issues **short-lived signed JWT access tokens** (15-minute TTL) for API requests, containing `user_id`, `roles`, and `session_id`.
2. Auth issues an **opaque refresh token** stored securely (HTTP-only cookie or secure storage) backed by `auth_sessions` table in `auth_db`.
3. Consuming services validate short-lived JWT access tokens locally using Auth's public key (RS256).
4. For high-security operations (e.g., password change, financial transfers), services perform active session status checks against Auth's introspection API.
5. "Logout Everywhere" or password resets set `revoked_at=NOW()` on active sessions in `auth_sessions`, preventing refresh tokens from issuing new JWTs and rendering stolen access tokens expired within 15 minutes max.

---

## 3. Comprehensive Evaluation Matrix

| Architectural Criteria | Option A: Pure Stateless JWT | Option B: Opaque Introspection | Option C: Stateful Sessions | Option D: Hybrid (JWT + Refresh) |
| :--- | :--- | :--- | :--- | :--- |
| **Security & Revocation** | Poor (No immediate revocation) | **Optimal** (Immediate) | **Optimal** (Immediate) | **High** (Max 15-min window; instant refresh block) |
| **API Request Latency** | **Optimal** (< 1ms local) | Poor (+20-50ms network hop) | Poor (+20-50ms network hop) | **Optimal** (< 1ms local) |
| **Database Pressure** | **Optimal** (Zero DB queries) | Poor (Query on every API call) | Poor (Query on every API call) | **Low** (DB query only on refresh every 15m) |
| **Horizontal Scaling** | **Optimal** | Low (Auth DB bottleneck) | Low (Session store bottleneck)| **Optimal** |
| **Browser Security (XSS)** | Moderate (LocalStorage risk) | High | **Optimal** (HTTP-only cookie) | **High** (Refresh token in HTTP-only cookie) |
| **Mobile & Multi-Product** | High | Moderate | Low | **High** |
| **Zero-Cost Infra Fit** | **Fully Aligned** | High DB pressure risk | High DB pressure risk | **Fully Aligned** |

---

## 4. Recommendation & Status
We accept **Option D: Hybrid Short-Lived JWT + Refresh Token with Revocation Check**.

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Next Steps:** Proceed with Phase 2 implementation of Identity & Authentication.
