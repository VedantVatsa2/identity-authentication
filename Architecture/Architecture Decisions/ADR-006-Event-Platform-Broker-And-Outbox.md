# ADR-006: Event Platform Architecture & Transactional Outbox Pattern

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
To decouple platform capabilities and consuming products, domain events (e.g., USER_REGISTERED, DOCUMENT_DELETED) must be published and consumed asynchronously.

## Problem
How can services guarantee that domain events are reliably published when database updates succeed, without dual-write inconsistencies?

## Options Considered

### Option A: Direct Dual-Write (Update DB then Publish to Event Broker)
* **Disadvantages:** If the process crashes after updating the database but before publishing the event, downstream consumers miss the event permanently.

### Option B: Transactional Outbox Pattern + Producer Dispatcher + Events Platform (Accepted)
Producers write domain events to a local `outbox_events` table inside the same database transaction as business updates. A producer-owned background dispatcher reads local pending outbox entries and pushes them via HTTP to the Events Platform API (`POST /v1/events/publish`), which routes events to subscribed consumer APIs.
* **Advantages:** Guarantees at-least-once event delivery; respects database isolation boundaries (Events Platform never reads producer databases directly); eliminates dual-write race conditions; works with zero paid external brokers.

## Decision
We accept **Option B: Transactional Outbox Pattern + Producer Dispatcher + Events Platform**.
