# ==============================================================================
# LifeOS Phase 12 - Hybrid RAG Retrieval Engine Live Verification
# ==============================================================================

$BaseUrl = "http://localhost:8080/api/v1"
$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  LIFEOS PHASE 12 HYBRID RAG RETRIEVAL LIVE VERIFICATION" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$Passed = 0
$Failed = 0

function Assert-Check {
    param(
        [string]$Name,
        [bool]$Condition,
        [string]$Details = ""
    )
    if ($Condition) {
        Write-Host " [PASS] $Name" -ForegroundColor Green
        $script:Passed++
    } else {
        Write-Host " [FAIL] $Name - $Details" -ForegroundColor Red
        $script:Failed++
    }
}

# 1. System Health Ping
$Health = Invoke-RestMethod -Uri "$BaseUrl/health/ping" -Method Get
Assert-Check "1. Health ping status is UP" ($Health.data.status -eq "UP")
Assert-Check "2. pgvector extension is AVAILABLE" ($Health.data.pgvectorExtension -eq "AVAILABLE")

# 2. Register Users
$UserAEmail = "rag.liveA." + [guid]::NewGuid().ToString().Substring(0,8) + "@example.com"
$UserBEmail = "rag.liveB." + [guid]::NewGuid().ToString().Substring(0,8) + "@example.com"
$Password = "TestPass123!@#"

$RegA = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType "application/json" -Body (@{
    email = $UserAEmail
    password = $Password
    firstName = "RetrieveA"
    lastName = "Alpha"
} | ConvertTo-Json)
$TokenA = $RegA.data.accessToken
$UserAId = $RegA.data.user.id
Assert-Check "3. Registered User A successfully" ($null -ne $TokenA)

$RegB = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType "application/json" -Body (@{
    email = $UserBEmail
    password = $Password
    firstName = "RetrieveB"
    lastName = "Beta"
} | ConvertTo-Json)
$TokenB = $RegB.data.accessToken
$UserBId = $RegB.data.user.id
Assert-Check "4. Registered User B successfully" ($null -ne $TokenB)

$HeadersA = @{ "Authorization" = "Bearer $TokenA" }
$HeadersB = @{ "Authorization" = "Bearer $TokenB" }
$AuthH = "Authorization: Bearer $TokenA"

# 3. User A creates a Dependent
$DepRes = Invoke-RestMethod -Uri "$BaseUrl/dependents" -Method Post -Headers $HeadersA -ContentType "application/json" -Body (@{
    fullName = "Alice Jr."
    relationship = "CHILD"
} | ConvertTo-Json)
$DependentId = $DepRes.data.id
Assert-Check "5. User A created Dependent 'Alice Jr.'" ($null -ne $DependentId)

# Helper function to create minimal valid PDF
function New-SamplePdf {
    param([string]$FilePath, [string]$Text)
    $StreamBytes = [System.Text.Encoding]::UTF8.GetBytes("BT /F1 12 Tf 100 700 Td ($Text) Tj ET")
    $StreamLength = $StreamBytes.Length

    $PdfContent = @"
%PDF-1.4
1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj
4 0 obj << /Length $StreamLength >> stream
$([System.Text.Encoding]::UTF8.GetString($StreamBytes))
endstream
endobj
xref
0 5
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000214 00000 n 
trailer << /Size 5 /Root 1 0 R >>
startxref
380
%%EOF
"@
    [System.IO.File]::WriteAllBytes($FilePath, [System.Text.Encoding]::UTF8.GetBytes($PdfContent))
}

# 4. User A uploads Document 1: Health Policy
$PdfPath1 = [System.IO.Path]::Combine($PWD, "scratch", "live_health_policy.pdf")
New-SamplePdf -FilePath $PdfPath1 -Text "SECTION 1: Inpatient Hospitalization. Comprehensive surgical coverage requires a $500 deductible and 20% coinsurance."

$MetaPath1 = [System.IO.Path]::Combine($PWD, "scratch", "meta1.json")
Set-Content -Path $MetaPath1 -Value "{`"title`":`"Health Insurance Policy 2026`",`"category`":`"INSURANCE`",`"documentType`":`"POLICY`",`"dependentId`":`"$DependentId`"}"

$RawUpload1 = & curl.exe -s -X POST "$BaseUrl/documents/upload" `
    -H $AuthH `
    -F "file=@$PdfPath1;type=application/pdf" `
    -F "metadata=<$MetaPath1;type=application/json"

$UploadRes1 = $RawUpload1 | ConvertFrom-Json
$DocId1 = $UploadRes1.data.id
Assert-Check "6. User A uploaded Health Policy (ID: $DocId1)" ($null -ne $DocId1)

# 5. User A uploads Document 2: Dell Equipment Invoice
$PdfPath2 = [System.IO.Path]::Combine($PWD, "scratch", "live_dell_invoice.pdf")
New-SamplePdf -FilePath $PdfPath2 -Text "SECTION 1: Invoice Details. Invoice INV-2026-8891 for Dell UltraSharp 32-inch 4K Monitor. Serial Number: SN-DELL-998811."

$MetaPath2 = [System.IO.Path]::Combine($PWD, "scratch", "meta2.json")
Set-Content -Path $MetaPath2 -Value '{"title":"Dell Tech Equipment Invoice","category":"FINANCIAL","documentType":"INVOICE"}'

$RawUpload2 = & curl.exe -s -X POST "$BaseUrl/documents/upload" `
    -H $AuthH `
    -F "file=@$PdfPath2;type=application/pdf" `
    -F "metadata=<$MetaPath2;type=application/json"

$UploadRes2 = $RawUpload2 | ConvertFrom-Json
$DocId2 = $UploadRes2.data.id
Assert-Check "7. User A uploaded Dell Invoice (ID: $DocId2)" ($null -ne $DocId2)

Start-Sleep -Seconds 1

# 6. Test 1: HYBRID Retrieval for surgical coverage deductible
$ReqHybrid = @{
    query = "surgical coverage deductible"
    mode = "HYBRID"
    topK = 5
} | ConvertTo-Json

$ResHybrid = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqHybrid
Assert-Check "8. HYBRID retrieval succeeded" ($ResHybrid.success -eq $true)
Assert-Check "9. hasRelevantContext is true" ($ResHybrid.data.hasRelevantContext -eq $true)
Assert-Check "10. Results contain Health Policy" ($ResHybrid.data.results[0].documentTitle -eq "Health Insurance Policy 2026")
Assert-Check "11. Content contains breadcrumb and text" ($ResHybrid.data.results[0].content -match "Health Insurance Policy 2026")
Assert-Check "12. Provenance citation generated" ($ResHybrid.data.citations[0].sourceCitation -match "Health Insurance Policy 2026")
Assert-Check "13. Observability telemetry present" ($ResHybrid.data.metadata.executionTimeMs -ge 0)

# 7. Test 2: Exact Identifier Retrieval (INV-2026-8891)
$ReqId = @{
    query = "INV-2026-8891"
    mode = "HYBRID"
    topK = 5
} | ConvertTo-Json

$ResId = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqId
Assert-Check "14. Exact identifier search returned Dell Invoice" ($ResId.data.results[0].documentTitle -eq "Dell Tech Equipment Invoice")

# 8. Test 3: LEXICAL Mode Retrieval
$ReqLex = @{
    query = "SN-DELL-998811"
    mode = "LEXICAL"
    topK = 5
} | ConvertTo-Json

$ResLex = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqLex
Assert-Check "15. LEXICAL mode succeeded" ($ResLex.data.hasRelevantContext -eq $true)
Assert-Check "16. Match source is LEXICAL_ONLY" ($ResLex.data.results[0].matchSource -eq "LEXICAL_ONLY")

# 9. Test 4: SEMANTIC Mode Retrieval
$ReqSem = @{
    query = "surgical coverage"
    mode = "SEMANTIC"
    topK = 5
    minRelevanceScore = -1.0
} | ConvertTo-Json

$ResSem = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqSem
Assert-Check "17. SEMANTIC mode succeeded" ($ResSem.data.hasRelevantContext -eq $true)
Assert-Check "18. Match source is SEMANTIC_ONLY" ($ResSem.data.results[0].matchSource -eq "SEMANTIC_ONLY")

# 10. Test 5: Metadata Filtering (Category & Dependent)
$ReqCatMatch = @{
    query = "coverage"
    filters = @{ category = "INSURANCE" }
} | ConvertTo-Json
$ResCatMatch = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqCatMatch
Assert-Check "19. Filter category=INSURANCE returned Health Policy" ($ResCatMatch.data.results.Count -ge 1)

$ReqCatMismatch = @{
    query = "coverage"
    filters = @{ category = "TRAVEL" }
} | ConvertTo-Json
$ResCatMismatch = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqCatMismatch
Assert-Check "20. Filter category=TRAVEL returned 0 results" ($ResCatMismatch.data.hasRelevantContext -eq $false)

# 11. Test 6: Multi-Tenant Data Isolation
$ReqUserB = @{
    query = "INV-2026-8891 surgical deductible"
    mode = "HYBRID"
} | ConvertTo-Json
$ResUserB = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersB -ContentType "application/json" -Body $ReqUserB
Assert-Check "21. User B query returned 0 results (Strict multi-tenant isolation)" ($ResUserB.data.hasRelevantContext -eq $false -and $ResUserB.data.results.Count -eq 0)

# 12. Test 7: Multi-Tenant Dependent Isolation
$ReqDepCross = @{
    query = "health policy"
    filters = @{ dependentId = $DependentId }
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersB -ContentType "application/json" -Body $ReqDepCross
    Assert-Check "22. User B accessing User A dependent returns 404" $false "Expected 404 but got success"
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    Assert-Check "22. User B accessing User A dependent returned 404 Not Found" ($StatusCode -eq 404)
}

# 13. Test 8: Document Versioning & Inactive Chunk Exclusion
$PdfPathV2 = [System.IO.Path]::Combine($PWD, "scratch", "live_health_policy_v2.pdf")
New-SamplePdf -FilePath $PdfPathV2 -Text "SECTION 1: Inpatient Hospitalization Version 2. Surgical deductible reduced to $250."

$RawUploadV2 = & curl.exe -s -X POST "$BaseUrl/documents/$DocId1/versions" `
    -H $AuthH `
    -F "file=@$PdfPathV2;type=application/pdf"

$UploadResV2 = $RawUploadV2 | ConvertFrom-Json
Assert-Check "23. Uploaded version 2 for Health Policy" ($UploadResV2.data.version -eq 2)

Start-Sleep -Seconds 1

$ReqV2 = @{
    query = "deductible"
    mode = "HYBRID"
} | ConvertTo-Json
$ResV2 = Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqV2
Assert-Check "24. Only active version 2 chunk retrieved (v1 excluded)" ($ResV2.data.results[0].documentVersion -eq 2 -and $ResV2.data.results.Count -eq 1)

# 14. Test 9: Input Validation (Blank query returns 400)
try {
    $ReqBlank = @{ query = "   " } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/search/retrieve" -Method Post -Headers $HeadersA -ContentType "application/json" -Body $ReqBlank
    Assert-Check "25. Blank query returns 400" $false "Expected 400"
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    Assert-Check "25. Blank query returned RFC 7807 400 Bad Request" ($StatusCode -eq 400)
}

# Clean up temp files
Remove-Item -Path $PdfPath1, $PdfPath2, $PdfPathV2, $MetaPath1, $MetaPath2 -ErrorAction SilentlyContinue

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  LIVE VERIFICATION COMPLETE: $Passed PASSED, $Failed FAILED" -ForegroundColor $(if ($Failed -eq 0) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan

if ($Failed -gt 0) {
    exit 1
}
