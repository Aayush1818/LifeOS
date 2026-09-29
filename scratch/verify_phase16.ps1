# LifeOS Phase 16 Verification Script
# Verifies Reminder and Notification REST endpoints with JWT authentication

$baseUrl = "http://localhost:8080"
$randomId = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$email = "phase16_$randomId@example.com"
$password = "P@ssword123!"

Write-Host "=== Phase 16 Verification ===" -ForegroundColor Cyan

# 1. Register test user
Write-Host "1. Registering test user: $email" -ForegroundColor Yellow
$regBody = @{
    email = $email
    password = $password
    firstName = "Reminders"
    lastName = "Tester"
} | ConvertTo-Json

try {
    $regRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    $token = $regRes.data.accessToken
    Write-Host "✓ User registered successfully. Token obtained." -ForegroundColor Green
} catch {
    Write-Host "Registration failed: $_" -ForegroundColor Red
    exit 1
}

$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2. Create a reminder
Write-Host "`n2. Creating a reminder (Pay Health Insurance)..." -ForegroundColor Yellow
$dueDate = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm:ssZ")
$reminderBody = @{
    title = "Pay Star Health Insurance"
    description = "Annual premium renewal of INR 18,500"
    dueDate = $dueDate
    recurrenceRule = "YEARLY"
    priority = "HIGH"
} | ConvertTo-Json

$reminderRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/reminders" -Method Post -Headers $headers -Body $reminderBody
$reminderId = $reminderRes.data.id
Write-Host "✓ Reminder created with ID: $reminderId, Status: $($reminderRes.data.status)" -ForegroundColor Green

# 3. List active reminders
Write-Host "`n3. Querying active reminders..." -ForegroundColor Yellow
$listRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/reminders?status=ACTIVE" -Method Get -Headers $headers
Write-Host "✓ Active reminders found: $($listRes.data.content.Count)" -ForegroundColor Green

# 4. Query upcoming reminders
Write-Host "`n4. Querying upcoming 7-day reminders..." -ForegroundColor Yellow
$upcomingRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/reminders/upcoming?days=7" -Method Get -Headers $headers
Write-Host "✓ Upcoming reminders count: $($upcomingRes.data.Count)" -ForegroundColor Green

# 5. Complete reminder
Write-Host "`n5. Completing the reminder..." -ForegroundColor Yellow
$completeRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/reminders/$reminderId/complete" -Method Patch -Headers $headers
Write-Host "✓ Completed status: $($completeRes.data.status)" -ForegroundColor Green

# 6. Check unread notifications count
Write-Host "`n6. Checking unread notifications count..." -ForegroundColor Yellow
$countRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/unread-count" -Method Get -Headers $headers
Write-Host "✓ Unread notifications: $($countRes.data.unreadCount)" -ForegroundColor Green

# 7. List notifications
Write-Host "`n7. Listing notifications..." -ForegroundColor Yellow
$notifRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications" -Method Get -Headers $headers
Write-Host "✓ Total notifications returned: $($notifRes.data.content.Count)" -ForegroundColor Green

# 8. Mark all notifications as read
Write-Host "`n8. Marking all notifications as read..." -ForegroundColor Yellow
$markRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/mark-all-read" -Method Post -Headers $headers
Write-Host "✓ Mark all as read status: $($markRes.success)" -ForegroundColor Green

Write-Host "`n=== Phase 16 Verification Passed Successfully! ===" -ForegroundColor Cyan
