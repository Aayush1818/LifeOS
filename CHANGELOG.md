# LifeOS — Changelog

All notable changes to the **LifeOS** platform will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0-alpha] - 2026-09-19
### Added
* **Phase 3: Security, Authentication, Users & Dependent Management**
  * Integrated `spring-boot-starter-security` and `io.jsonwebtoken:jjwt:0.12.6` with Spring Security 6 stateless filter chain.
  * Implemented BCrypt password hashing (strength 12) and password complexity validation (`@ValidPassword`: min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special character).
  * Dual-token security architecture:
    * Short-lived HMAC-SHA512 signed JWT Access Tokens (15-minute TTL) containing user ID, email, role, and claims.
    * Cryptographically secure 64-character hex Refresh Tokens (7-day TTL) stored exclusively as SHA-256 hashes in PostgreSQL `refresh_tokens` table.
    * Refresh Token Rotation with automatic revocation of consumed tokens and immediate invalidation on reuse detection.
  * Role-Based Access Control (RBAC): `ROLE_USER` and `ROLE_ADMIN` with Spring Security `GrantedAuthority` mapping.
  * Resource-Level Authorization:
    * Strict tenant isolation where all queries and mutations are scoped to `userId = SecurityUtils.getCurrentUserId()`.
    * Zero cross-tenant leakage: cross-tenant access attempts return RFC 7807 `404 Not Found` rather than `403 Forbidden`, preventing resource ID enumeration.
  * User profile management:
    * `GET /api/v1/users/me` — fetch authenticated user profile.
    * `PUT /api/v1/users/me` — update profile name, phone number, and timezone.
  * Family member & dependent management:
    * `POST /api/v1/dependents` — create dependent with relationship type (`SPOUSE`, `CHILD`, `PARENT`, `SIBLING`, `OTHER`) and JSONB `preferences` & `medicalNotes`.
    * `GET /api/v1/dependents` — list authenticated user's active dependents.
    * `GET /api/v1/dependents/{id}` — fetch single dependent with strict owner validation.
    * `PUT /api/v1/dependents/{id}` — update dependent metadata and JSONB attributes.
    * `DELETE /api/v1/dependents/{id}` — soft-delete dependent.
  * Standardized RFC 7807 problem details error handling for 401 Unauthorized (`CustomAuthenticationEntryPoint`) and 403 Forbidden (`CustomAccessDeniedHandler`).
  * Comprehensive test suite: 16 automated integration tests passing (`AuthControllerTest`, `UserControllerTest`, `DependentControllerTest`, `FlywayMigrationTest`, `HealthControllerTest`, `LifeOSApplicationTests`).
  * 13-step live HTTP verification passed over Tomcat 8080 against live PostgreSQL 18.

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
