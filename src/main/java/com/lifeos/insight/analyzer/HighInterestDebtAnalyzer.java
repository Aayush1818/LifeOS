package com.lifeos.insight.analyzer;

import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.loan.entity.LoanEntity;
import com.lifeos.loan.entity.LoanStatus;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HighInterestDebtAnalyzer implements InsightAnalyzer {

    private final LoanRepository loanRepository;

    @Override
    public List<InsightEntity> analyze(UserEntity user) {
        List<InsightEntity> insights = new ArrayList<>();
        List<LoanEntity> loans = loanRepository.findAllByUserIdAndStatusAndIsDeletedFalse(user.getId(), LoanStatus.ACTIVE);

        for (LoanEntity loan : loans) {
            BigDecimal rate = loan.getInterestRate();
            if (rate != null && rate.compareTo(BigDecimal.valueOf(7.50)) >= 0) {
                InsightSeverity severity = rate.compareTo(BigDecimal.valueOf(10.00)) >= 0
                        ? InsightSeverity.WARNING : InsightSeverity.INFO;

                BigDecimal balance = loan.getOutstandingBalance() != null ? loan.getOutstandingBalance() : BigDecimal.ZERO;

                insights.add(InsightEntity.builder()
                        .user(user)
                        .insightType(InsightType.HIGH_INTEREST_LOAN)
                        .severity(severity)
                        .title(String.format("Debt Optimization: %s (%s%% APR)", loan.getLenderName(), rate.toPlainString()))
                        .description(String.format("Your %s loan with %s has a high interest rate of %s%% with $%s outstanding. Simulating lump-sum prepayments can significantly reduce your tenure and total interest paid.",
                                loan.getLoanType(), loan.getLenderName(), rate.toPlainString(), balance.toPlainString()))
                        .actionType(InsightActionType.SIMULATE_PREPAYMENT)
                        .actionPayload(Map.of(
                                "loanId", loan.getId().toString(),
                                "interestRate", rate,
                                "outstandingBalance", balance,
                                "lenderName", loan.getLenderName()
                        ))
                        .build());
            }
        }

        return insights;
    }
}
