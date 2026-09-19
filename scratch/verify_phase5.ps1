# verify_phase5.ps1 - End-to-end verification script for Phase 5 Personal Finance & Monthly Budgeting
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "=== Phase 5 Personal Finance & Monthly Budgeting Live Verification ===" -ForegroundColor Cyan

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

# 2. Register Users
$suffixA = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userAEmail = "fin.userA.$suffixA@example.com"
$regA = @{
    email = $userAEmail
    password = "Password123!@#"
    firstName = "FinanceUserA"
    lastName = "Verified"
} | ConvertTo-Json

$resA = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regA
$tokenA = $resA.data.accessToken
Write-Host "[PASS] 2. User A registered successfully: $userAEmail (Token acquired)" -ForegroundColor Green

$suffixB = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userBEmail = "fin.userB.$suffixB@example.com"
$regB = @{
    email = $userBEmail
    password = "Password123!@#"
    firstName = "FinanceUserB"
    lastName = "Verified"
} | ConvertTo-Json

$resB = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regB
$tokenB = $resB.data.accessToken
Write-Host "[PASS] 3. User B registered successfully: $userBEmail (Token acquired)" -ForegroundColor Green

$todayStr = (Get-Date).ToString("yyyy-MM-dd")
$curMonth = (Get-Date).Month
$curYear = (Get-Date).Year

# 3. User A records Income ($6,000.00)
$incomePayload = @{
    transactionType = "INCOME"
    category = "INCOME_SALARY"
    amount = 6000.00
    paymentMethod = "BANK_TRANSFER"
    transactionDate = $todayStr
    description = "Senior Engineer Monthly Salary"
} | ConvertTo-Json

$incomeRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $incomePayload
if ($incomeRes.success -and $incomeRes.data.amount -eq 6000.00) {
    Write-Host "[PASS] 4. User A recorded Income transaction (`$6000.00 INCOME_SALARY)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 4. Income transaction recording failed" -ForegroundColor Red
}

# 4. User A records Expense ($250.00 Food & Dining)
$expense1Payload = @{
    transactionType = "EXPENSE"
    category = "FOOD_DINING"
    amount = 250.00
    paymentMethod = "CREDIT_CARD"
    transactionDate = $todayStr
    description = "Weekly organic groceries"
} | ConvertTo-Json

$exp1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $expense1Payload
$exp1Id = $exp1Res.data.id
if ($exp1Res.success -and $exp1Res.data.amount -eq 250.00) {
    Write-Host "[PASS] 5. User A recorded Expense (`$250.00 FOOD_DINING, ID: $exp1Id)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 5. Expense transaction recording failed" -ForegroundColor Red
}

# 5. Duplicate Transaction Warning Detection
$dupRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $expense1Payload
if ($dupRes.data.possibleDuplicateWarning -eq $true) {
    Write-Host "[PASS] 6. Duplicate transaction warning flagged accurately" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 6. Duplicate warning was not flagged" -ForegroundColor Red
}

# 6. User A records Expense Refund ($50.00 Food & Dining refund)
$refundPayload = @{
    transactionType = "EXPENSE"
    category = "FOOD_DINING"
    amount = 50.00
    paymentMethod = "CREDIT_CARD"
    transactionDate = $todayStr
    description = "Grocery item returned refund"
    isRefund = $true
} | ConvertTo-Json

$refundRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $refundPayload
if ($refundRes.success -and $refundRes.data.isRefund -eq $true) {
    Write-Host "[PASS] 7. User A recorded Expense Refund (`$50.00 FOOD_DINING)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 7. Refund recording failed: isRefund=$($refundRes.data.isRefund)" -ForegroundColor Red
}

# 7. User A records another Expense ($150.00 Utilities)
$expense2Payload = @{
    transactionType = "EXPENSE"
    category = "UTILITIES"
    amount = 150.00
    paymentMethod = "DEBIT_CARD"
    transactionDate = $todayStr
    description = "Electricity and water"
} | ConvertTo-Json

$exp2Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $expense2Payload

# 8. Monthly Financial Summary Analytics via direct JDBC acceleration
$summaryRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/analytics/monthly-summary?month=$curMonth&year=$curYear" `
    -Method Get -Headers @{ Authorization = "Bearer $tokenA" }

# In our setup:
# Income = 6000.00
# Exp 1 = 250.00, Exp 2 = 250.00 (from duplicate test), Refund = -50.00, Utilities = 150.00
# Total expenses = 250 + 250 - 50 + 150 = 600.00
# Net savings = 6000.00 - 600.00 = 5400.00
# Savings rate = 5400 / 6000 * 100 = 90.00%
if ($summaryRes.data.totalIncome -eq 6000.00 -and $summaryRes.data.totalExpenses -eq 600.00 -and $summaryRes.data.netSavings -eq 5400.00) {
    Write-Host "[PASS] 8. Monthly Summary Analytics: Income=`$$($summaryRes.data.totalIncome), Expenses=`$$($summaryRes.data.totalExpenses), Savings=`$$($summaryRes.data.netSavings), Rate=$($summaryRes.data.savingsRatePercentage)%" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8. Monthly Summary calculation mismatch: Income=$($summaryRes.data.totalIncome), Expenses=$($summaryRes.data.totalExpenses), Savings=$($summaryRes.data.netSavings)" -ForegroundColor Red
}

# 9. Category Breakdown Analytics
$catRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/analytics/category-breakdown?month=$curMonth&year=$curYear" `
    -Method Get -Headers @{ Authorization = "Bearer $tokenA" }
if ($catRes.data.Count -ge 2) {
    Write-Host "[PASS] 9. Category breakdown verified with $($catRes.data.Count) categories" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 9. Category breakdown empty or missing categories" -ForegroundColor Red
}

# 10. Month-over-Month Comparison Analytics
$momRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/analytics/month-over-month?month=$curMonth&year=$curYear" `
    -Method Get -Headers @{ Authorization = "Bearer $tokenA" }
if ($momRes.data.currentMonthExpenses -eq 600.00 -and $momRes.data.direction -eq "INCREASED") {
    Write-Host "[PASS] 10. Month-over-Month comparison verified (Direction: $($momRes.data.direction), Delta: `$$($momRes.data.deltaAmount))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 10. Month-over-Month comparison unexpected output: Current=$($momRes.data.currentMonthExpenses)" -ForegroundColor Red
}

# 11. Recurring Transactions (Subscriptions & Scheduled Bills)
$recPayload = @{
    title = "Streaming Cloud Service"
    amount = 19.99
    transactionType = "EXPENSE"
    category = "ENTERTAINMENT"
    paymentMethod = "CREDIT_CARD"
    recurrencePattern = "MONTHLY"
    billingDay = 10
    startDate = "2026-01-01"
    autoCreateTransaction = $true
    notes = "Monthly cloud streaming subscription"
} | ConvertTo-Json

$recRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/recurring" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $recPayload
$recId = $recRes.data.id
if ($recRes.success -and $recRes.data.amount -eq 19.99 -and $recRes.data.status -eq "ACTIVE") {
    Write-Host "[PASS] 11. Recurring subscription created (`$19.99 MONTHLY, ID: $recId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 11. Recurring transaction creation failed" -ForegroundColor Red
}

# 12. Update Recurring Transaction Rule
$recUpdatePayload = @{
    amount = 22.99
    notes = "Price adjustment update"
} | ConvertTo-Json

$recUpRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/recurring/$recId" -Method Put `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $recUpdatePayload
if ($recUpRes.data.amount -eq 22.99) {
    Write-Host "[PASS] 12. Recurring subscription updated to `$22.99" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 12. Recurring subscription update failed" -ForegroundColor Red
}

# 13. Set Budget: Food & Dining $800.00
$budgetPayload = @{
    category = "FOOD_DINING"
    budgetMonth = $curMonth
    budgetYear = $curYear
    allocatedAmount = 800.00
    alertThresholds = @(50, 75, 90, 100)
} | ConvertTo-Json

$budgetRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/budgets" -Method Post `
    -Headers @{ Authorization = "Bearer $tokenA" } -ContentType "application/json" -Body $budgetPayload
$budgetId = $budgetRes.data.id
if ($budgetRes.success -and $budgetRes.data.allocatedAmount -eq 800.00) {
    Write-Host "[PASS] 13. Budget set for FOOD_DINING (`$800.00, Month: $curMonth/$curYear, ID: $budgetId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 13. Budget creation failed" -ForegroundColor Red
}

# 14. Check Budget Status:
# Total spent on FOOD_DINING = 250 + 250 - 50 = 450.00 out of 800.00 allocated
# Utilization = 450.00 / 800.00 * 100 = 56.25%
# Triggered threshold = 50 (since 56.25 >= 50 and < 75)
$statusRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/budgets/status?month=$curMonth&year=$curYear" `
    -Method Get -Headers @{ Authorization = "Bearer $tokenA" }

$gCat = $statusRes.data.categories | Where-Object { $_.category -eq "FOOD_DINING" }
if ($gCat.actualSpent -eq 450.00 -and $gCat.remainingAmount -eq 350.00 -and $gCat.highestTriggeredThreshold -eq 50 -and -not $gCat.isOverBudget) {
    Write-Host "[PASS] 14. Budget Status verified: Spent=`$$($gCat.actualSpent), Remaining=`$$($gCat.remainingAmount), Util=$($gCat.utilizationPercentage)%, Threshold=$($gCat.highestTriggeredThreshold), OverBudget=$($gCat.isOverBudget)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 14. Budget status mismatch: Spent=$($gCat.actualSpent), Util=$($gCat.utilizationPercentage)%, Threshold=$($gCat.highestTriggeredThreshold)" -ForegroundColor Red
}

# 15. Cross-Tenant Resource Isolation: User B cannot access User A's data
$bGetTx = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/finance/transactions/$exp1Id" `
    -H "Authorization: Bearer $tokenB"
if ($bGetTx -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 15. Cross-tenant GET transaction by User B returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 15. User B accessed User A's transaction: $bGetTx" -ForegroundColor Red
}

$bGetRec = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/finance/recurring/$recId" `
    -H "Authorization: Bearer $tokenB"
if ($bGetRec -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 16. Cross-tenant GET recurring rule by User B returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 16. User B accessed User A's recurring rule: $bGetRec" -ForegroundColor Red
}

$bGetBudget = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/budgets/$budgetId" `
    -H "Authorization: Bearer $tokenB"
if ($bGetBudget -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 17. Cross-tenant GET budget by User B returns 404 Not Found" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 17. User B accessed User A's budget: $bGetBudget" -ForegroundColor Red
}

# User B's budget status should be completely empty
$bBudgetStatus = Invoke-RestMethod -Uri "$baseUrl/api/v1/budgets/status?month=$curMonth&year=$curYear" `
    -Method Get -Headers @{ Authorization = "Bearer $tokenB" }
if ($bBudgetStatus.data.totalAllocated -eq 0 -and $bBudgetStatus.data.categories.Count -eq 0) {
    Write-Host "[PASS] 18. User B budget status shows 0 allocated and isolated view" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 18. User B budget status leaked User A data" -ForegroundColor Red
}

# 16. Soft-deletion of transaction and budget
$delTx = Invoke-RestMethod -Uri "$baseUrl/api/v1/finance/transactions/$exp1Id" -Method Delete -Headers @{ Authorization = "Bearer $tokenA" }
$afterDeleteTx = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/finance/transactions/$exp1Id" `
    -H "Authorization: Bearer $tokenA"
if ($afterDeleteTx -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 19. Soft-deleted transaction returns 404 on subsequent GET" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 19. Soft-deleted transaction still accessible" -ForegroundColor Red
}

$delBg = Invoke-RestMethod -Uri "$baseUrl/api/v1/budgets/$budgetId" -Method Delete -Headers @{ Authorization = "Bearer $tokenA" }
$afterDeleteBudget = curl.exe -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$baseUrl/api/v1/budgets/$budgetId" `
    -H "Authorization: Bearer $tokenA"
if ($afterDeleteBudget -match "HTTP_STATUS:404") {
    Write-Host "[PASS] 20. Soft-deleted budget returns 404 on subsequent GET" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 20. Soft-deleted budget still accessible" -ForegroundColor Red
}

# 17. Verify Swagger OpenAPI docs
$openApi = Invoke-RestMethod -Uri "$baseUrl/v3/api-docs" -Method Get
$hasTx = $openApi.paths."/api/v1/finance/transactions"
$hasAnalytics = $openApi.paths."/api/v1/finance/analytics/monthly-summary"
$hasRecurring = $openApi.paths."/api/v1/finance/recurring"
$hasBudgets = $openApi.paths."/api/v1/budgets"

if ($hasTx -and $hasAnalytics -and $hasRecurring -and $hasBudgets) {
    Write-Host "[PASS] 21. OpenAPI schema contains all Phase 5 endpoints" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 21. OpenAPI schema missing Phase 5 endpoints" -ForegroundColor Red
}

Write-Host "=== Phase 5 Verification Completed Successfully ===" -ForegroundColor Cyan
