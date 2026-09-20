# LifeOS Phase 14 Verification Script
# Safe Agentic AI & Tool Calling Integration

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080"
$Timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " LIFEOS PHASE 14: AGENT TOOLS & HITL ACTION VERIFICATION " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Health Check
Write-Host "`n[1/12] Checking Application Health..." -ForegroundColor Yellow
$healthResp = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
if ($healthResp.status -ne "UP") {
    Write-Error "Health check failed: $($healthResp.status)"
}
Write-Host "[OK] Backend is healthy: $($healthResp.status)" -ForegroundColor Green

# 2. Register Primary User
Write-Host "`n[2/12] Registering Test User 1..." -ForegroundColor Yellow
$user1Email = "agent_verifier_${Timestamp}@lifeos.io"
$regBody = @{
    email = $user1Email
    password = "Password@123"
    firstName = "Agent"
    lastName = "Tester"
} | ConvertTo-Json

$authResp1 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json"
$token1 = $authResp1.data.accessToken
$user1Id = $authResp1.data.user.id
$headers1 = @{
    "Authorization" = "Bearer $token1"
    "Content-Type" = "application/json"
}
Write-Host "[OK] User 1 registered: $user1Id ($user1Email)" -ForegroundColor Green

# 3. Check Pending Actions (initially empty)
Write-Host "`n[3/12] Listing Pending Actions (expecting empty)..." -ForegroundColor Yellow
$pendingResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/actions/pending" -Method Get -Headers $headers1
if ($pendingResp.data.Count -ne 0) {
    Write-Error "Expected 0 pending actions initially, got $($pendingResp.data.Count)"
}
Write-Host "[OK] 0 pending actions for fresh user" -ForegroundColor Green

# 4. Create Conversation
Write-Host "`n[4/12] Creating Agent Conversation..." -ForegroundColor Yellow
$convBody = @{
    title = "Phase 14 Agent Verification"
} | ConvertTo-Json
$convResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/conversations" -Method Post -Headers $headers1 -Body $convBody
$convId = $convResp.data.id
Write-Host "[OK] Conversation created: $convId" -ForegroundColor Green

# 5. Direct Conversational Turn (no tool calls)
Write-Host "`n[5/12] Sending pure conversational prompt..." -ForegroundColor Yellow
$msgBody1 = @{
    content = "Hello, what can you do as my LifeOS assistant?"
} | ConvertTo-Json
$msgResp1 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/conversations/$convId/messages" -Method Post -Headers $headers1 -Body $msgBody1
Write-Host "[OK] Assistant replied: $($msgResp1.data.assistantMessage.content.Substring(0, [Math]::Min(80, $msgResp1.data.assistantMessage.content.Length)))..." -ForegroundColor Green
if ($msgResp1.data.pendingAction -ne $null) {
    Write-Error "Expected pendingAction to be null for conversational query"
}

# 6. Read-Only Tool Invocation (Loan Summary)
Write-Host "`n[6/12] Sending prompt triggering read-only tool (loan summary)..." -ForegroundColor Yellow
$msgBody2 = @{
    content = "Can you give me a summary of my active loans?"
} | ConvertTo-Json
$msgResp2 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/conversations/$convId/messages" -Method Post -Headers $headers1 -Body $msgBody2
Write-Host "[OK] Assistant executed tool and responded: $($msgResp2.data.assistantMessage.content.Substring(0, [Math]::Min(100, $msgResp2.data.assistantMessage.content.Length)))..." -ForegroundColor Green
if ($msgResp2.data.toolCalls.Count -gt 0) {
    Write-Host "[OK] Tool call recorded: $($msgResp2.data.toolCalls[0].name)" -ForegroundColor Green
}

# 7. Mutating Tool Invocation (Reminder creation - HITL Interception)
Write-Host "`n[7/12] Sending mutating prompt: Create a reminder (HITL Interception)..." -ForegroundColor Yellow
$msgBody3 = @{
    content = "Create a reminder for Dentist Appointment due at 2026-10-15T14:00:00Z"
} | ConvertTo-Json
$msgResp3 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/conversations/$convId/messages" -Method Post -Headers $headers1 -Body $msgBody3

if ($null -eq $msgResp3.data.pendingAction) {
    Write-Error "Expected pendingAction to be non-null for state-mutating reminder request!"
}
$actionId = $msgResp3.data.pendingAction.id
Write-Host "[OK] HITL Interception SUCCESS! PendingAction generated: $actionId" -ForegroundColor Green
Write-Host "  Tool Name: $($msgResp3.data.pendingAction.toolName)" -ForegroundColor Cyan
Write-Host "  Prompt: $($msgResp3.data.pendingAction.prompt)" -ForegroundColor Cyan
Write-Host "  Status: $($msgResp3.data.pendingAction.status)" -ForegroundColor Cyan

# 8. Verify Pending Actions endpoint lists the card
Write-Host "`n[8/12] Verifying GET /api/v1/assistant/actions/pending lists the card..." -ForegroundColor Yellow
$pendingResp2 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/actions/pending" -Method Get -Headers $headers1
if ($pendingResp2.data.Count -ne 1 -or $pendingResp2.data[0].id -ne $actionId) {
    Write-Error "Pending actions list mismatch: expected 1 action with id $actionId"
}
Write-Host "[OK] GET /pending returned active action $actionId" -ForegroundColor Green

# 9. Confirm the Pending Action
Write-Host "`n[9/12] Confirming Pending Action via POST /confirm..." -ForegroundColor Yellow
$confirmResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/actions/$actionId/confirm" -Method Post -Headers $headers1
if ($confirmResp.data.status -ne "CONFIRMED" -or $confirmResp.data.success -ne $true) {
    Write-Error "Action confirmation failed: $($confirmResp.data.message)"
}
Write-Host "[OK] Action confirmed: $($confirmResp.data.status), message: $($confirmResp.data.message)" -ForegroundColor Green

# 10. Verify Pending Actions list is now empty
Write-Host "`n[10/12] Verifying pending actions list is now clear..." -ForegroundColor Yellow
$pendingResp3 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/actions/pending" -Method Get -Headers $headers1
if ($pendingResp3.data.Count -ne 0) {
    Write-Error "Expected 0 pending actions after confirmation, found $($pendingResp3.data.Count)"
}
Write-Host "[OK] Pending action successfully cleared from queue" -ForegroundColor Green

# 11. Test Action Rejection Flow
Write-Host "`n[11/12] Testing action rejection flow..." -ForegroundColor Yellow
$msgBody4 = @{
    content = "Create a reminder for Unwanted Task due at 2026-11-01T10:00:00Z"
} | ConvertTo-Json
$msgResp4 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/conversations/$convId/messages" -Method Post -Headers $headers1 -Body $msgBody4
$actionId2 = $msgResp4.data.pendingAction.id

$rejectResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/actions/$actionId2/reject" -Method Post -Headers $headers1
if ($rejectResp.data.status -ne "REJECTED") {
    Write-Error "Expected status REJECTED, got $($rejectResp.data.status)"
}
Write-Host "[OK] Action rejected cleanly: $($rejectResp.data.status)" -ForegroundColor Green

# 12. Cross-Tenant Isolation
Write-Host "`n[12/12] Testing Cross-Tenant Security Protection..." -ForegroundColor Yellow
$user2Email = "agent_alien_${Timestamp}@lifeos.io"
$regBody2 = @{
    email = $user2Email
    password = "Password@123"
    firstName = "Alien"
    lastName = "User"
} | ConvertTo-Json
$authResp2 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/register" -Method Post -Body $regBody2 -ContentType "application/json"
$headers2 = @{
    "Authorization" = "Bearer $($authResp2.data.accessToken)"
    "Content-Type" = "application/json"
}

try {
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/assistant/actions/$actionId/confirm" -Method Post -Headers $headers2
    Write-Error "SECURITY BREACH: User 2 was able to confirm User 1 action!"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[OK] Cross-tenant confirmation blocked with HTTP 404 (multi-tenant isolation confirmed)" -ForegroundColor Green
    } else {
        Write-Error "Expected 404 for cross-tenant action, got $statusCode"
    }
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " ALL 12 LIVE VERIFICATION CHECKS PASSED SUCCESSFULLY!    " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
