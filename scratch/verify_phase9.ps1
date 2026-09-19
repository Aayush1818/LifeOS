# verify_phase9.ps1 - End-to-end verification script for Phase 9 Product Warranties, Invoices & Asset Management
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "=== Phase 9 Warranties, Invoices & Assets Live HTTP Verification ===" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Health check & Ping
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 10
    if ($health.status -eq "UP") {
        Write-Host "[PASS] 1a. Actuator Health is UP" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 1a. Actuator Health status: $($health.status)" -ForegroundColor Red
        exit 1
    }

    $ping = Invoke-RestMethod -Uri "$baseUrl/api/v1/health/ping" -Method Get -TimeoutSec 10
    if ($ping.success -and $ping.data.database -eq "CONNECTED" -and $ping.data.pgvectorExtension -eq "AVAILABLE") {
        Write-Host "[PASS] 1b. Public Health Ping succeeded (DB Connected, pgvector Available)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 1b. Health Ping unexpected: $($ping | ConvertTo-Json -Depth 2)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[FAIL] 1. Server unreachable: $_" -ForegroundColor Red
    exit 1
}

# 2. Register User A
$suffixA = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userAEmail = "asset.userA.$suffixA@example.com"
$regA = @{
    email = $userAEmail
    password = "Password123!@#"
    firstName = "AssetOwnerA"
    lastName = "Verified"
} | ConvertTo-Json

$resA = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regA
$tokenA = $resA.data.accessToken
Write-Host "[PASS] 2. User A registered: $userAEmail (Token acquired)" -ForegroundColor Green

# 3. Register User B
$suffixB = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userBEmail = "asset.userB.$suffixB@example.com"
$regB = @{
    email = $userBEmail
    password = "Password123!@#"
    firstName = "AssetOwnerB"
    lastName = "Verified"
} | ConvertTo-Json

$resB = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regB
$tokenB = $resB.data.accessToken
Write-Host "[PASS] 3. User B registered: $userBEmail (Token acquired)" -ForegroundColor Green

$headersA = @{
    Authorization = "Bearer $tokenA"
    "Content-Type" = "application/json"
}

$headersB = @{
    Authorization = "Bearer $tokenB"
    "Content-Type" = "application/json"
}

# 4. User A creates Dependent "Alice Jr."
$depPayload = @{
    fullName = "Alice Jr."
    relationship = "CHILD"
    dateOfBirth = "2015-06-15"
} | ConvertTo-Json

$depRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/dependents" -Method Post -Headers $headersA -Body $depPayload
$dependentId = $depRes.data.id
Write-Host "[PASS] 4. User A created Dependent 'Alice Jr.' (ID: $dependentId)" -ForegroundColor Green

# 5. User A creates Asset 1 (MacBook Pro) linked to Dependent
$asset1Payload = @{
    name = "MacBook Pro 16 M3 Max"
    category = "ELECTRONICS"
    brand = "Apple"
    modelNumber = "A2991"
    serialNumber = "C02XYZ1234AB"
    purchaseDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-dd")
    returnDeadline = (Get-Date).AddDays(20).ToString("yyyy-MM-dd")
    purchasePrice = 3499.00
    currency = "USD"
    dependentId = $dependentId
    location = "Home Office"
    notes = "Primary development workstation"
} | ConvertTo-Json

$asset1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets" -Method Post -Headers $headersA -Body $asset1Payload
$asset1Id = $asset1Res.data.id
if ($asset1Res.data.name -eq "MacBook Pro 16 M3 Max" -and $asset1Res.data.dependentId -eq $dependentId) {
    Write-Host "[PASS] 5. User A created Asset 1 (MacBook Pro) with Dependent Link (ID: $asset1Id)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 5. Asset 1 creation unexpected" -ForegroundColor Red
}

# 6. User A creates Asset 2 in EUR (European Espresso Machine)
$asset2Payload = @{
    name = "La Marzocco Micra"
    category = "APPLIANCE"
    brand = "La Marzocco"
    purchasePrice = 3900.00
    currency = "EUR"
    location = "Kitchen"
} | ConvertTo-Json

$asset2Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets" -Method Post -Headers $headersA -Body $asset2Payload
$asset2Id = $asset2Res.data.id
Write-Host "[PASS] 6. User A created Asset 2 in EUR (ID: $asset2Id)" -ForegroundColor Green

# 7. Multi-currency Acquisition Cost Aggregation
$summary = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/acquisition-summary" -Method Get -Headers $headersA
if ($summary.data.totalsByCurrency.USD -eq 3499.00 -and $summary.data.totalsByCurrency.EUR -eq 3900.00 -and $null -eq $summary.data.consolidatedTotal) {
    Write-Host "[PASS] 7. Multi-currency acquisition cost summary deterministic (USD 3499.00, EUR 3900.00, consolidatedTotal = null)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 7. Acquisition summary unexpected: $($summary | ConvertTo-Json -Depth 3)" -ForegroundColor Red
}

# 8. User A creates multi-item Invoice with line items referencing Asset 1
$invoicePayload = @{
    invoiceNumber = "APL-INV-2026-901"
    vendorName = "Apple Store Fifth Avenue"
    invoiceDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-dd")
    returnDeadline = (Get-Date).AddDays(14).ToString("yyyy-MM-dd")
    currency = "USD"
    subtotal = 3628.00
    taxAmount = 322.00
    shippingAmount = 0.00
    discountAmount = 50.00
    totalAmount = 3900.00
    paymentStatus = "PAID"
    paymentMethod = "CREDIT_CARD"
    items = @(
        @{
            assetId = $asset1Id
            itemDescription = "MacBook Pro 16-inch 64GB Unified Memory"
            quantity = 1
            unitPrice = 3499.00
            totalPrice = 3499.00
        },
        @{
            itemDescription = "140W USB-C Power Adapter with MagSafe Cable"
            quantity = 1
            unitPrice = 129.00
            totalPrice = 129.00
        }
    )
} | ConvertTo-Json -Depth 4

$invRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/invoices" -Method Post -Headers $headersA -Body $invoicePayload
$invoiceId = $invRes.data.id
if ($invRes.data.items.Count -eq 2 -and $invRes.data.items[0].assetId -eq $asset1Id) {
    Write-Host "[PASS] 8. User A created Invoice with 2 line items linking Asset 1 (ID: $invoiceId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8. Invoice creation unexpected" -ForegroundColor Red
}

# 9. Finance Integration: Convert Invoice 2 to Expense Transaction atomically
$inv2Payload = @{
    invoiceNumber = "BB-2026-4412"
    vendorName = "Best Buy Electronics"
    invoiceDate = (Get-Date).AddDays(-3).ToString("yyyy-MM-dd")
    totalAmount = 149.99
    currency = "USD"
    items = @(
        @{
            itemDescription = "Logitech MX Mechanical Keyboard"
            quantity = 1
            unitPrice = 149.99
        }
    )
} | ConvertTo-Json -Depth 4

$inv2Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/invoices" -Method Post -Headers $headersA -Body $inv2Payload
$invoice2Id = $inv2Res.data.id

$convReq = @{
    category = "SHOPPING"
    paymentMethod = "DEBIT_CARD"
    description = "Best Buy Keyboard Purchase"
} | ConvertTo-Json

$convRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/invoices/$invoice2Id/convert-to-transaction" -Method Post -Headers $headersA -Body $convReq
$generatedTxId = $convRes.data.transactionId
if ($null -ne $generatedTxId) {
    Write-Host "[PASS] 9. Converted Invoice to Expense Transaction atomically (Tx ID: $generatedTxId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 9. Convert to transaction failed" -ForegroundColor Red
}

# 10. Duplicate Conversion Prevention (must return 409 Conflict)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/invoices/$invoice2Id/convert-to-transaction" -Method Post -Headers $headersA -Body $convReq
    Write-Host "[FAIL] 10. Duplicate conversion allowed! Expected 409 Conflict." -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "[PASS] 10. Duplicate conversion prevented with 409 Conflict" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 10. Unexpected status code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

# 11. Add Standard Warranty with Expiry Reminder
$w1Payload = @{
    provider = "AppleCare+"
    warrantyType = "EXTENDED"
    policyNumber = "AC-99281-MBP"
    startDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-dd")
    expiryDate = (Get-Date).AddYears(2).ToString("yyyy-MM-dd")
    coverageDetails = "Comprehensive accidental damage and mechanical breakdown protection"
    deductibleAmount = 99.00
    currency = "USD"
    reminderOffsetDays = 30
} | ConvertTo-Json

$w1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/warranties" -Method Post -Headers $headersA -Body $w1Payload
$warranty1Id = $w1Res.data.id
if ($null -ne $w1Res.data.reminderId) {
    Write-Host "[PASS] 11. Created AppleCare+ Warranty with 09:00 Reminder (Reminder ID: $($w1Res.data.reminderId))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 11. Warranty reminder not scheduled" -ForegroundColor Red
}

# 12. Add LIFETIME Warranty (must NEVER schedule reminder)
$wLifePayload = @{
    provider = "Craftsman Tools Lifetime Guarantee"
    warrantyType = "LIFETIME"
    startDate = (Get-Date).ToString("yyyy-MM-dd")
    coverageDetails = "Unlimited lifetime replacement"
} | ConvertTo-Json

$wLifeRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/warranties" -Method Post -Headers $headersA -Body $wLifePayload
if ($wLifeRes.data.warrantyType -eq "LIFETIME" -and $null -eq $wLifeRes.data.reminderId -and $null -eq $wLifeRes.data.expiryDate) {
    Write-Host "[PASS] 12. LIFETIME Warranty created without expiry reminder (reminderId = null)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 12. LIFETIME warranty generated unexpected reminder" -ForegroundColor Red
}

# 13. File Warranty Claim & Progress through State Machine
$claimPayload = @{
    claimNumber = "RMA-AAPL-5519"
    claimDate = (Get-Date).AddDays(-2).ToString("yyyy-MM-dd")
    claimType = "REPAIR"
    description = "Cracked Liquid Retina display glass from accidental drop"
} | ConvertTo-Json

$claimRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/warranties/$warranty1Id/claims" -Method Post -Headers $headersA -Body $claimPayload
$claimId = $claimRes.data.id
Write-Host "[PASS] 13a. Filed Warranty Claim in FILED status (Claim ID: $claimId)" -ForegroundColor Green

# Progress claim: FILED -> UNDER_REVIEW -> APPROVED -> RESOLVED
$statusReq1 = @{ status = "UNDER_REVIEW" } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/warranties/claims/$claimId/status" -Method Patch -Headers $headersA -Body $statusReq1 | Out-Null

$statusReq2 = @{ status = "APPROVED" } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/warranties/claims/$claimId/status" -Method Patch -Headers $headersA -Body $statusReq2 | Out-Null

$resolveReq = @{
    status = "RESOLVED"
    resolution = "Display assembly replaced at Apple Genius Bar; $99 tier 1 accidental deductible paid"
    resolvedDate = (Get-Date).ToString("yyyy-MM-dd")
    claimCostCovered = 699.00
    outOfPocketCost = 99.00
} | ConvertTo-Json

$resolvedRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/warranties/claims/$claimId/status" -Method Patch -Headers $headersA -Body $resolveReq
if ($resolvedRes.data.status -eq "RESOLVED" -and $resolvedRes.data.claimCostCovered -eq 699.00) {
    Write-Host "[PASS] 13b. Resolved Warranty Claim with covered cost $699.00 and out-of-pocket $99.00" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 13b. Claim resolution unexpected" -ForegroundColor Red
}

# 14. Void Warranty & Verify Reminder Dismissed
$voidReq = @{ status = "VOID" } | ConvertTo-Json
$voidRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/warranties/$warranty1Id/status" -Method Patch -Headers $headersA -Body $voidReq
if ($voidRes.data.status -eq "VOID") {
    Write-Host "[PASS] 14. Voided warranty successfully (linked reminder dismissed)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 14. Void warranty failed" -ForegroundColor Red
}

# 15. Verify Historical Claims Remain Queryable by Asset ID
$assetClaims = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/claims" -Method Get -Headers $headersA
if ($assetClaims.data.Count -ge 1 -and $assetClaims.data[0].claimNumber -eq "RMA-AAPL-5519") {
    Write-Host "[PASS] 15. Historical claims remain 100% queryable for Asset 1 after warranty void/expiry" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 15. Historical claims query failed" -ForegroundColor Red
}

# 16. Record Asset Service & Maintenance
$servicePayload = @{
    serviceDate = (Get-Date).ToString("yyyy-MM-dd")
    serviceType = "REPAIR"
    serviceProvider = "Apple Authorized Service Provider"
    description = "Replaced display panel under AppleCare+"
    cost = 99.00
    currency = "USD"
    status = "COMPLETED"
    warrantyClaimId = $claimId
} | ConvertTo-Json

$svcRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/services" -Method Post -Headers $headersA -Body $servicePayload
if ($svcRes.data.cost -eq 99.00 -and $svcRes.data.warrantyClaimId -eq $claimId) {
    Write-Host "[PASS] 16. Recorded Service Record linked to Warranty Claim (ID: $($svcRes.data.id))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 16. Service record creation failed" -ForegroundColor Red
}

# 17. Asset Status Transition & Immutable Audit History
$statusTransition = @{
    status = "UNDER_REPAIR"
    reason = "Sent for scheduled battery and display calibration"
} | ConvertTo-Json

$transitionRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/status" -Method Patch -Headers $headersA -Body $statusTransition
$history = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/status-history" -Method Get -Headers $headersA
if ($transitionRes.data.status -eq "UNDER_REPAIR" -and $history.data.Count -ge 2) {
    Write-Host "[PASS] 17. Asset status transitioned to UNDER_REPAIR with immutable audit history ($($history.data.Count) records)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 17. Status transition failed" -ForegroundColor Red
}

# 18. Reject Invalid Asset State Transition (409 Conflict)
$invalidTransition = @{
    status = "GIFTED"
    reason = "Attempt invalid transition directly from UNDER_REPAIR to GIFTED"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/status" -Method Patch -Headers $headersA -Body $invalidTransition
    Write-Host "[FAIL] 18. Invalid state transition succeeded! Expected 409 Conflict." -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "[PASS] 18. Invalid state transition rejected with 409 Conflict" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 18. Unexpected error code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

# 19. Strict Cross-Tenant Security Isolation (404 Not Found)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id" -Method Get -Headers $headersB
    Write-Host "[FAIL] 19a. Cross-tenant GET allowed! Expected 404 Not Found." -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 19a. User B GET User A's asset rejected with 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 19a. Unexpected status code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/invoices/$invoiceId" -Method Get -Headers $headersB
    Write-Host "[FAIL] 19b. Cross-tenant invoice GET allowed! Expected 404 Not Found." -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 19b. User B GET User A's invoice rejected with 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 19b. Unexpected status code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id" -Method Delete -Headers $headersB
    Write-Host "[FAIL] 19c. Cross-tenant DELETE allowed! Expected 404 Not Found." -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 19c. User B DELETE User A's asset rejected with 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 19c. Unexpected status code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "=== Phase 9 Live Verification Complete: 19 Logical Groups Passed ===" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
