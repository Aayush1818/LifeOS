package com.lifeos.insurance.service;

import com.lifeos.common.exception.DuplicateResourceException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.insurance.dto.*;
import com.lifeos.insurance.entity.InsurancePolicyEntity;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.repository.InsurancePolicyRepository;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceService {

    public static final String REMINDER_TYPE_INSURANCE_RENEWAL = "INSURANCE_RENEWAL";

    private final InsurancePolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final DependentRepository dependentRepository;
    private final DocumentRepository documentRepository;
    private final ReminderRepository reminderRepository;

    @Transactional
    public InsuranceResponse createPolicy(CreateInsuranceRequest request, UUID userId) {
        if (policyRepository.existsByUserIdAndPolicyNumberAndIsDeletedFalse(userId, request.getPolicyNumber().trim())) {
            throw new DuplicateResourceException("An active insurance policy with number '" + request.getPolicyNumber() + "' already exists");
        }

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DependentEntity dependent = null;
        if (request.getDependentId() != null) {
            dependent = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));
        }

        DocumentEntity document = null;
        if (request.getDocumentId() != null) {
            document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
        }

        LocalDate nextRenewal = request.getNextRenewalDate() != null ? request.getNextRenewalDate() : request.getExpiryDate();

        InsurancePolicyEntity policy = InsurancePolicyEntity.builder()
                .user(user)
                .dependent(dependent)
                .document(document)
                .policyNumber(request.getPolicyNumber().trim())
                .policyName(request.getPolicyName() != null ? request.getPolicyName().trim() : request.getProviderName())
                .providerName(request.getProviderName().trim())
                .policyType(request.getPolicyType())
                .coverageAmount(request.getCoverageAmount())
                .premiumAmount(request.getPremiumAmount())
                .premiumFrequency(request.getPremiumFrequency())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .nextRenewalDate(nextRenewal)
                .status(PolicyStatus.ACTIVE)
                .notes(request.getNotes())
                .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
                .build();

        InsurancePolicyEntity saved = policyRepository.save(policy);
        log.info("User [{}] created insurance policy [{}] ({}, #{})",
                userId, saved.getId(), saved.getPolicyName(), saved.getPolicyNumber());

        // Synchronize with core reminder subsystem
        syncRenewalReminder(saved, user);

        return InsuranceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public InsuranceResponse getPolicy(UUID id, UUID userId) {
        InsurancePolicyEntity policy = policyRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance policy not found with id: " + id));
        return InsuranceResponse.fromEntity(policy);
    }

    @Transactional(readOnly = true)
    public Page<InsuranceResponse> listPolicies(UUID userId, InsuranceType type, PolicyStatus status, Pageable pageable) {
        Page<InsurancePolicyEntity> page;
        if (type != null) {
            page = policyRepository.findAllByUserIdAndPolicyTypeAndIsDeletedFalse(userId, type, pageable);
        } else if (status != null) {
            page = policyRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, status, pageable);
        } else {
            page = policyRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
        }
        return page.map(InsuranceResponse::fromEntity);
    }

    @Transactional
    public InsuranceResponse updatePolicy(UUID id, UpdateInsuranceRequest request, UUID userId) {
        InsurancePolicyEntity policy = policyRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance policy not found with id: " + id));

        if (request.getPolicyName() != null && !request.getPolicyName().isBlank()) {
            policy.setPolicyName(request.getPolicyName().trim());
        }
        if (request.getProviderName() != null && !request.getProviderName().isBlank()) {
            policy.setProviderName(request.getProviderName().trim());
        }
        if (request.getPolicyType() != null) {
            policy.setPolicyType(request.getPolicyType());
        }
        if (request.getCoverageAmount() != null) {
            policy.setCoverageAmount(request.getCoverageAmount());
        }
        if (request.getPremiumAmount() != null) {
            policy.setPremiumAmount(request.getPremiumAmount());
        }
        if (request.getPremiumFrequency() != null) {
            policy.setPremiumFrequency(request.getPremiumFrequency());
        }
        if (request.getExpiryDate() != null) {
            policy.setExpiryDate(request.getExpiryDate());
        }
        if (request.getNextRenewalDate() != null) {
            policy.setNextRenewalDate(request.getNextRenewalDate());
        }
        if (request.getStatus() != null) {
            policy.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            policy.setNotes(request.getNotes());
        }
        if (request.getMetadata() != null) {
            policy.setMetadata(request.getMetadata());
        }
        if (request.getDependentId() != null) {
            DependentEntity dep = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));
            policy.setDependent(dep);
        }
        if (request.getDocumentId() != null) {
            DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
            policy.setDocument(doc);
        }

        InsurancePolicyEntity updated = policyRepository.save(policy);
        log.info("User [{}] updated insurance policy [{}]", userId, id);

        // Update reminder
        syncRenewalReminder(updated, updated.getUser());

        return InsuranceResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePolicy(UUID id, UUID userId) {
        InsurancePolicyEntity policy = policyRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance policy not found with id: " + id));

        policy.setDeleted(true);
        policy.setStatus(PolicyStatus.CANCELLED);
        policyRepository.save(policy);

        // Dismiss linked reminder
        reminderRepository.findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(id, REMINDER_TYPE_INSURANCE_RENEWAL)
                .ifPresent(reminder -> {
                    reminder.setStatus(ReminderStatus.DISMISSED);
                    reminderRepository.save(reminder);
                });

        log.info("User [{}] soft-deleted insurance policy [{}] and dismissed reminder", userId, id);
    }

    @Transactional
    public InsuranceResponse renewPolicy(UUID id, RenewPolicyRequest request, UUID userId) {
        InsurancePolicyEntity policy = policyRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance policy not found with id: " + id));

        LocalDate oldExpiry = policy.getExpiryDate();
        policy.setExpiryDate(request.getNewExpiryDate());

        LocalDate newRenewal = request.getNewRenewalDate() != null
                ? request.getNewRenewalDate()
                : request.getNewExpiryDate();
        policy.setNextRenewalDate(newRenewal);

        if (request.getNewPremiumAmount() != null) {
            policy.setPremiumAmount(request.getNewPremiumAmount());
        }
        if (request.getDocumentId() != null) {
            DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
            policy.setDocument(doc);
        }

        policy.setStatus(PolicyStatus.ACTIVE);

        String renewalNote = String.format("Renewed on %s (Previous expiry: %s). %s",
                LocalDate.now(), oldExpiry, request.getNotes() != null ? request.getNotes() : "");
        policy.setNotes(policy.getNotes() != null ? policy.getNotes() + "\n" + renewalNote : renewalNote);

        InsurancePolicyEntity saved = policyRepository.save(policy);
        log.info("User [{}] renewed policy [{}] to new expiry date {}", userId, id, saved.getExpiryDate());

        // Advance reminder to new renewal date
        syncRenewalReminder(saved, saved.getUser());

        return InsuranceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public UpcomingRenewalsResponse getUpcomingRenewals(UUID userId, int windowDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(windowDays);

        List<InsurancePolicyEntity> upcoming = policyRepository
                .findAllByUserIdAndStatusAndNextRenewalDateBetweenAndIsDeletedFalse(
                        userId, PolicyStatus.ACTIVE, today, cutoff);

        List<InsuranceResponse> dtos = upcoming.stream()
                .map(InsuranceResponse::fromEntity)
                .toList();

        return UpcomingRenewalsResponse.builder()
                .windowDays(windowDays)
                .upcomingCount(dtos.size())
                .policies(dtos)
                .build();
    }

    /**
     * Programmatic search tool contract for future AI Agent invocation.
     */
    @Transactional(readOnly = true)
    public List<InsuranceResponse> searchInsurancePolicies(UUID userId, InsuranceType type, PolicyStatus status) {
        if (type != null) {
            return policyRepository.findAllByUserIdAndPolicyTypeAndIsDeletedFalse(userId, type, Pageable.unpaged())
                    .map(InsuranceResponse::fromEntity)
                    .getContent();
        } else if (status != null) {
            return policyRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, status, Pageable.unpaged())
                    .map(InsuranceResponse::fromEntity)
                    .getContent();
        }
        return policyRepository.findAllByUserIdAndIsDeletedFalse(userId, Pageable.unpaged())
                .map(InsuranceResponse::fromEntity)
                .getContent();
    }

    private void syncRenewalReminder(InsurancePolicyEntity policy, UserEntity user) {
        OffsetDateTime dueAt = policy.getNextRenewalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        String title = "Insurance Renewal: " + policy.getPolicyName() + " (" + policy.getProviderName() + ")";
        String desc = "Policy #" + policy.getPolicyNumber() + " (" + policy.getPolicyType() + ") renewal due on " + policy.getNextRenewalDate();

        Optional<ReminderEntity> existing = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(policy.getId(), REMINDER_TYPE_INSURANCE_RENEWAL);

        ReminderEntity reminder;
        if (existing.isPresent()) {
            reminder = existing.get();
            reminder.setTitle(title);
            reminder.setDescription(desc);
            reminder.setDueAt(dueAt);
            reminder.setStatus(ReminderStatus.ACTIVE);
            reminder.setRecurrencePattern(policy.getPremiumFrequency().name());
        } else {
            reminder = ReminderEntity.builder()
                    .user(user)
                    .title(title)
                    .description(desc)
                    .dueAt(dueAt)
                    .recurrencePattern(policy.getPremiumFrequency().name())
                    .reminderType(REMINDER_TYPE_INSURANCE_RENEWAL)
                    .status(ReminderStatus.ACTIVE)
                    .targetEntityId(policy.getId())
                    .build();
        }
        reminderRepository.save(reminder);
        log.debug("Synchronized renewal reminder [{}] for policy [{}] due at {}", reminder.getId(), policy.getId(), dueAt);
    }
}
