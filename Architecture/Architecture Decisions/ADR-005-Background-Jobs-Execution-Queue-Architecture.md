# ADR-005: Background Jobs Platform Queue & Worker Architecture

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
Applications require executing long-running or asynchronous operations (e.g., report generation, data imports, batch tasks) outside the HTTP request-response lifecycle.

## Problem
What queue and execution architecture should handle background jobs without introducing expensive or complex distributed message brokers (Kafka/RabbitMQ) prematurely?

## Options Considered

### Option A: Introduce Redis / Celery / RabbitMQ Infrastructure
* **Disadvantages:** Introduces Redis/RabbitMQ infrastructure components, increasing operational memory footprint and violating our directive to avoid complex infrastructure unless justified.

### Option B: PostgreSQL-Backed Job Queue with SKIP LOCKED Concurrency (Recommended)
Use PostgreSQL table jobs within jobs_db. Workers claim available jobs using SQL FOR UPDATE SKIP LOCKED.
* **Advantages:** Zero additional infrastructure cost; ACID transactional job submission; robust crash recovery; supported natively in PostgreSQL 9.5+.
* **Disadvantages:** Higher DB IOPS under extreme concurrency (>10,000 jobs/sec), manageable via worker polling tuning.

## Decision Recommendation
Propose **Option B**. Use PostgreSQL SKIP LOCKED job queueing for early platform phases.
