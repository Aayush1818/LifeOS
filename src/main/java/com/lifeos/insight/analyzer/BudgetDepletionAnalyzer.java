package com.lifeos.insight.analyzer;

import com.lifeos.budget.entity.BudgetEntity;
import com.lifeos.budget.repository.BudgetRepository;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionEntity;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.repository.TransactionRepository;
import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetDepletionAnalyzer implements InsightAnalyzer {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public List<InsightEntity> analyze(UserEntity user) {
        List<InsightEntity> insights = new ArrayList<>();
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        List<BudgetEntity> budgets = budgetRepository.findAllByUserIdAndBudgetMonthAndBudgetYearAndIsDeletedFalse(
                user.getId(), month, year);
        if (budgets.isEmpty()) {
            return insights;
        }

        List<TransactionEntity> monthlyTxs = transactionRepository.findAllByUserIdAndTransactionDateBetweenAndIsDeletedFalse(
                user.getId(), startOfMonth, endOfMonth);

        Map<TransactionCategory, BigDecimal> categorySpend = new EnumMap<>(TransactionCategory.class);
        for (TransactionEntity tx : monthlyTxs) {
            if (tx.getTransactionType() == TransactionType.EXPENSE && tx.getCategory() != null) {
                categorySpend.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
            }
        }

        int dayOfMonth = now.getDayOfMonth();
        int lengthOfMonth = now.lengthOfMonth();
        int daysRemaining = lengthOfMonth - dayOfMonth;

        for (BudgetEntity budget : budgets) {
            BigDecimal allocated = budget.getAllocatedAmount();
            if (allocated == null || allocated.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal spent = categorySpend.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
            BigDecimal utilization = spent.divide(allocated, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            if (utilization.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
                InsightSeverity severity;
                if (utilization.compareTo(BigDecimal.valueOf(100.0)) >= 0) {
                    severity = InsightSeverity.CRITICAL;
                } else if (dayOfMonth <= (lengthOfMonth * 0.75)) {
                    severity = InsightSeverity.WARNING;
                } else {
                    severity = InsightSeverity.INFO;
                }

                String catName = budget.getCategory() != null ? budget.getCategory().name() : "GENERAL";
                insights.add(InsightEntity.builder()
                        .user(user)
                        .insightType(InsightType.BUDGET_DEPLETION)
                        .severity(severity)
                        .title("Budget Alert: " + catName + " at " + Math.round(utilization.doubleValue()) + "%")
                        .description(String.format("You have utilized %d%% of your %s budget ($%s of $%s) with %d days remaining in the month.",
                                Math.round(utilization.doubleValue()), catName, spent.toPlainString(), allocated.toPlainString(), daysRemaining))
                        .actionType(InsightActionType.VIEW_BUDGET)
                        .actionPayload(Map.of(
                                "category", catName,
                                "allocatedAmount", allocated,
                                "spentAmount", spent,
                                "utilizationPercent", utilization
                        ))
                        .build());
            }
        }

        return insights;
    }
}
