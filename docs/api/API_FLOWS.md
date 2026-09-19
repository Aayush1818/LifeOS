# LifeOS — API Flows & Interaction Sequences

---

## 1. Authentication & Token Refresh Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Angular SPA
    participant Auth as AuthController
    participant Svc as AuthService
    participant DB as PostgreSQL

    User->>App: Enter email & password
    App->>Auth: POST /api/v1/auth/login
    Auth->>Svc: authenticate(email, password)
    Svc->>DB: Find user & verify BCrypt hash
    DB-->>Svc: UserEntity(role=ROLE_USER)
    Svc->>Svc: Generate Access JWT (15 min exp)
    Svc->>Svc: Generate Refresh Token (7 days exp)
    Svc->>DB: Save Refresh Token hash
    Svc-->>Auth: Tokens
    Auth-->>App: JSON Body: Access Token<br/>Set-Cookie: Refresh Token (HttpOnly, Secure)

    Note over App,Auth: 15 minutes elapse; Access Token expires

    App->>Auth: POST /api/v1/auth/refresh (Cookie: Refresh Token)
    Auth->>Svc: rotateRefreshToken(tokenFromCookie)
    Svc->>DB: Verify token hash & validate not revoked
    Svc->>DB: Revoke old token & store new token hash
    Svc->>Svc: Generate new Access JWT
    Svc-->>Auth: New Tokens
    Auth-->>App: JSON Body: New Access Token<br/>Set-Cookie: New Refresh Token
```

---

## 2. Multipart Document Ingestion Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Angular SPA
    participant DocCtrl as DocumentController
    participant DocSvc as DocumentStorageService
    participant Parser as Apache Tika
    participant Chunker as TextChunker
    participant Embed as EmbeddingProvider
    participant DB as PostgreSQL (pgvector)

    User->>App: Select PDF & enter metadata
    App->>DocCtrl: POST /api/v1/documents/upload (multipart/form-data)
    DocCtrl->>DocSvc: storeAndIngest(file, metadata, userId)
    DocSvc->>Parser: detectMimeType(magicBytes)
    DocSvc->>DocSvc: Save file to encrypted storage (UUID.pdf)
    DocSvc->>DB: INSERT into documents (status='PROCESSING')
    DocSvc-->>DocCtrl: Document Upload Accepted (202)
    DocCtrl-->>App: 202 Accepted (documentId)

    Note over DocSvc,DB: Asynchronous Ingestion Process
    DocSvc->>Parser: extractTextWithPages(fileStream)
    Parser-->>DocSvc: Extracted Pages [p1, p2, ...]
    DocSvc->>Chunker: chunk(pages, 500 tokens, 100 overlap)
    Chunker-->>DocSvc: List of DocumentChunks
    DocSvc->>Embed: generateEmbeddings(chunkTexts)
    Embed-->>DocSvc: List of 1536-dimensional vectors
    DocSvc->>DB: Batch INSERT into document_chunks (embedding, tsv_content)
    DocSvc->>DB: UPDATE documents SET status='COMPLETED'
```
