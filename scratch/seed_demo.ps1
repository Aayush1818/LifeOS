# Seed Demo User & Sample Data
$ErrorActionPreference = "SilentlyContinue"

$regBody = @{
    email = "demo@lifeos.com"
    password = "Password123!@#"
    firstName = "Alex"
    lastName = "Morgan"
} | ConvertTo-Json

$loginBody = @{
    email = "demo@lifeos.com"
    password = "Password123!@#"
} | ConvertTo-Json

$token = $null

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    $token = $res.data.accessToken
} catch {
    # If already registered, log in
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $res.data.accessToken
}

if (-not $token) {
    Write-Host "Failed to obtain token for demo@lifeos.com" -ForegroundColor Red
    exit 1
}

Write-Host "Demo user ready: demo@lifeos.com" -ForegroundColor Green

# 1. Seed sample active loan with high APR (triggers debt optimization insight)
$loanBody = @{
    lenderName = "SoFi Personal Loans"
    loanAccountNumber = "SOFI-882109"
    loanType = "PERSONAL"
    principalAmount = 18000.00
    outstandingBalance = 14500.00
    interestRate = 11.25
    interestType = "FIXED"
    paymentFrequency = "MONTHLY"
    tenureMonths = 36
    monthlyEmi = 591.40
    emiDueDay = 15
    startDate = "2025-03-01"
    endDate = "2028-03-01"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/loans" -Method Post -Body $loanBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" }
    Write-Host "Loan seeded" -ForegroundColor Green
} catch {}

# 2. Trigger proactive insights generation
try {
    $insightsRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/insights/generate" -Method Post -Headers @{ Authorization = "Bearer $token" }
    Write-Host "Generated $($insightsRes.data.totalActive) active insights for demo user!" -ForegroundColor Green
} catch {}
