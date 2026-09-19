package com.lifeos.budget.service;

import com.lifeos.budget.dto.*;
import com.lifeos.budget.entity.BudgetEntity;
import com.lifeos.budget.event.BudgetThresholdReachedEvent;
import com.lifeos.budget.repository.BudgetRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.repository.FinanceAnalyticsRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final FinanceAnalyticsRepository analyticsRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BudgetResponse setBudget(CreateBudgetRequest request, UUID userId) {
        validateAmount(request.getAllocatedAmount());

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<BudgetEntity> existing = budgetRepository.findByUserIdAndCategoryAndBudgetMonthAndBudgetYearAndIsDeletedFalse(
                userId, request.getCategory(), request.getBudgetMonth(), request.getBudgetYear());

        BudgetEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setAllocatedAmount(request.getAllocatedAmount());
            if (request.getAlertThresholds() != null && !request.getAlertThresholds().isEmpty()) {
                entity.setAlertThresholds(request.getAlertThresholds());
            }
            log.info("User [{}] updated existing budget [{}] for {}/{}", userId, entity.getId(), request.getBudgetMonth(), request.getBudgetYear());
        } else {
            entity = BudgetEntity.builder()
                    .user(user)
                    .category(request.getCategory())
                    .budgetMonth(request.getBudgetMonth())
                    .budgetYear(request.getBudgetYear())
                    .allocatedAmount(request.getAllocatedAmount())
                    .alertThresholds(request.getAlertThresholds() != null ? request.getAlertThresholds() : List.of(50, 75, 90, 100))
                    .build();
            log.info("User [{}] created new budget for category [{}] in {}/{}", userId, request.getCategory(), request.getBudgetMonth(), request.getBudgetYear());
        }

        BudgetEntity saved = budgetRepository.save(entity);
        return BudgetResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID id, UUID userId) {
        BudgetEntity entity = budgetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        return BudgetResponse.fromEntity(entity);
    }

    @Transactional
    public BudgetResponse updateBudget(UUID id, UpdateBudgetRequest request, UUID userId) {
        BudgetEntity entity = budgetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));

        if (request.getAllocatedAmount() != null) {
            validateAmount(request.getAllocatedAmount());
            entity.setAllocatedAmount(request.getAllocatedAmount());
        }
        if (request.getAlertThresholds() != null && !request.getAlertThresholds().isEmpty()) {
            entity.setAlertThresholds(request.getAlertThresholds());
        }

        BudgetEntity updated = budgetRepository.save(entity);
        log.info("User [{}] updated budget [{}]", userId, id);
        return BudgetResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteBudget(UUID id, UUID userId) {
        BudgetEntity entity = budgetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));

        entity.setDeleted(true);
        budgetRepository.save(entity);
        log.info("User [{}] deleted budget [{}]", userId, id);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> listBudgets(UUID userId, int month, int year) {
        return budgetRepository.findAllByUserIdAndBudgetMonthAndBudgetYearAndIsDeletedFalse(userId, month, year)
                .stream()
                .map(BudgetResponse::fromEntity)
                .toList();
    }

    /**
     * Deterministic calculation of budget vs actuals, remaining amounts,
     * percentage utilization, in-flight projected spending, and threshold evaluation.
     */
    @Transactional(readOnly = true)
    public BudgetStatusResponse getBudgetStatus(UUID userId, int month, int year) {
        YearMonth targetPeriod = YearMonth.of(year, month);
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();

        List<BudgetEntity> budgets = budgetRepository.findAllByUserIdAndBudgetMonthAndBudgetYearAndIsDeletedFalse(userId, month, year);
        Map<TransactionCategory, BigDecimal> spendingMap = analyticsRepository.getActualSpendingPerCategory(userId, startDate, endDate);

        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        List<CategoryBudgetStatus> categoryStatuses = new ArrayList<>();

        LocalDate today = LocalDate.now();
        YearMonth currentPeriod = YearMonth.from(today);

        for (BudgetEntity b : budgets) {
            BigDecimal allocated = b.getAllocatedAmount();
            BigDecimal spent = spendingMap.getOrDefault(b.getCategory(), BigDecimal.ZERO);
            BigDecimal remaining = allocated.subtract(spent);

            // Utilization percentage calculation with zero-budget edge case protection
            BigDecimal utilization;
            if (allocated.compareTo(BigDecimal.ZERO) > 0) {
                utilization = spent.multiply(BigDecimal.valueOf(100))
                        .divide(allocated, 2, RoundingMode.HALF_UP);
            } else {
                utilization = spent.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100.00) : BigDecimal.ZERO;
            }

            boolean isOverBudget = spent.compareTo(allocated) > 0;

            // In-flight projected spending calculation
            BigDecimal projected;
            if (targetPeriod.isBefore(currentPeriod)) {
                // Historical month: final spent is the projected spend
                projected = spent;
            } else if (targetPeriod.isAfter(currentPeriod)) {
                // Future month
                projected = allocated;
            } else {
                // Current in-flight month: run-rate projection
                int daysElapsed = Math.max(1, today.getDayOfMonth());
                int totalDays = targetPeriod.lengthOfMonth();
                projected = spent.divide(BigDecimal.valueOf(daysElapsed), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(totalDays))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Threshold evaluation
            Integer highestTriggered = null;
            if (b.getAlertThresholds() != null) {
                List<Integer> sortedThresholds = b.getAlertThresholds().stream()
                        .sorted(Comparator.reverseOrder())
                        .toList();
                for (int threshold : sortedThresholds) {
                    if (utilization.compareTo(BigDecimal.valueOf(threshold)) >= 0) {
                        highestTriggered = threshold;
                        break;
                    }
                }
            }

            if (highestTriggered != null) {
                eventPublisher.publishEvent(new BudgetThresholdReachedEvent(
                        this, userId, b.getId(), b.getCategory(), highestTriggered, utilization, allocated, spent
                ));
            }

            categoryStatuses.add(CategoryBudgetStatus.builder()
                    .budgetId(b.getId())
                    .category(b.getCategory())
                    .allocatedAmount(allocated)
                    .actualSpent(spent)
                    .remainingAmount(remaining)
                    .utilizationPercentage(utilization)
                    .projectedSpend(projected)
                    .isOverBudget(isOverBudget)
                    .highestTriggeredThreshold(highestTriggered)
                    .build());

            totalAllocated = totalAllocated.add(allocated);
            totalSpent = totalSpent.add(spent);
        }

        BigDecimal totalRemaining = totalAllocated.subtract(totalSpent);
        BigDecimal overallUtilization = totalAllocated.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.multiply(BigDecimal.valueOf(100)).divide(totalAllocated, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return BudgetStatusResponse.builder()
                .month(month)
                .year(year)
                .totalAllocated(totalAllocated)
                .totalSpent(totalSpent)
                .totalRemaining(totalRemaining)
                .overallUtilizationPercentage(overallUtilization)
                .categories(categoryStatuses)
                .build();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget allocated amount must be strictly positive");
        }
    }
}
