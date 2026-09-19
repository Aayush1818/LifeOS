# LifeOS — Architecture Overview & Bounded Contexts

---

## 1. Domain-Driven Design (DDD) Bounded Contexts

```mermaid
graph TB
    subgraph Identity_Access["Identity & Access Management Context"]
        IAM_User[User Aggregate]
        IAM_Session[Session / RefreshToken]
        IAM_Dependent[Dependent / Family Aggregate]
    end

    subgraph Knowledge_Intelligence["Document & Knowledge Context"]
        Doc_Meta[Document Metadata]
        Doc_Chunks[Document Chunks & Embeddings]
        Doc_Entities[Document Entity Links]
    end

    subgraph Financial_Obligations["Financial & Obligations Context"]
        Fin_Tx[Transaction Aggregate]
        Fin_Budget[Budget & Threshold Alerts]
        Fin_Loan[Loan & Amortization Schedule]
        Fin_Ins[Insurance Policy Aggregate]
    end

    subgraph Life_Operations["Life Operations Context"]
        Life_Health[Doctor & Appointment Aggregate]
        Life_Trip[Trip & Itinerary Aggregate]
        Life_Reminder[Reminder & Notification Engine]
    end

    subgraph AI_Orchestration["AI & Agent Context"]
        AI_Retrieval[Hybrid Vector + Lexical Retriever]
        AI_Agent[Agentic Tool Orchestrator]
        AI_Guardrail[Anti-Hallucination Guardrail]
    end

    Identity_Access --> Knowledge_Intelligence
    Identity_Access --> Financial_Obligations
    Identity_Access --> Life_Operations

    Knowledge_Intelligence --> AI_Orchestration
    Financial_Obligations --> AI_Orchestration
    Life_Operations --> AI_Orchestration
```

---

## 2. Request Processing Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor Client as Angular SPA
    participant GW as Spring Security Filter
    participant Ctrl as Domain Controller
    participant Svc as Domain Service
    participant Repo as JPA / JDBC Repo
    participant DB as PostgreSQL 16

    Client->>GW: HTTP POST /api/v1/... (Bearer JWT)
    GW->>GW: Validate JWT Signature & Expiration
    GW->>GW: Populate SecurityContext (UserPrincipal)
    GW->>Ctrl: Forward Request
    Ctrl->>Ctrl: Validate Payload (@Valid DTO)
    Ctrl->>Svc: Invoke Business Logic
    Svc->>Svc: Verify Resource Ownership (userId == principal.id)
    Svc->>Repo: Execute Query
    Repo->>DB: SQL Query with tenant filter
    DB-->>Repo: Result Set
    Repo-->>Svc: Domain Entities / Projections
    Svc->>>Ctrl: Return Response DTO
    Ctrl-->>Client: HTTP 200/201 (ApiResponse<T>)
```
