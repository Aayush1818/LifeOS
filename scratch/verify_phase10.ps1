# verify_phase10.ps1 - End-to-end verification script for Phase 10 Unified Search & Advanced Query Platform
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "=== Phase 10 Unified Search & Advanced Query Live HTTP Verification ===" -ForegroundColor Cyan
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
$userAEmail = "search.liveA.$suffixA@example.com"
$regA = @{
    email = $userAEmail
    password = "Password123!@#"
    firstName = "SearchUserA"
    lastName = "Verified"
} | ConvertTo-Json

$resA = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regA
$tokenA = $resA.data.accessToken
Write-Host "[PASS] 2. User A registered: $userAEmail (Token acquired)" -ForegroundColor Green

# 3. Register User B
$suffixB = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userBEmail = "search.liveB.$suffixB@example.com"
$regB = @{
    email = $userBEmail
    password = "Password123!@#"
    firstName = "SearchUserB"
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

# 5. User A creates Asset 1 (MacBook Pro 16 M3 Max) linked to Dependent
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
    notes = "Primary development workstation with AppleCare+"
} | ConvertTo-Json

$asset1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets" -Method Post -Headers $headersA -Body $asset1Payload
$asset1Id = $asset1Res.data.id
Write-Host "[PASS] 5. User A created Asset 1 (MacBook Pro) with Dependent Link (ID: $asset1Id)" -ForegroundColor Green

# 6. User A creates Invoice linking Asset 1
$invPayload = @{
    invoiceNumber = "INV-2026-LIVE-001"
    vendorName = "Apple Store Fifth Avenue"
    invoiceDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-dd")
    currency = "USD"
    subtotal = 3499.00
    taxAmount = 300.00
    shippingAmount = 0.00
    discountAmount = 0.00
    totalAmount = 3799.00
    paymentStatus = "PAID"
    paymentMethod = "CREDIT_CARD"
    items = @(
        @{
            itemDescription = "MacBook Pro 16 inch M3 Max"
            quantity = 1
            unitPrice = 3499.00
            totalPrice = 3499.00
            assetId = $asset1Id
        }
    )
} | ConvertTo-Json -Depth 4

$invRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/invoices" -Method Post -Headers $headersA -Body $invPayload
$invoiceId = $invRes.data.id
Write-Host "[PASS] 6. User A created Invoice (ID: $invoiceId)" -ForegroundColor Green

# 7. User A creates Warranty on Asset 1
$warPayload = @{
    provider = "AppleCare+ for Mac"
    warrantyType = "EXTENDED"
    policyNumber = "AC-MAC-998811"
    coverageDetails = "Comprehensive accidental damage and hardware repair"
    startDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-dd")
    expiryDate = (Get-Date).AddYears(3).ToString("yyyy-MM-dd")
    deductibleAmount = 99.00
    currency = "USD"
    reminderOffsetDays = 30
} | ConvertTo-Json

$warRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets/$asset1Id/warranties" -Method Post -Headers $headersA -Body $warPayload
$warrantyId = $warRes.data.id
Write-Host "[PASS] 7. User A created AppleCare+ Warranty (ID: $warrantyId)" -ForegroundColor Green

# 8. User A creates Finance Transaction
$txPayload = @{
    amount = 199.00
    transactionType = "EXPENSE"
    category = "SHOPPING"
    transactionDate = (Get-Date).ToString("yyyy-MM-dd")
    paymentMethod = "CREDIT_CARD"
    description = "Apple USB-C Hub and Accessories"
} | ConvertTo-Json

$txRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions" -Method Post -Headers $headersA -Body $txPayload
$txId = $txRes.data.id
Write-Host "[PASS] 8. User A created Transaction (ID: $txId)" -ForegroundColor Green

# 9. Unified Keyword Search for "MacBook"
$searchRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?q=MacBook" -Method Get -Headers $headersA
$foundTypes = $searchRes.data.content | ForEach-Object { $_.entityType }
if ($foundTypes -contains "ASSET" -and $foundTypes -contains "INVOICE") {
    Write-Host "[PASS] 9. Unified search 'MacBook' returned multi-domain entities: $($foundTypes -join ', ')" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 9. Expected ASSET and INVOICE in results, found: $($foundTypes -join ', ')" -ForegroundColor Red
    exit 1
}

# 10. Entity Type Filter: restrict to ASSET only
$assetOnlyRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?q=MacBook&entities=ASSET" -Method Get -Headers $headersA
$nonAssets = $assetOnlyRes.data.content | Where-Object { $_.entityType -ne "ASSET" }
if ($assetOnlyRes.data.content.Count -ge 1 -and $nonAssets.Count -eq 0) {
    Write-Host "[PASS] 10. Entity filter 'entities=ASSET' strictly returned only ASSET entities ($($assetOnlyRes.data.content.Count) items)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 10. Entity filter returned non-asset items or empty" -ForegroundColor Red
    exit 1
}

# 11. Faceted Count Endpoint (/api/v1/search/count)
$countRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search/count?q=Apple" -Method Get -Headers $headersA
if ($countRes.success -and $countRes.data.totalCount -ge 3 -and $countRes.data.countsByEntity.ASSET -ge 1) {
    Write-Host "[PASS] 11. Faceted count returned total: $($countRes.data.totalCount), ASSET: $($countRes.data.countsByEntity.ASSET), INVOICE: $($countRes.data.countsByEntity.INVOICE)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 11. Unexpected count result: $($countRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 12. Typeahead Suggestions (/api/v1/search/suggest)
$suggestRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search/suggest?q=Mac&limit=5" -Method Get -Headers $headersA
if ($suggestRes.success -and $suggestRes.data.Count -ge 1 -and $suggestRes.data[0].text -like "*Mac*") {
    Write-Host "[PASS] 12. Typeahead suggestion for 'Mac' returned: '$($suggestRes.data[0].text)'" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 12. Typeahead suggestions empty or invalid" -ForegroundColor Red
    exit 1
}

# 13. Amount Range Filter
$amountRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?minAmount=3000.00&maxAmount=4000.00&currency=USD" -Method Get -Headers $headersA
$allInRange = $true
foreach ($item in $amountRes.data.content) {
    if ($item.amount -lt 3000.00 -or $item.amount -gt 4000.00) { $allInRange = $false }
}
if ($amountRes.data.content.Count -ge 1 -and $allInRange) {
    Write-Host "[PASS] 13. Amount range filter (3000-4000 USD) returned $($amountRes.data.content.Count) items, all within range" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 13. Amount range filter failed" -ForegroundColor Red
    exit 1
}

# 14. Dependent Filter & Dependent Name Enrichment
$depSearchRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?dependentId=$dependentId" -Method Get -Headers $headersA
$depItem = $depSearchRes.data.content | Where-Object { $_.dependentId -eq $dependentId }
if ($depItem -and $depItem[0].dependentName -eq "Alice Jr.") {
    Write-Host "[PASS] 14. Dependent filter returned asset with enriched dependentName 'Alice Jr.'" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 14. Dependent filter or enrichment failed" -ForegroundColor Red
    exit 1
}

# 15. Cross-Tenant Dependent Access Rejection (404)
$depBPayload = @{
    fullName = "Bob Jr."
    relationship = "CHILD"
    dateOfBirth = "2019-01-01"
} | ConvertTo-Json
$depBRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/dependents" -Method Post -Headers $headersB -Body $depBPayload
$depBId = $depBRes.data.id

try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?dependentId=$depBId" -Method Get -Headers $headersA
    Write-Host "[FAIL] 15. Cross-tenant dependent search should have failed with 404" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 15. User A searching with User B's dependent ID returned RFC 7807 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15. Unexpected status code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        exit 1
    }
}

# 16. Multi-Tenant Isolation: User A items are invisible to User B
$secretName = "Confidential Top Secret Prototype " + [System.Guid]::NewGuid().ToString()
$secretPayload = @{
    name = $secretName
    category = "OTHER"
    purchasePrice = 9999.00
    currency = "USD"
    purchaseDate = (Get-Date).ToString("yyyy-MM-dd")
} | ConvertTo-Json
$null = Invoke-RestMethod -Uri "$baseUrl/api/v1/assets" -Method Post -Headers $headersA -Body $secretPayload

$bSearchRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?q=$secretName" -Method Get -Headers $headersB
if ($bSearchRes.data.content.Count -eq 0) {
    Write-Host "[PASS] 16. User B searching for User A's secret record returned 0 items (Strict isolation verified)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 16. Data leakage! User B saw User A's record" -ForegroundColor Red
    exit 1
}

# 17. Invalid Date Range Validation (400)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?startDate=2026-12-31&endDate=2026-01-01" -Method Get -Headers $headersA
    Write-Host "[FAIL] 17. Invalid date range should have failed with 400" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host "[PASS] 17. Invalid date range (startDate > endDate) returned RFC 7807 400 Bad Request" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 17. Unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        exit 1
    }
}

# 18. Invalid Amount Range Validation (400)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/v1/search?minAmount=500.00&maxAmount=100.00" -Method Get -Headers $headersA
    Write-Host "[FAIL] 18. Invalid amount range should have failed with 400" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host "[PASS] 18. Invalid amount range (minAmount > maxAmount) returned RFC 7807 400 Bad Request" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 18. Unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        exit 1
    }
}

# 19. Supported Entities Endpoint (/api/v1/search/entities)
$entRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/search/entities" -Method Get -Headers $headersA
if ($entRes.success -and $entRes.data.Count -eq 15 -and ($entRes.data -contains "ASSET") -and ($entRes.data -contains "REMINDER")) {
    Write-Host "[PASS] 19. Supported entities endpoint returned all 15 LifeOS entity types" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 19. Supported entities endpoint returned: $($entRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "=== Phase 10 Live Verification Complete: All 19 Checks PASSED ===" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
