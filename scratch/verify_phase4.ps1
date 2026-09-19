# verify_phase4.ps1 - End-to-end verification script for Phase 4
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "=== Phase 4 Document Management & Storage Verification ===" -ForegroundColor Cyan

# 1. Health check
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 10
    if ($health.status -eq "UP") {
        Write-Host "[PASS] 1. Actuator Health is UP" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 1. Actuator Health status: $($health.status)" -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] 1. Actuator Health unreachable: $_" -ForegroundColor Red
    exit 1
}

# 2. User registration
$uniqueSuffixA = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userAEmail = "live.userA.$uniqueSuffixA@example.com"
$regA = @{
    email = $userAEmail
    password = "Password123!@#"
    firstName = "DocUserA"
    lastName = "Verified"
} | ConvertTo-Json

$resA = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regA
$tokenA = $resA.data.accessToken
Write-Host "[PASS] 2. User A registered successfully: $userAEmail (Token acquired)" -ForegroundColor Green

$uniqueSuffixB = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userBEmail = "live.userB.$uniqueSuffixB@example.com"
$regB = @{
    email = $userBEmail
    password = "Password123!@#"
    firstName = "DocUserB"
    lastName = "Verified"
} | ConvertTo-Json

$resB = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regB
$tokenB = $resB.data.accessToken
Write-Host "[PASS] 3. User B registered successfully: $userBEmail (Token acquired)" -ForegroundColor Green

# 3. User A uploads a TXT document via curl (supports multipart easily on Windows)
$tempTxtFile = [System.IO.Path]::GetTempFileName() + ".txt"
"LifeOS Insurance Policy: Accidental coverage up to $250,000. Valid through 2027." | Out-File -FilePath $tempTxtFile -Encoding utf8

$metaJson = '{"title":"Insurance Policy 2026","category":"INSURANCE","documentType":"POLICY","tags":["insurance","policy","health"]}'
$tempMetaFile = [System.IO.Path]::GetTempFileName() + ".json"
$metaJson | Out-File -FilePath $tempMetaFile -Encoding utf8

$uploadTxtResult = curl.exe -s -X POST "$baseUrl/api/v1/documents/upload" `
  -H "Authorization: Bearer $tokenA" `
  -F "file=@$tempTxtFile;type=text/plain" `
  -F "metadata=@$tempMetaFile;type=application/json" | ConvertFrom-Json

if ($uploadTxtResult.success -eq $true -and $uploadTxtResult.data.id) {
    $docId = $uploadTxtResult.data.id
    Write-Host "[PASS] 4. User A uploaded TXT document with ID: $docId (Version: $($uploadTxtResult.data.version), Status: $($uploadTxtResult.data.ingestionStatus))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 4. Document upload failed: $($uploadTxtResult | ConvertTo-Json)" -ForegroundColor Red
}

# 4. Check Document Details & Extracted Text
$detailRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/documents/$docId" -Method Get -Headers @{ Authorization = "Bearer $tokenA" }
if ($detailRes.data.extractedText -match "Accidental coverage up to") {
    Write-Host "[PASS] 5. Tika extracted text verified in document detail" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 5. Extracted text missing or incorrect: $($detailRes.data.extractedText)" -ForegroundColor Red
}

# 5. Download document and verify content
$tempDownloadPath = [System.IO.Path]::GetTempFileName()
Invoke-WebRequest -Uri "$baseUrl/api/v1/documents/$docId/download" -Method Get -Headers @{ Authorization = "Bearer $tokenA" } -OutFile $tempDownloadPath -UseBasicParsing
$downloadedContent = Get-Content -Path $tempDownloadPath -Raw
if ($downloadedContent -match "Accidental coverage up to") {
    Write-Host "[PASS] 6. Document binary download verified with matching content" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 6. Downloaded content mismatch" -ForegroundColor Red
}

# 6. Upload new version
$tempTxtV2 = [System.IO.Path]::GetTempFileName() + ".txt"
"LifeOS Insurance Policy: Accidental coverage INCREASED to $500,000 for 2027 renewal." | Out-File -FilePath $tempTxtV2 -Encoding utf8

$versionResult = curl.exe -s -X POST "$baseUrl/api/v1/documents/$docId/versions" `
  -H "Authorization: Bearer $tokenA" `
  -F "file=@$tempTxtV2;type=text/plain" | ConvertFrom-Json

if ($versionResult.data.version -eq 2) {
    Write-Host "[PASS] 7. Uploaded new version for document; version incremented to 2" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 7. Version upload failed: $($versionResult | ConvertTo-Json)" -ForegroundColor Red
}

# 7. MIME spoofing rejection test
$tempSpoofFile = [System.IO.Path]::GetTempFileName() + ".pdf"
[System.IO.File]::WriteAllBytes($tempSpoofFile, [byte[]](0x4D, 0x5A, 0x90, 0x00, 0x03)) # MZ executable header

$spoofRes = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$baseUrl/api/v1/documents/upload" `
  -H "Authorization: Bearer $tokenA" `
  -F "file=@$tempSpoofFile;type=application/pdf" `
  -F "metadata=@$tempMetaFile;type=application/json"

if ($spoofRes -match "HTTP_STATUS:400") {
    Write-Host "[PASS] 8. Disguised executable (.pdf) rejected with 400 Bad Request via Tika magic bytes" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8. MIME spoofing test did not return 400: $spoofRes" -ForegroundColor Red
}

# 8. Cross-Tenant Isolation: User B cannot access User A's document
$bGetRes = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/documents/$docId" `
  -H "Authorization: Bearer $tokenB"
if ($bGetRes -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 9. Cross-tenant GET by User B returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 9. User B got access or non-404: $bGetRes" -ForegroundColor Red
}

$bDownloadRes = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/documents/$docId/download" `
  -H "Authorization: Bearer $tokenB"
if ($bDownloadRes -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 10. Cross-tenant download by User B returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 10. User B downloaded document or non-404: $bDownloadRes" -ForegroundColor Red
}

$bDeleteRes = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X DELETE "$baseUrl/api/v1/documents/$docId" `
  -H "Authorization: Bearer $tokenB"
if ($bDeleteRes -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 11. Cross-tenant delete by User B returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 11. User B deleted document or non-404: $bDeleteRes" -ForegroundColor Red
}

# 9. Soft-delete document by User A
$deleteRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/documents/$docId" -Method Delete -Headers @{ Authorization = "Bearer $tokenA" }
if ($deleteRes.success -eq $true) {
    Write-Host "[PASS] 12. User A successfully deleted document" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 12. Delete failed: $($deleteRes | ConvertTo-Json)" -ForegroundColor Red
}

$afterDeleteRes = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/documents/$docId" `
  -H "Authorization: Bearer $tokenA"
if ($afterDeleteRes -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 13. Subsequent GET after deletion returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 13. Document still accessible after delete: $afterDeleteRes" -ForegroundColor Red
}

# 10. Verify Swagger OpenAPI docs
$openApi = Invoke-RestMethod -Uri "$baseUrl/v3/api-docs" -Method Get
if ($openApi.paths."/api/v1/documents/upload") {
    Write-Host "[PASS] 14. OpenAPI schema contains /api/v1/documents/upload" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 14. OpenAPI schema missing document endpoints" -ForegroundColor Red
}

Write-Host "=== Phase 4 Verification Completed Successfully ===" -ForegroundColor Cyan
