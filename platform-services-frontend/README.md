# Platform Services Frontend

A modern, secure, frontend-only single-page application (SPA) built with React 19, TypeScript, and Vite. This application serves as the frontend interface for the reusable application platform, connecting to the platform's backend services—primarily the **Identity & Authentication Service** (`platform-services/auth-service`).

---

## 1. Project Purpose & Boundary

- **What it is:** The user-facing web interface for authentication (login, registration, email verification flow) and protected application access within the platform ecosystem.
- **Relationship with Backend:** Consumes REST APIs provided by `platform-services/auth-service` (Spring Boot modular monolith running on `http://127.0.0.1:8080`).
- **Frontend-Only Scope:** This repository is strictly frontend-only. It does **not** contain backend code, does **not** directly query PostgreSQL database schemas (`auth_db`), does **not** inspect transactional outbox tables (`auth_outbox_events`), and does **not** expose internal data.

---

## 2. Technology Stack

- **Core Framework:** [React 19](https://react.dev/) + [TypeScript 6](https://www.typescriptlang.org/)
- **Build Tooling & Dev Server:** [Vite 8](https://vite.dev/) with `@vitejs/plugin-react` & `@tailwindcss/vite`
- **Routing:** [React Router v7](https://reactrouter.com/) (`createBrowserRouter`, `RouterProvider`)
- **Data Fetching & Cache:** [TanStack React Query v5](https://tanstack.com/query/v5) (`QueryClientProvider`)
- **Form Management & Validation:** [React Hook Form v7](https://react-hook-form.com/) + [Zod v3](https://zod.dev/) via `@hookform/resolvers`
- **Styling & UI:** [Tailwind CSS v4](https://tailwindcss.com/) with `clsx`, `tailwind-merge`, and [Lucide React](https://lucide.dev/) icons
- **Testing & Quality:** [Vitest v4](https://vitest.dev/), [@testing-library/react](https://testing-library.com/), [Oxlint](https://oxc.rs/docs/guide/usage/linter), [Playwright](https://playwright.dev/) for E2E

---

## 3. Repository Structure

```
platform-services-frontend/
├── .env                     # Local environment configuration (VITE_API_BASE_URL)
├── .env.example             # Example environment template
├── .oxlintrc.json           # Oxlint code quality configuration
├── package.json             # Dependencies and npm scripts
├── vite.config.ts           # Vite server, dev proxy (/v1 -> http://127.0.0.1:8080), & Vitest config
├── src/
│   ├── api/
│   │   ├── auth/
│   │   │   ├── authApi.ts   # Typed methods for auth endpoints (register, verify, login, refresh, logout)
│   │   │   └── authTypes.ts # DTO interfaces & ApiError class matching backend RFC 7807 errors
│   │   └── client/
│   │       ├── apiClient.ts   # Centralized fetch wrapper (credentials: 'include', 401 auto-refresh retry)
│   │       └── tokenManager.ts# In-memory access token storage & auth failure listener registry
│   ├── auth/
│   │   ├── AuthContext.ts     # Context interface (status, user session, auth methods)
│   │   ├── AuthProvider.tsx   # Top-level auth state provider (INITIALIZING -> AUTHENTICATED / UNAUTHENTICATED)
│   │   └── useAuth.ts         # Hook to consume AuthContext
│   ├── components/
│   │   ├── ProtectedRoute.tsx # Route guard requiring AUTHENTICATED status
│   │   ├── PublicOnlyRoute.tsx# Route guard restricting access for already-authenticated users
│   │   └── ui/                # Reusable UI elements (Alert, Button, Card, Input, Spinner)
│   ├── pages/
│   │   ├── AppShell/          # Protected application shell dashboard
│   │   ├── Login/             # Sign-in page with email/password
│   │   ├── Register/          # Account registration page
│   │   └── VerifyEmail/       # OTP email verification page (URL query param challengeId validation)
│   ├── router/
│   │   └── index.tsx          # React Router route definitions (/login, /register, /verify-email, /verify, /)
│   ├── test/
│   │   └── setup.ts           # Vitest setup & testing-library/jest-dom extensions
│   ├── App.tsx                # App entry component (QueryClientProvider + AuthProvider + RouterProvider)
│   └── main.tsx               # DOM mounting entrypoint
└── e2e/                       # Playwright end-to-end test scenarios
```

---

## 4. Backend API Integration

The frontend integrates directly with `auth-service` REST endpoints under `/v1/auth/*`:

### 1. `POST /v1/auth/register`
- **Consumed in:** [`src/api/auth/authApi.ts`](file:///d:/Startup/platform-services-frontend/src/api/auth/authApi.ts) -> `RegisterPage.tsx`
- **Request Body:** `{ "email": "user@example.com", "password": "Password123!" }`
- **Response (201 Created):** `{ "userId": "<UUID>", "status": "PENDING_VERIFICATION" }`
- **Error Handling:** Returns `400 Bad Request` or `409 Conflict` (`EMAIL_ALREADY_REGISTERED`).
- **Cookie Behavior:** None.

### 2. `POST /v1/auth/verify`
- **Consumed in:** [`src/api/auth/authApi.ts`](file:///d:/Startup/platform-services-frontend/src/api/auth/authApi.ts) -> `VerifyEmailPage.tsx`
- **Request Body:** `{ "challengeId": "<UUID>", "otp": "<6-digit OTP>" }`
- **Response (200 OK):** `{ "status": "ACTIVE", "message": "Email verification successful." }`
- **Error Handling:** Returns `400 Bad Request` (`VERIFICATION_FAILED`) on invalid/expired challenge or incorrect OTP.
- **Cookie Behavior:** None.

### 3. `POST /v1/auth/login`
- **Consumed in:** [`src/api/auth/authApi.ts`](file:///d:/Startup/platform-services-frontend/src/api/auth/authApi.ts) -> `LoginPage.tsx`
- **Request Body:** `{ "email": "user@example.com", "password": "Password123!" }`
- **Response (200 OK):** `{ "userId": "<UUID>", "sessionId": "<UUID>", "accessToken": "<RS256 JWT>" }`
- **Set-Cookie Header:** Backend sets `refreshToken` as an `HttpOnly; Secure; SameSite=Strict` cookie.
- **Frontend Action:** Stores `accessToken` in memory via `tokenManager.ts` and sets session state.

### 4. `POST /v1/auth/refresh`
- **Consumed in:** [`src/api/client/apiClient.ts`](file:///d:/Startup/platform-services-frontend/src/api/client/apiClient.ts) (`performRefresh()`)
- **Request Body:** None.
- **Cookie Requirement:** `credentials: 'include'` sends the HttpOnly `refreshToken` cookie.
- **Response (200 OK):** `{ "userId": "<UUID>", "sessionId": "<UUID>", "accessToken": "<RS256 JWT>" }`
- **Set-Cookie Header:** Rotates refresh token cookie.
- **Frontend Action:** Updates in-memory access token and retries failed original request seamlessly.

### 5. `POST /v1/auth/logout`
- **Consumed in:** [`src/api/auth/authApi.ts`](file:///d:/Startup/platform-services-frontend/src/api/auth/authApi.ts) -> `AuthProvider.tsx`
- **Request Body:** None (`credentials: 'include'`).
- **Response (204 No Content):** Revokes session server-side and clears refresh cookie.
- **Frontend Action:** Clears in-memory access token and transitions state to `UNAUTHENTICATED`.

---

## 5. Authentication Architecture & Security Model

```
 ┌────────────────────────────────────────────────────────┐
 │                      BROWSER                           │
 │                                                        │
 │  ┌──────────────────────────────────────────────────┐  │
 │  │ React SPA State (JS Memory)                      │  │
 │  │ - Access Token (RS256 JWT, 15-min TTL)           │  │
 │  │ - User Session (userId, sessionId)               │  │
 │  └──────────────────────────────────────────────────┘  │
 │                           │                            │
 │  ┌────────────────────────┴─────────────────────────┐  │
 │  │ HttpOnly Cookie Store (Browser Managed)          │  │
 │  │ - Refresh Token Cookie (SameSite=Strict)         │  │
 │  │   (JavaScript CANNOT access or read this!)       │  │
 │  └──────────────────────────────────────────────────┘  │
 └───────────────────────────┬────────────────────────────┘
                             │ credentials: 'include'
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │  platform-services / auth-service (Port 8080)          │
 └────────────────────────────────────────────────────────┘
```

1. **Short-Lived Access Tokens in Memory:** The 15-minute JWT access token is stored **strictly in JavaScript memory** (`inMemoryAccessToken` in `tokenManager.ts`).
2. **HttpOnly Refresh Token Cookies:** Server-side sessions and refresh tokens use `HttpOnly`, `Secure`, `SameSite=Strict` cookies. Client-side JavaScript cannot read, inspect, or modify the refresh token.
3. **No Storage Persistence:** No tokens, credentials, or sensitive auth data are ever written to `localStorage` or `sessionStorage`.
4. **Transparent 401 Refresh Handling:** When an API request returns `401 Unauthorized`, [`apiClient.ts`](file:///d:/Startup/platform-services-frontend/src/api/client/apiClient.ts) automatically queues incoming requests, executes `POST /v1/auth/refresh` once with `credentials: 'include'`, updates the in-memory token, and retries the original request.
5. **Sanitized Logging:** Zero passwords, raw OTPs, JWTs, or session tokens are logged to `console` or telemetry.

---

## 6. Registration & Email Verification Status

> [!IMPORTANT]
> **Current Backend Contract Limitation:**
> - `POST /v1/auth/register` returns **only** `userId` and `status: PENDING_VERIFICATION`. It does **not** return `challengeId`.
> - `POST /v1/auth/verify` requires `{ "challengeId": "<UUID>", "otp": "<6-digit OTP>" }`.
> - The existing backend does **not** expose an API endpoint to look up or query `challengeId`.
>
> As a result, the user-facing verification flow **cannot be completed end-to-end** directly from a fresh registration without an external verification link containing `challengeId`.

### Intended Future Flow
1. User registers via `POST /v1/auth/register`.
2. Backend creates challenge record in `auth_verification_challenges` and emits a `USER_REGISTERED` event to `auth_outbox_events`.
3. Outbox Dispatcher + Notifications Service (Phase 4 backend infrastructure) sends an email containing a verification link:
   `https://app.example.com/verify?challengeId=8788d19d-48d0-415d-80fd-241c92003a27`
4. Clicking the link opens `VerifyEmailPage.tsx` with `challengeId` in URL search parameters.
5. Frontend validates `challengeId` UUID format and presents **only** the 6-digit OTP input to the user.
6. User enters the 6-digit OTP from the email.
7. Frontend calls `POST /v1/auth/verify` with `{ challengeId, otp }`.
8. User status transitions to `ACTIVE`.

---

## 7. Known Limitations & Deferred Work

- **No Real Email Delivery:** Notification service and outbox dispatcher worker have not yet been implemented in the backend repository.
- **No Manual UUID Input:** Per UX guidelines, users are **not** asked to manually type Challenge ID UUID strings into form fields.
- **No Database Workarounds:** The frontend strictly respects backend platform boundaries and does **not** attempt to query PostgreSQL or mock outbox event endpoints.

---

## 8. Local Development Setup

### Prerequisites
- Node.js 22+ or 24+
- npm 9+
- Backend `auth-service` running locally on port 8080 (or Docker PostgreSQL container running)

### Installation
```bash
cd platform-services-frontend
npm install
```

### Environment Configuration
Copy `.env.example` to `.env`:
```env
# Leave empty in local dev to enable Vite dev server proxying (/v1 -> http://127.0.0.1:8080)
VITE_API_BASE_URL=
```

### Running Locally
```bash
npm run dev
```
The application will start at `http://localhost:5173`. Requests to `/v1/*` are proxied to `http://127.0.0.1:8080` by Vite.

---

## 9. Validation Commands

All four core quality validation commands are verified and passing cleanly:

```bash
# 1. Code Quality & Linting (Oxlint 1.75.0)
npm run lint

# 2. TypeScript Static Type Check (tsc --noEmit)
npm run typecheck

# 3. Unit Test Suite (Vitest - 16/16 tests pass)
npm test

# 4. Production Build Verification
npm run build
```

---

## 10. Security Compliance Rules

- **Zero Client Secrets:** No private keys or secret environment variables in Vite frontend code.
- **Browser Untrusted Model:** All security controls, role checks, and token verifications are enforced by the Spring Boot backend.
- **CSRF & Token Isolation:** Refresh tokens are bound to `HttpOnly`, `SameSite=Strict` cookies.

---

## 11. Next Steps / Handoff

> [!NOTE]
> **Frontend Integration Checkpoint:**
> Frontend authentication UI, route guarding, React Hook Form validation, and API integration are currently **PAUSED at the Phase 2 frontend integration checkpoint**.
>
> The frontend authentication foundation is implemented and validated, but the complete registration-to-email-verification user journey remains dependent on the backend notification/email infrastructure.
>
> **Recommended Next Platform Work:**
> The next engineering effort should focus on backend/infrastructure capability:
> 1. Implement Phase 4 **Notifications Service** (`notifications-service`).
> 2. Implement the **Auth Outbox Dispatcher** worker to poll `auth_outbox_events` and dispatch registration emails containing `/verify?challengeId=<UUID>` links and OTP codes.
