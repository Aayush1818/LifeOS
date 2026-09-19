# LifeOS — Changelog

All notable changes to the **LifeOS** platform will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.2.0-alpha] - 2026-09-19
### Added
* **Phase 2: Spring Boot 3.3.5 Foundation, PostgreSQL + pgvector & Flyway Migrations**
  * Configured root Maven project strictly targeting **Java 21 LTS** with Spring Boot 3.3.5 parent.
  * Generated official Maven Wrapper (`mvnw` and `mvnw.cmd`) using Apache Maven 3.9.9.
  * Verified and connected to local PostgreSQL with native `pgvector` v0.8.6 extension.
  * Applied foundational Flyway migration `V1__init_schema.sql` creating all 18 approved domain tables.
  * Verified HNSW vector index (`idx_chunks_hnsw` on `document_chunks` using `vector_cosine_ops`, `m=16`, `ef_construction=64`).
  * Verified full-text search GIN index (`idx_chunks_tsv` on `tsv_content`).
  * Implemented centralized configuration and environment-specific profiles:
    * `application.yml` (Base configuration, actuator exposure, multipart limits, Jackson rules).
    * `application-postgres.yml` (PostgreSQL connection, HikariCP connection pool with 10 max connections, Flyway validate).
    * `application-dev.yml` (H2 fallback profile for standalone test runs).
  * Implemented Base Entity architecture (`BaseEntity.java`) with JPA auditing, UUID generation, and soft delete.
  * Implemented standardized API response contracts (`ApiResponse.java`, `ErrorResponse.java`) with RFC 7807 problem details in `GlobalExceptionHandler.java`.
  * Configured OpenAPI 3.0 / Swagger UI documentation via `springdoc-openapi-starter-webmvc-ui` (v2.6.0).
  * Implemented `/api/v1/health/ping` diagnostics endpoint verifying live database and pgvector extension status.
  * Automated testing suite (5 tests passing: `FlywayMigrationTest`, `HealthControllerTest`, `LifeOSApplicationTests`).

---

## [0.1.0-alpha] - 2026-09-19
### Added
* **Phase 1: Architecture & Requirements Specification**
  * Complete domain analysis and bounded context decomposition for LifeOS modular monolith.
  * Technical documentation suite: `README.md`, `ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `RAG_ARCHITECTURE.md`, `API_DOCUMENTATION.md`, `SECURITY.md`, `SETUP.md`.
  * Database schema with 18 normalized tables, foreign key constraints, soft-delete indexes, and PostgreSQL 16/18 + pgvector support.
  * HNSW vector indexing design (`vector_cosine_ops`, `m=16`, `ef_construction=64`) for 1536-dimensional embeddings.
  * GIN full-text search indexing on `tsvector` with Reciprocal Rank Fusion (RRF) hybrid retrieval algorithm.
  * Deterministic Agentic AI tool registry and confirmation gate design.
  * Strict resource-level authorization model for multi-tenant data privacy.
  * Environment variable templates (`.env.example`) and comprehensive `.gitignore`.
  * Setup instructions with explicit Java 21 LTS installation guide across Windows, macOS, and Linux.
