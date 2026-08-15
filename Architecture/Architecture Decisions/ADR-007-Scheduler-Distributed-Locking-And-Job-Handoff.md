# ADR-007: Scheduler Architecture & Job Platform Handoff

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
The platform requires executing scheduled operations (cron schedules, recurring tasks, delayed execution) reliably across multiple application instances.

## Problem
How should scheduling be designed to prevent duplicate execution when multiple instances of the Scheduler service run concurrently?

## Options Considered

### Option A: Embedded In-Memory Schedulers in Every Instance
* **Disadvantages:** Causes duplicate task triggers when multiple container instances run behind a load balancer.

### Option B: Leader-Elected Scheduler Service with Background Job Handoff (Recommended)
Deploy a dedicated Scheduler Service container using PostgreSQL row-level locks (SELECT FOR UPDATE NOWAIT) to ensure only one instance active leader triggers schedules. When a schedule is due, Scheduler creates a job in **Background Jobs Platform** for worker execution.
* **Advantages:** Strict single-trigger guarantee; separates scheduling logic from heavy task execution; zero extra lock manager needed.

## Decision Recommendation
Propose **Option B**.
