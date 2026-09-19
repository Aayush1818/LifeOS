# ==============================================================================
# LifeOS Phase 11 - Document Intelligence & RAG Ingestion Live Verification
# ==============================================================================

$BaseUrl = "http://localhost:8080/api/v1"
$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  LIFEOS PHASE 11 LIVE HTTP VERIFICATION RUNNER" -ForegroundColor Cyan
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
$UserAEmail = "rag.alpha." + [guid]::NewGuid().ToString().Substring(0,8) + "@example.com"
$UserBEmail = "rag.beta." + [guid]::NewGuid().ToString().Substring(0,8) + "@example.com"
$Password = "TestPass123!@#"

$RegA = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType "application/json" -Body (@{
    email = $UserAEmail
    password = $Password
    firstName = "RAG"
    lastName = "Alpha"
} | ConvertTo-Json)
$TokenA = $RegA.data.accessToken
$UserAId = $RegA.data.user.id
Assert-Check "3. Registered User A successfully" ($null -ne $TokenA)

$RegB = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType "application/json" -Body (@{
    email = $UserBEmail
    password = $Password
    firstName = "RAG"
    lastName = "Beta"
} | ConvertTo-Json)
$TokenB = $RegB.data.accessToken
Assert-Check "4. Registered User B successfully" ($null -ne $TokenB)

$HeadersA = @{ "Authorization" = "Bearer $TokenA" }
$HeadersB = @{ "Authorization" = "Bearer $TokenB" }

# 3. Create Sample PDF for Ingestion
$PdfBytes = [System.Text.Encoding]::UTF8.GetBytes(
"%PDF-1.4
1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj
4 0 obj << /Length 125 >> stream
BT /F1 12 Tf 100 700 Td (SECTION 1: Premium Health Coverage. This policy covers all medical surgeries and prescription medications.) Tj ET
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
385
%%EOF
")

$TempPdfPath = "$PSScriptRoot\temp_policy_doc.pdf"
[System.IO.File]::WriteAllBytes($TempPdfPath, $PdfBytes)

# 4. Upload Document via curl
$TempMetaPath = "$PSScriptRoot\temp_meta.json"
Set-Content -Path $TempMetaPath -Value '{"title":"Premium Health Optima Policy","category":"INSURANCE","documentType":"POLICY"}'

$AuthH = "Authorization: Bearer $TokenA"
$RawUpload = & curl.exe -s -X POST "$BaseUrl/documents/upload" `
    -H $AuthH `
    -F "file=@$TempPdfPath;type=application/pdf" `
    -F "metadata=<$TempMetaPath;type=application/json"

Write-Host "DEBUG RawUpload response:" $RawUpload
$UploadJson = $RawUpload | ConvertFrom-Json

Assert-Check "5. Document uploaded successfully" ($UploadJson.success -eq $true)
$DocId = $UploadJson.data.id
Assert-Check "6. Document ID generated" ($null -ne $DocId)
Assert-Check "7. Document ingestionStatus is PROCESSED" ($UploadJson.data.ingestionStatus -eq "PROCESSED")
Assert-Check "8. Document chunkCount >= 1" ($UploadJson.data.chunkCount -ge 1)

# 5. Check Ingestion Status Endpoint
$StatusRes = Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId/ingestion-status" -Method Get -Headers $HeadersA
Assert-Check "9. Ingestion status endpoint returned PROCESSED" ($StatusRes.data.ingestionStatus -eq "PROCESSED")
Assert-Check "10. Ingestion status returned chunk count >= 1" ($StatusRes.data.chunkCount -ge 1)
Assert-Check "11. Ingestion status returned embedding model" ($null -ne $StatusRes.data.embeddingModel)
Assert-Check "12. Ingestion status returned ingestedAt timestamp" ($null -ne $StatusRes.data.ingestedAt)

# 6. Check Document Chunks Endpoint
$ChunksRes = Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId/chunks?page=0&size=10" -Method Get -Headers $HeadersA
Assert-Check "13. Chunks endpoint returned content" ($ChunksRes.data.content.Count -ge 1)
$FirstChunk = $ChunksRes.data.content[0]
Assert-Check "14. First chunk chunkIndex is 0" ($FirstChunk.chunkIndex -eq 0)
Assert-Check "15. First chunk pageNumber is 1" ($FirstChunk.pageNumber -eq 1)
Assert-Check "16. First chunk content has breadcrumb" ($FirstChunk.content.Contains("[Document:"))
Assert-Check "17. First chunk isActive is true" ($FirstChunk.isActive -eq $true)

# 7. Check Reprocess Endpoint
$ReprocessRes = Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId/reprocess" -Method Post -Headers $HeadersA -ContentType "application/json" -Body (@{ force = $true } | ConvertTo-Json)
Assert-Check "18. Reprocess endpoint accepted (HTTP 202)" ($null -ne $ReprocessRes.data.id)

# 8. Upload New Version (v2)
$RawVersion = & curl.exe -s -X POST "$BaseUrl/documents/$DocId/versions" `
    -H $AuthH `
    -F "file=@$TempPdfPath;type=application/pdf"

$VersionJson = $RawVersion | ConvertFrom-Json

Assert-Check "19. Upload new version returned success" ($VersionJson.success -eq $true)
Assert-Check "20. Document version incremented to 2" ($VersionJson.data.version -eq 2)
Assert-Check "21. Version 2 has chunkCount >= 1" ($VersionJson.data.chunkCount -ge 1)

# 9. Multi-Tenant Isolation Checks
try {
    Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId/ingestion-status" -Method Get -Headers $HeadersB
    Assert-Check "22. Cross-tenant ingestion status rejected with 404" $false
} catch {
    Assert-Check "22. Cross-tenant ingestion status rejected with 404" ($_.Exception.Response.StatusCode.value__ -eq 404)
}

try {
    Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId/chunks" -Method Get -Headers $HeadersB
    Assert-Check "23. Cross-tenant chunks list rejected with 404" $false
} catch {
    Assert-Check "23. Cross-tenant chunks list rejected with 404" ($_.Exception.Response.StatusCode.value__ -eq 404)
}

try {
    Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId/reprocess" -Method Post -Headers $HeadersB -ContentType "application/json" -Body (@{ force = $true } | ConvertTo-Json)
    Assert-Check "24. Cross-tenant reprocess rejected with 404" $false
} catch {
    Assert-Check "24. Cross-tenant reprocess rejected with 404" ($_.Exception.Response.StatusCode.value__ -eq 404)
}

# 10. Delete Document & Evict Chunks
$DeleteRes = Invoke-RestMethod -Uri "$BaseUrl/documents/$DocId" -Method Delete -Headers $HeadersA
Assert-Check "25. Document deleted successfully" ($DeleteRes.success -eq $true)

# Cleanup temp file
if (Test-Path $TempPdfPath) {
    Remove-Item $TempPdfPath -Force
}
if (Test-Path $TempMetaPath) {
    Remove-Item $TempMetaPath -Force
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  VERIFICATION COMPLETE: $Passed Passed, $Failed Failed" -ForegroundColor $(if ($Failed -eq 0) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
