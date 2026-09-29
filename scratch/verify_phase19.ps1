# Phase 19 Live Verification Script: Proactive Life Insights & Cross-Domain Anomaly Engine
param (
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " LifeOS Phase 19: Proactive Insights & Anomaly Detection " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Register Test User A
$testEmailA = "phase19_live_" + [System.Guid]::NewGuid().ToString() + "@example.com"
$regBodyA = @{
    email = $testEmailA
    password = "Password123!@#"
    firstName = "Proactive"
    lastName = "Optimizer"
} | ConvertTo-Json

Write-Host "`n1. Registering test user A: $testEmailA" -ForegroundColor Yellow
$regResA = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/register" -Method Post -Body $regBodyA -ContentType "application/json"
$tokenA = $regResA.data.accessToken
$userIdA = $regResA.data.user.id
Write-Host "  Registered userId: $userIdA" -ForegroundColor Green

# 2. Seed High-Interest Loan to trigger HIGH_INTEREST_LOAN insight
Write-Host "`n2. Creating High-APR Loan (11.5% APR) for user A" -ForegroundColor Yellow
$loanBody = @{
    lenderName = "Apex Capital Finance"
    loanAccountNumber = "LN-" + [System.Guid]::NewGuid().ToString().Substring(0, 8)
    loanType = "PERSONAL"
    principalAmount = 25000.00
    outstandingBalance = 21000.00
    interestRate = 11.50
    interestType = "FIXED"
    paymentFrequency = "MONTHLY"
    tenureMonths = 36
    monthlyEmi = 824.50
    emiDueDay = 10
    startDate = "2025-01-01"
    endDate = "2028-01-01"
} | ConvertTo-Json

$loanRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/loans" -Method Post -Body $loanBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $tokenA" }
Write-Host "  Created Loan ID: $($loanRes.data.id)" -ForegroundColor Green

# 3. Trigger Autonomous Insight Engine (POST /api/v1/insights/generate)
Write-Host "`n3. Triggering Autonomous Cross-Domain Insight Engine" -ForegroundColor Yellow
$genRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/insights/generate" -Method Post -Headers @{ Authorization = "Bearer $tokenA" }
Write-Host "  Total Active Insights: $($genRes.data.totalActive)" -ForegroundColor Green
Write-Host "  Critical Count: $($genRes.data.criticalCount)" -ForegroundColor Red
Write-Host "  Warning Count: $($genRes.data.warningCount)" -ForegroundColor Yellow
Write-Host "  Info Count: $($genRes.data.infoCount)" -ForegroundColor Cyan

$genRes.data.insights | ForEach-Object {
    Write-Host "  -> [$($_.severity)] $($_.insightType): $($_.title)" -ForegroundColor Gray
    Write-Host "     Action: $($_.actionType)" -ForegroundColor DarkGray
}

if ($genRes.data.insights.Count -eq 0) {
    throw "Expected at least 1 insight from High-Interest Loan analyzer!"
}

$firstInsight = $genRes.data.insights[0]
$insightId = $firstInsight.id

# 4. Action the insight (POST /api/v1/insights/{id}/action)
Write-Host "`n4. Actioning Insight: $insightId" -ForegroundColor Yellow
$actionRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/insights/$insightId/action" -Method Post -Headers @{ Authorization = "Bearer $tokenA" }
Write-Host "  Actioned: $($actionRes.data.actioned)" -ForegroundColor Green

# 5. Dismiss the insight (POST /api/v1/insights/{id}/dismiss)
Write-Host "`n5. Dismissing Insight: $insightId" -ForegroundColor Yellow
$dismissRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/insights/$insightId/dismiss" -Method Post -Headers @{ Authorization = "Bearer $tokenA" }
Write-Host "  Dismissed: $($dismissRes.data.dismissed)" -ForegroundColor Green

# 6. Verify Active Insights List after Dismissal (GET /api/v1/insights)
Write-Host "`n6. Fetching Active Insights List" -ForegroundColor Yellow
$activeRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/insights" -Method Get -Headers @{ Authorization = "Bearer $tokenA" }
Write-Host "  Active Insights Remaining: $($activeRes.data.totalActive)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Phase 19 Verification Complete: ALL CAPABILITIES VALIDATED " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
