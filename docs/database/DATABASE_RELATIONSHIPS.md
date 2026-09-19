# LifeOS — Database Relationships & Data Integrity Guarantees

---

## 1. Foreign Key Cascades & Deletion Rules

| Parent Table | Child Table | Foreign Key | On Delete Action | Justification |
| :--- | :--- | :--- | :--- | :--- |
| `users` | `refresh_tokens` | `user_id` | **CASCADE** | Sessions are strictly owned by user. |
| `users` | `dependents` | `user_id` | **CASCADE** | Dependents belong to user account. |
| `users` | `documents` | `user_id` | **CASCADE** | User account deletion purges documents. |
| `users` | `insurance_policies` | `user_id` | **CASCADE** | Insurance policies owned by user. |
| `users` | `loans` | `user_id` | **CASCADE** | Loans owned by user. |
| `users` | `transactions` | `user_id` | **CASCADE** | Financial ledger belongs to user. |
| `dependents` | `documents` | `dependent_id` | **SET NULL** | Retain user document even if dependent profile is removed. |
| `dependents` | `insurance_policies` | `dependent_id` | **SET NULL** | Retain policy record if insured dependent is unlinked. |
| `documents` | `document_chunks` | `document_id` | **CASCADE** | Deleting a document instantly purges its vector embeddings. |
| `documents` | `insurance_policies` | `document_id` | **SET NULL** | Preserves insurance details even if source PDF is unlinked. |
| `loans` | `loan_payments` | `loan_id` | **CASCADE** | Amortization payments strictly tied to parent loan. |
| `trips` | `trip_expenses` | `trip_id` | **CASCADE** | Trip expenses tied to trip lifecycle. |
| `conversations` | `chat_messages` | `conversation_id` | **CASCADE** | Message thread belongs to conversation. |
| `chat_messages` | `message_citations` | `message_id` | **CASCADE** | Citations tied to assistant message. |

---

## 2. Soft-Delete Design Pattern

Tables with legal or financial significance (`users`, `documents`, `insurance_policies`, `loans`, `health_appointments`, `transactions`) implement soft-delete via `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`.

### Query Filter Pattern
All repository queries automatically append `AND is_deleted = FALSE` (via Hibernate `@SQLRestriction` or explicit repository query criteria).

### Vector Storage Synchronization
When a `documents` row has `is_deleted` set to `TRUE`, a transactional event fires `DELETE FROM document_chunks WHERE document_id = :id`, ensuring dead chunks are immediately evicted from vector similarity searches.
