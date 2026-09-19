# LifeOS — REST API Specification

---

## 1. Global API Standards

* **Base URL**: `/api/v1`
* **Content Negotiation**: `application/json` (Requests & Responses)
* **File Uploads**: `multipart/form-data`
* **Authentication**: `Authorization: Bearer <access_jwt>` header
* **Pagination**: Standard Spring Data parameters: `?page=0&size=20&sort=createdAt,desc`
* **Error Standards**: Conforms to **RFC 7807 Problem Details for HTTP APIs**

### Standard API Response Envelope
```json
{
  "success": true,
  "data": { ... },
  "message": "Resource retrieved successfully",
  "timestamp": "2026-09-19T12:00:00Z"
}
```

### Standard Error Response (RFC 7807)
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Field 'amount' must be greater than zero",
  "instance": "/api/v1/finance/transactions",
  "timestamp": "2026-09-19T12:00:00Z",
  "validationErrors": [
    {
      "field": "amount",
      "rejectedValue": -50.00,
      "message": "Must be greater than 0"
    }
  ]
}
```

---

## 2. Authentication & Identity (`/api/v1/auth`)

### `POST /api/v1/auth/register`
* **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "Alex",
    "lastName": "Morgan",
    "phone": "+1234567890"
  }
  ```
* **Response `201 Created`**:
  ```json
  {
    "success": true,
    "data": {
      "userId": "e7c11f4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "email": "user@example.com",
      "role": "ROLE_USER"
    }
  }
  ```

### `POST /api/v1/auth/login`
* **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }
  ```
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 900
    }
  }
  ```
  *(Refresh token is simultaneously set in an `HttpOnly`, `SameSite=Strict`, `Secure` cookie)*.

### `POST /api/v1/auth/refresh`
* **Request Body**:
  ```json
  {
    "refreshToken": "4f2a9c7b1e8d..."
  }
  ```
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "refreshToken": "9a8b7c6d5e...",
      "user": { ... }
    }
  }
  ```

### `POST /api/v1/auth/logout`
* Revokes the specified refresh token or all active refresh tokens for the authenticated user.
* **Headers**: `Authorization: Bearer <access_token>` (optional if passing body)
* **Request Body (optional)**:
  ```json
  {
    "refreshToken": "4f2a9c7b1e8d..."
  }
  ```
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "message": "Logout successful"
  }
  ```

---

## 3. User Profile Management (`/api/v1/users/me`)

### `GET /api/v1/users/me`
* Retrieves authenticated user's profile.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "id": "e7c11f4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "email": "user@example.com",
      "firstName": "Alex",
      "lastName": "Morgan",
      "phone": "+1234567890",
      "timezone": "UTC",
      "role": "ROLE_USER",
      "isActive": true
    }
  }
  ```

### `PUT /api/v1/users/me`
* Updates profile information.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Request Body**:
  ```json
  {
    "firstName": "Alexander",
    "lastName": "Morgan",
    "phone": "+1234567899",
    "timezone": "America/New_York"
  }
  ```
* **Response `200 OK`** with updated `UserResponse`.

---

## 4. Dependent / Family Management (`/api/v1/dependents`)

*Strictly isolated per authenticated user.*

### `POST /api/v1/dependents`
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Request Body**:
  ```json
  {
    "fullName": "Jane Doe",
    "relationship": "SPOUSE",
    "dateOfBirth": "1992-04-12",
    "isEmergencyContact": true,
    "emergencyContactPhone": "+1987654321",
    "preferences": { "dietary": "Vegetarian" },
    "medicalNotes": { "bloodGroup": "O+", "allergies": "Penicillin" }
  }
  ```
* **Response `201 Created`**: Returns created `DependentResponse`.

### `GET /api/v1/dependents`
* Lists all active family members and dependents belonging to the current user.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Response `200 OK`**: Array of `DependentResponse`.

### `GET /api/v1/dependents/{id}`
* Fetches single dependent by UUID.
* **Security Enforcement**: Returns `404 Not Found` if dependent belongs to another user or does not exist.

### `PUT /api/v1/dependents/{id}`
* Updates dependent metadata and JSONB notes.
* **Security Enforcement**: Returns `404 Not Found` if dependent belongs to another user.

### `DELETE /api/v1/dependents/{id}`
* Soft-deletes dependent (`is_deleted = true`).
* **Security Enforcement**: Returns `404 Not Found` if dependent belongs to another user.

---

## 5. Administrative Operations (`/api/v1/admin`)

*Protected by `@PreAuthorize("hasRole('ADMIN')")` and Spring Security role matcher.*

### `GET /api/v1/admin/status`
* Verifies administrative status and system access.
* **Headers**: `Authorization: Bearer <admin_jwt>`
* **Authorization**: Must have `ROLE_ADMIN`. If called by a user with `ROLE_USER`, returns RFC 7807 `403 Forbidden`.
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "message": "Admin access granted",
    "data": {
      "adminAccess": true,
      "message": "Admin authority confirmed"
    }
  }
  ```

---

## 6. Document Management & File Storage (`/api/v1/documents`)

*All document endpoints strictly enforce user ownership. Cross-tenant access returns RFC 7807 `404 Not Found`. Extracted text and metadata are provided with zero internal filesystem path exposure.*

### `POST /api/v1/documents/upload`
* Uploads document binary and metadata, performs Tika magic-byte MIME detection, enforces file limits, extracts text/metadata, and safely stores the file.
* **Content-Type**: `multipart/form-data`
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Parts**:
  * `file`: Binary file (PDF, DOCX, DOC, TXT, JPEG, PNG, WebP — max 25MB).
  * `metadata`: JSON object (`application/json`):
    ```json
    {
      "title": "Health Insurance Policy 2026",
      "category": "INSURANCE",
      "documentType": "POLICY",
      "dependentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "issueDate": "2026-01-01",
      "expiryDate": "2027-01-01",
      "tags": ["medical", "star-health", "policy"]
    }
    ```
* **Response `201 Created`**:
  ```json
  {
    "success": true,
    "message": "Document uploaded successfully",
    "data": {
      "id": "cb50eac7-547d-4dd8-b3e0-31c0fc4b39b9",
      "title": "Health Insurance Policy 2026",
      "originalFilename": "health_policy.txt",
      "mimeType": "text/plain",
      "fileSize": 142850,
      "category": "INSURANCE",
      "documentType": "POLICY",
      "dependentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "issueDate": "2026-01-01",
      "expiryDate": "2027-01-01",
      "tags": ["medical", "star-health", "policy"],
      "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "ingestionStatus": "PROCESSED",
      "version": 1,
      "createdAt": "2026-09-19T14:32:30.000Z",
      "updatedAt": "2026-09-19T14:32:30.000Z"
    }
  }
  ```

### `POST /api/v1/documents/{id}/versions`
* Uploads a replacement binary version for an existing document, auto-incrementing version integer.
* **Content-Type**: `multipart/form-data`
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Parts**:
  * `file`: New binary file revision.
* **Response `200 OK`**: Updated `DocumentResponse` with `version: 2`.

### `GET /api/v1/documents`
* Lists paginated documents belonging to the authenticated user, with optional filters.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Query Params**:
  * `category` (optional, enum: `PERSONAL`, `FINANCIAL`, `LEGAL`, `MEDICAL`, `TRAVEL`, `INSURANCE`, `TAX`, `OTHER`)
  * `dependentId` (optional, UUID)
  * `page` (optional, default: 0)
  * `size` (optional, default: 20)
  * `sort` (optional, default: `createdAt,desc`)
* **Response `200 OK`**: Spring Data `Page<DocumentResponse>`.

### `GET /api/v1/documents/{id}`
* Retrieves document metadata including extracted text and Tika technical metadata.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Security**: Returns `404 Not Found` if document does not belong to requesting user.
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "id": "cb50eac7-547d-4dd8-b3e0-31c0fc4b39b9",
      "title": "Health Insurance Policy 2026",
      "extractedText": "Health Insurance Policy #987654321...",
      "metadata": {
        "Content-Type": "text/plain",
        "Page-Count": "1"
      },
      "version": 1,
      "ingestionStatus": "PROCESSED"
    }
  }
  ```

### `GET /api/v1/documents/{id}/download`
* Streams the stored file binary.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Security**: Returns `404 Not Found` if document does not belong to requesting user.
* **Response `200 OK`**: Binary stream with headers:
  * `Content-Type: <mimeType>`
  * `Content-Length: <fileSize>`
  * `Content-Disposition: attachment; filename="<originalFilename>"`

### `DELETE /api/v1/documents/{id}`
* Soft-deletes the database record (`is_deleted = true`, status = `DELETED`) and purges physical file.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Security**: Returns `404 Not Found` if document does not belong to requesting user.
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "message": "Document deleted successfully"
  }
  ```

---

## 4. Personal Finance & Monthly Budgets (`/api/v1/finance`, `/api/v1/budgets`)

### `POST /api/v1/finance/transactions`
* Records a new income or expense transaction with strict `BigDecimal` precision.
* **Headers**: `Authorization: Bearer <access_jwt>`
* **Request Body**:
  ```json
  {
    "transactionType": "EXPENSE",
    "category": "FOOD_DINING",
    "amount": 150.75,
    "paymentMethod": "CREDIT_CARD",
    "transactionDate": "2026-09-19",
    "description": "Weekly supermarket groceries",
    "notes": "Organic produce",
    "documentId": "cb50eac7-547d-4dd8-b3e0-31c0fc4b39b9",
    "isRecurring": false,
    "isRefund": false
  }
  ```
* **Response `201 Created`**:
  ```json
  {
    "success": true,
    "message": "Transaction recorded successfully",
    "data": {
      "id": "2b95b8d2-7c38-4e8c-bb09-6447814b0b14",
      "amount": 150.75,
      "transactionType": "EXPENSE",
      "category": "FOOD_DINING",
      "transactionDate": "2026-09-19",
      "paymentMethod": "CREDIT_CARD",
      "description": "Weekly supermarket groceries",
      "notes": "Organic produce",
      "status": "POSTED",
      "documentId": "cb50eac7-547d-4dd8-b3e0-31c0fc4b39b9",
      "recurringId": null,
      "isRecurring": false,
      "isRefund": false,
      "possibleDuplicateWarning": false,
      "createdAt": "2026-09-19T14:40:00Z",
      "updatedAt": "2026-09-19T14:40:00Z"
    }
  }
  ```

### `GET /api/v1/finance/transactions`
* Lists paginated transactions belonging to the authenticated user.
* **Query Params**: `type`, `category`, `status`, `startDate`, `endDate`, `page`, `size`, `sort`.
* **Response `200 OK`**: `Page<TransactionResponse>`.

### `GET /api/v1/finance/transactions/{id}`
* Retrieves single transaction by ID. Returns `404 Not Found` if not owned by authenticated user.

### `PUT /api/v1/finance/transactions/{id}`
* Updates transaction details. Returns `404 Not Found` if not owned by authenticated user.

### `DELETE /api/v1/finance/transactions/{id}`
* Soft-deletes transaction (`is_deleted = true`). Returns `404 Not Found` if not owned by authenticated user.

### `GET /api/v1/finance/analytics/monthly-summary`
* High-performance deterministic aggregation via direct Spring JDBC (`FinanceAnalyticsJdbcRepository`).
* **Query Params**: `?month=9&year=2026` (defaults to current month/year if omitted)
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "month": 9,
      "year": 2026,
      "totalIncome": 6000.00,
      "totalExpenses": 600.00,
      "netSavings": 5400.00,
      "savingsRatePercentage": 90.00,
      "recurringExpensesTotal": 22.99,
      "categoryBreakdown": [
        {
          "category": "FOOD_DINING",
          "amount": 450.00,
          "percentageOfTotal": 75.00,
          "transactionCount": 3
        },
        {
          "category": "UTILITIES",
          "amount": 150.00,
          "percentageOfTotal": 25.00,
          "transactionCount": 1
        }
      ],
      "priorMonthComparison": {
        "currentMonthExpenses": 600.00,
        "priorMonthExpenses": 0.00,
        "deltaAmount": 600.00,
        "percentageChange": 100.00,
        "direction": "INCREASED"
      }
    }
  }
  ```

### `GET /api/v1/finance/analytics/category-breakdown`
* Retrieves list of category spending breakdowns with percentages and transaction counts.

### `GET /api/v1/finance/analytics/month-over-month`
* Returns month-over-month expense change, absolute delta, and direction (`INCREASED`, `DECREASED`, `UNCHANGED`).

### `POST /api/v1/finance/recurring`
* Creates recurring subscription or scheduled financial obligation.
* **Request Body**:
  ```json
  {
    "title": "Cloud Streaming Service",
    "amount": 19.99,
    "transactionType": "EXPENSE",
    "category": "ENTERTAINMENT",
    "paymentMethod": "CREDIT_CARD",
    "recurrencePattern": "MONTHLY",
    "billingDay": 10,
    "startDate": "2026-01-01",
    "autoCreateTransaction": true,
    "notes": "Subscription"
  }
  ```
* **Response `201 Created`**: `RecurringResponse`.

### `GET /api/v1/finance/recurring`
* Lists all recurring rules for authenticated user.

### `GET /api/v1/finance/recurring/{id}`, `PUT /api/v1/finance/recurring/{id}`, `DELETE /api/v1/finance/recurring/{id}`
* Manage individual recurring rules with strict tenant isolation (returns 404 for cross-tenant access).

### `POST /api/v1/budgets`
* Creates or updates monthly category budget with configurable alert thresholds.
* **Request Body**:
  ```json
  {
    "category": "FOOD_DINING",
    "budgetMonth": 9,
    "budgetYear": 2026,
    "allocatedAmount": 800.00,
    "alertThresholds": [50, 75, 90, 100]
  }
  ```
* **Response `201 Created`**: `BudgetResponse`.

### `GET /api/v1/budgets`
* Lists category budgets for specified month and year (`?month=9&year=2026`).

### `GET /api/v1/budgets/{id}`, `PUT /api/v1/budgets/{id}`, `DELETE /api/v1/budgets/{id}`
* Manage individual budget allocations with strict tenant isolation (returns 404 for cross-tenant access).

### `GET /api/v1/budgets/status`
* Deterministic calculation of budget vs actuals, remaining balance, utilization %, in-flight run-rate projected spend, and threshold evaluation.
* **Query Params**: `?month=9&year=2026`
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "month": 9,
      "year": 2026,
      "totalAllocated": 800.00,
      "totalSpent": 450.00,
      "totalRemaining": 350.00,
      "overallUtilizationPercentage": 56.25,
      "categories": [
        {
          "budgetId": "e12107e0-efa3-4eae-9cdc-4292ffec684c",
          "category": "FOOD_DINING",
          "allocatedAmount": 800.00,
          "actualSpent": 450.00,
          "remainingAmount": 350.00,
          "utilizationPercentage": 56.25,
          "projectedSpend": 710.53,
          "isOverBudget": false,
          "highestTriggeredThreshold": 50
        }
      ]
    }
  }
  ```

---

## 5. Loans & Liabilities (`/api/v1/loans`)

### `GET /api/v1/loans`
* **Response `200 OK`**:
  ```json
  [
    {
      "id": "8d3e91ca-...",
      "lenderName": "HDFC Bank",
      "loanType": "HOME",
      "principalAmount": 4500000.00,
      "outstandingBalance": 3820000.00,
      "interestRate": 8.45,
      "monthlyEmi": 38900.00,
      "emiDueDay": 5,
      "status": "ACTIVE"
    }
  ]
  ```

### `POST /api/v1/loans/{id}/payments`
* **Request Body**:
  ```json
  {
    "amount": 38900.00,
    "principalComponent": 12000.00,
    "interestComponent": 26900.00,
    "paymentDate": "2026-09-05",
    "transactionRef": "TXN-902184"
  }
  ```

---

## 6. Insurance Portfolio (`/api/v1/insurance`)

### `GET /api/v1/insurance/policies`
* Returns active and past policies, renewal dates, premium schedules, and coverage amounts.

### `POST /api/v1/insurance/compare`
* Deep semantic comparison between two policy document IDs.
* **Request Body**:
  ```json
  {
    "previousPolicyDocumentId": "4b92b6a2-...",
    "renewalPolicyDocumentId": "9c12b7a8-..."
  }
  ```
* **Response `200 OK`**:
  ```json
  {
    "premiumDifference": 1200.00,
    "coverageDifference": 500000.00,
    "clausesChanged": [
      "Room rent capping removed in renewal policy",
      "Co-pay deductible increased from 10% to 15% for pre-existing diseases"
    ]
  }
  ```

---

## 7. Healthcare & Appointments (`/api/v1/health`)
*Strictly Organizational & Administrative — No Medical Diagnosis*

### `POST /api/v1/health/appointments`
* **Request Body**:
  ```json
  {
    "doctorName": "Dr. Sarah Jenkins",
    "specialization": "Cardiology",
    "clinicOrHospital": "City Heart Institute",
    "appointmentTime": "2026-10-15T14:30:00Z",
    "purpose": "Annual cardiac stress test and general checkup",
    "notes": "Bring previous lipid profile report"
  }
  ```

---

## 8. AI Assistant & Hybrid RAG (`/api/v1/ai`)

### `POST /api/v1/ai/chat`
* **Request Body**:
  ```json
  {
    "conversationId": "7f8a12bc-...",
    "message": "When does my car insurance expire, and what is my total upcoming loan EMI this month?"
  }
  ```
* **Response `200 OK`**:
  ```json
  {
    "conversationId": "7f8a12bc-...",
    "response": "Your Car Insurance (Policy #MOT-2025-88) expires on **14 December 2026**. Your upcoming loan EMI for this month is **₹38,900.00** for your HDFC Home Loan, due on the 5th.",
    "citations": [
      {
        "documentTitle": "Car_Insurance_Policy_2025.pdf",
        "pageNumber": 2,
        "snippet": "Period of Insurance: Valid from 15/12/2025 to 14/12/2026 midnight.",
        "confidenceScore": 0.942
      }
    ],
    "toolCallsExecuted": [
      {
        "toolName": "getActiveLoans",
        "result": { "totalMonthlyEmi": 38900.00 }
      },
      {
        "toolName": "searchInsurancePolicies",
        "result": { "policyNumber": "MOT-2025-88", "expiryDate": "2026-12-14" }
      }
    ]
  }
  ```
