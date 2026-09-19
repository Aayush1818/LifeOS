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

1. **MIME Type Spoofing Defense**: File extensions (`.pdf`, `.docx`) can be forged. LifeOS uses **Apache Tika** to read file magic bytes from the binary stream before processing.
2. **Path Traversal Protection**: Uploaded files are stripped of directory traversal characters (`../`, `..\\`) and renamed to a random `UUID.ext` on disk.
3. **Storage Location**: Files are written to a dedicated directory completely outside the web server root context, preventing direct HTTP execution.
4. **File Size Caps**: Enforced at both the servlet gateway and Spring Boot multipart layer (maximum 25MB per file).

---

## 5. Anti-Hallucination & AI Privacy Boundaries

1. **Context Window Isolation**: AI conversation sessions strictly inject retrieved chunks tagged with the authenticated user's ID. No cross-tenant document chunks can enter the LLM prompt context.
2. **No Data Leakage in AI Logs**: Logs sanitize user PII, document binary excerpts, and authentication headers.
3. **Zero Automated Destructive Actions**: The AI Assistant cannot delete documents, mutate loans, or trigger payments without explicit, interactive UI confirmation from the user.

---

## 6. Secrets Management

* **No Hard-Coded Credentials**: API keys, database passwords, and JWT secrets are injected via system environment variables or `.env` files (ignored in `.gitignore`).
* **Environment Template**: A fully documented `.env.example` template is provided with production-recommended defaults.
