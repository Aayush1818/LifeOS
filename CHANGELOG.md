# LifeOS — Changelog

All notable changes to the **LifeOS** platform will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.6.0-alpha] - 2026-09-19
### Added
* **Phase 6: Loan Amortization & Insurance Management**
  * Flyway migration `V4__loan_and_insurance_enhancements.sql`:
    * Enhanced `loans` table with `lender_name`, `loan_type`, `interest_type` (`FIXED`, `VARIABLE`), `payment_frequency`, `tenure_months`, `monthly_emi`, `emi_due_day`, `start_date`, `end_date`, `total_principal_paid`, `total_interest_paid`, `document_id` (FK to `documents`), `is_deleted`.
    * Created `loan_payments` table supporting `payment_amount`, `principal_component`, `interest_component`, `payment_date`, `payment_type` (`REGULAR_EMI`, `PARTIAL_PREPAYMENT`, `FULL_CLOSURE`), `prepayment_strategy` (`REDUCE_TENURE`, `REDUCE_EMI`), `transaction_ref`, `notes`.
    * Enhanced `insurance_policies` table with `policy_name`, `provider_name`, `policy_type`, `coverage_amount`, `premium_frequency`, `start_date`, `expiry_date`, `next_renewal_date`, `dependent_id` (FK to `dependents`), `document_id` (FK to `documents`), JSONB `metadata`, `is_deleted`.
    * Enhanced `reminders` table with `status` (`PENDING`, `DISMISSED`, `SNOOZED`, `COMPLETED`), `due_at`, `reminder_type`, `target_entity_type`, `target_entity_id`, and multi-tenant performance indexes.
  * Pure Domain Mathematical Amortization Engine (`LoanAmortizationEngine`):
    * Standard reducing-balance mathematical formula: $EMI = P \times \frac{r(1+r)^n}{(1+r)^n - 1}$.
    * Zero-interest ($r=0$) handling: $EMI = \frac{P}{n}$.
    * Strict `BigDecimal` calculation pipeline with `MathContext.DECIMAL128` intermediate precision and `RoundingMode.HALF_UP` scale 2 at monetary boundaries. Zero floating-point types (`double`/`float`).
    * Exact penny rounding reconciliation on final installment ($C_n = 0.00$, $P_n = \text{openingPrincipal}$).
    * Automated validation of mathematical invariants:
      1. $\sum \text{principalComponent} = \text{original principal}$ (subject to prepayments).
      2. $\text{closing principal} = 0.00$ on final installment.
      3. $\text{total payments} = \text{principal} + \text{calculated interest} \pm \text{explicitly modeled adjustments}$.
  * Loan Prepayment & Early Closure Engine:
    * Prepayments apply 100% directly to outstanding principal ($0.00 interest component).
    * `REDUCE_TENURE` strategy: maintains existing monthly EMI and shortens loan tenure.
    * `REDUCE_EMI` strategy: maintains remaining tenure and recomputes lower monthly EMI.
    * `FULL_CLOSURE` payment: verifies exact remaining balance, sets loan status to `CLOSED` and balance to `0.00`.
  * Loan Portfolio Aggregation (`LoanAnalyticsJdbcRepository`):
    * High-performance SQL aggregation via Spring `NamedParameterJdbcTemplate` for active loan count, total outstanding balance, total monthly EMI commitment, total principal paid, and total interest paid.
  * Insurance Management & Core Reminder Subsystem Synchronization:
    * Full CRUD for insurance policies across all categories (`HEALTH`, `LIFE`, `VEHICLE`, `HOME_PROPERTY`, etc.).
    * Automatic synchronization with core `reminders` table (`reminder_type = 'INSURANCE_RENEWAL'`, `target_entity_type = 'INSURANCE_POLICY'`).
    * Policy renewal (`POST /api/v1/insurance/{id}/renew`) advances expiry date, records new premium, and updates linked reminder due date.
    * Policy soft-deletion dismisses linked renewal reminder.
    * Upcoming renewals query (`GET /api/v1/insurance/renewals/upcoming?windowDays=30`) for proactive alerting.
    * Cross-module tenant validation: verifies linked `document_id` and `dependent_id` belong to the authenticated user.
  * Multi-Tenant Resource Authorization:
    * All loans, payments, amortization schedules, insurance policies, and renewal reminders strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
    * Cross-tenant access returns RFC 7807 `404 Not Found`.
    * Unique constraint duplicates return RFC 7807 `409 Conflict`.
  * Automated and Live Verification:
    * 21 new automated tests (11 in pure engine `LoanAmortizationEngineTest`, 6 in `LoanControllerTest`, 4 in `InsuranceControllerTest`), bringing total automated test suite to 63 passed tests (0 failures, 0 errors).
    * 15-step live HTTP verification passed over Tomcat 8080 against PostgreSQL 18.

---

## [0.5.0-alpha] - 2026-09-19
### Added
* **Phase 5: Personal Finance & Monthly Budgeting**
  * Flyway migration `V3__finance_enhancements.sql`:
    * Enhanced `transactions` table with `is_refund`, `notes`, `document_id` (FK to `documents`), `recurring_id` (FK to `recurring_transactions`), and `is_recurring`.
    * Enhanced `budgets` table with JSONB `alert_thresholds` and soft-delete column `is_deleted`.
    * Created `recurring_transactions` table supporting recurrence patterns (`DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY`), billing days, and auto-creation flags.
    * Added composite query indexes: `idx_transactions_user_date_type`, `idx_transactions_user_cat_date`, `idx_transactions_dup_check`, and `uq_budgets_user_cat_month_year`.
  * Deterministic Monetary Math:
    * Strict `BigDecimal` representation for all monetary calculations (`NUMERIC(14,2)`, percentages `NUMERIC(5,2)`), rounding `RoundingMode.HALF_UP`. Zero floating-point types (`float`/`double`).
    * Zero-budget edge case handling preventing divide-by-zero exceptions (`ArithmeticException`).
  * High-Performance Hybrid Persistence Architecture:
    * JPA repositories for domain CRUD operations (`TransactionRepository`, `RecurringTransactionRepository`, `BudgetRepository`).
    * Direct Spring JDBC (`FinanceAnalyticsJdbcRepository` with `NamedParameterJdbcTemplate`) for SQL aggregation pushdown (monthly income/expenses, category breakdowns, month-over-month trend analytics).
    * Refund modeling: expenses flagged with `is_refund = true` are subtracted in aggregations (`SUM(CASE WHEN is_refund THEN -amount ELSE amount END)`), properly reducing net expenses and increasing net savings.
  * Monthly Budgeting & Alerting Subsystem:
    * Dynamic, configurable alert thresholds (`[50, 75, 90, 100]` default) stored in PostgreSQL JSONB.
    * In-flight run-rate projected spending calculation: `(spent / daysElapsed) * totalDaysInMonth`.
    * Decoupled Spring application event `BudgetThresholdReachedEvent` published upon reaching alert thresholds.
  * Duplicate Transaction Detection:
    * Heuristic check detecting duplicate submissions on the same user, date, amount, and category, setting `possibleDuplicateWarning` without rejecting the transaction.
  * Multi-Tenant Resource Authorization:
    * All financial transactions, budgets, recurring rules, and summaries strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
    * Cross-tenant access returns RFC 7807 `404 Not Found` to prevent ID enumeration.
  * AI Agent Tool Readiness:
    * Exposes clean Java service contracts (`searchTransactions`, `getMonthlySummary`, `getBudgetStatus`, `getRecurringExpenses`) designed for direct Phase 9 LLM Agent tool execution.
  * Automated and Live Verification:
    * 12 new comprehensive integration tests (8 in `FinanceControllerTest`, 4 in `BudgetControllerTest`), bringing total automated integration tests to 42 (100% pass rate).
    * 21/21 live HTTP checks passed over Tomcat 8080 against PostgreSQL 18.

---

## [0.4.0-alpha] - 2026-09-19
### Added
* **Phase 4: Document Management & File Storage**
  * Integrated Apache Tika `2.9.2` (`tika-core`, `tika-parsers-standard-package`) for magic-byte content inspection and text/metadata extraction.
  * Applied Flyway migration `V2__document_enhancements.sql` adding `version`, `extracted_text`, JSONB `metadata`, `extraction_error`, and composite tenant indexes.
  * Implemented pluggable storage abstraction behind `DocumentStorageService` interface, with initial implementation `LocalStorageService`:
    * Storage path traversal protection with directory normalization and path confinement checks.
    * Storage files stored in user-isolated directories (`{userId}/{randomUuid}.{ext}`) preventing collision and path leakage.
    * Compensation cleanup: if database persistence fails, physical file is immediately purged (`deleteQuietly`).
    * Two-stage deletion: database record is immediately marked soft-deleted and status set to `DELETED`, followed by fault-tolerant disk cleanup.
  * Strict server-side MIME type allowlist (`application/pdf`, DOCX, DOC, `text/plain`, JPEG, PNG, WebP) enforced via content byte inspection (Tika Detector), rejecting spoofed extensions (e.g. PE executables renamed to `.pdf`).
  * Enforced configurable file size limits (`storage.max-file-size-bytes`: 25 MB default) before file ingestion.
  * Resilient decoupled extraction pipeline (`TikaDocumentTextExtractor`): graceful handling of corrupt files via `EXTRACTION_FAILED` status without failing storage or download capabilities.
  * Document versioning: `POST /api/v1/documents/{id}/versions` stores replacement binaries and increments version integer while maintaining document history metadata.
  * Multi-tenant resource authorization:
    * All queries, updates, downloads, versioning, and deletions strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
    * Cross-tenant access returns RFC 7807 `404 Not Found` to prevent resource ID harvesting.
    * Zero internal filesystem path leakage in API response DTOs (`DocumentResponse`, `DocumentDetailResponse`).
  * Automated testing: 10 new comprehensive integration tests in `DocumentControllerTest`, bringing total passed automated tests to 30.
  * 14-step live HTTP verification passed over Tomcat 8080 against PostgreSQL 18.

---

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
  * Administrative operations:
    * `GET /api/v1/admin/status` — guarded by `@PreAuthorize("hasRole('ADMIN')")` and Spring Security role matchers.
  * Standardized RFC 7807 problem details error handling for 401 Unauthorized (`CustomAuthenticationEntryPoint`) and 403 Forbidden (`CustomAccessDeniedHandler`).
  * Comprehensive test suite: 20 automated integration tests passing across `AuthControllerTest`, `UserControllerTest`, `DependentControllerTest`, `FlywayMigrationTest`, `HealthControllerTest`, `LifeOSApplicationTests`.
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
