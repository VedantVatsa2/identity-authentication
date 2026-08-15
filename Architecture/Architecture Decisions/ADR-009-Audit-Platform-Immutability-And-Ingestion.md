# ADR-009: Audit Platform Architecture & Immutable Ingestion

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
Compliance and security regulations require recording an immutable audit trail answering: *Who did what, when, and to what resource?*

## Problem
How should audit logging be separated from application operational logging to guarantee immutability, tamper-resistance, and high query performance?

## Options Considered

### Option A: Standard Application Log Statements
* **Disadvantages:** Audit entries get mixed with debug/info logs; difficult to query structured historical events; vulnerable to log truncation.

### Option B: Dedicated Audit Ingestion Service & Append-Only Storage (Recommended)
Deploy an Audit Service backing an append-only database (udit_db). Services push structured audit events (user_id, ction, 
esource_type, 
esource_id, 	imestamp, ip, correlation_id). Audit tables prohibit UPDATE and DELETE SQL operations via DB triggers.
* **Advantages:** Strict immutability; dedicated security query indexes; isolated retention policies.

## Decision Recommendation
Propose **Option B**.
