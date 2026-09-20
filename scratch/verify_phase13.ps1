# ==============================================================================
# LifeOS Phase 13 - Grounded AI Assistant & LLM Integration Live Verification
# ==============================================================================

$BaseUrl = "http://localhost:8080/api/v1"
$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  LIFEOS PHASE 13 GROUNDED AI ASSISTANT LIVE VERIFICATION" -ForegroundColor Cyan
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
$UserAEmail = "assistant.liveA." + [guid]::NewGuid().ToString().Substring(0,8) + "@example.com"
$UserBEmail = "assistant.liveB." + [guid]::NewGuid().ToString().Substring(0,8) + "@example.com"
$Password = "TestPass123!@#"

$RegA = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType "application/json" -Body (@{
    email = $UserAEmail
    password = $Password
    firstName = "AssistantA"
    lastName = "Alpha"
} | ConvertTo-Json)
$TokenA = $RegA.data.accessToken
$UserAId = $RegA.data.user.id
Assert-Check "3. Registered User A successfully" ($null -ne $TokenA)

$RegB = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType "application/json" -Body (@{
    email = $UserBEmail
    password = $Password
    firstName = "AssistantB"
    lastName = "Beta"
} | ConvertTo-Json)
$TokenB = $RegB.data.accessToken
$UserBId = $RegB.data.user.id
Assert-Check "4. Registered User B successfully" ($null -ne $TokenB)

$HeadersA = @{ "Authorization" = "Bearer $TokenA"; "Content-Type" = "application/json" }
$HeadersB = @{ "Authorization" = "Bearer $TokenB"; "Content-Type" = "application/json" }

# 3. User A creates a conversation
$ConvCreateBody = @{
    title = "Phase 13 Grounded Live Session"
} | ConvertTo-Json

$ConvA = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations" -Method Post -Headers $HeadersA -Body $ConvCreateBody
$ConvAId = $ConvA.data.id
Assert-Check "5. User A created conversation successfully" ($null -ne $ConvAId -and $ConvA.data.title -eq "Phase 13 Grounded Live Session")

# 4. User A lists conversations
$ConvListA = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations" -Method Get -Headers $HeadersA
Assert-Check "6. User A retrieved conversation list" ($ConvListA.data.content.Count -ge 1)

# 5. User A gets conversation details
$ConvDetailsA = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId" -Method Get -Headers $HeadersA
Assert-Check "7. User A retrieved conversation details" ($ConvDetailsA.data.id -eq $ConvAId -and $ConvDetailsA.data.messages.Count -eq 0)

# 6. Multi-tenant Isolation: User B tries to GET User A's conversation -> Expect 404
$UserBGetBlocked = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId" -Method Get -Headers $HeadersB
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        $UserBGetBlocked = $true
    }
}
Assert-Check "8. User B blocked from accessing User A conversation (404 Not Found)" $UserBGetBlocked

# 7. Multi-tenant Isolation: User B tries to POST message to User A's conversation -> Expect 404
$UserBPostBlocked = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId/messages" -Method Post -Headers $HeadersB -Body (@{
        content = "Malicious injection attempt"
    } | ConvertTo-Json)
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        $UserBPostBlocked = $true
    }
}
Assert-Check "9. User B blocked from posting to User A conversation (404 Not Found)" $UserBPostBlocked

# 8. Multi-tenant Isolation: User B tries to DELETE User A's conversation -> Expect 404
$UserBDeleteBlocked = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId" -Method Delete -Headers $HeadersB
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        $UserBDeleteBlocked = $true
    }
}
Assert-Check "10. User B blocked from deleting User A conversation (404 Not Found)" $UserBDeleteBlocked

# 9. Ingest a test document for User A via multipart upload
$TxtPath1 = [System.IO.Path]::Combine($PWD, "scratch", "live_assistant_policy.txt")
Set-Content -Path $TxtPath1 -Value "SECTION 1: HEALTH INSURANCE COVERAGE Comprehensive annual policy deductible is $1500 per family member. Surgical coverage requires 20% coinsurance."

$MetaPath1 = [System.IO.Path]::Combine($PWD, "scratch", "meta_assistant.json")
Set-Content -Path $MetaPath1 -Value '{"title":"Live Health Insurance Policy 2026","category":"INSURANCE","documentType":"POLICY"}'

$RawUpload1 = & curl.exe -s -X POST "$BaseUrl/documents/upload" `
    -H "Authorization: Bearer $TokenA" `
    -F "file=@$TxtPath1;type=text/plain" `
    -F "metadata=<$MetaPath1;type=application/json"

$UploadRes1 = $RawUpload1 | ConvertFrom-Json
$DocIdA = $UploadRes1.data.id
Assert-Check "11. Uploaded document for User A successfully" ($null -ne $DocIdA)

# Wait for async background ingestion (chunking + mock embedding)
Start-Sleep -Seconds 2

# 10. User A asks a grounded question
$AskBody = @{
    content = "What is the deductible on my health insurance policy?"
    retrievalMode = "HYBRID"
    topK = 5
} | ConvertTo-Json

$MsgRespA = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId/messages" -Method Post -Headers $HeadersA -Body $AskBody
Assert-Check "12. Assistant processed message successfully" ($MsgRespA.success -eq $true)
Assert-Check "13. User message echoed in response" ($MsgRespA.data.userMessage.content -eq "What is the deductible on my health insurance policy?")
Assert-Check "14. Assistant message generated" ($null -ne $MsgRespA.data.assistantMessage.content -and $MsgRespA.data.assistantMessage.role -eq "ASSISTANT")
Assert-Check "15. Model metadata populated" ($MsgRespA.data.modelMetadata.provider -eq "MOCK")

# 11. Multi-tenant Data Isolation: User B asks question in User B's own conversation
$ConvB = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations" -Method Post -Headers $HeadersB -Body (@{
    title = "User B Independent Chat"
} | ConvertTo-Json)
$ConvBId = $ConvB.data.id

$MsgRespB = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvBId/messages" -Method Post -Headers $HeadersB -Body (@{
    content = "What is my health insurance deductible?"
} | ConvertTo-Json)

Assert-Check "16. User B hasRelevantContext is FALSE (no leakage from User A)" ($MsgRespB.data.hasRelevantContext -eq $false)
Assert-Check "17. User B receives 0 citations (zero data leakage)" ($MsgRespB.data.citations.Count -eq 0)

# 12. User A asks an unanswerable question
$UnanswerableResp = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId/messages" -Method Post -Headers $HeadersA -Body (@{
    content = "What time is my flight to Mars tomorrow?"
} | ConvertTo-Json)

$UnansContent = $UnanswerableResp.data.assistantMessage.content
$HasInsufficientNotice = ($null -ne $UnansContent) -and ($UnansContent.Contains("sufficient information"))
Assert-Check "18. Unanswerable question hasRelevantContext is FALSE" ($UnanswerableResp.data.hasRelevantContext -eq $false)
Assert-Check "19. Assistant responds with polite insufficient information message" $HasInsufficientNotice "Actual: [$UnansContent]"

# 13. Medical Safety Guardrail Check
$MedicalResp = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId/messages" -Method Post -Headers $HeadersA -Body (@{
    content = "I have severe sudden chest pain, what medicine should I take?"
} | ConvertTo-Json)

Assert-Check "20. Medical query returns safety disclaimer" ($MedicalResp.data.assistantMessage.content -like "*not a doctor or healthcare professional*")

# 14. Conversation History continuity check
$UpdatedDetailsA = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId" -Method Get -Headers $HeadersA
Assert-Check "21. Conversation history contains all user and assistant turns" ($UpdatedDetailsA.data.messages.Count -ge 6)

# 15. Validation on blank input -> Expect 400 Bad Request
$BlankInputBlocked = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId/messages" -Method Post -Headers $HeadersA -Body (@{
        content = "   "
    } | ConvertTo-Json)
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        $BlankInputBlocked = $true
    }
}
Assert-Check "22. Blank message rejected with HTTP 400 Bad Request" $BlankInputBlocked

# 16. Delete conversation
$DelResp = Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId" -Method Delete -Headers $HeadersA
Assert-Check "23. User A deleted conversation successfully" ($DelResp.success -eq $true)

# 17. Verify conversation is no longer accessible
$DeletedGetBlocked = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvAId" -Method Get -Headers $HeadersA
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        $DeletedGetBlocked = $true
    }
}
Assert-Check "24. Deleted conversation returns 404 Not Found" $DeletedGetBlocked

# 18. Cleanup User B conversation
Invoke-RestMethod -Uri "$BaseUrl/assistant/conversations/$ConvBId" -Method Delete -Headers $HeadersB | Out-Null
Assert-Check "25. Cleanup completed successfully" $true

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  RESULTS: $Passed PASSED, $Failed FAILED" -ForegroundColor $(if ($Failed -eq 0) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan

if ($Failed -gt 0) {
    exit 1
}
