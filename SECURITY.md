# LifeOS — Security Architecture & Data Privacy Specification

---

## 1. Threat Model & Security Principles

LifeOS handles sensitive personal documents, financial transactions, and healthcare schedules. Security is designed with defense-in-depth across the network, application, persistence, and AI tiers.

```
Security Boundaries
┌──────────────────────────────────────────────────────────┐
│                    Client Browser                        │
└────────────────────────────┬─────────────────────────────┘
                             │ TLS 1.3 / HTTPS
┌────────────────────────────▼─────────────────────────────┐
│                 Spring Security 6 Gateway                │
│  ├── CORS & CSRF Strategy (Stateless JWT)                │
│  ├── Rate Limiter (Bucket4j / Redis token bucket)        │
│  └── JwtAuthenticationFilter (Extract, Validate, Context)│
├──────────────────────────────────────────────────────────┤
│             Resource-Level Authorization                 │
│  ├── Method Security: @PreAuthorize                      │
│  └── Ownership Validation: currentUserId == entity.userId│
├──────────────────────────────────────────────────────────┤
│             Document Sanitization Pipeline               │
│  ├── Apache Tika Magic-Byte MIME Validation              │
│  ├── Size Limit (25MB) & Filename Path Normalization     │
│  └── UUID Randomization in Encrypted Storage             │
├──────────────────────────────────────────────────────────┤
│             Database & Vector Isolation                  │
│  ├── Parameterized JPA & JDBC (Zero SQL Injection)       │
│  └── pgvector Queries Scoped by WHERE user_id = :userId  │
└──────────────────────────────────────────────────────────┘
```

---

## 2. Authentication & Token Lifecycle

### Stateless JWT with Refresh Token Rotation
1. **Access Token**:
   - Short-lived: **15 minutes** (`900,000 ms`).
   - Signed using **HMAC-SHA512 (`HS512`)** with a cryptographically secure 512-bit secret key.
   - Claims include: `sub` (userId UUID), `email`, `role`, `iat`, `exp`.
2. **Refresh Token**:
   - Long-lived: **7 days** (`604,800,000 ms`).
   - Generated as 64-character random URL-safe tokens via `java.security.SecureRandom`.
   - Stored in PostgreSQL `refresh_tokens` table exclusively as a secure SHA-256 hash (`token_hash`).
   - Supports both `HttpOnly`, `Secure`, `SameSite=Strict` cookie transport and JSON body transport for multi-client support.
   - **Rotation**: Every time `/api/v1/auth/refresh` is called, the used refresh token is permanently revoked (`is_revoked = true`) and replaced with a fresh token pair.
   - **Reuse Detection**: Presenting a revoked or previously-used token immediately returns `403 Forbidden` and audits the suspicious event.
3. **Password Security**:
   - Stored using **BCrypt** with strength factor 12.
   - Validated via `@ValidPassword` constraint: minimum 8 characters, at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character.
4. **Role-Based Access Control (RBAC)**:
   - `ROLE_USER`: Standard access to personal domain entities.
   - `ROLE_ADMIN`: Administrative endpoints guarded by `@PreAuthorize("hasRole('ADMIN')")` and `/api/v1/admin/**`.

---

## 3. Strict Resource-Level Authorization (Multi-Tenancy)

Endpoint authentication alone is insufficient. LifeOS enforces **Resource-Level Isolation** across all operations:

```java
// Example: Service-layer ownership verification pattern
@Transactional(readOnly = true)
public DocumentDto getDocumentById(UUID documentId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    DocumentEntity doc = documentRepository.findByIdAndUserId(documentId, currentUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    return documentMapper.toDto(doc);
}
```

* **Documents & Vectors**: Every vector similarity query (`pgvector`) and full-text search (`tsvector`) includes an un-bypassable SQL predicate: `WHERE user_id = :currentUserId`.
* **Health & Financial Records**: A user can never inspect, calculate, or retrieve records belonging to another user.
* **Family / Dependent Scoping**: Access to a dependent's records requires an explicit `dependent_access_grant` relationship owned by the active user.

---

## 4. File Ingestion & Storage Security

1. **MIME Type Spoofing Defense**: Client-supplied `Content-Type` headers and file extensions are fundamentally untrusted. LifeOS employs **Apache Tika 2.9.2** magic-byte stream inspection on raw input streams before any ingestion logic.
2. **Server-Side MIME Allowlist**: Restricted strictly to supported business formats (`application/pdf`, DOCX, DOC, `text/plain`, JPEG, PNG, WebP) configured via `StorageProperties`. Unrecognized or spoofed binaries (e.g. DOS/PE `.exe` renamed to `.pdf`) are rejected with RFC 7807 `400 Bad Request`.
3. **Path Traversal Protection & Directory Confinement**:
   * Storage paths are structured as `{storageDir}/{userId}/{randomUuid}.{ext}`.
   * Path resolution uses `rootLocation.resolve(...).normalize()` with strict `.startsWith(userDir)` confinement checks.
   * Path traversal filenames (e.g. `../../../../etc/passwd.txt`) are sanitized using `Paths.get(filename).getFileName()`, preventing local file overwrite or arbitrary write exploits.
4. **Compensation Cleanup & Consistency**:
   * If database persistence fails after storing physical bytes, `LocalStorageService.deleteQuietly` executes immediately in the exception handler to prevent orphaned disk files.
   * Deletion uses a two-stage process: database soft delete (`is_deleted = true`, status = `DELETED`) commits immediately, followed by fault-tolerant physical file purge.
5. **Zero Internal Filesystem Path Leakage**:
   * Public DTOs (`DocumentResponse`, `DocumentDetailResponse`) never expose internal server disk paths (`storagePath`).
   * Download endpoints (`GET /api/v1/documents/{id}/download`) stream bytes directly via Spring `Resource` abstraction with user ownership verification.
6. **Cross-Tenant Access Protection**:
   * Attempted access, download, new version upload, or deletion of a document belonging to another user returns RFC 7807 `404 Not Found` rather than `403 Forbidden`, preventing document UUID enumeration.
7. **Configurable File Size Caps**: Enforced at Spring Boot multipart layer (`spring.servlet.multipart.max-file-size: 25MB`) and validated before processing in `DocumentService`.

---

## 5. Personal Finance & Monthly Budgeting Security

1. **Multi-Tenant Financial Scoping**:
   * All transactions, recurring transaction rules, category budgets, and analytical aggregations are strictly filtered by `user_id = SecurityUtils.getCurrentUserId()`.
   * Cross-tenant access attempts to transactions, budgets, or recurring rules return RFC 7807 `404 Not Found` rather than `403 Forbidden` to prevent financial resource enumeration.
   * Analytical summary endpoints (`/api/v1/finance/analytics/monthly-summary`, `/category-breakdown`, `/month-over-month`, `/budgets/status`) execute strictly within the authenticated tenant context at the SQL level.
2. **Deterministic Monetary Integrity**:
   * All monetary math is handled exclusively with `BigDecimal` using `RoundingMode.HALF_UP` and stored in PostgreSQL as `NUMERIC(14,2)` (percentages as `NUMERIC(5,2)`).
   * Zero usage of IEEE 754 floating-point types (`float`, `double`) across the entire backend, preventing rounding drift and financial calculation discrepancies.
   * LLMs are never used to compute monetary aggregates, remaining balances, or budget utilization rates.
3. **Zero-Budget Edge Case Protection**:
   * Division-by-zero protection prevents `ArithmeticException` when evaluating budgets with zero allocated or zero spent amounts.
4. **Heuristic Duplicate Detection**:
   * Automatically inspects recent transactions matching the same user, date, amount, and category, setting a `possibleDuplicateWarning` flag to warn the client against double-submission without blocking valid intentional transactions.

---

## 6. Loans, Amortization & Insurance Security

1. **Multi-Tenant Scoping & Resource Isolation**:
   * All loans, loan payments, mathematical amortization schedules, insurance policies, and renewal reminders are strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
   * Cross-tenant access attempts return RFC 7807 `404 Not Found` rather than `403 Forbidden` to prevent loan or policy ID enumeration.
   * Cross-module foreign key verification: referencing a `documentId` or `dependentId` on a loan or insurance policy validates that the referenced entity belongs to the authenticated user and is not soft-deleted. Foreign tenant references trigger RFC 7807 `404 Not Found`.
2. **Mathematical Precision & Amortization Invariants**:
   * Pure domain amortization engine executes exclusively with `BigDecimal` using `MathContext.DECIMAL128` intermediate calculations and `RoundingMode.HALF_UP` scale 2 at monetary boundaries.
   * Zero usage of IEEE 754 floating-point types (`float`, `double`), eliminating monetary drift and rounding exploitation.
   * Exact penny rounding reconciliation on final installment guarantees that closing principal reaches strictly `0.00`.
   * Automated verification of domain invariants:
     1. $\sum \text{principalComponent} = \text{originalPrincipal}$ (subject to prepayments).
     2. Closing principal on final installment $= 0.00$.
     3. Total payments $= \text{principal} + \text{calculated interest} \pm \text{explicitly modeled adjustments}$.
3. **Prepayment Integrity & Early Closure Safety**:
   * Prepayments are applied 100% directly to outstanding principal ($0.00 interest component).
   * Full early closure (`FULL_CLOSURE`) verifies exact match with remaining balance before transitioning loan status to `CLOSED` and zeroing out outstanding balance.
4. **Reminder Subsystem Synchronization**:
   * Insurance renewal reminders synchronize directly into the core `reminders` table (`reminder_type = 'INSURANCE_RENEWAL'`).
   * Renewal updates automatically advance reminder due dates; policy soft-deletions automatically dismiss active reminders, preventing ghost notification spam.

---

## 7. Healthcare & Medical Data Security & Non-Diagnostic Boundaries

1. **Multi-Tenant Scoping & Resource Isolation**:
   * All healthcare appointments, linked medical documents, and dependent associations are strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
   * Cross-tenant access attempts return RFC 7807 `404 Not Found` rather than `403 Forbidden` to prevent appointment or health record enumeration.
   * Cross-module tenant verification: referencing a `dependentId` or `documentId` validates that the referenced entity belongs to the authenticated user and is active. Cross-tenant references trigger RFC 7807 `404 Not Found`.
2. **Regulatory Non-Diagnostic Safety Boundary**:
   * The healthcare module is strictly organizational, scheduling, and administrative.
   * LifeOS strictly prohibits automated medical diagnosis, clinical interpretation of lab values, treatment recommendations, symptom checking, or prescription generation.
   * Every appointment response enforces an explicit static safety disclaimer contract: `"Strictly organizational & non-diagnostic. LifeOS does not provide medical diagnosis, clinical evaluation, or treatment advice."`
3. **Medical Document Protection**:
   * Prescriptions, lab reports, and consultation notes leverage Phase 4 document storage with magic-byte MIME type validation (Apache Tika), 25MB file caps, and user-isolated disk directories.
   * Document linking (`document_entity_links`) requires verified ownership of both the appointment and the document before establishing an association.
4. **Reminder Synchronization Integrity**:
   * Consultation reminders synchronize directly into the core `reminders` table (`reminder_type = 'HEALTH_APPOINTMENT'`).
   * Rescheduling updates the reminder's `due_at` timestamp based on configured `reminder_offset_minutes`.
   * Appointment completion (`COMPLETED`), cancellation (`CANCELLED`), or deletion automatically dismisses the linked reminder (`ReminderStatus.DISMISSED`), eliminating zombie alerts.

---

## 8. Travel & Trip Management Security & Currency Integrity

1. **Multi-Tenant Scoping & Strict Resource Isolation**:
   * All trips, itinerary items, travelers, and linked travel documents are strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
   * Cross-tenant access attempts return RFC 7807 `404 Not Found` rather than `403 Forbidden` to prevent resource enumeration.
   * Cross-module verification: registering a family dependent as a traveler validates that the `dependentId` belongs to the authenticated user and is active; referencing an unowned dependent returns RFC 7807 `404 Not Found`.
2. **Deterministic Multi-Currency Guardrails**:
   * 100% `BigDecimal` representation with PostgreSQL `NUMERIC(14,2)` and `RoundingMode.HALF_UP`.
   * **Zero Silent Currency Conversion**: Trips containing mixed currencies without explicit user-recorded exchange rates strictly omit `consolidatedTotal` (`null`) and group totals by currency (`totalsByCurrency`), preventing distorted financial reporting.
   * Cancelled bookings are strictly excluded from active trip spending calculations.
3. **Subsystem Reuse & Document Security**:
   * Boarding passes, tickets, hotel confirmations, and travel insurance policies leverage Phase 4 document management with Apache Tika magic-byte MIME validation, 25MB file caps, and user-isolated storage.
   * Document-to-entity linking (`document_entity_links`) validates dual-ownership: both the trip/itinerary item and the document must belong to the caller.
4. **Timezone Integrity & Reminder Synchronization**:
   * Local IANA timezone strings (`startTimeZone`, `endTimeZone`) are validated and stored as domain attributes alongside UTC `OffsetDateTime`.
   * Reminders in the core `reminders` subsystem (`TRAVEL_DEPARTURE`, `TRAVEL_CHECKIN`, `TRAVEL_ACTIVITY`) automatically synchronize on itinerary creation/rescheduling and automatically dismiss on cancellation or deletion.
5. **No Autonomous External Purchasing Boundary**:
   * LifeOS strictly functions as a personal itinerary and organizational life management platform. It does not initiate automated bookings, purchases, or external third-party API mutations.

---

## 9. Product Warranties, Invoices & Asset Management Security & Integrity

1. **Multi-Tenant Scoping & Strict Resource Isolation**:
   * All assets, invoices, invoice line items, warranties, warranty claims, and service records are scoped to `userId = SecurityUtils.getCurrentUserId()`.
   * Cross-tenant access attempts return RFC 7807 `404 Not Found` rather than `403 Forbidden` to prevent resource enumeration.
   * Dependent assignment to assets validates that `dependentId` belongs to the authenticated user; unauthorized dependents return RFC 7807 `404 Not Found`.
2. **Dual-Ownership Verification for Document Links**:
   * Attaching documents (receipts, warranty certificates, user manuals) to assets or invoices via `document_entity_links` strictly enforces dual tenant ownership: both the target entity and the referenced document must belong to the caller.
3. **Double-Accounting Prevention & Finance Integration Boundary**:
   * Creating an invoice never automatically records a financial expense.
   * Converting an invoice to a finance transaction or explicitly linking to an existing transaction is atomic and enforces same-user ownership.
   * Duplicate linking is prevented via unique constraints (`invoices.transaction_id UNIQUE`) and returns RFC 7807 `409 Conflict`.
4. **Deterministic Multi-Currency Valuation & Zero Speculative FX**:
   * Asset acquisition costs and invoice line items use PostgreSQL `NUMERIC(14,2)` with Java `BigDecimal`.
   * When assets are denominated in multiple currencies, portfolio totals are grouped strictly by currency (`totalsByCurrency`) with `consolidatedTotal = null`. No speculative conversion or arbitrary FX rates are applied.
5. **Warranty Lifecycle, Expiration Reminders & Claims Retention**:
   * Expiry reminders schedule at 09:00:00 in the user's local timezone (UTC fallback).
   * `LIFETIME` warranties strictly have `expiry_date = null` and never schedule reminders.
   * Voiding a warranty dismisses the linked reminder, while preserving all historical claims and service records in perpetuity for auditability.
6. **Immutable Asset Status Audit History**:
   * Asset status transitions write immutable audit logs to `asset_status_history` with transition timestamp, user ID, previous status, new status, and optional reason.

---

## 10. Anti-Hallucination & AI Privacy Boundaries (Phase 13)

Phase 13 establishes the Grounded AI Assistant with multi-layered defenses spanning isolation, prompt injection immunity, grounding verification, and safety guardrails:

1. **Multi-Tenant Context & Session Isolation**:
   * All conversation sessions (`conversations`), messages (`chat_messages`), and citations (`message_citations`) are strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
   * Cross-tenant access, message posting, or deletion attempts return RFC 7807 `404 Not Found` (mitigating IDOR enumeration).
   * Context retrieval is delegated to Phase 12 Hybrid RAG which enforces `user_id = :userId` and `is_active = true` at the database index layer. Foreign document chunks never enter the context window.
2. **Prompt Injection Defense & Delimiter Escaping**:
   * Raw retrieved content is isolated within `<untrusted_document_source index="n">` XML delimiters.
   * If document content contains literal `<untrusted_document_source>` or `</untrusted_document_source>` tags, they are automatically sanitized and escaped by `ContextAssembler` to prevent delimiter breakouts.
   * Internal database UUIDs (`documentId`, `chunkId`) are stripped from the prompt payload before sending to the model, preventing internal identifier exposure.
   * System prompt directives (`lifeos-assistant-2026-v1.0`) explicitly instruct the LLM to treat source text strictly as passive reference data and disregard any embedded override commands.
3. **Traceable Footnote Citations & Hallucination Defense**:
   * The assistant requires numerical inline citation markers (`[1]`, `[2]`) referencing the 1-indexed source map.
   * `CitationValidator` parses all citation markers via regex and cross-references them with the authorized source map.
   * Any citation markers pointing to nonexistent indices or hallucinated sources are stripped from the message content before persistence.
   * If no relevant context exists, the assistant politely declines to answer rather than guessing or extrapolating facts.
4. **Safety Guardrails & Action Boundaries**:
   * **Medical Safety**: The assistant is strictly non-diagnostic. Queries requesting medical diagnoses or prescriptions trigger an immediate safety disclaimer advising professional healthcare or emergency services.
   * **Financial Read-Only**: The assistant is strictly informational and read-only. It cannot execute transactions, initiate transfers, or mutate account balances.
   * **Action Boundary**: The assistant cannot autonomously mutate state, delete records, or book travel. State mutations are reserved for interactive user actions or future Phase 14 agent workflows.
5. **Deterministic Offline CI/CD Security**:
   * All automated unit and integration tests run against `MockLlmProvider` with zero live network calls to third-party commercial LLM endpoints, eliminating token costs and API credential leaks in CI/CD pipelines.


---

## 11. Safe Agentic AI & Tool Calling Boundaries (Phase 14)

Phase 14 introduces safe autonomous tool calling while maintaining airtight boundaries to prevent unauthorized execution, data leakage, and runaway execution loops:

1. **Human-in-the-Loop (HITL) Barrier**:
   * Any tool marked with `requiresConfirmation = true` (e.g. `create_reminder`, `record_loan_payment`) cannot execute automatically.
   * When an agent attempts a mutating tool call, the `AgentOrchestrator` intercepts execution, serializes the call into a `pending_actions` database record with status `PENDING`, and immediately halts orchestration.
   * The action is only dispatched to the real Java service when the authenticated user explicitly sends a `POST /api/v1/assistant/actions/{id}/confirm` request.
2. **Deterministic Java-Only Tool Execution**:
   * The LLM never synthesizes financial calculations, interest rates, or schedule states directly.
   * All analytical queries (loan amortization, monthly spend summaries, insurance renewals, upcoming appointments) delegate to verified, compiled Java domain services (`LoanAnalyticsService`, `FinanceAnalyticsService`, `InsuranceService`, `HealthAppointmentService`, `TripService`, `DocumentRetrievalEngine`).
3. **Multi-Tenant Tool & Action Isolation**:
   * Tools always receive the caller's verified `UUID userId` extracted from the Spring Security context, never an unverified parameter from the LLM prompt.
   * All tool repository queries enforce tenant filtering (`WHERE user_id = :userId`).
   * Attempting to view, confirm, or reject another tenant's `PendingAction` strictly returns RFC 7807 `404 Not Found` to prevent action ID enumeration.
4. **Agent Turn Limits & Loop Protection**:
   * The `AgentOrchestrator` enforces a hard limit of `MAX_AGENT_TURNS = 3` per user interaction.
   * If the model attempts to call tools beyond turn 3, the loop forcibly breaks and returns the current assistant response to prevent infinite loops, rate-limit exhaustion, or runaway token consumption.
5. **Action Expiration & Tamper-Resistance**:
   * Pending actions carry an explicit `expires_at` timestamp (default: 24 hours).
   * Expired actions are blocked from confirmation (`400 Bad Request: Pending action has expired`).
   * Once an action is confirmed, rejected, or expired, its status is terminal; replay attacks are blocked.

---

## 12. Secrets Management

* **No Hard-Coded Credentials**: API keys, database passwords, and JWT secrets are injected via system environment variables or `.env` files (ignored in `.gitignore`).
* **Environment Template**: A fully documented `.env.example` template is provided with production-recommended defaults.



