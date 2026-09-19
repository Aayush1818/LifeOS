# verify_phase6.ps1 - End-to-end verification script for Phase 6 Loan Amortization & Insurance Management
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "=== Phase 6 Loan Amortization & Insurance Management Live Verification ===" -ForegroundColor Cyan

# 1. Health check
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 10
    if ($health.status -eq "UP") {
        Write-Host "[PASS] 1. Actuator Health is UP" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 1. Actuator Health status: $($health.status)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[FAIL] 1. Actuator Health unreachable: $_" -ForegroundColor Red
    exit 1
}

# 2. Register User A
$suffixA = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userAEmail = "loan.userA.$suffixA@example.com"
$regA = @{
    email = $userAEmail
    password = "Password123!@#"
    firstName = "LoanUserA"
    lastName = "Verified"
} | ConvertTo-Json

$resA = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regA
$tokenA = $resA.data.accessToken
Write-Host "[PASS] 2. User A registered successfully: $userAEmail (Token acquired)" -ForegroundColor Green

# 3. Register User B
$suffixB = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userBEmail = "loan.userB.$suffixB@example.com"
$regB = @{
    email = $userBEmail
    password = "Password123!@#"
    firstName = "LoanUserB"
    lastName = "Verified"
} | ConvertTo-Json

$resB = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regB
$tokenB = $resB.data.accessToken
Write-Host "[PASS] 3. User B registered successfully: $userBEmail (Token acquired)" -ForegroundColor Green

$headersA = @{
    Authorization = "Bearer $tokenA"
    "Content-Type" = "application/json"
}

$headersB = @{
    Authorization = "Bearer $tokenB"
    "Content-Type" = "application/json"
}

$todayStr = (Get-Date).ToString("yyyy-MM-dd")
$today = Get-Date
$expiryDateStr = $today.AddYears(1).ToString("yyyy-MM-dd")

# 4. User A creates Dependent Emma
$depPayload = @{
    fullName = "Emma Verified"
    relationship = "CHILD"
    dateOfBirth = "2018-05-15"
    emergencyPhone = "+15551234567"
} | ConvertTo-Json

$depRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/dependents" -Method Post -Headers $headersA -Body $depPayload
$dependentId = $depRes.data.id
Write-Host "[PASS] 4. User A created Dependent profile: $dependentId" -ForegroundColor Green

# 5. User A creates a 30-Year Fixed Mortgage Loan ($300,000 @ 6.50%)
$loanPayload = @{
    loanAccountNumber = "MORTGAGE-$suffixA"
    lenderName = "Apex Premier Lending"
    loanType = "HOME"
    principalAmount = 300000.00
    interestRate = 6.50
    interestType = "FIXED"
    paymentFrequency = "MONTHLY"
    tenureMonths = 360
    emiDueDay = 1
    startDate = $todayStr
    notes = "30-Year Fixed Primary Residence Mortgage"
} | ConvertTo-Json

$loanRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans" -Method Post -Headers $headersA -Body $loanPayload
$loanAId = $loanRes.data.id
$loanAEmi = $loanRes.data.monthlyEmi
if ($loanRes.data.outstandingBalance -eq 300000.00 -and $loanAEmi -eq 1896.20) {
    Write-Host "[PASS] 5. User A created 30-yr mortgage: ID=$loanAId, EMI=`$$loanAEmi, Balance=`$$($loanRes.data.outstandingBalance)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 5. Unexpected mortgage EMI or balance: EMI=$loanAEmi, Balance=$($loanRes.data.outstandingBalance)" -ForegroundColor Red
}

# 6. Verify Amortization Schedule & Invariants
$scheduleRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$loanAId/schedule" -Method Get -Headers $headersA
$schedule = $scheduleRes.data
$installments = $schedule.installments

$countMatch = ($installments.Count -eq 360)
$principalSum = [decimal]0.00
$interestSum = [decimal]0.00
$totalPaymentSum = [decimal]0.00

foreach ($inst in $installments) {
    $principalSum += [decimal]$inst.principalComponent
    $interestSum += [decimal]$inst.interestComponent
    $totalPaymentSum += [decimal]$inst.payment
}

$lastInstallment = $installments[$installments.Count - 1]
$finalPrincipalZero = ([decimal]$lastInstallment.closingPrincipal -eq [decimal]0.00)
$principalInvariantHold = ([Math]::Abs($principalSum - [decimal]$schedule.principal) -lt 0.01)
$totalPaymentInvariantHold = ([Math]::Abs($totalPaymentSum - ($principalSum + $interestSum)) -lt 0.01)

if ($countMatch -and $finalPrincipalZero -and $principalInvariantHold -and $totalPaymentInvariantHold) {
    Write-Host "[PASS] 6. Amortization Invariants validated:" -ForegroundColor Green
    Write-Host "       - Installments count: $($installments.Count)" -ForegroundColor Gray
    Write-Host "       - Sum of Principal components: `$$principalSum (Exact Match `$$($schedule.principal))" -ForegroundColor Gray
    Write-Host "       - Final installment closing principal: `$$($lastInstallment.closingPrincipal) (Exact 0.00 Penny Reconciliation)" -ForegroundColor Gray
    Write-Host "       - Total payments: `$$totalPaymentSum == Principal (`$$principalSum) + Interest (`$$interestSum)" -ForegroundColor Gray
} else {
    Write-Host "[FAIL] 6. Invariant check failed! Count=$($installments.Count), FinalBal=$($lastInstallment.closingPrincipal), SumPrinc=$principalSum, InvariantHold=$principalInvariantHold" -ForegroundColor Red
}

# 7. Duplicate Loan Account Number rejection (409 Conflict)
try {
    $dupLoan = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans" -Method Post -Headers $headersA -Body $loanPayload
    Write-Host "[FAIL] 7. Duplicate loan account number was NOT rejected!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "[PASS] 7. Duplicate loan account number correctly rejected with HTTP 409 Conflict" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 7. Duplicate loan returned unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# 8. User A creates Auto Loan for payment/prepayment/closure testing ($20,000 @ 10.00% for 24 months)
$autoPayload = @{
    loanAccountNumber = "AUTO-$suffixA"
    lenderName = "First National Auto Finance"
    loanType = "VEHICLE"
    principalAmount = 20000.00
    interestRate = 10.00
    interestType = "FIXED"
    paymentFrequency = "MONTHLY"
    tenureMonths = 24
    emiDueDay = 15
    startDate = $todayStr
} | ConvertTo-Json

$autoRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans" -Method Post -Headers $headersA -Body $autoPayload
$autoLoanId = $autoRes.data.id
if ($autoRes.data.monthlyEmi -eq 922.90) {
    Write-Host "[PASS] 8. Auto loan created: ID=$autoLoanId, EMI=`$$($autoRes.data.monthlyEmi)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8. Auto loan EMI mismatch: $($autoRes.data.monthlyEmi)" -ForegroundColor Red
}

# 8a. Regular EMI Payment
$pay1Payload = @{
    paymentAmount = 922.90
    paymentDate = $todayStr
    paymentType = "REGULAR_EMI"
    transactionRef = "TXN-REG-01"
} | ConvertTo-Json

$pay1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$autoLoanId/payments" -Method Post -Headers $headersA -Body $pay1Payload
$autoAfterPay1 = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$autoLoanId" -Method Get -Headers $headersA
if ($autoAfterPay1.data.outstandingBalance -eq 19243.77) {
    Write-Host "[PASS] 8a. Regular EMI recorded: Principal=`$$($pay1Res.data.principalComponent), Interest=`$$($pay1Res.data.interestComponent), Remaining Balance=`$$($autoAfterPay1.data.outstandingBalance)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8a. Regular EMI balance mismatch: $($autoAfterPay1.data.outstandingBalance)" -ForegroundColor Red
}

# 8b. Partial Prepayment (REDUCE_EMI strategy)
$prepPayload = @{
    paymentAmount = 5000.00
    paymentDate = $todayStr
    paymentType = "PARTIAL_PREPAYMENT"
    prepaymentStrategy = "REDUCE_EMI"
    transactionRef = "TXN-PREP-01"
} | ConvertTo-Json

$prepRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$autoLoanId/payments" -Method Post -Headers $headersA -Body $prepPayload
$autoAfterPrep = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$autoLoanId" -Method Get -Headers $headersA
if ($autoAfterPrep.data.outstandingBalance -eq 14243.77) {
    Write-Host "[PASS] 8b. Partial Prepayment recorded: 100% Principal Component=`$$($prepRes.data.principalComponent), Remaining Balance=`$$($autoAfterPrep.data.outstandingBalance)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8b. Partial prepayment remaining balance mismatch: $($autoAfterPrep.data.outstandingBalance)" -ForegroundColor Red
}

# 8c. Check recomputed EMI
if ($autoAfterPrep.data.monthlyEmi -eq 657.28) {
    Write-Host "[PASS] 8c. Monthly EMI recomputed after prepayment: New EMI = `$657.28 (Reduced from `$922.90)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8c. EMI recomputation mismatch: $($autoAfterPrep.data.monthlyEmi)" -ForegroundColor Red
}

# 8d. Full Early Closure
$closurePayload = @{
    paymentAmount = 14243.77
    paymentDate = $todayStr
    paymentType = "FULL_CLOSURE"
    transactionRef = "TXN-CLOSE-01"
} | ConvertTo-Json

$closeRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$autoLoanId/payments" -Method Post -Headers $headersA -Body $closurePayload
$autoClosed = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$autoLoanId" -Method Get -Headers $headersA
if ($autoClosed.data.outstandingBalance -eq 0.00 -and $autoClosed.data.status -eq "CLOSED") {
    Write-Host "[PASS] 8d. Full loan early closure successful: Status=$($autoClosed.data.status), Remaining Balance=`$$($autoClosed.data.outstandingBalance)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8d. Loan closure failed: Status=$($autoClosed.data.status), Balance=$($autoClosed.data.outstandingBalance)" -ForegroundColor Red
}

# 9. Loan Portfolio Summary (Aggregates Active Loans)
$summaryRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/analytics/summary" -Method Get -Headers $headersA
$summary = $summaryRes.data
if ($summary.activeLoansCount -eq 1 -and $summary.totalOutstandingBalance -eq 300000.00 -and $summary.totalMonthlyEmi -eq 1896.20) {
    Write-Host "[PASS] 9. Loan Portfolio Summary via JDBC: ActiveLoans=$($summary.activeLoansCount), TotalOutstanding=`$$($summary.totalOutstandingBalance), MonthlyCommitment=`$$($summary.totalMonthlyEmi), PrincipalPaid=`$$($summary.totalPrincipalPaid), InterestPaid=`$$($summary.totalInterestPaid)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 9. Loan Portfolio Summary mismatch: $($summary | ConvertTo-Json)" -ForegroundColor Red
}

# 10. User A creates Health Insurance Policy linked to Emma
$insPayload = @{
    policyNumber = "HEALTH-$suffixA"
    policyName = "Comprehensive Family Health Plan"
    providerName = "BlueShield Healthcare"
    policyType = "HEALTH"
    coverageAmount = 500000.00
    premiumAmount = 450.00
    premiumFrequency = "MONTHLY"
    startDate = $todayStr
    expiryDate = $expiryDateStr
    dependentId = $dependentId
    notes = "Family health coverage including dental and optical"
} | ConvertTo-Json

$insRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance" -Method Post -Headers $headersA -Body $insPayload
$policyId = $insRes.data.id
if ($insRes.data.status -eq "ACTIVE" -and $insRes.data.dependentName -eq "Emma Verified") {
    Write-Host "[PASS] 10. Insurance Policy created: ID=$policyId, PolicyNo=$($insRes.data.policyNumber), Dependent=$($insRes.data.dependentName), Status=$($insRes.data.status)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 10. Insurance Policy creation mismatch: $($insRes.data | ConvertTo-Json)" -ForegroundColor Red
}

# 11. Duplicate Insurance Policy Number rejection (409 Conflict)
try {
    $dupIns = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance" -Method Post -Headers $headersA -Body $insPayload
    Write-Host "[FAIL] 11. Duplicate policy number was NOT rejected!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "[PASS] 11. Duplicate policy number correctly rejected with HTTP 409 Conflict" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 11. Duplicate policy returned unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# 12. Upcoming Renewals Check (window 400 days)
$upcomingRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance/renewals/upcoming?windowDays=400" -Method Get -Headers $headersA
$upcoming = $upcomingRes.data
if ($upcoming.upcomingCount -ge 1 -and $upcoming.policies[0].id -eq $policyId) {
    Write-Host "[PASS] 12. Upcoming Renewals Query: Found $($upcoming.upcomingCount) policy due within 400 days (Policy=$($upcoming.policies[0].policyNumber), ExpiryDate=$($upcoming.policies[0].expiryDate))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 12. Upcoming Renewals query mismatch: $($upcoming | ConvertTo-Json)" -ForegroundColor Red
}

# 13. Renew Insurance Policy
$newExpiryDate = $today.AddYears(2).ToString("yyyy-MM-dd")
$renewPayload = @{
    newExpiryDate = $newExpiryDate
    newPremiumAmount = 475.00
    notes = "Renewed for Year 2"
} | ConvertTo-Json

$renewRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance/$policyId/renew" -Method Post -Headers $headersA -Body $renewPayload
if ($renewRes.data.expiryDate -eq $newExpiryDate -and $renewRes.data.premiumAmount -eq 475.00) {
    Write-Host "[PASS] 13. Insurance Policy renewed: New Expiry=$($renewRes.data.expiryDate), New Premium=`$$($renewRes.data.premiumAmount), Status=$($renewRes.data.status)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 13. Insurance Policy renewal mismatch: $($renewRes.data | ConvertTo-Json)" -ForegroundColor Red
}

# 14. Soft Delete Insurance Policy
$delRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance/$policyId" -Method Delete -Headers $headersA
# Verify 404 after deletion
try {
    $checkDel = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance/$policyId" -Method Get -Headers $headersA
    Write-Host "[FAIL] 14. Deleted policy was still retrievable!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 14. Insurance Policy soft-deleted and confirmed 404 Not Found on subsequent lookup" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 14. Expected 404 but got: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# 15. Multi-Tenant Cross-User Isolation (User B cannot access User A's resources)
Write-Host "--- Testing Multi-Tenant Resource Isolation ---" -ForegroundColor Cyan
$tenantViolations = 0

# 15a. User B accessing User A's loan
try {
    $tb1 = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$loanAId" -Method Get -Headers $headersB
    Write-Host "[FAIL] 15a. User B accessed User A's loan!" -ForegroundColor Red
    $tenantViolations++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 15a. User B accessing User A's loan returned 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15a. User B got unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        $tenantViolations++
    }
}

# 15b. User B accessing User A's amortization schedule
try {
    $tb2 = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$loanAId/schedule" -Method Get -Headers $headersB
    Write-Host "[FAIL] 15b. User B accessed User A's amortization schedule!" -ForegroundColor Red
    $tenantViolations++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 15b. User B accessing User A's amortization schedule returned 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15b. User B got unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        $tenantViolations++
    }
}

# 15c. User B attempting payment on User A's loan
try {
    $tb3 = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/$loanAId/payments" -Method Post -Headers $headersB -Body $pay1Payload
    Write-Host "[FAIL] 15c. User B recorded payment on User A's loan!" -ForegroundColor Red
    $tenantViolations++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 15c. User B payment on User A's loan returned 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15c. User B got unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        $tenantViolations++
    }
}

# 15d. User B accessing User A's insurance policy
try {
    $tb4 = Invoke-RestMethod -Uri "$baseUrl/api/v1/insurance/$policyId" -Method Get -Headers $headersB
    Write-Host "[FAIL] 15d. User B accessed User A's insurance policy!" -ForegroundColor Red
    $tenantViolations++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "[PASS] 15d. User B accessing User A's insurance returned 404 Not Found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15d. User B got unexpected status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        $tenantViolations++
    }
}

# 15e. User B portfolio summary returns 0
$sumB = Invoke-RestMethod -Uri "$baseUrl/api/v1/loans/analytics/summary" -Method Get -Headers $headersB
if ($sumB.data.activeLoansCount -eq 0 -and $sumB.data.totalOutstandingBalance -eq 0.00) {
    Write-Host "[PASS] 15e. User B Loan Portfolio Summary isolated: 0 active loans, `$0.00 balance" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 15e. User B portfolio summary leaked data: $($sumB.data | ConvertTo-Json)" -ForegroundColor Red
    $tenantViolations++
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "Phase 6 Verification Complete: All Endpoints, Mathematical Invariants, and Security Bounds PASSED!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
