package com.lifeos.finance.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.finance.dto.CreateRecurringRequest;
import com.lifeos.finance.dto.RecurringResponse;
import com.lifeos.finance.dto.UpdateRecurringRequest;
import com.lifeos.finance.entity.RecurringStatus;
import com.lifeos.finance.entity.RecurringTransactionEntity;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final UserRepository userRepository;

    @Transactional
    public RecurringResponse createRecurring(CreateRecurringRequest request, UUID userId) {
        validateAmount(request.getAmount());

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocalDate nextDue = calculateInitialNextDueDate(request.getStartDate(), request.getBillingDay());

        RecurringTransactionEntity entity = RecurringTransactionEntity.builder()
                .user(user)
                .title(request.getTitle().trim())
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .category(request.getCategory())
                .paymentMethod(request.getPaymentMethod())
                .recurrencePattern(request.getRecurrencePattern())
                .billingDay(request.getBillingDay())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextDueDate(nextDue)
                .status(RecurringStatus.ACTIVE)
                .autoCreateTransaction(request.isAutoCreateTransaction())
                .notes(request.getNotes())
                .build();

        RecurringTransactionEntity saved = recurringRepository.save(entity);
        log.info("User [{}] created recurring transaction rule [{}] ({}, next due: {})",
                userId, saved.getId(), saved.getTitle(), saved.getNextDueDate());

        return RecurringResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public RecurringResponse getRecurring(UUID id, UUID userId) {
        RecurringTransactionEntity entity = recurringRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found with id: " + id));
        return RecurringResponse.fromEntity(entity);
    }

    @Transactional
    public RecurringResponse updateRecurring(UUID id, UpdateRecurringRequest request, UUID userId) {
        RecurringTransactionEntity entity = recurringRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found with id: " + id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            entity.setTitle(request.getTitle().trim());
        }
        if (request.getAmount() != null) {
            validateAmount(request.getAmount());
            entity.setAmount(request.getAmount());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getPaymentMethod() != null) {
            entity.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getRecurrencePattern() != null) {
            entity.setRecurrencePattern(request.getRecurrencePattern());
        }
        if (request.getBillingDay() != null) {
            entity.setBillingDay(request.getBillingDay());
        }
        if (request.getEndDate() != null) {
            entity.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getAutoCreateTransaction() != null) {
            entity.setAutoCreateTransaction(request.getAutoCreateTransaction());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        RecurringTransactionEntity updated = recurringRepository.save(entity);
        log.info("User [{}] updated recurring transaction [{}]", userId, id);
        return RecurringResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteRecurring(UUID id, UUID userId) {
        RecurringTransactionEntity entity = recurringRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found with id: " + id));

        entity.setDeleted(true);
        entity.setStatus(RecurringStatus.COMPLETED);
        recurringRepository.save(entity);
        log.info("User [{}] soft-deleted recurring transaction [{}]", userId, id);
    }

    @Transactional(readOnly = true)
    public List<RecurringResponse> listRecurring(UUID userId) {
        return recurringRepository.findAllByUserIdAndIsDeletedFalse(userId).stream()
                .map(RecurringResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Clean programmatic tool contract for future AI Agent invocation.
     */
    @Transactional(readOnly = true)
    public List<RecurringResponse> getRecurringExpenses(UUID userId) {
        return recurringRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, RecurringStatus.ACTIVE).stream()
                .filter(r -> r.getTransactionType() == TransactionType.EXPENSE)
                .map(RecurringResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private LocalDate calculateInitialNextDueDate(LocalDate startDate, int billingDay) {
        LocalDate today = LocalDate.now();
        LocalDate base = startDate.isAfter(today) ? startDate : today;

        int dayToSet = Math.min(billingDay, base.lengthOfMonth());
        LocalDate candidate = base.withDayOfMonth(dayToSet);

        if (candidate.isBefore(base)) {
            LocalDate nextMonth = base.plusMonths(1);
            dayToSet = Math.min(billingDay, nextMonth.lengthOfMonth());
            candidate = nextMonth.withDayOfMonth(dayToSet);
        }
        return candidate;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be strictly greater than zero");
        }
    }
}
