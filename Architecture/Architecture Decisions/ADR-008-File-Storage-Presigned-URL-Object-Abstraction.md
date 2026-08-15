# ADR-008: File Storage Architecture & Presigned URL Transfers

* **Status:** ACCEPTED / FINALIZED (Architecture Baseline Frozen)
* **Date:** 2026-08-15
* **Deciders:** Human Architect, AI Architecture Assistant

---

## Context
Applications require uploading, downloading, and storing user files (documents, images, research exports).

## Problem
Should file binary data be stored directly in PostgreSQL database bytea columns or routed through application servers?

## Options Considered

### Option A: Store File Bytes in PostgreSQL Database
* **Disadvantages:** Causes massive database bloat, slow backups, high memory consumption, and degrades query performance.

### Option B: Presigned URL Object Storage Abstraction (MinIO / S3) (Recommended)
File Storage Service manages file metadata in 
iles_db and generates short-lived presigned upload/download URLs for an S3-compatible object store (MinIO for local dev, AWS S3 / Cloudflare R2 for production). Clients transfer binary bytes directly to object storage.
* **Advantages:** Bypasses application server CPU/bandwidth bottlenecks; database remains lean; MinIO provides a 100% free self-hosted S3-compatible backend.

## Decision Recommendation
Propose **Option B**.
