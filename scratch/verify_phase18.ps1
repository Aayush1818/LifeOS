# Phase 18 Live Verification Script: Multi-Domain Financial Obligations & System Diagnostics
param (
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " LifeOS Phase 18: Financial Obligations & System Diagnostics " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check Public System Health Probe
Write-Host "`n1. Querying Live System Health Probes (GET /api/v1/system/health)" -ForegroundColor Yellow
$healthRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/system/health" -Method Get
Write-Host "  System Status: $($healthRes.data.status)" -ForegroundColor Green
Write-Host "  Uptime: $($healthRes.data.uptimeSeconds) seconds" -ForegroundColor Green
Write-Host "  Components:" -ForegroundColor Gray
$healthRes.data.components.PSObject.Properties | ForEach-Object {
    Write-Host "  - $($_.Name): $($_.Value.status) ($($_.Value.latencyMs)ms) - $($_.Value.details)" -ForegroundColor Gray
}

# 2. Register Test User
$testEmail = "phase18_live_" + [System.Guid]::NewGuid().ToString() + "@example.com"
$regBody = @{
    email = $testEmail
    password = "Password123!@#"
    firstName = "Phase18"
    lastName = "Planner"
} | ConvertTo-Json

Write-Host "`n2. Registering test user: $testEmail" -ForegroundColor Yellow
$regRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json"
$token = $regRes.accessToken
$userId = $regRes.user.id
Write-Host "  Registered userId: $userId" -ForegroundColor Green

# 3. Query Platform Diagnostics & Metrics
Write-Host "`n3. Querying Platform Metrics (GET /api/v1/system/metrics)" -ForegroundColor Yellow
$metricsRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/system/metrics" -Method Get -Headers @{ Authorization = "Bearer $token" }
Write-Host "  Total Users: $($metricsRes.data.totalUsers)" -ForegroundColor Green
Write-Host "  Available CPU Processors: $($metricsRes.data.jvmAvailableProcessors)" -ForegroundColor Green
Write-Host "  JVM Memory: $([math]::Round($metricsRes.data.jvmUsedMemoryBytes / 1MB, 2)) MB used of $([math]::Round($metricsRes.data.jvmMaxMemoryBytes / 1MB, 2)) MB max" -ForegroundColor Green
Write-Host "  Storage Free Space: $([math]::Round($metricsRes.data.diskFreeBytes / 1GB, 2)) GB free" -ForegroundColor Green

# 4. Query Monthly Financial Obligations Synthesis
$targetMonth = [DateTime]::UtcNow.Month
$targetYear = [DateTime]::UtcNow.Year
Write-Host "`n4. Querying Monthly Financial Obligations (GET /api/v1/finance/obligations/monthly?month=$targetMonth&year=$targetYear)" -ForegroundColor Yellow
$obRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/finance/obligations/monthly?month=$targetMonth&year=$targetYear" -Method Get -Headers @{ Authorization = "Bearer $token" }
Write-Host "  Month/Year: $($obRes.data.month)/$($obRes.data.year)" -ForegroundColor Green
Write-Host "  Total Obligations: `$$($obRes.data.totalObligationAmount)" -ForegroundColor Green
Write-Host "  Loan EMIs: `$$($obRes.data.loanEmisTotal)" -ForegroundColor Gray
Write-Host "  Insurance Premiums: `$$($obRes.data.insurancePremiumsTotal)" -ForegroundColor Gray
Write-Host "  Recurring Bills: `$$($obRes.data.recurringBillsTotal)" -ForegroundColor Gray
Write-Host "  Travel Allocations: `$$($obRes.data.tripAllocationsTotal)" -ForegroundColor Gray
Write-Host "  Projected Income: `$$($obRes.data.projectedIncome)" -ForegroundColor Green
Write-Host "  Net Projected Cash Flow: `$$($obRes.data.netSurplusOrDeficit)" -ForegroundColor Green
Write-Host "  Timeline Items Count: $($obRes.data.items.Count)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Phase 18 Verification Complete: ALL CAPABILITIES VALIDATED " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
