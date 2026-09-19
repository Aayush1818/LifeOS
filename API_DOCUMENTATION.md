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

## 5. Loans & Amortization Engine (`/api/v1/loans`)

### `POST /api/v1/loans`
Creates a new loan obligation and auto-computes the monthly EMI using deterministic reducing-balance amortization math ($EMI = P \times \frac{r(1+r)^n}{(1+r)^n - 1}$).
* **Request Body**:
  ```json
  {
    "loanAccountNumber": "MORTGAGE-00129",
    "lenderName": "Apex Premier Lending",
    "loanType": "HOME",
    "principalAmount": 300000.00,
    "interestRate": 6.50,
    "interestType": "FIXED",
    "paymentFrequency": "MONTHLY",
    "tenureMonths": 360,
    "emiDueDay": 1,
    "startDate": "2026-10-01",
    "documentId": "4b92b6a2-...",
    "notes": "30-Year Fixed Primary Residence Mortgage"
  }
  ```
* **Response `201 Created`**:
  ```json
  {
    "success": true,
    "data": {
      "id": "8d3e91ca-...",
      "loanAccountNumber": "MORTGAGE-00129",
      "lenderName": "Apex Premier Lending",
      "loanType": "HOME",
      "principalAmount": 300000.00,
      "outstandingBalance": 300000.00,
      "interestRate": 6.50,
      "interestType": "FIXED",
      "paymentFrequency": "MONTHLY",
      "tenureMonths": 360,
      "monthlyEmi": 1896.20,
      "emiDueDay": 1,
      "startDate": "2026-10-01",
      "endDate": "2056-10-01",
      "totalPrincipalPaid": 0.00,
      "totalInterestPaid": 0.00,
      "status": "ACTIVE",
      "documentId": "4b92b6a2-...",
      "createdAt": "2026-09-19T12:00:00Z"
    }
  }
  ```

### `GET /api/v1/loans`
Retrieves paginated loans for the authenticated user, optionally filtered by `status`.
* **Query Parameters**: `status=ACTIVE`, `page=0`, `size=20`

### `GET /api/v1/loans/{id}`
Retrieves details of a specific loan. Returns RFC 7807 `404 Not Found` if nonexistent or owned by another tenant.

### `GET /api/v1/loans/{id}/schedule`
Generates the complete mathematical reducing-balance amortization schedule with final installment penny reconciliation ($C_n = 0.00$).
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "loanId": "8d3e91ca-...",
      "principal": 300000.00,
      "annualInterestRate": 6.50,
      "tenureMonths": 360,
      "monthlyPayment": 1896.20,
      "totalPayment": 682636.71,
      "totalInterest": 382636.71,
      "installments": [
        {
          "installmentNumber": 1,
          "dueDate": "2026-11-01",
          "openingPrincipal": 300000.00,
          "payment": 1896.20,
          "principalComponent": 271.20,
          "interestComponent": 1625.00,
          "closingPrincipal": 299728.80
        },
        ...
        {
          "installmentNumber": 360,
          "dueDate": "2056-10-01",
          "openingPrincipal": 1886.03,
          "payment": 1896.25,
          "principalComponent": 1886.03,
          "interestComponent": 10.22,
          "closingPrincipal": 0.00
        }
      ]
    }
  }
  ```

### `POST /api/v1/loans/{id}/payments`
Records a regular EMI payment, partial prepayment, or full early closure. Prepayments apply 100% directly to principal.
* **Request Body (Regular EMI)**:
  ```json
  {
    "paymentAmount": 1896.20,
    "paymentDate": "2026-11-01",
    "paymentType": "REGULAR_EMI",
    "transactionRef": "TXN-AUTO-DEBIT-01"
  }
  ```
* **Request Body (Partial Prepayment)**:
  ```json
  {
    "paymentAmount": 10000.00,
    "paymentDate": "2026-11-15",
    "paymentType": "PARTIAL_PREPAYMENT",
    "prepaymentStrategy": "REDUCE_EMI",
    "transactionRef": "TXN-PREPAY-01"
  }
  ```
* **Request Body (Full Early Closure)**:
  ```json
  {
    "paymentAmount": 289728.80,
    "paymentDate": "2026-12-01",
    "paymentType": "FULL_CLOSURE",
    "transactionRef": "TXN-WIRE-CLOSE"
  }
  ```

### `GET /api/v1/loans/analytics/summary`
Calculates portfolio-wide aggregations across active loans via direct JDBC query pushdown.
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "activeLoansCount": 2,
      "totalOriginalPrincipal": 320000.00,
      "totalOutstandingBalance": 314243.77,
      "totalMonthlyEmi": 2819.10,
      "totalPrincipalPaid": 5756.23,
      "totalInterestPaid": 333.34
    }
  }
  ```

---

## 6. Insurance Portfolio & Renewal Tracking (`/api/v1/insurance`)

### `POST /api/v1/insurance`
Creates a new insurance policy, links to documents/dependents with tenant verification, and automatically synchronizes a renewal reminder in the core `reminders` table.
* **Request Body**:
  ```json
  {
    "policyNumber": "HEALTH-BCBS-2026-99",
    "policyName": "Comprehensive Family Health Plan",
    "providerName": "Blue Cross Blue Shield",
    "policyType": "HEALTH",
    "coverageAmount": 500000.00,
    "premiumAmount": 450.00,
    "premiumFrequency": "MONTHLY",
    "startDate": "2026-09-19",
    "expiryDate": "2027-09-19",
    "dependentId": "cdcc6ef1-...",
    "documentId": "4b92b6a2-...",
    "notes": "Covers primary policyholder and dependent child"
  }
  ```

### `GET /api/v1/insurance`
Retrieves paginated policies for the authenticated user, optionally filtered by `type` or `status`.

### `GET /api/v1/insurance/{id}`
Retrieves insurance policy details by ID. Returns RFC 7807 `404 Not Found` if not owned by the authenticated tenant.

### `POST /api/v1/insurance/{id}/renew`
Renews an existing policy, updates the expiration date and premium amount, and automatically advances the linked reminder in the `reminders` table.
* **Request Body**:
  ```json
  {
    "newExpiryDate": "2028-09-19",
    "newPremiumAmount": 475.00,
    "notes": "Policy renewed for Year 2"
  }
  ```

### `GET /api/v1/insurance/renewals/upcoming`
Retrieves policies due for renewal within a configurable time window (default 30 days).
* **Query Parameters**: `windowDays=60`
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "windowDays": 60,
      "upcomingCount": 1,
      "policies": [
        {
          "id": "7af9b2db-...",
          "policyNumber": "HEALTH-BCBS-2026-99",
          "policyName": "Comprehensive Family Health Plan",
          "providerName": "Blue Cross Blue Shield",
          "policyType": "HEALTH",
          "coverageAmount": 500000.00,
          "premiumAmount": 450.00,
          "expiryDate": "2027-09-19",
          "status": "ACTIVE"
        }
      ]
    }
  }
  ```

### `DELETE /api/v1/insurance/{id}`
Soft-deletes the insurance policy and automatically dismisses the linked renewal reminder in the core `reminders` table.

---

## 7. Healthcare & Doctor Appointments Organization (`/api/v1/healthcare`)
*Strictly Organizational, Administrative & Scheduling — Regulatory Non-Diagnostic Safety Boundary*

Every response returned by the healthcare subsystem includes the static safety contract disclaimer:
> `"Strictly organizational & non-diagnostic. LifeOS does not provide medical diagnosis, clinical evaluation, or treatment advice."`

### `POST /api/v1/healthcare/appointments`
Creates a doctor consultation appointment for the user or their verified family dependent, and automatically schedules a due date reminder (`HEALTH_APPOINTMENT`).
* **Request Body**:
  ```json
  {
    "doctorName": "Dr. Sarah Jenkins",
    "specialization": "Cardiology",
    "clinicOrHospital": "Apex Cardiology Institute",
    "clinicPhone": "+1-555-019-2834",
    "clinicAddress": "100 Medical Center Way, Suite 400",
    "appointmentTime": "2026-10-15T14:30:00Z",
    "scheduledEndTime": "2026-10-15T15:15:00Z",
    "timeZone": "America/New_York",
    "purpose": "Annual cardiovascular evaluation and ECG review",
    "notes": "Bring current medication list and past lipid panels",
    "reminderOffsetMinutes": 60,
    "dependentId": "c84fefd8-4dd7-4ff3-9ec0-6c538d140b44",
    "primaryDocumentId": "96ff3ef3-f773-4ab9-bbc9-93fd6d97e4d4"
  }
  ```
* **Response `201 Created`**:
  ```json
  {
    "success": true,
    "message": "Appointment created successfully",
    "data": {
      "id": "68bde08a-9fc3-45ca-afc1-410c618d6f88",
      "doctorName": "Dr. Sarah Jenkins",
      "specialization": "Cardiology",
      "clinicOrHospital": "Apex Cardiology Institute",
      "clinicPhone": "+1-555-019-2834",
      "clinicAddress": "100 Medical Center Way, Suite 400",
      "appointmentTime": "2026-10-15T14:30:00Z",
      "scheduledEndTime": "2026-10-15T15:15:00Z",
      "timeZone": "America/New_York",
      "purpose": "Annual cardiovascular evaluation and ECG review",
      "notes": "Bring current medication list and past lipid panels",
      "status": "SCHEDULED",
      "dependentId": "c84fefd8-4dd7-4ff3-9ec0-6c538d140b44",
      "dependentName": "Emma Verified",
      "reminderOffsetMinutes": 60,
      "linkedDocuments": [],
      "disclaimer": "Strictly organizational & non-diagnostic. LifeOS does not provide medical diagnosis, clinical evaluation, or treatment advice.",
      "createdAt": "2026-09-19T17:57:32Z"
    }
  }
  ```

### `GET /api/v1/healthcare/appointments`
Retrieves paginated consultation records for the authenticated user, optionally filtered by `dependentId`, `status`, `doctorName`, `startDate`, or `endDate`.

### `GET /api/v1/healthcare/appointments/{id}`
Retrieves full details of an appointment including all linked medical documents (prescriptions, reports, summaries). Returns RFC 7807 `404 Not Found` if accessed by another user.

### `PUT /api/v1/healthcare/appointments/{id}`
Updates appointment metadata (doctor name, specialty, clinic address, notes, purpose).

### `POST /api/v1/healthcare/appointments/{id}/reschedule`
Modifies the appointment time and automatically updates the linked reminder's `due_at` date.
* **Request Body**:
  ```json
  {
    "newAppointmentTime": "2026-10-18T10:00:00Z",
    "newScheduledEndTime": "2026-10-18T10:45:00Z",
    "timeZone": "America/New_York",
    "notes": "Rescheduled due to conference travel",
    "reminderOffsetMinutes": 60
  }
  ```

### `POST /api/v1/healthcare/appointments/{id}/status`
Transitions the appointment status (`SCHEDULED`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`, `NO_SHOW`). When transitioning to `COMPLETED`, `CANCELLED`, or `NO_SHOW`, the linked reminder is automatically dismissed (`ReminderStatus.DISMISSED`).
* **Request Body**:
  ```json
  {
    "status": "COMPLETED",
    "notes": "Consultation concluded. Blood pressure measured at 120/80."
  }
  ```

### `DELETE /api/v1/healthcare/appointments/{id}`
Soft-deletes the consultation and automatically dismisses the linked reminder.

### `GET /api/v1/healthcare/appointments/upcoming`
Retrieves active appointments (`SCHEDULED`, `RESCHEDULED`) falling within the upcoming day window (default: 14 days).
* **Query Parameters**: `windowDays=14`
* **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "windowDays": 14,
      "upcomingCount": 1,
      "appointments": [ ... ]
    }
  }
  ```

### `POST /api/v1/healthcare/appointments/{id}/documents/{documentId}`
Attaches an existing uploaded document (category `MEDICAL` or associated) to the appointment via `document_entity_links`. Validates that both the appointment and document belong to the authenticated user.

### `DELETE /api/v1/healthcare/appointments/{id}/documents/{documentId}`
Unlinks the document from the appointment.

### `GET /api/v1/healthcare/documents`
Retrieves all medical documents belonging to the user with optional filters:
* **Query Parameters**: `type=PRESCRIPTION`, `dependentId={uuid}`, `page=0&size=20`
* **Response `200 OK`**: Paginated `MedicalDocumentLinkResponse` containing `id`, `title`, `documentType`, `mimeType`, `originalFilename`.

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
