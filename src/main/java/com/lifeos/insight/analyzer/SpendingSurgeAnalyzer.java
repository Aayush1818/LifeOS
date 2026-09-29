package com.lifeos.insight.analyzer;

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
public class SpendingSurgeAnalyzer implements InsightAnalyzer {

    private final TransactionRepository transactionRepository;

    @Override
    public List<InsightEntity> analyze(UserEntity user) {
        List<InsightEntity> insights = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate currentMonthStart = now.withDayOfMonth(1);
        LocalDate trailingStart = currentMonthStart.minusMonths(3);

        // Fetch trailing 3-month transactions
        List<TransactionEntity> trailingTxs = transactionRepository.findAllByUserIdAndTransactionDateBetweenAndIsDeletedFalse(
                user.getId(), trailingStart, currentMonthStart.minusDays(1));

        Map<TransactionCategory, BigDecimal> trailingSums = new EnumMap<>(TransactionCategory.class);
        for (TransactionEntity tx : trailingTxs) {
            if (tx.getTransactionType() == TransactionType.EXPENSE && tx.getCategory() != null) {
                trailingSums.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
            }
        }

        // Fetch current month transactions
        List<TransactionEntity> currentTxs = transactionRepository.findAllByUserIdAndTransactionDateBetweenAndIsDeletedFalse(
                user.getId(), currentMonthStart, now);

        Map<TransactionCategory, BigDecimal> currentSums = new EnumMap<>(TransactionCategory.class);
        for (TransactionEntity tx : currentTxs) {
            if (tx.getTransactionType() == TransactionType.EXPENSE && tx.getCategory() != null) {
                currentSums.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
            }
        }

        for (Map.Entry<TransactionCategory, BigDecimal> entry : currentSums.entrySet()) {
            TransactionCategory cat = entry.getKey();
            BigDecimal currentSpend = entry.getValue();
            BigDecimal trailingTotal = trailingSums.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal trailingMonthlyAvg = trailingTotal.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            if (trailingMonthlyAvg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = trailingMonthlyAvg.multiply(BigDecimal.valueOf(1.50));
                if (currentSpend.compareTo(threshold) > 0 && currentSpend.subtract(trailingMonthlyAvg).compareTo(BigDecimal.valueOf(50)) > 0) {
                    BigDecimal ratio = currentSpend.divide(trailingMonthlyAvg, 2, RoundingMode.HALF_UP);
                    InsightSeverity severity = ratio.compareTo(BigDecimal.valueOf(2.0)) >= 0 ? InsightSeverity.CRITICAL : InsightSeverity.WARNING;

                    insights.add(InsightEntity.builder()
                            .user(user)
                            .insightType(InsightType.SPENDING_SURGE)
                            .severity(severity)
                            .title("Spending Surge in " + cat.name())
                            .description(String.format("You have spent $%s on %s this month, exceeding your 3-month trailing monthly average of $%s by %d%%.",
                                    currentSpend.toPlainString(), cat.name(), trailingMonthlyAvg.toPlainString(), ratio.multiply(BigDecimal.valueOf(100)).intValue() - 100))
                            .actionType(InsightActionType.VIEW_BUDGET)
                            .actionPayload(Map.of(
                                    "category", cat.name(),
                                    "currentSpend", currentSpend,
                                    "trailingMonthlyAvg", trailingMonthlyAvg,
                                    "surgeRatio", ratio
                            ))
                            .build());
                }
            }
        }

        return insights;
    }
}
