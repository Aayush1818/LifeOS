package com.lifeos.user.export.service;

import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.asset.repository.AssetRepository;
import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditOutcome;
import com.lifeos.audit.event.SecurityAuditEvent;
import com.lifeos.budget.repository.BudgetRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import com.lifeos.finance.repository.TransactionRepository;
import com.lifeos.healthcare.repository.AppointmentRepository;
import com.lifeos.insurance.repository.InsurancePolicyRepository;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.reminder.repository.NotificationRepository;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.export.dto.LifeOSUserExportDto;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDataExportService implements DataExportService {

    private final UserRepository userRepository;
    private final DependentRepository dependentRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final BudgetRepository budgetRepository;
    private final LoanRepository loanRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final AppointmentRepository appointmentRepository;
    private final TripRepository tripRepository;
    private final AssetRepository assetRepository;
    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public LifeOSUserExportDto exportUserData(UUID userId) {
        log.info("Generating full LifeOS GDPR data export for user [{}]", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put("id", user.getId());
        userProfile.put("email", user.getEmail());
        userProfile.put("firstName", user.getFirstName());
        userProfile.put("lastName", user.getLastName());
        userProfile.put("phone", user.getPhone());
        userProfile.put("role", user.getRole());
        userProfile.put("createdAt", user.getCreatedAt());

        // Dependents
        List<Map<String, Object>> dependents = dependentRepository.findAllByUserIdAndIsDeletedFalse(userId).stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", d.getId());
                    map.put("fullName", d.getFullName());
                    map.put("relationship", d.getRelationship());
                    map.put("dateOfBirth", d.getDateOfBirth());
                    map.put("emergencyPhone", d.getEmergencyPhone());
                    map.put("medicalNotes", d.getMedicalNotes());
                    return map;
                }).toList();

        // Finance Transactions
        List<Map<String, Object>> transactions = transactionRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 1000)).stream()
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", t.getId());
                    map.put("amount", t.getAmount());
                    map.put("type", t.getTransactionType());
                    map.put("category", t.getCategory());
                    map.put("transactionDate", t.getTransactionDate());
                    map.put("paymentMethod", t.getPaymentMethod());
                    map.put("description", t.getDescription());
                    map.put("isRefund", t.isRefund());
                    map.put("isRecurring", t.isRecurring());
                    map.put("status", t.getStatus());
                    return map;
                }).toList();

        // Recurring Transactions
        List<Map<String, Object>> recurring = recurringTransactionRepository.findAllByUserIdAndIsDeletedFalse(userId).stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", r.getId());
                    map.put("title", r.getTitle());
                    map.put("amount", r.getAmount());
                    map.put("type", r.getTransactionType());
                    map.put("category", r.getCategory());
                    map.put("frequency", r.getRecurrencePattern());
                    map.put("nextDueDate", r.getNextDueDate());
                    map.put("status", r.getStatus());
                    return map;
                }).toList();

        // Budgets
        List<Map<String, Object>> budgets = budgetRepository.findAllByUserIdAndIsDeletedFalse(userId).stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", b.getId());
                    map.put("category", b.getCategory());
                    map.put("allocatedAmount", b.getAllocatedAmount());
                    map.put("month", b.getBudgetMonth());
                    map.put("year", b.getBudgetYear());
                    return map;
                }).toList();

        // Loans
        List<Map<String, Object>> loans = loanRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 500)).stream()
                .map(l -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", l.getId());
                    map.put("lenderName", l.getLenderName());
                    map.put("loanType", l.getLoanType());
                    map.put("principalAmount", l.getPrincipalAmount());
                    map.put("outstandingBalance", l.getOutstandingBalance());
                    map.put("interestRate", l.getInterestRate());
                    map.put("monthlyEmi", l.getMonthlyEmi());
                    map.put("status", l.getStatus());
                    return map;
                }).toList();

        // Insurance Policies
        List<Map<String, Object>> policies = insurancePolicyRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 500)).stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", p.getId());
                    map.put("policyName", p.getPolicyName());
                    map.put("providerName", p.getProviderName());
                    map.put("policyNumber", p.getPolicyNumber());
                    map.put("coverageAmount", p.getCoverageAmount());
                    map.put("premiumAmount", p.getPremiumAmount());
                    map.put("nextRenewalDate", p.getNextRenewalDate());
                    map.put("status", p.getStatus());
                    return map;
                }).toList();

        // Appointments
        List<Map<String, Object>> appointments = appointmentRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 500)).stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", a.getId());
                    map.put("doctorName", a.getDoctorName());
                    map.put("specialization", a.getSpecialization());
                    map.put("clinicOrHospital", a.getClinicOrHospital());
                    map.put("appointmentTime", a.getAppointmentTime());
                    map.put("status", a.getStatus());
                    map.put("purpose", a.getPurpose());
                    return map;
                }).toList();

        // Trips
        List<Map<String, Object>> trips = tripRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 500)).stream()
                .map(tr -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", tr.getId());
                    map.put("tripTitle", tr.getTripTitle());
                    map.put("destination", tr.getDestination());
                    map.put("startDate", tr.getStartDate());
                    map.put("endDate", tr.getEndDate());
                    map.put("totalBudget", tr.getTotalBudget());
                    map.put("actualSpend", tr.getActualSpend());
                    map.put("currency", tr.getCurrency());
                    map.put("status", tr.getStatus());
                    return map;
                }).toList();

        // Assets
        List<Map<String, Object>> assets = assetRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 500)).stream()
                .map(as -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", as.getId());
                    map.put("name", as.getName());
                    map.put("category", as.getCategory());
                    map.put("purchasePrice", as.getPurchasePrice());
                    map.put("currency", as.getCurrency());
                    map.put("purchaseDate", as.getPurchaseDate());
                    map.put("status", as.getStatus());
                    return map;
                }).toList();

        // Reminders
        List<Map<String, Object>> reminders = reminderRepository.findAllByUserIdAndIsDeletedFalseOrderByDueAtAsc(userId, PageRequest.of(0, 500)).stream()
                .map(rm -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", rm.getId());
                    map.put("title", rm.getTitle());
                    map.put("description", rm.getDescription());
                    map.put("dueAt", rm.getDueAt());
                    map.put("recurrencePattern", rm.getRecurrencePattern());
                    map.put("reminderType", rm.getReminderType());
                    map.put("status", rm.getStatus());
                    return map;
                }).toList();

        // Notifications
        List<Map<String, Object>> notifications = notificationRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 500)).stream()
                .map(nt -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", nt.getId());
                    map.put("title", nt.getTitle());
                    map.put("message", nt.getMessage());
                    map.put("notificationType", nt.getNotificationType());
                    map.put("channel", nt.getChannel());
                    map.put("isRead", nt.isRead());
                    map.put("createdAt", nt.getCreatedAt());
                    return map;
                }).toList();

        // Documents
        List<Map<String, Object>> documents = documentRepository.findAllByUserIdAndIsDeletedFalse(userId, PageRequest.of(0, 500)).stream()
                .map(dc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", dc.getId());
                    map.put("title", dc.getTitle());
                    map.put("category", dc.getCategory());
                    map.put("mimeType", dc.getMimeType());
                    map.put("fileSize", dc.getFileSize());
                    map.put("ingestionStatus", dc.getIngestionStatus());
                    map.put("version", dc.getVersion());
                    return map;
                }).toList();

        // Conversations
        List<Map<String, Object>> conversations = conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId, PageRequest.of(0, 100)).stream()
                .map(cv -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", cv.getId());
                    map.put("title", cv.getTitle());
                    map.put("createdAt", cv.getCreatedAt());
                    map.put("lastMessageAt", cv.getLastMessageAt());
                    return map;
                }).toList();

        // Emit audit event
        eventPublisher.publishEvent(SecurityAuditEvent.builder()
                .userId(userId)
                .eventType(AuditEventType.DATA_EXPORT_REQUESTED)
                .outcome(AuditOutcome.SUCCESS)
                .details(String.format("{\"exportedItemsCount\": %d}",
                        dependents.size() + transactions.size() + loans.size() + policies.size() + documents.size()))
                .build());

        return LifeOSUserExportDto.builder()
                .exportVersion("1.0")
                .exportedAt(OffsetDateTime.now())
                .userId(userId)
                .userProfile(userProfile)
                .dependents(dependents)
                .transactions(transactions)
                .recurringTransactions(recurring)
                .budgets(budgets)
                .loans(loans)
                .insurancePolicies(policies)
                .appointments(appointments)
                .trips(trips)
                .assets(assets)
                .reminders(reminders)
                .notifications(notifications)
                .documents(documents)
                .conversations(conversations)
                .build();
    }
}
