# LifeOS — Changelog

All notable changes to the **LifeOS** platform will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.14.0-alpha] - 2026-09-20
### Added
* **Phase 14: Safe Agentic AI & Tool Calling Integration**
  * Flyway Migration `V11__agent_tools_and_pending_actions.sql`:
    * Created `pending_actions` table: first-class Human-in-the-Loop (HITL) barrier for state-mutating agent actions with user isolation, JSONB parameters, status lifecycle, and TTL expiration.
    * Created composite index `idx_pending_actions_user_status` and partial index `idx_pending_actions_expires` (`WHERE status = 'PENDING'`).
  * Pluggable Tool Registry SPI (`com.lifeos.ai.agent.tool`):
    * `LifeOSTool` SPI decoupling tool definition, execution, and confirmation requirements from AI orchestration.
    * `LifeOSToolRegistry`: auto-discovering Spring component registering all active tools and exposing schemas to LLM requests.
    * Read-only deterministic tools: `LoanSummaryTool`, `InsuranceRenewalsTool`, `MonthlySpendSummaryTool`, `HealthcareAppointmentsTool`, `TripItineraryTool`, `DocumentSearchTool`.
    * State-mutating tools: `CreateReminderTool`, `RecordLoanPaymentTool` (marked `requiresConfirmation = true`).
  * Safe Agentic Orchestration & HITL Guardrails (`com.lifeos.ai.agent`):
    * `AgentOrchestrator`: multi-turn tool calling engine with hard loop limit (`MAX_AGENT_TURNS = 3`) preventing infinite loops.
    * State-mutation interception: automatically captures mutating tool calls, persists `pending_actions` records, and halts orchestration until explicit user confirmation.
    * `ActionConfirmationService`: manages lifecycle of pending actions (`PENDING`, `CONFIRMED`, `REJECTED`, `EXPIRED`, `FAILED`) and dispatches confirmed actions to target domain services.
  * REST API (`AgentActionController`):
    * `GET /api/v1/assistant/actions/pending`: retrieve pending actions awaiting confirmation for the authenticated tenant.
    * `POST /api/v1/assistant/actions/{id}/confirm`: confirm and execute a pending action.
    * `POST /api/v1/assistant/actions/{id}/reject`: cancel and reject a pending action.
    * Updated `POST /api/v1/assistant/conversations/{id}/messages`: returns `pendingAction` and `toolCallsExecuted` metadata.
  * Multi-Tenant Isolation & Safety:
    * Attempted access to another tenant's pending actions returns RFC 7807 `404 Not Found`.
    * Expired actions are blocked from confirmation (`400 Bad Request`).
    * All financial/domain analytics originate strictly from verified Java services, never synthesized by LLM.

## [0.13.0-alpha] - 2026-09-20
### Added
* **Phase 13: Grounded AI Assistant & LLM Integration**
  * Flyway Migration `V10__ai_assistant_enhancements.sql`:
    * Created composite index `idx_conversations_user_updated` on `conversations(user_id, last_message_at DESC)` for high-performance paginated conversation loading.
    * Enhanced `chat_messages` table with token telemetry columns (`prompt_tokens`, `completion_tokens`) and `model_name`.
    * Enhanced `message_citations` table with display metadata (`citation_index`, `section_title`, `source_citation`).
  * Pluggable LLM Provider SPI & Adapters (`com.lifeos.ai.llm`):
    * `LlmProvider` Java SPI decoupling conversation logic from LLM runtime vendors.
    * `MockLlmProvider`: deterministic, offline LLM mock supporting citation insertion, insufficient-context signaling, medical disclaimers, and simulated failure injection (`TRIGGER_TIMEOUT`, `TRIGGER_RATE_LIMIT`, `TRIGGER_SERVER_ERROR`).
    * `OpenAiCompatibleLlmProvider`: production HTTP client utilizing Spring `RestClient` with configurable timeouts and exponential backoff retry.
    * `LlmProperties`: configuration binder for `lifeos.assistant.llm` (`provider`, `baseUrl`, `apiKey`, `model`, `timeoutMs`, `maxRetries`).
    * `LlmException`: typed runtime exception differentiating retryable vs non-retryable AI provider failures.
  * Context Assembly & Prompt Engineering (`com.lifeos.ai.grounding`):
    * `ContextAssembler`: formats retrieved Phase 12 chunks within XML `<untrusted_document_source index="n">` tags, escapes malicious embedded delimiters, and strips internal database UUIDs.
    * `PromptBuilder`: constructs versioned system prompts (`lifeos-assistant-2026-v1.0`), enforces prompt injection immunity, bounds conversation history (max 6 turns), and embeds medical non-diagnostic guardrails.
    * `CitationValidator`: parses inline footnote markers (`[1]`, `[2]`), cross-references them with the authorized source map, and purges hallucinated citations from persisted assistant messages.
  * Multi-Tenant Conversation Session Services (`com.lifeos.ai.service`):
    * `ConversationService`: CRUD operations on `conversations`, ensuring strict multi-tenant ownership (foreign access yields RFC 7807 `404 Not Found`).
    * `AssistantService` / `DefaultAssistantService`: end-to-end conversation pipeline orchestrating Phase 12 Hybrid RAG retrieval, context assembly, LLM generation, citation validation, and atomic database persistence.
  * REST API (`AssistantController`):
    * `POST /api/v1/assistant/conversations`: create conversation session.
    * `GET /api/v1/assistant/conversations`: paginated list of conversations ordered by recency.
    * `GET /api/v1/assistant/conversations/{id}`: retrieve conversation details with message history and citations.
    * `DELETE /api/v1/assistant/conversations/{id}`: delete conversation with cascading message and citation removal.
    * `POST /api/v1/assistant/conversations/{id}/messages`: send user message, retrieve grounded context via Phase 12 RAG, and generate verified cited answer.
  * Verification & Testing:
    * 34 new automated tests bringing full suite to **190 / 190 tests passed** (0 failures, 0 errors, 0 skipped).
    * **25 / 25 live HTTP verification checks passed** in `scratch/verify_phase13.ps1` testing health, CRUD, multi-tenant isolation (User B $\rightarrow$ 404), document grounding, insufficient information handling, medical safety disclaimers, input validation, and cleanup.

## [0.12.0-alpha] - 2026-09-19
### Added
* **Phase 12: Hybrid RAG Retrieval Engine**
  * Core Retrieval Architecture:
    * Production-grade hybrid retrieval pipeline combining PostgreSQL Full-Text Search (Phase 10) and pgvector semantic vector search (Phase 11).
    * Zero database migrations required; fully leverages existing `idx_chunks_tsv` GIN and `idx_chunks_hnsw` HNSW indexes.
  * Deterministic Query Processing (`QueryProcessor`):
    * Normalizes whitespace, strips null bytes `\u0000` and control characters, and clamps queries to 1000 characters.
    * Extracts quoted phrases and alphanumeric codes for exact-match boosting during reranking.
  * Candidate Fusion Engine (`CandidateFusionEngine`):
    * Scale-invariant Reciprocal Rank Fusion (RRF) with configurable constant $k = 60$.
    * Normalized relevance scores in $[0.0, 1.0]$.
    * Deduplication by `chunkId` with `matchSource` classification (`LEXICAL_ONLY`, `SEMANTIC_ONLY`, `HYBRID_BOTH`).
  * Cross-Signal Local Reranker (`DeterministicCrossSignalReranker`):
    * Deterministic in-memory reranking applying exact-phrase boosts ($1.25\times$), identifier boosts ($1.20\times$), section/title match boosts ($1.15\times$), and version recency boosts ($1.05\times$).
    * Zero external network latency; completes in $< 1\text{ms}$.
  * Provenance & Footnote Citation Generator (`CitationGenerator`):
    * Produces traceable citations with document title, version, page number, section title, and formatted string for Phase 13 LLM grounding.
  * Orchestration & Fallback Service (`HybridRetrievalService`):
    * Enforces multi-tenant query pushdown (`user_id = :userId`, `is_active = true`, `is_deleted = false`).
    * Implements defensive post-retrieval authorization verification.
    * Graceful degradation: automatically falls back to lexical FTS if external embedding provider fails or times out.
    * Relevance threshold cutoffs with `hasRelevantContext: false` signaling to prevent downstream LLM hallucinations.
  * REST API (`RetrievalController`):
    * `POST /api/v1/search/retrieve` supporting `HYBRID`, `LEXICAL`, and `SEMANTIC` retrieval modes with structured metadata filtering.
  * Verification & Testing:
    * 23 new automated tests bringing full test suite to **156 / 156 tests passed** (0 failures, 0 errors, 0 skipped).
    * **25 / 25 live HTTP checks passed** in `scratch/verify_phase12.ps1` against Tomcat 8080 and PostgreSQL 18.

## [0.11.0-alpha] - 2026-09-19
### Added
* **Phase 11: Document Intelligence & RAG Ingestion Foundation**
  * Flyway migration `V9__document_chunks_enhancements.sql`:
    * Enhanced `document_chunks` table with `document_version`, `section_title`, `token_count`, `char_count`, `is_active`, `embedding_model`, and `updated_at`.
    * Added unique constraint `uq_document_chunks_doc_ver_idx` on `(document_id, document_version, chunk_index)` guaranteeing idempotent chunk writes.
    * Created index `idx_chunks_active_user` on `(user_id, is_active)` for efficient tenant and active version filtering.
    * Created index `idx_chunks_doc_ver` on `(document_id, document_version)`.
    * Enhanced `documents` table with `chunk_count`, `ingested_at`, and `embedding_model`.
  * Layout-Aware Document Chunker (`DocumentChunker`):
    * Deterministic sliding window chunker with 500-token target window and 100-token overlap.
    * Structural boundary preservation: prioritizes page breaks, section headings (`SECTION`, `ARTICLE`, Markdown `#`), paragraphs, and sentences via Java `BreakIterator`.
    * Context enrichment: injects document filename, section title, and page number breadcrumbs into every chunk.
    * Deterministic UUID v5 generation per chunk preventing duplicate identifiers.
  * Provider-Agnostic Embedding SPI (`EmbeddingProvider`):
    * Extensible interface decoupling domain logic from AI embedding providers.
    * `MockEmbeddingProvider`: deterministic unit-normalized 1536-dimensional vector generator using SHA-256 seed hashing for offline testing and CI/CD.
    * `OpenAiEmbeddingProvider`: production-grade REST adapter for OpenAI `text-embedding-3-small` with batched requests (100 texts/call) and exponential backoff retry on 429/503.
    * `EmbeddingProperties`: configuration properties for provider selection, dimensions, base URL, and timeout.
  * Ingestion Orchestration & Asynchronous Processing (`DocumentIngestionService`):
    * Asynchronous worker using bounded `ThreadPoolTaskExecutor` (`documentIngestionExecutor`, 4 core / 8 max threads / 100 queue capacity, `CallerRunsPolicy`).
    * End-to-end pipeline: file loading $\rightarrow$ structured text extraction $\rightarrow$ sliding window chunking $\rightarrow$ embedding generation $\rightarrow$ atomic vector persistence.
    * Explicit lifecycle management: `PENDING` / `STORED` $\rightarrow$ `PROCESSING` $\rightarrow$ `PROCESSED` / `EXTRACTION_FAILED` / `EMBEDDING_FAILED`.
    * Version activation & deactivation: uploading version $V+1$ automatically marks version $V$ chunks `is_active = false` without breaking historical `message_citations` references.
  * REST API Endpoints (`DocumentController`):
    * `GET /api/v1/documents/{id}/ingestion-status`: Retrieves RAG ingestion state, chunk counts, page counts, and model metadata.
    * `POST /api/v1/documents/{id}/reprocess`: Manually triggers full re-ingestion with optional force flag.
    * `GET /api/v1/documents/{id}/chunks`: Retrieves paginated layout-aware chunks with section breadcrumbs and page numbers.
  * Security & Tenant Isolation:
    * All chunk queries and vector searches enforce `user_id = SecurityUtils.getCurrentUserId()` and `is_active = true`.
    * Cross-tenant access to ingestion status or chunks strictly returns RFC 7807 `404 Not Found`.
  * Verification:
    * 14 new automated tests bringing full test suite to **133 / 133 tests passed** (0 failures, 0 errors, 0 skipped).
    * **25 / 25 live HTTP checks passed** in `scratch/verify_phase11.ps1` against Tomcat 8080 and PostgreSQL 18.

---

## [0.10.0-alpha] - 2026-09-19
### Added
* **Phase 10: Unified Search & Advanced Query Platform**
  * Flyway migration `V8__unified_search_indexes.sql`:
    * Created expression-based full-text GIN indexes (`to_tsvector('english', ...)`) across 14 domain tables (`documents`, `assets`, `invoices`, `warranties`, `warranty_claims`, `asset_service_records`, `health_appointments`, `trips`, `itinerary_items`, `transactions`, `loans`, `insurance_policies`, `dependents`, `reminders`).
    * Created `lifeos_unified_search_view` standard SQL view projecting all 14 LifeOS domain entities with normalized attributes and weighted `tsv_content` tiers (A, B, C).
  * Native PostgreSQL Lexical & Full-Text Search Engine (`PostgresLexicalSearchEngine`):
    * Implemented `SearchEngine` SPI/port with Spring Data JDBC `NamedParameterJdbcTemplate` pushdown.
    * Integrated `websearch_to_tsquery` for safe natural query syntax (words, quoted exact phrases, negation `-term`, `OR`) without syntax errors.
    * Calculated cover density relevance scoring via `ts_rank_cd(tsv_content, query, 32)` with recency tie-breaking.
    * Extracted matching highlights and contextual snippets via `ts_headline`.
  * Cross-Domain Unified Search API (`SearchController`):
    * `GET /api/v1/search`: Cross-domain search supporting keyword matching, entity filtering (`entities=...`), date range filtering (`startDate`, `endDate`), monetary bounds (`minAmount`, `maxAmount`, `currency`), category and status filtering, and sorting (`RELEVANCE`, `DATE_DESC`, `DATE_ASC`, `AMOUNT_DESC`, `AMOUNT_ASC`, `TITLE_ASC`).
    * `GET /api/v1/search/count`: Faceted matching count summary partitioned by entity type.
    * `GET /api/v1/search/suggest`: Rapid prefix autocomplete typeahead suggestions.
    * `GET /api/v1/search/entities`: Enumerates all 15 supported search entity types.
  * Multi-Tenant Security & Dependent Enforcement (`UnifiedSearchService`):
    * Scoped strictly to `SecurityUtils.getCurrentUserId()`. User A never sees User B's search results.
    * Verified dependent ownership when filtering by `dependentId`; cross-tenant references return RFC 7807 `404 Not Found`.
    * Enriched dependent names dynamically for matched entities.
  * Future RAG Port Compatibility:
    * Clean interface boundary ready for Phase 12 `HybridRrfSearchEngine` (Lexical + Dense Vector Reciprocal Rank Fusion) without breaking REST contracts.
  * Verification:
    * 16 new automated integration tests in `SearchControllerTest.java` bringing the full automated test suite to 119/119 passed tests (0 failures, 0 errors, 0 skipped).
    * 19/19 live HTTP verification checks passed in `scratch/verify_phase10.ps1` against live Tomcat 8080 and PostgreSQL 18.

---

## [0.9.0-alpha] - 2026-09-19
### Added
* **Phase 9: Product Warranties, Invoices & Asset Management**
  * Flyway migration `V7__assets_warranties_and_invoices.sql`:
    * Created `invoices` table with supplier, invoice number, subtotal, tax, discount, shipping, total, payment status, return deadline, and optional unique finance `transaction_id` FK.
    * Created `assets` table with category, brand, model, serial number, purchase date, acquisition cost, currency, return deadline, status, and dependent FK.
    * Created `invoice_items` table with line item descriptions, unit price, quantity, total price, and optional asset FK.
    * Created `warranties` table supporting `MANUFACTURER`, `EXTENDED`, `STORE`, `CREDIT_CARD_PROTECTION`, and `LIFETIME` warranties, expiry dates, provider, policy numbers, and reminder FK.
    * Created `warranty_claims` table with claims state machine (`FILED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `RESOLVED`, `CANCELLED`), covered vs out-of-pocket costs, and resolution notes.
    * Created `asset_service_records` table logging maintenance, repairs, inspections, and upgrades linked to assets, invoices, and claims.
    * Created `asset_status_history` immutable audit table tracking lifecycle transitions.
    * Created composite performance indexes on user IDs, asset IDs, invoice IDs, and warranty IDs.
  * Deterministic Multi-Currency Valuation:
    * 100% `BigDecimal` calculations with PostgreSQL `NUMERIC(14,2)` and `RoundingMode.HALF_UP` (zero float/double drift).
    * Portfolio acquisition cost aggregated strictly by currency (`totalsByCurrency`) with `consolidatedTotal = null` for mixed-currency holdings; zero speculative conversion.
  * Finance Integration & Duplicate Accounting Prevention:
    * 1-to-0..1 bidirectional mapping (`invoices.transaction_id UNIQUE`).
    * Creating an invoice never automatically spawns a transaction.
    * Idempotent conversion of invoices to finance expense transactions, and explicit transaction linking with strict same-user ownership validation (`404 Not Found` on cross-tenant, `409 Conflict` on duplicate).
  * Multi-Tier Warranty Lifecycle & Reminders:
    * Expiry reminders schedule at 09:00:00 in user's local timezone (UTC fallback).
    * `LIFETIME` warranties strictly have `expiry_date = null` and never schedule reminders.
    * Voiding a warranty dismisses the linked reminder, while preserving all historical claims and service records in perpetuity.
  * First-Class Warranty Claims:
    * State machine tracking incident dates, repair service providers, covered costs, and out-of-pocket expenses.
    * Historical claims remain 100% queryable after warranty expiration or voiding.
  * Subsystem Reuse (Zero Duplication):
    * Reused Phase 4 document storage via `document_entity_links` for receipts, warranty certificates, and user manuals with dual-ownership verification.
    * Reused Phase 3 notifications/reminders for warranty expiry and return deadlines.
    * Reused Phase 2 dependents for asset assignment with tenant validation.
  * Verification:
    * 15 automated integration tests in `AssetControllerTest.java` bringing full test suite to 103/103 passed tests (0 failures, 0 errors).
    * 19 logical verification groups (23 discrete PASS assertions) verified live on Tomcat port 8080 against PostgreSQL 18.

---

## [0.8.0-alpha] - 2026-09-19
### Added
* **Phase 8: Travel & Trip Itinerary Management**
  * Flyway migration `V6__travel_and_trip_itinerary.sql`:
    * Enhanced `trips` table with `currency` (`VARCHAR(3)` default `'USD'`), `notes` (`TEXT`), `cover_image_url` (`VARCHAR(500)`), and `metadata` (`JSONB NOT NULL DEFAULT '{}'::jsonb`).
    * Created `trip_travelers` table supporting registered primary users and verified dependents (`dependent_id` FK).
    * Created `itinerary_items` unified extensible table supporting `FLIGHT`, `TRAIN`, `BUS`, `LODGING`, `ACTIVITY`, `RESTAURANT`, `RENTAL_CAR`, `TRANSFER`, and `CUSTOM` (with `custom_type_name`), explicit IANA timezones (`start_time_zone`, `end_time_zone`), `cost`, `currency`, `exchange_rate_to_base`, and reminder offsets.
    * Enhanced `trip_expenses` table with `currency`, `exchange_rate_to_base`, `notes`, and `document_id` (FK to `documents`).
    * Created performance query indexes: `idx_trips_user_dates`, `idx_trip_travelers_trip`, `idx_itinerary_items_trip_time`, `idx_itinerary_items_user_time`, `idx_trip_expenses_trip_date`.
  * Strict Multi-Currency Financial Aggregation:
    * 100% `BigDecimal` calculations with PostgreSQL `NUMERIC(14,2)` and `RoundingMode.HALF_UP` (zero float/double drift).
    * **Zero Silent Currency Conversion**: Totals are strictly partitioned and reported by currency (`totalsByCurrency`).
    * `consolidatedTotal` is populated *only* when all items are in the trip's base currency or when explicit user-recorded exchange rates are provided (`exchangeRateToBase`); otherwise `consolidatedTotal` is strictly `null` with a clear explanation notice.
    * Active spend calculation correctly excludes cancelled bookings.
  * Rigorous International Timezone Architecture:
    * Local IANA timezone identifiers (`startTimeZone`, `endTimeZone`, e.g., `Asia/Kolkata`, `Europe/London`, `America/New_York`) are preserved as first-class domain attributes alongside UTC `OffsetDateTime` (PostgreSQL `TIMESTAMP WITH TIME ZONE`).
    * Accurately models cross-timezone travel, midnight date transitions, and local hotel check-ins.
  * Subsystem Reuse (Zero Duplication):
    * **Travel Documents**: Reuses Phase 4 document management; extended `DocumentType` enum with `BOARDING_PASS`, `HOTEL_CONFIRMATION`, `ITINERARY`, `TRAVEL_INSURANCE`; links documents to trips or individual bookings via `document_entity_links`.
    * **Automated Travel Reminders**: Reuses core `reminders` subsystem (`TRAVEL_DEPARTURE`, `TRAVEL_CHECKIN`, `TRAVEL_ACTIVITY`); automatically resynchronizes on itinerary rescheduling and automatically dismisses on cancellation or deletion.
  * Family & Traveler Coordination:
    * Multi-traveler registration supporting primary user and verified family dependents (`TripTravelerEntity`).
    * Dependent ownership validation rejecting cross-tenant traveler references.
  * Upcoming Trips Scanner:
    * Proactive scanning of upcoming trips within configurable day windows (`/api/v1/travel/trips/upcoming?windowDays=30`).
  * Multi-Tenant Resource Authorization:
    * All trips, itinerary items, travelers, and document links strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
    * Cross-tenant access attempts return RFC 7807 `404 Not Found`.
  * Verification:
    * 15 automated integration tests in `TravelControllerTest.java` bringing total test suite to 88/88 passed tests (0 failures, 0 errors).
    * 16/16 live HTTP verification checks passed (`scratch/verify_phase8.ps1`) on Tomcat port 8080 against PostgreSQL 18.

---

## [0.7.0-alpha] - 2026-09-19
### Added
* **Phase 7: Healthcare & Doctor Appointments (Non-Diagnostic) & Medical Document Organization**
  * Flyway migration `V5__healthcare_and_appointments.sql`:
    * Enhanced `appointments` table with `clinic_phone`, `clinic_address`, `follow_up_to_id` (self-referencing FK), `scheduled_end_time`, `time_zone`, `reminder_offset_minutes`, JSONB `metadata`, `is_deleted`.
    * Added composite query indexes: `idx_appointments_user_time`, `idx_appointments_user_status`, `idx_appointments_user_dep`.
    * Added unique constraint `uq_document_entity_link` on `document_entity_links (document_id, entity_type, entity_id)` for idempotent document associations.
  * Non-Diagnostic Regulatory Safety Invariants:
    * Strictly organizational, administrative, and scheduling.
    * Zero diagnostic evaluation, treatment recommendations, clinical interpretation, or medication suggestions.
    * Static non-diagnostic disclaimer delivered on every appointment response (`AppointmentResponse.NON_DIAGNOSTIC_DISCLAIMER`).
  * Healthcare Appointments Management:
    * Full CRUD for doctor consultations across all medical specialties (`Cardiology`, `Pediatrics`, `Dermatology`, etc.).
    * Strict status lifecycle: `SCHEDULED`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`, `NO_SHOW`.
    * Rescheduling recalculates appointment start/end times and automatically synchronizes the linked reminder `due_at`.
    * Status transition to `COMPLETED`, `CANCELLED`, `NO_SHOW`, or appointment deletion automatically dismisses the linked reminder (`ReminderStatus.DISMISSED`).
    * Proactive upcoming appointments query (`/api/v1/healthcare/appointments/upcoming`) with configurable day window (default: 14 days).
    * Follow-up appointment linking via `follow_up_to_id`.
  * Family & Dependent Integration:
    * Appointments can be booked for the user directly or on behalf of verified family dependents.
    * Multi-tenant validation ensures dependent belongs to the authenticated user (returns 404 otherwise).
  * Medical Document Subsystem Expansion:
    * Extended `DocumentType` enum with medical types: `CONSULTATION_SUMMARY`, `DISCHARGE_SUMMARY`, `DIAGNOSTIC_REPORT`, `VACCINATION_RECORD`, `MEDICAL_BILL` alongside existing `PRESCRIPTION`.
    * Bi-directional multi-document linking via `DocumentEntityLinkEntity` and `DocumentEntityLinkRepository`.
    * Medical documents list endpoint (`/api/v1/healthcare/documents`) with filtering by document type and dependent ID.
    * 100% reuse of Phase 4 document storage (`LocalStorageService`) and Apache Tika text extraction with zero duplicate infrastructure.
  * Reminder Subsystem Synchronization:
    * Synchronized with core `reminders` table (`reminder_type = 'HEALTH_APPOINTMENT'`, `target_entity_type = 'APPOINTMENT'`).
    * Configurable `reminderOffsetMinutes` (default: 1440 min / 24 hours), calculating `due_at = appointment_time - reminder_offset`.
  * Multi-Tenant Resource Authorization:
    * All appointments, linked documents, and dependents strictly scoped to `userId = SecurityUtils.getCurrentUserId()`.
    * Cross-tenant access attempts return RFC 7807 `404 Not Found`.
  * Verification:
    * 10 automated integration tests in `HealthcareControllerTest.java` bringing total automated test suite to 73/73 passed tests (0 failures, 0 errors).
    * 16/16 live HTTP verification checks passed (`scratch/verify_phase7.ps1`) on Tomcat port 8080 against PostgreSQL 18.

---

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
