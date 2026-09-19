# LifeOS — Changelog

All notable changes to the **LifeOS** platform will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0-alpha] - 2026-09-19
### Added
* **Phase 1: Architecture & Requirements Specification**
  * Complete domain analysis and bounded context decomposition for LifeOS modular monolith.
  * Technical documentation suite: `README.md`, `ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `RAG_ARCHITECTURE.md`, `API_DOCUMENTATION.md`, `SECURITY.md`, `SETUP.md`.
  * Database schema with 18 normalized tables, foreign key constraints, soft-delete indexes, and PostgreSQL 16 + pgvector support.
  * HNSW vector indexing design (`vector_cosine_ops`, `m=16`, `ef_construction=64`) for 1536-dimensional embeddings.
  * GIN full-text search indexing on `tsvector` with Reciprocal Rank Fusion (RRF) hybrid retrieval algorithm.
  * Deterministic Agentic AI tool registry and confirmation gate design.
  * Strict resource-level authorization model for multi-tenant data privacy.
  * Environment variable templates (`.env.example`) and comprehensive `.gitignore`.
  * Setup instructions with explicit Java 21 LTS installation guide across Windows, macOS, and Linux.
