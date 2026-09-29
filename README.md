# LifeOS — AI-Powered Personal Life Management & Knowledge Platform

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-blue.svg)](https://github.com/pgvector/pgvector)
[![Angular](https://img.shields.io/badge/Angular-18%2B-red.svg)](https://angular.io/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

> **LifeOS** is an intelligent, secure, centralized personal information and life-management platform. It unifies scattered personal documents, insurance policies, loans, healthcare records, monthly budgets, and itineraries into a cohesive digital brain powered by hybrid vector-lexical RAG (Retrieval-Augmented Generation) and deterministic agentic tools.

---

## 1. Problem We Are Solving

Modern personal knowledge is fragmented across physical files, email inboxes, messaging apps, and banking portals:
* **Insurance policies & clauses** buried in 40-page PDF contracts.
* **Loan amortization schedules** and payment dates forgotten across multiple banks.
* **Doctor visits, medical history, and prescriptions** dispersed across messages and papers.
* **Monthly budgets and recurring subscriptions** hidden in spreadsheets.
* **Product warranties, invoices, and rent agreements** lost when needed most.

Users struggle to answer basic questions:
* *"When does my car insurance expire, and what does it cover?"*
* *"What is my exact total monthly obligation across EMI, insurance premiums, and bills?"*
* *"What did my doctor prescribe during my visit last November?"*
* *"Summarize the penalty clauses in my lease agreement."*

LifeOS solves this fragmentation by combining **Structured Domain Management**, **Secure Document Storage**, **Hybrid Vector RAG**, and **Anti-Hallucination Agentic AI**.

---

## 2. Core Architecture

LifeOS is structured as a high-cohesion, low-coupling modular monolith in Spring Boot 3.3.5, engineered for maintainability and horizontal domain extension.

```
LifeOS Architecture
┌─────────────────────────────────────────────────────────────┐
│                      Angular 18+ Client                     │
└──────────────────────────────┬──────────────────────────────┘
                               │ REST / JSON (RFC 7807)
┌──────────────────────────────▼──────────────────────────────┐
│                  Spring Security 6 Gateway                  │
│       (Stateless JWT, Refresh Token Rotation, RBAC)         │
├─────────────────────────────────────────────────────────────┤
│                      Business Modules                       │
│  ├── Identity & Users        ├── Personal Finance & Budgets │
│  ├── Family & Dependents     ├── Loans & Liabilities        │
│  ├── Document Intelligence   ├── Insurance Portfolio        │
│  ├── Health & Appointments   ├── Travel & Itineraries       │
│  └── Automation Engine (Scheduler, Reminders, Notifications)│
├─────────────────────────────────────────────────────────────┤
│                    AI & RAG Subsystem                       │
│  ├── Apache Tika Text Extractor                             │
│  ├── Semantic Sliding-Window Chunker                        │
│  ├── pgvector Dense Index (HNSW) + tsvector BM25 (GIN)      │
│  ├── Hybrid Reciprocal Rank Fusion (RRF) Retriever          │
│  └── Agentic Tool Registry (Deterministic Calculations)     │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│             PostgreSQL 16 / 18 + pgvector DB                │
│          (Flyway Migrations, JPA CRUD, JDBC Analytics)      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Technology Stack

### Backend
* **Language**: Java 21 LTS (Virtual Threads, Pattern Matching, Records)
* **Framework**: Spring Boot 3.3.5
* **Security**: Spring Security 6, JJWT (HMAC-SHA512), BCrypt (strength 12), Refresh Token Rotation
* **Persistence**: Spring Data JPA (Domain transactions), Spring Data JDBC (Complex analytics & aggregations)
* **Database**: PostgreSQL 16 / 18 with native `pgvector` v0.8.6 extension
* **Database Migrations**: Flyway
* **Document Processing**: Apache Tika, PDFBox
* **API Documentation**: OpenAPI 3.0 / Swagger UI (Springdoc)
* **Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers

### AI & Retrieval
* **Vector Store**: PostgreSQL `pgvector` with HNSW cosine distance indexing
* **Lexical Search**: PostgreSQL Full-Text Search (`tsvector` + GIN indexing)
* **Retrieval Strategy**: Hybrid Reciprocal Rank Fusion (RRF) with page/chunk level citations
* **Orchestration**: Abstracted AI provider interfaces (`LLMProvider`, `EmbeddingProvider`, `VectorStorePort`) with tool execution guardrails

### Frontend
* **Framework**: Angular 18+
* **State Management**: Reactive Signals & RxJS
* **Styling**: Tailwind CSS & Modern UI component layout
* **HTTP**: Functional Interceptors for JWT authorization and auto-refresh

---

## 4. Key Functional Features

### A. Document Intelligence & Hybrid RAG
* Upload PDF, DOCX, TXT documents with automated metadata tagging.
* Sliding-window chunking (500 tokens, 100 token overlap) with exact page tracking.
* Dual retrieval: dense cosine similarity combined with full-text keyword ranking.
* Strict grounding: every answer includes `[Document Name, Page #, Chunk ID]`. Low-confidence queries return an honest *"Insufficient information in your stored documents"* rather than hallucinations.

### B. Agentic AI Assistant
* Natural language interface powered by a controlled tool registry:
  * `searchDocuments(query, category)`
  * `getInsurancePolicies(activeOnly)`
  * `getLoanDetails(loanId)`
  * `calculateMonthlyObligations(month, year)`
  * `getHealthAppointments(upcoming)`
  * `getTripItinerary(tripId)`
* Modifying actions (e.g., creating reminders, recording payments) trigger mandatory confirmation prompts before execution.

### C. Personal Finance & Monthly Budgeting (Phase 5 Implemented)
* **Strict Monetary Math**: 100% `BigDecimal` calculations with PostgreSQL `NUMERIC(14,2)` and `RoundingMode.HALF_UP` (zero float/double drift).
* **Hybrid Persistence Engine**: Spring Data JPA for domain CRUD paired with direct Spring JDBC (`NamedParameterJdbcTemplate`) for deterministic monthly summaries, category breakdowns, and month-over-month trend analytics.
* **Transparent Refund Modeling**: Expense refunds reduce net monthly expenses and properly increase savings calculations.
* **Recurring Obligations & Subscriptions**: Scheduled bills and subscriptions with recurrence patterns (`DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY`) and next due date calculation.
* **Monthly Budgeting & Alerts**: Category budgets with dynamic JSONB alert thresholds (`[50, 75, 90, 100]`), in-flight run-rate projected spending, and decoupled Spring application events (`BudgetThresholdReachedEvent`).
* **Heuristic Duplicate Detection**: Flags suspicious duplicate submissions on same user, date, amount, and category without blocking valid repeat entries.
* **Multi-Tenant Isolation**: Strict resource-level authorization returning RFC 7807 `404 Not Found` for cross-tenant access attempts.

### D. Loan Amortization & Insurance Portfolio (Phase 6 Implemented)
* **Pure Domain Mathematical Amortization Engine**:
  * Deterministic reducing-balance EMI formula ($EMI = P \times \frac{r(1+r)^n}{(1+r)^n - 1}$) and zero-interest loans ($EMI = \frac{P}{n}$).
  * Strict `BigDecimal` calculation pipeline with `MathContext.DECIMAL128` intermediate precision and `RoundingMode.HALF_UP` scale 2 at monetary boundaries (zero floating-point types).
  * Exact penny rounding reconciliation on final installment ensuring final closing principal is strictly `0.00`.
  * Verified mathematical invariants: $\sum \text{principalComponent} = \text{originalPrincipal}$, final $C_n = 0.00$, and $\text{totalPayments} = P + I$.
* **Flexible Prepayments & Early Closure**:
  * Partial prepayments apply 100% directly to principal ($0.00 interest component).
  * Dual prepayment strategies: `REDUCE_TENURE` (keeps existing EMI, reduces loan duration) or `REDUCE_EMI` (recomputes lower monthly EMI for remaining tenure).
  * Full early closure: verifies exact remaining balance, transitions status to `CLOSED`, and zeros out balance.
* **Direct JDBC Portfolio Aggregation**: High-performance SQL aggregation pushdown (`LoanAnalyticsJdbcRepository`) calculating active loans count, total outstanding balance, total monthly EMI commitment, and lifetime principal/interest paid.
* **Insurance Portfolio Management & Reminder Synchronization**:
  * Full lifecycle tracking for Health, Life, Vehicle, Home/Property, Travel, and Disability policies.
  * Cross-tenant validation linking policies to verified family dependents and stored documents.
  * Automatic synchronization with core `reminders` subsystem (`INSURANCE_RENEWAL` reminder type) on policy creation, updates, and renewals.
  * Policy renewal workflow (`/api/v1/insurance/{id}/renew`) updating expiry date, adjusting premium, and advancing the linked renewal reminder.
  * Proactive upcoming renewals scanning (`/api/v1/insurance/renewals/upcoming?windowDays=30`).
  * Soft-deletion automatically dismisses linked renewal reminders.

### E. Health & Doctor Appointments Organization (Phase 7 Implemented)
* **Strict Non-Diagnostic Safety Boundary**: The healthcare subsystem is strictly organizational, scheduling, and administrative. LifeOS enforces explicit static medical disclaimers on every response; no medical diagnosis, clinical evaluation, treatment recommendations, or medication prescriptions are ever generated.
* **Doctor & Clinic Organization**: Comprehensive tracking of doctors, medical specialties, clinic addresses, contact details, appointment start/end times, and client timezones.
* **Lifecycle & Status Transitions**: Appointment statuses (`SCHEDULED`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`, `NO_SHOW`) with notes and follow-up appointment linking (`follow_up_to_id`).
* **Family & Dependent Care**: Schedule and manage consultations for the authenticated user or any verified family dependent.
* **Medical Document Organization**: Bi-directional document linking (`document_entity_links`) to associate prescriptions, lab reports, discharge summaries, and bills directly to consultation records, utilizing Phase 4 file storage without duplication.
* **Reminder Subsystem Synchronization**: Automatic creation, rescheduling, and dismissal of `HEALTH_APPOINTMENT` reminders in the core notifications subsystem based on configurable reminder offset minutes.
* **Proactive Upcoming Visits**: Rapid querying of upcoming appointments within a configurable day window (`/api/v1/healthcare/appointments/upcoming?windowDays=14`).

### F. Travel & Trip Itinerary Management (Phase 8 Implemented)
* **Trips & Destinations**: Complete lifecycle tracking (`PLANNED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), destination metadata, start/end dates, total budget, and actual spend.
* **Extensible Unified Itinerary Model**: Unified `itinerary_items` structure supporting `FLIGHT`, `TRAIN`, `BUS`, `LODGING`, `ACTIVITY`, `RESTAURANT`, `RENTAL_CAR`, `TRANSFER`, and `CUSTOM` (with `customTypeName`) without schema proliferation.
* **Strict Multi-Currency Financial Aggregation**:
  * 100% `BigDecimal` monetary values (`NUMERIC(14,2)`).
  * **Zero silent mixing**: expenses are strictly grouped and reported by currency (`totalsByCurrency`).
  * `consolidatedTotal` is populated *only* when all items are in the trip's base currency or when explicit user-recorded exchange rates are provided (`exchangeRateToBase`); otherwise `consolidatedTotal` is strictly `null` with a clear explanation notice. Cancelled items are excluded from active spend totals.
* **Rigorous International Timezone Architecture**: Local IANA timezone identifiers (`startTimeZone`, `endTimeZone`, e.g., `Asia/Kolkata`, `Europe/London`, `America/New_York`) are preserved as first-class domain attributes alongside UTC `OffsetDateTime` (PostgreSQL `TIMESTAMP WITH TIME ZONE`), supporting complex cross-timezone flights, midnight crossings, and local check-ins.
* **Zero Duplication Subsystem Reuse**:
  * **Travel Documents**: Reuses Phase 4 document management (`DocumentCategory.TRAVEL`, `DocumentType.BOARDING_PASS`, `HOTEL_CONFIRMATION`, `ITINERARY`, `TRAVEL_INSURANCE`) linked via `document_entity_links` to trips or individual bookings.
  * **Automated Travel Reminders**: Reuses core `reminders` table (`TRAVEL_DEPARTURE`, `TRAVEL_CHECKIN`, `TRAVEL_ACTIVITY`) with automatic resynchronization upon itinerary rescheduling and automatic dismissal upon cancellation or deletion.
* **Family & Traveler Coordination**: Multi-traveler registration supporting primary user and verified family dependents (`TripTravelerEntity`).
* **Upcoming Trips Scanner**: Rapid querying of upcoming itineraries within configurable day windows (`/api/v1/travel/trips/upcoming?windowDays=30`).

### G. Product Warranties, Invoices & Asset Management (Phase 9 Implemented)
* **First-Class Asset Inventory**: Full lifecycle management across 9 explicit domain statuses (`ACTIVE`, `UNDER_REPAIR`, `RETIRED`, `SOLD`, `DISPOSED`, `LOST`, `STOLEN`, `GIFTED`, `RETURNED`) with immutable audit logging (`asset_status_history`).
* **First-Class Invoices & Line Items**: Invoices decoupled from documents with structured line items (`InvoiceItemEntity`), tax, discounts, shipping fees, return deadlines, and optional links to assets.
* **Deterministic Multi-Currency Valuation**: Portfolio acquisition cost grouped strictly by currency (`totalsByCurrency`) with `consolidatedTotal = null` for mixed-currency collections; zero speculative conversion or arbitrary FX rates.
* **Zero Double-Counting Finance Integration**: 1-to-0..1 bidirectional mapping (`invoices.transaction_id UNIQUE`) preventing duplicate accounting. Invoices can be atomically converted to finance expense transactions or linked to existing transactions with strict tenant validation.
* **Multi-Tier Warranty Lifecycle**: Support for `MANUFACTURER`, `EXTENDED`, `STORE`, `CREDIT_CARD_PROTECTION`, and `LIFETIME` warranties. Automatic expiry reminder synchronization at 09:00 in user's timezone; `LIFETIME` warranties never expire and never schedule reminders.
* **First-Class Warranty Claims**: State machine (`FILED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `RESOLVED`, `CANCELLED`) tracking covered vs out-of-pocket costs and repair providers. Historical claims remain 100% queryable even after policy expiration or voiding.
* **Service & Maintenance Records**: Complete maintenance, repair, and upgrade logs (`asset_service_records`) linked to assets, invoices, and claims.
* **Zero Duplication Subsystem Reuse**: Full reuse of Phase 4 document storage via `document_entity_links` for receipts, warranty certificates, and user manuals with dual-ownership verification.

### H. Unified Search & Advanced Query Platform (Phase 10 Implemented)
* **Single-Pass Cross-Domain Discovery**: Search simultaneously across all 14 LifeOS bounded contexts (Assets, Invoices, Warranties, Claims, Service Records, Loans, Insurance Policies, Doctor Visits, Travel Itineraries, Financial Transactions, Budgets, Documents, Dependents, and Reminders).
* **Native PostgreSQL Full-Text Search**: Powered by `tsvector` with cover density relevance ranking (`ts_rank_cd`), natural syntax parsing (`websearch_to_tsquery`), matched highlights (`ts_headline`), and expression-based GIN indexing.
* **Dynamic Structured Filtering**: Fine-grained query refinement by entity type subset (`entities=...`), closed or open date ranges (`startDate`, `endDate`), monetary bounds (`minAmount`, `maxAmount`, `currency`), categories, lifecycle statuses, and verified dependents.
* **Faceted Navigation & Quick Typeahead**: Faceted count breakdowns (`/api/v1/search/count`) for multi-tab UI displays and rapid autocomplete suggestions (`/api/v1/search/suggest`).
* **Multi-Tenant Scoping**: All search operations strictly scoped to `userId = SecurityUtils.getCurrentUserId()`. Unauthorized dependent references trigger RFC 7807 `404 Not Found`.

### I. Document Intelligence & RAG Ingestion Foundation (Phase 11 Implemented)
* **Layout-Aware Semantic Chunking**: Deterministic sliding window chunker (500-token window, 100-token overlap) respecting physical page breaks, headings, paragraph boundaries, and sentence integrity via Java `BreakIterator`.
* **Context & Provenance Enrichment**: Automatic injection of hierarchical breadcrumbs (`[Document: ... | Section: ... | Page: ...]`) into every chunk, enabling precise citation pills in Phase 12.
* **Provider-Agnostic Embedding Architecture**: Decoupled `EmbeddingProvider` SPI supporting OpenAI `text-embedding-3-small`, local Ollama instances, and deterministic unit-normalized mock providers for test suites.
* **pgvector Dense Vector Storage & Indexing**: Persists 1536-dimensional embeddings with HNSW cosine distance indexes (`vector_cosine_ops`), per-chunk `tsvector` GIN indexes, and composite tenant indexes.
* **Version Invalidation & Idempotency**: Atomic activation and deactivation across document versions (`is_active` flag) preserving historical citations in `message_citations` while preventing outdated chunks from surfacing in vector searches.
* **Granular Ingestion Lifecycle & Diagnostic APIs**: Asynchronous processing with bounded thread pool (`documentIngestionExecutor`), explicit error tracking (`EXTRACTION_FAILED`, `EMBEDDING_FAILED`), `/ingestion-status`, `/reprocess`, and chunk inspector `/chunks`.

### J. Hybrid RAG Retrieval Engine (Phase 12 Implemented)
* **Production-Grade Dual Retrieval Pipeline**: Combines PostgreSQL Full-Text Search (lexical) from Phase 10 and pgvector semantic vector search from Phase 11 into a unified, high-precision retrieval engine (`POST /api/v1/search/retrieve`).
* **Deterministic Query Processing**: Normalizes whitespace, strips control characters, clamps length, and extracts exact quoted phrases and alphanumeric codes for reranking boosts.
* **Scale-Invariant Reciprocal Rank Fusion (RRF)**: Combines disparate lexical scores (`ts_rank_cd`) and cosine similarities via standard RRF ($k = 60$) with deduplication by chunk ID and normalized $[0.0, 1.0]$ score calculation.
* **Deterministic Cross-Signal Reranker**: High-performance in-memory reranking ($< 1\text{ms}$) factoring in exact phrase match ($1.25\times$), identifier match ($1.20\times$), section/title match ($1.15\times$), and version recency ($1.05\times$) with zero external network overhead.
* **Traceable Provenance & Citations**: Generates structured citation objects (`RetrievalCitationDto`) with document title, version, page number, section title, and formatted markdown citation strings for downstream Phase 13 LLM grounding.
* **Multi-Tenant Pushdown & Defensive Authorization**: Strict SQL pushdown (`user_id = :userId`, `is_active = true`, `is_deleted = false`) plus secondary defensive in-memory ownership verification.
* **Graceful Degradation & Hallucination Guardrails**: Automatic fallback to lexical FTS if external embedding providers are unavailable; relevance threshold filtering with `hasRelevantContext: false` signaling to prevent downstream hallucinations.

### K. Grounded AI Assistant & Multi-Role Conversational Pipeline (Phase 13 Implemented)
* **Pluggable LLM Provider SPI**: Decoupled `LlmProvider` interface supporting local offline deterministic `MockLlmProvider` for tests/CI and `OpenAiCompatibleLlmProvider` for OpenAI, Azure OpenAI, Ollama, and vLLM runtimes.
* **Grounded Context Assembly**: Context assembler formatting retrieved chunks with XML isolation delimiters (`<untrusted_document_source index="n">`), stripping internal database UUIDs, and escaping malicious tags.
* **Prompt Injection Defense & Versioned Prompting**: Multi-role prompt engineering (`lifeos-assistant-2026-v1.0`) enforcing strict reference-only answering, instruction override immunity, sliding conversation memory (max 6 messages), and medical non-diagnostic guardrails.
* **Traceable Footnote Verification**: Automated `CitationValidator` verifying inline footnote numbers (`[1]`, `[2]`), stripping hallucinated references, and linking validated citations to `message_citations` database records.
* **Multi-Tenant Conversation Privacy**: Conversations (`conversations`) and message history (`chat_messages`) are strictly owned by `user_id`, with cross-tenant attempts returning RFC 7807 `404 Not Found`.
* **Zero Autonomous Mutations**: The assistant is strictly conversational and information-providing, maintaining a rigorous boundary preventing accidental fund transfers or state mutations.

### L. Safe Agentic AI & Tool Calling Integration (Phase 14 Implemented)
* **Deterministic LifeOS Tool Registry**: Decoupled `LifeOSTool` SPI registering deterministic Java domain tools (`get_loan_summary`, `get_insurance_renewals`, `get_monthly_spend_summary`, `get_upcoming_appointments`, `get_trip_itinerary`, `search_documents`).
* **Human-in-the-Loop (HITL) Barrier**: State-mutating actions (`create_reminder`, `record_loan_payment`) are automatically intercepted, recorded in `pending_actions` with a 24-hour TTL, and halted until explicit user confirmation via REST endpoints (`/api/v1/assistant/actions`).
* **Multi-Turn Orchestration & Loop Bounds**: Multi-turn agent loop bounded by `MAX_AGENT_TURNS = 3` with recursive tool-result feedback, prompt injection defenses, and strict multi-tenant authorization.
* **100% Deterministic Financial & Analytical Math**: The LLM is strictly prohibited from estimating, halluncinating, or synthesizing calculations; all summaries are computed by compiled Java domain services.

### M. Modern Angular 20 SPA Experience (Phase 15 Implemented)
* **Design System & Aesthetics**: Dark-mode glassmorphic interface with custom HSL tailored palettes, Inter/Outfit typography, and responsive grid layouts.
* **Interactive AI Copilot**: Multi-turn chat feed, grounded footnote citations drawer (`[1]`, `[2]`), and inline **Human-in-the-Loop (HITL)** pending action cards for one-click approval/rejection.
* **Knowledge Vault & Chunk Explorer**: Multi-category document management with drag-and-drop file upload, MIME validation, and pgvector HNSW layout-aware chunk inspection drawer.
* **Unified Cross-Domain Search**: High-performance full-text search with debounced typeahead suggestions and entity filtering across all 14 LifeOS domain entities.
* **Finance & Prepayment Simulator**: Real-time monthly cashflow analysis, interactive category budget bars with threshold alerts, and amortization schedule with prepayment impact simulator.
* **Life Operations**: Comprehensive tabbed hub for healthcare consultations & biometrics, travel itinerary timelines, and warranty countdown tracking.

### N. Automated Reminders & Notification Engine (Phase 16 Implemented)
* **Background Due-Date Scanner**: Automated cron scheduler scanning due dates across domain entities: loan EMIs, insurance renewals, warranty expirations, healthcare appointments, and travel itineraries with 24-hour alert deduplication.
* **Notification Engine & Channels**: In-app notifications with read tracking, channels (`IN_APP`, `EMAIL`, `WEBHOOK`), and composite partial index queries for real-time badge counts.
* **Recurrence Rule Advancement**: Automated advancement of recurring tasks (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`) upon completion.
* **Interactive Frontend Management**: Dedicated Reminders view with KPI counters (Active, Overdue, Due Soon, Completed), tab filters, priority tags, and glassmorphic Notification Bell flyout in the global navigation shell.

### O. Security Audit Logging, GDPR Portability & Policy Comparison (Phase 17 Implemented)
* **Asynchronous Audit Logging**: Event-driven decoupled audit trail capturing authentication events, HITL confirmation decisions, and privacy exports with client IP and user-agent logging.
* **Multi-Domain GDPR Data Portability**: Comprehensive user data export compiling records across all 14 bounded contexts into an encrypted or downloadable JSON archive.
* **Hybrid RAG Insurance Policy Comparison**: Comparative diffing engine cross-referencing insurance policy documents to highlight clause changes, coverage limits, exclusions, and cost differences.

### P. Multi-Domain Financial Obligations & System Diagnostics (Phase 18 Implemented)
* **Financial Obligation Synthesis**: Aggregates active Loan EMIs, Insurance Premiums & Renewals, Recurring Bills/Subscriptions, and Planned Travel Budgets for any target month with deterministic cash flow forecasting.
* **Platform Diagnostics & Observability**: Real-time probes for PostgreSQL, native `pgvector`, Apache Tika extraction engine, and local storage write readiness; JVM telemetry and entity count metrics.
* **Deterministic Agent Tool**: `get_monthly_obligations` auto-discovered and registered in the `LifeOSToolRegistry`.

### Q. Proactive Life Insights, Financial Anomaly Detection & Optimization Engine (Phase 19 Implemented)
* **Cross-Domain Anomaly Engine**: Extensible `InsightAnalyzer` SPI orchestrating 6 deterministic rule analyzers detecting category spending spikes (>150% trailing average), budget exhaustion (>80% depletion), high-interest loan prepayment savings (APR >= 7.5%), upcoming policy renewals and uninsured dependents, expiring equipment warranties, and travel/medical schedule conflicts.
* **Autonomous Intelligence & State Machine**: Deduplication across runs, severity classification (`CRITICAL`, `WARNING`, `INFO`), and full state lifecycle transitions (one-click dismiss and action tracking).
* **Deterministic Agent Tool**: `get_proactive_insights` enabling conversational querying of active platform anomalies and optimizations.
* **Executive Dashboard Experience**: Real-time "Autonomous Optimization & Anomaly Engine" deck with live indicator, severity badges, and contextual action buttons with intelligent route navigation.


---

## 5. Security & Multi-Tenant Privacy

1. **Resource-Level Authorization**: Every database query, vector search, and document download strictly enforces the caller's `user_id`. One user can never access or query another user's documents or financial data.
2. **Stateless Authentication**: Access tokens expire in 15 minutes; refresh tokens are stored securely and rotated upon each refresh.
3. **File Sanitization**: MIME inspection via file magic bytes (Apache Tika), size caps (25MB), and isolated UUID-based storage paths outside the web application context.
4. **Audit Logging**: Comprehensive logging of security-critical actions (logins, credential changes, data exports).

---

## 6. Quick Start & Setup

### Prerequisites
* Java 21 LTS installed ([Setup Guide](SETUP.md))
* PostgreSQL 16 / 18 with `pgvector` enabled ([Setup Guide](SETUP.md))
* Node.js v20+ / npm 10+

### Clone & Configure
```bash
git clone https://github.com/your-repo/lifeos.git
cd lifeos
cp .env.example .env
```

### Build & Run
```bash
# Backend (using bundled Maven wrapper)
./mvnw clean install
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm start
```

For complete setup instructions, see [SETUP.md](SETUP.md).

---

## 7. Documentation Directory

* [ARCHITECTURE.md](ARCHITECTURE.md) — Comprehensive architectural specification and bounded contexts.
* [DATABASE_DESIGN.md](DATABASE_DESIGN.md) — Complete relational schema, indexes, ER diagrams, and pgvector configuration.
* [RAG_ARCHITECTURE.md](RAG_ARCHITECTURE.md) — Text extraction, chunking, hybrid retrieval (RRF), citations, and safety boundaries.
* [API_DOCUMENTATION.md](API_DOCUMENTATION.md) — OpenAPI/REST endpoints, request/response contracts, and status codes.
* [SECURITY.md](SECURITY.md) — Auth flow, JWT rotation, resource isolation, and input validation.
* [SETUP.md](SETUP.md) — Local environment, Java 21 LTS, and PostgreSQL setup.
* [CHANGELOG.md](CHANGELOG.md) — Development milestones and phase logs.

---

## 8. License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
