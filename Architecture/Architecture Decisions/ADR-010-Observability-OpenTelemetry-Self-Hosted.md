# ADR-010: Observability Architecture & Self-Hosted Telemetry Stack

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
All platform capabilities and consuming applications must produce structured logs, metrics, and distributed traces to enable rapid root-cause diagnosis.

## Problem
How should observability be implemented without relying on expensive commercial SaaS products (Datadog/NewRelic) or causing application failures if telemetry is down?

## Options Considered

### Option A: Commercial SaaS Monitoring Integration
* **Disadvantages:** Violates free/no-cost constraint; creates vendor lock-in.

### Option B: OpenTelemetry Standard Collector + Self-Hosted Stack (Recommended)
Applications emit standardized OpenTelemetry traces/metrics and JSON logs to stdout. An out-of-band OpenTelemetry Collector container gathers telemetry and routes it to self-hosted tools (Prometheus, Jaeger, Grafana).
* **Advantages:** Zero-cost open-source stack; standardized OpenTelemetry API; fail-open execution (if collector fails, applications continue operating normally).

## Decision Recommendation
Propose **Option B**.
