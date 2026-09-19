# LifeOS — RAG Pipeline & Retrieval Mechanics

---

## 1. Chunking Strategy & Preservation of Structure

LifeOS uses a layout-aware sliding window chunker:

```
Document Text Stream
┌────────────────────────────────────────────────────────────┐
│ [Page 1 Header]                                            │
│ [Chunk 1: Tokens 0 - 500]                                  │
│                 [Chunk 2: Tokens 400 - 900] (100 overlap)  │
│                                  [Chunk 3: Tokens 800 - 1300]
└────────────────────────────────────────────────────────────┘
```

1. **Page Number Stamping**: Every token is mapped back to its physical source page in the original PDF.
2. **Heading Context Injection**: Major headings (e.g. `Section 4.1: Critical Illness Coverage`) are prepended to every child chunk to preserve semantic context across chunk splits.
3. **Chunk Size Tuning**:
   - Optimal token size: 500 tokens (approx. 375 words / 1,800 characters).
   - Overlap: 100 tokens (approx. 75 words / 350 characters) to avoid fragmenting key clauses across boundaries.

---

## 2. Vector Indexing Mechanics (`pgvector`)

In PostgreSQL 16, dense vector embeddings (1536 dimensions) are stored in the `embedding` column of type `vector(1536)`:

```sql
-- Cosine Distance Operator (<=>) with HNSW Index
SELECT 
    id, 
    document_id, 
    page_number, 
    content,
    1 - (embedding <=> :queryVector) AS cosine_similarity
FROM document_chunks
WHERE user_id = :userId
ORDER BY embedding <=> :queryVector
LIMIT :topK;
```

* `embedding <=> :queryVector` computes the cosine distance ($1 - \text{cosine similarity}$).
* Ordering by `<=>` utilizes the HNSW index in logarithmic time $O(\log N)$.

---

## 3. Citation Assembly & Verification

When a response is generated, citations are structured as follows:

```json
{
  "citations": [
    {
      "citationId": "cite-1",
      "documentId": "4b92b6a2-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "documentTitle": "Vehicle_Insurance_2026.pdf",
      "pageNumber": 3,
      "snippet": "Zero-depreciation rider is applicable up to 5 claims within the policy period.",
      "confidenceScore": 0.938
    }
  ]
}
```
In the Angular UI, this is displayed as an interactive citation pill `[Vehicle_Insurance_2026.pdf: Page 3]`, which allows the user to click and view the highlighted PDF page directly in the document viewer.
