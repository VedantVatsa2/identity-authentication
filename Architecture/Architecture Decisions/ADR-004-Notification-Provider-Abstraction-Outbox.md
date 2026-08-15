# ADR-004: Notifications Platform Architecture & Vendor Abstraction

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
Multiple capabilities (Auth, Background Jobs) and consuming applications require delivering transactional messages (Email, SMS, Push). Directly hard-coding SendGrid, Twilio, or AWS SES SDKs into business modules causes tight coupling and vendor lock-in.

## Problem
How should notification delivery be architected to isolate vendor dependencies, guarantee asynchronous non-blocking delivery, handle retries, and support zero-cost development?

## Options Considered

### Option A: Direct Vendor Integration in Each Capability
Each capability (Auth, Jobs) imports third-party SDKs directly and calls external email APIs synchronously.
* **Disadvantages:** Exposes API controllers to external timeouts; duplicates vendor credentials; breaks free/local testing.

### Option B: Standalone Notifications Service with Pluggable Vendor Gateways (Recommended)
Deploy a dedicated Notifications Platform Service backed by a database (
otifications_db). Expose a REST API for notification dispatch requests, render templates server-side, and process delivery via pluggable provider adapters (e.g. SendGrid, SMTP, Mock).
* **Advantages:** Pluggable providers allow swapping email vendors without code changes; local development uses SMTP/Mock adapters with zero cost; retries and rate limits are centralized.

## Decision Recommendation
Propose **Option B**. Notifications Platform handles template rendering, delivery status tracking, and provider failover asynchronously.
