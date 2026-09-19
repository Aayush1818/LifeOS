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

### F. Travel & Trips
* Itinerary management with dates, hotels, transport tickets, and expense budgets.

### G. Automated Reminders & Notifications
* Daily scheduler scanning for upcoming policy renewals, loan EMIs, doctor visits, and warranty expirations.

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
