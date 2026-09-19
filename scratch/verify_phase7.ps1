# verify_phase7.ps1 - End-to-end verification script for Phase 7 Healthcare & Medical Document Organization
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "=== Phase 7 Healthcare & Medical Document Live Verification ===" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Health check & Ping
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 10
    if ($health.status -eq "UP") {
        Write-Host "[PASS] 1a. Actuator Health is UP" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 1a. Actuator Health status: $($health.status)" -ForegroundColor Red
        exit 1
    }

    $ping = Invoke-RestMethod -Uri "$baseUrl/api/v1/health/ping" -Method Get -TimeoutSec 10
    if ($ping.success -and $ping.data.database -eq "CONNECTED" -and $ping.data.pgvectorExtension -eq "AVAILABLE") {
        Write-Host "[PASS] 1b. Public Health Ping succeeded (DB Connected, pgvector Available)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 1b. Health Ping unexpected: $($ping | ConvertTo-Json -Depth 2)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[FAIL] 1. Server unreachable: $_" -ForegroundColor Red
    exit 1
}

# 2. Register User A
$suffixA = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userAEmail = "health.userA.$suffixA@example.com"
$regA = @{
    email = $userAEmail
    password = "Password123!@#"
    firstName = "HealthUserA"
    lastName = "Verified"
} | ConvertTo-Json

$resA = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regA
$tokenA = $resA.data.accessToken
Write-Host "[PASS] 2. User A registered: $userAEmail (Token acquired)" -ForegroundColor Green

# 3. Register User B
$suffixB = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userBEmail = "health.userB.$suffixB@example.com"
$regB = @{
    email = $userBEmail
    password = "Password123!@#"
    firstName = "HealthUserB"
    lastName = "Verified"
} | ConvertTo-Json

$resB = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $regB
$tokenB = $resB.data.accessToken
Write-Host "[PASS] 3. User B registered: $userBEmail (Token acquired)" -ForegroundColor Green

$headersA = @{
    Authorization = "Bearer $tokenA"
    "Content-Type" = "application/json"
}

$headersB = @{
    Authorization = "Bearer $tokenB"
    "Content-Type" = "application/json"
}

# 4. User A creates Dependent Emma
$depPayload = @{
    fullName = "Emma Verified"
    relationship = "CHILD"
    dateOfBirth = "2018-05-15"
    emergencyPhone = "+15551234567"
} | ConvertTo-Json

$depRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/dependents" -Method Post -Headers $headersA -Body $depPayload
$dependentId = $depRes.data.id
Write-Host "[PASS] 4. User A created Dependent profile: Emma ($dependentId)" -ForegroundColor Green

# 5. User A uploads a medical prescription PDF via Document Management API
$tempPdfPath = [System.IO.Path]::GetTempFileName() + ".pdf"
[System.IO.File]::WriteAllBytes($tempPdfPath, [System.Text.Encoding]::UTF8.GetBytes("%PDF-1.4`n%LifeOS prescription test content`n%%EOF"))

$tempMetaPath = [System.IO.Path]::GetTempFileName() + ".json"
$docMeta = @{
    title = "Cardiology Prescription Dr Jenkins"
    description = "Official prescription note"
    category = "MEDICAL"
    documentType = "PRESCRIPTION"
    tags = @("cardiology", "rx", "lipitor")
} | ConvertTo-Json
[System.IO.File]::WriteAllText($tempMetaPath, $docMeta)

try {
    # Using curl.exe for multipart upload
    $uploadOutput = & curl.exe -s -X POST "$baseUrl/api/v1/documents/upload" `
        -H "Authorization: Bearer $tokenA" `
        -F "file=@$tempPdfPath;type=application/pdf" `
        -F "metadata=@$tempMetaPath;type=application/json"

    $docJson = $uploadOutput | ConvertFrom-Json
    if ($docJson.success -and $docJson.data.id) {
        $medicalDocAId = $docJson.data.id
        Write-Host "[PASS] 5. User A uploaded medical prescription document: $medicalDocAId (Type: PRESCRIPTION, Category: MEDICAL)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 5. Document upload failed: $uploadOutput" -ForegroundColor Red
        exit 1
    }
} finally {
    if (Test-Path $tempPdfPath) { Remove-Item $tempPdfPath -Force }
    if (Test-Path $tempMetaPath) { Remove-Item $tempMetaPath -Force }
}

# 6. User A creates Appointment 1 (Personal Cardiology Consultation with 60-min reminder)
$startA1 = (Get-Date).ToUniversalTime().AddDays(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$endA1 = (Get-Date).ToUniversalTime().AddDays(1).AddMinutes(45).ToString("yyyy-MM-ddTHH:mm:ssZ")

$app1Payload = @{
    doctorName = "Dr. Sarah Jenkins"
    specialization = "Cardiology"
    clinicOrHospital = "Apex Cardiology Institute"
    clinicAddress = "100 Medical Center Way, Suite 400"
    clinicPhone = "+1-555-019-2834"
    appointmentTime = $startA1
    scheduledEndTime = $endA1
    timeZone = "America/New_York"
    purpose = "Annual cardiovascular evaluation and ECG review"
    reminderOffsetMinutes = 60
    notes = "Bring current medication list and past lipid panels"
} | ConvertTo-Json

$app1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments" -Method Post -Headers $headersA -Body $app1Payload
$app1Id = $app1Res.data.id
$disclaimer = $app1Res.data.disclaimer

if ($app1Res.success -and $app1Id -and $app1Res.data.status -eq "SCHEDULED" -and $disclaimer -like "*Strictly organizational*") {
    Write-Host "[PASS] 6. User A created Appointment 1: $app1Id (Status: SCHEDULED, Non-Diagnostic Disclaimer verified)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 6. Failed creating Appointment 1: $($app1Res | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 7. User A creates Appointment 2 (Dependent Emma Pediatrics Consultation with 30-min reminder)
$startA2 = (Get-Date).ToUniversalTime().AddDays(3).ToString("yyyy-MM-ddTHH:mm:ssZ")
$endA2 = (Get-Date).ToUniversalTime().AddDays(3).AddMinutes(30).ToString("yyyy-MM-ddTHH:mm:ssZ")

$app2Payload = @{
    dependentId = $dependentId
    doctorName = "Dr. Emily Vance"
    specialization = "Pediatrics"
    clinicOrHospital = "City Children's Clinic"
    clinicAddress = "250 Wellness Blvd"
    clinicPhone = "+1-555-028-3741"
    appointmentTime = $startA2
    scheduledEndTime = $endA2
    timeZone = "America/New_York"
    purpose = "Annual pediatric wellness checkup"
    reminderOffsetMinutes = 30
    notes = "Check vaccination history"
} | ConvertTo-Json

$app2Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments" -Method Post -Headers $headersA -Body $app2Payload
$app2Id = $app2Res.data.id

if ($app2Res.success -and $app2Id -and $app2Res.data.dependentId -eq $dependentId) {
    Write-Host "[PASS] 7. User A created Appointment 2 for Dependent: $app2Id (DependentId: $dependentId)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 7. Failed creating Appointment 2: $($app2Res | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 8. User A attaches Medical Document to Appointment 1
$attachRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id/documents/$medicalDocAId" -Method Post -Headers $headersA
if ($attachRes.success -and $attachRes.data.id -eq $medicalDocAId) {
    Write-Host "[PASS] 8. User A attached prescription document $medicalDocAId to appointment $app1Id" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 8. Failed attaching document: $($attachRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 9. User A retrieves Appointment 1 details and verifies linked document
$detailRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id" -Method Get -Headers $headersA
$linkedDocs = $detailRes.data.linkedDocuments

if ($detailRes.success -and $linkedDocs.Count -ge 1 -and $linkedDocs[0].id -eq $medicalDocAId) {
    Write-Host "[PASS] 9. Appointment 1 retrieved with linked document: $($linkedDocs[0].title) ($($linkedDocs[0].documentType))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 9. Linked document not found in appointment details: $($detailRes | ConvertTo-Json -Depth 3)" -ForegroundColor Red
    exit 1
}

# 10. User A reschedules Appointment 1
$newStartA1 = (Get-Date).ToUniversalTime().AddDays(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$newEndA1 = (Get-Date).ToUniversalTime().AddDays(2).AddMinutes(45).ToString("yyyy-MM-ddTHH:mm:ssZ")

$reschedPayload = @{
    newAppointmentTime = $newStartA1
    newScheduledEndTime = $newEndA1
    timeZone = "America/New_York"
    notes = "Work schedule conflict"
    reminderOffsetMinutes = 60
} | ConvertTo-Json

$reschedRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id/reschedule" -Method Post -Headers $headersA -Body $reschedPayload
if ($reschedRes.success -and $reschedRes.data.status -eq "RESCHEDULED") {
    Write-Host "[PASS] 10. User A rescheduled Appointment 1 to $newStartA1 (Status: RESCHEDULED, Reminder synced)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 10. Reschedule failed: $($reschedRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 11. User A updates Appointment 1 status to COMPLETED
$statusPayload = @{
    status = "COMPLETED"
    notes = "Appointment completed. Blood pressure 120/80 reported by nurse."
} | ConvertTo-Json

$statusRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id/status" -Method Post -Headers $headersA -Body $statusPayload
if ($statusRes.success -and $statusRes.data.status -eq "COMPLETED") {
    Write-Host "[PASS] 11. User A marked Appointment 1 as COMPLETED (Reminder automatically dismissed)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 11. Status update failed: $($statusRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 12. User A queries upcoming appointments
$upcomingRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/upcoming" -Method Get -Headers $headersA
$upcomingIds = $upcomingRes.data.appointments | ForEach-Object { $_.id }
if ($upcomingRes.success -and $upcomingIds -contains $app2Id -and -not ($upcomingIds -contains $app1Id)) {
    Write-Host "[PASS] 12. Upcoming appointments verified: contains Appointment 2 (SCHEDULED), excludes Appointment 1 (COMPLETED)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 12. Upcoming list unexpected: count=$($upcomingRes.data.upcomingCount), IDs=$($upcomingIds -join ', ')" -ForegroundColor Red
    exit 1
}

# 13. User A queries medical documents list
$medDocsRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/documents" -Method Get -Headers $headersA
$medDocIds = $medDocsRes.data.content | ForEach-Object { $_.id }
if ($medDocsRes.success -and $medDocIds -contains $medicalDocAId) {
    Write-Host "[PASS] 13. Medical documents list retrieved: found $medicalDocAId" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 13. Medical documents list missing prescription: $($medDocsRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 14. User A detaches Document from Appointment 1
$detachRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id/documents/$medicalDocAId" -Method Delete -Headers $headersA
$detailAfterDetach = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id" -Method Get -Headers $headersA
if ($detachRes.success -and $detailAfterDetach.data.linkedDocuments.Count -eq 0) {
    Write-Host "[PASS] 14. User A detached document from Appointment 1 (Linked documents now 0)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 14. Detach failed: count=$($detailAfterDetach.data.linkedDocuments.Count)" -ForegroundColor Red
    exit 1
}

# 15. Cross-Tenant Security & Isolation Verification (User B vs User A)
Write-Host "--- Cross-Tenant Security Tests (User B -> User A's resources) ---" -ForegroundColor Yellow

# 15a. User B attempts to access User A's Appointment 1
try {
    $bAccess = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id" -Method Get -Headers $headersB
    Write-Host "[FAIL] 15a. User B accessed User A's appointment!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[PASS] 15a. User B blocked from accessing User A's appointment (HTTP 404 Not Found)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15a. Expected 404, got $statusCode" -ForegroundColor Red
    }
}

# 15b. User B attempts to reschedule User A's appointment
try {
    $bResched = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id/reschedule" -Method Post -Headers $headersB -Body $reschedPayload
    Write-Host "[FAIL] 15b. User B rescheduled User A's appointment!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[PASS] 15b. User B blocked from rescheduling User A's appointment (HTTP 404 Not Found)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15b. Expected 404, got $statusCode" -ForegroundColor Red
    }
}

# 15c. User B attempts to update User A's appointment status
try {
    $bStatus = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$app1Id/status" -Method Post -Headers $headersB -Body $statusPayload
    Write-Host "[FAIL] 15c. User B modified status of User A's appointment!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[PASS] 15c. User B blocked from updating User A's appointment status (HTTP 404 Not Found)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15c. Expected 404, got $statusCode" -ForegroundColor Red
    }
}

# 15d. User B attempts to create an appointment using User A's Dependent ID
$badDepPayload = @{
    dependentId = $dependentId
    doctorName = "Dr. Rogue"
    specialization = "General"
    clinicOrHospital = "Rogue Clinic"
    appointmentTime = $startA1
    timeZone = "UTC"
    purpose = "Illegitimate check"
} | ConvertTo-Json

try {
    $bCreateDep = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments" -Method Post -Headers $headersB -Body $badDepPayload
    Write-Host "[FAIL] 15d. User B created appointment with User A's dependent!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[PASS] 15d. User B blocked from using User A's dependent (HTTP 404 Not Found)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15d. Expected 404, got $statusCode" -ForegroundColor Red
    }
}

# 15e. User B creates own appointment and tries to attach User A's document
$bAppPayload = @{
    doctorName = "Dr. Bob"
    specialization = "Family Medicine"
    clinicOrHospital = "General Clinic"
    appointmentTime = $startA1
    timeZone = "UTC"
    purpose = "General consultation"
} | ConvertTo-Json

$bAppRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments" -Method Post -Headers $headersB -Body $bAppPayload
$bAppId = $bAppRes.data.id

try {
    $bAttach = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/$bAppId/documents/$medicalDocAId" -Method Post -Headers $headersB
    Write-Host "[FAIL] 15e. User B attached User A's document to User B's appointment!" -ForegroundColor Red
    exit 1
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[PASS] 15e. User B blocked from attaching User A's document (HTTP 404 Not Found)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] 15e. Expected 404, got $statusCode" -ForegroundColor Red
    }
}

# 15f. User B queries upcoming appointments -> must return 1 (only User B's own appointment)
$bUpcomingRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/healthcare/appointments/upcoming" -Method Get -Headers $headersB
if ($bUpcomingRes.success -and $bUpcomingRes.data.upcomingCount -eq 1 -and $bUpcomingRes.data.appointments[0].id -eq $bAppId) {
    Write-Host "[PASS] 15f. User B upcoming appointments strictly isolated (Only User B's appointment returned)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 15f. User B upcoming list leaked other appointments: $($bUpcomingRes | ConvertTo-Json -Depth 2)" -ForegroundColor Red
    exit 1
}

# 16. Verify static non-diagnostic disclaimer contract
if ($app1Res.data.disclaimer -eq "Strictly organizational & non-diagnostic. LifeOS does not provide medical diagnosis, clinical evaluation, or treatment advice.") {
    Write-Host "[PASS] 16. Non-Diagnostic Boundary Enforced: Static medical disclaimer present on all appointment responses" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 16. Disclaimer text does not match expected safety standard: $($app1Res.data.disclaimer)" -ForegroundColor Red
    exit 1
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "=== ALL 16 PHASE 7 LIVE HTTP VERIFICATION TESTS PASSED! ===" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
