package com.lifeos.finance.obligation.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.finance.entity.RecurringStatus;
import com.lifeos.finance.entity.RecurringTransactionEntity;
import com.lifeos.finance.entity.TransactionEntity;
import com.lifeos.finance.entity.TransactionStatus;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.obligation.dto.MonthlyObligationItemDto;
import com.lifeos.finance.obligation.dto.MonthlyObligationSummaryResponse;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import com.lifeos.finance.repository.TransactionRepository;
import com.lifeos.insurance.entity.InsurancePolicyEntity;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.entity.PremiumFrequency;
import com.lifeos.insurance.repository.InsurancePolicyRepository;
import com.lifeos.loan.entity.LoanEntity;
import com.lifeos.loan.entity.LoanStatus;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.entity.TripStatus;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFinancialObligationService implements FinancialObligationService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TripRepository tripRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public MonthlyObligationSummaryResponse getMonthlyObligations(UUID userId, int month, int year) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12, got: " + month);
        }
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100, got: " + year);
        }

        YearMonth targetYm = YearMonth.of(year, month);
        LocalDate startOfMonth = targetYm.atDay(1);
        LocalDate endOfMonth = targetYm.atEndOfMonth();

        List<MonthlyObligationItemDto> items = new ArrayList<>();
        BigDecimal loanEmisTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal insurancePremiumsTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal recurringBillsTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tripAllocationsTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 1. Active Loan EMIs
        List<LoanEntity> loans = loanRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, LoanStatus.ACTIVE);
        for (LoanEntity loan : loans) {
            if (loan.getStartDate().isAfter(endOfMonth)) {
                continue;
            }
            if (loan.getEndDate() != null && loan.getEndDate().isBefore(startOfMonth)) {
                continue;
            }

            int dueDay = loan.getEmiDueDay() > 0 ? Math.min(loan.getEmiDueDay(), targetYm.lengthOfMonth()) : 1;
            LocalDate dueDate = targetYm.atDay(dueDay);
            BigDecimal emi = loan.getMonthlyEmi() != null ? loan.getMonthlyEmi().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            items.add(MonthlyObligationItemDto.builder()
                    .id(UUID.randomUUID())
                    .title("Loan EMI: " + loan.getLenderName() + " (" + loan.getLoanType() + ")")
                    .category("LOAN_EMI")
                    .dueDate(dueDate)
                    .amount(emi)
                    .currency("USD")
                    .providerOrLender(loan.getLenderName())
                    .sourceEntityId(loan.getId())
                    .status("SCHEDULED")
                    .build());

            loanEmisTotal = loanEmisTotal.add(emi);
        }

        // 2. Insurance Premiums
        List<InsurancePolicyEntity> policies = insurancePolicyRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, PolicyStatus.ACTIVE);
        for (InsurancePolicyEntity policy : policies) {
            if (policy.getStartDate() != null && policy.getStartDate().isAfter(endOfMonth)) {
                continue;
            }
            if (policy.getExpiryDate() != null && policy.getExpiryDate().isBefore(startOfMonth)) {
                continue;
            }

            boolean isDueThisMonth = false;
            PremiumFrequency freq = policy.getPremiumFrequency() != null ? policy.getPremiumFrequency() : PremiumFrequency.ANNUALLY;

            if (freq == PremiumFrequency.MONTHLY) {
                isDueThisMonth = true;
            } else if (freq == PremiumFrequency.QUARTERLY) {
                int startMonth = policy.getStartDate() != null ? policy.getStartDate().getMonthValue() : 1;
                isDueThisMonth = Math.abs(month - startMonth) % 3 == 0;
            } else if (freq == PremiumFrequency.SEMI_ANNUALLY) {
                int startMonth = policy.getStartDate() != null ? policy.getStartDate().getMonthValue() : 1;
                isDueThisMonth = Math.abs(month - startMonth) % 6 == 0;
            } else { // ANNUALLY
                if (policy.getNextRenewalDate() != null) {
                    isDueThisMonth = policy.getNextRenewalDate().getMonthValue() == month;
                } else if (policy.getStartDate() != null) {
                    isDueThisMonth = policy.getStartDate().getMonthValue() == month;
                }
            }

            if (isDueThisMonth) {
                int day = policy.getNextRenewalDate() != null ? policy.getNextRenewalDate().getDayOfMonth() :
                        (policy.getStartDate() != null ? policy.getStartDate().getDayOfMonth() : 1);
                LocalDate dueDate = targetYm.atDay(Math.min(day, targetYm.lengthOfMonth()));
                BigDecimal premium = policy.getPremiumAmount() != null ? policy.getPremiumAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

                items.add(MonthlyObligationItemDto.builder()
                        .id(UUID.randomUUID())
                        .title("Insurance Premium: " + policy.getPolicyName() + " (" + policy.getPolicyType() + ")")
                        .category("INSURANCE_PREMIUM")
                        .dueDate(dueDate)
                        .amount(premium)
                        .currency("USD")
                        .providerOrLender(policy.getProviderName())
                        .sourceEntityId(policy.getId())
                        .status("SCHEDULED")
                        .build());

                insurancePremiumsTotal = insurancePremiumsTotal.add(premium);
            }
        }

        // 3. Recurring Bills / Subscriptions (Expense)
        List<RecurringTransactionEntity> recurringList = recurringTransactionRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, RecurringStatus.ACTIVE);
        for (RecurringTransactionEntity rec : recurringList) {
            if (rec.getTransactionType() != TransactionType.EXPENSE) {
                continue;
            }
            if (rec.getStartDate().isAfter(endOfMonth)) {
                continue;
            }
            if (rec.getEndDate() != null && rec.getEndDate().isBefore(startOfMonth)) {
                continue;
            }

            int billingDay = rec.getBillingDay() > 0 ? Math.min(rec.getBillingDay(), targetYm.lengthOfMonth()) :
                    (rec.getNextDueDate() != null ? Math.min(rec.getNextDueDate().getDayOfMonth(), targetYm.lengthOfMonth()) : 1);
            LocalDate dueDate = targetYm.atDay(billingDay);
            BigDecimal amount = rec.getAmount() != null ? rec.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            items.add(MonthlyObligationItemDto.builder()
                    .id(UUID.randomUUID())
                    .title(rec.getTitle())
                    .category(rec.getCategory() != null ? rec.getCategory().name() : "RECURRING_BILL")
                    .dueDate(dueDate)
                    .amount(amount)
                    .currency("USD")
                    .providerOrLender(rec.getPaymentMethod() != null ? rec.getPaymentMethod().name() : "AUTO_DEBIT")
                    .sourceEntityId(rec.getId())
                    .status("SCHEDULED")
                    .build());

            recurringBillsTotal = recurringBillsTotal.add(amount);
        }

        // 4. Trips / Travel Allocations
        List<TripEntity> trips = tripRepository.findAllByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsDeletedFalse(userId, endOfMonth, startOfMonth);
        for (TripEntity trip : trips) {
            if (trip.getStatus() == TripStatus.CANCELLED) {
                continue;
            }
            BigDecimal budget = trip.getTotalBudget() != null ? trip.getTotalBudget() : BigDecimal.ZERO;
            BigDecimal actual = trip.getActualSpend() != null ? trip.getActualSpend() : BigDecimal.ZERO;
            BigDecimal remaining = budget.subtract(actual);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                LocalDate dueDate = trip.getStartDate().isAfter(startOfMonth) ? trip.getStartDate() : startOfMonth;
                BigDecimal allocation = remaining.setScale(2, RoundingMode.HALF_UP);

                items.add(MonthlyObligationItemDto.builder()
                        .id(UUID.randomUUID())
                        .title("Travel Budget: " + trip.getTripTitle() + " (" + trip.getDestination() + ")")
                        .category("TRAVEL")
                        .dueDate(dueDate)
                        .amount(allocation)
                        .currency(trip.getCurrency() != null ? trip.getCurrency() : "USD")
                        .providerOrLender(trip.getDestination())
                        .sourceEntityId(trip.getId())
                        .status(trip.getStatus().name())
                        .build());

                tripAllocationsTotal = tripAllocationsTotal.add(allocation);
            }
        }

        // 5. Projected Income Calculation
        BigDecimal recurringIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (RecurringTransactionEntity rec : recurringList) {
            if (rec.getTransactionType() == TransactionType.INCOME) {
                BigDecimal inc = rec.getAmount() != null ? rec.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                recurringIncome = recurringIncome.add(inc);
            }
        }

        List<TransactionEntity> postedTxs = transactionRepository.findAllByUserIdAndTransactionDateBetweenAndIsDeletedFalse(userId, startOfMonth, endOfMonth);
        BigDecimal postedIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (TransactionEntity tx : postedTxs) {
            if (tx.getTransactionType() == TransactionType.INCOME && tx.getStatus() == TransactionStatus.POSTED) {
                BigDecimal txAmt = tx.getAmount() != null ? tx.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                postedIncome = postedIncome.add(txAmt);
            }
        }

        BigDecimal projectedIncome = postedIncome.compareTo(recurringIncome) > 0 ? postedIncome : recurringIncome;
        if (projectedIncome.compareTo(BigDecimal.ZERO) == 0) {
            // Check previous month as baseline
            YearMonth prevYm = targetYm.minusMonths(1);
            List<TransactionEntity> prevTxs = transactionRepository.findAllByUserIdAndTransactionDateBetweenAndIsDeletedFalse(
                    userId, prevYm.atDay(1), prevYm.atEndOfMonth());
            for (TransactionEntity tx : prevTxs) {
                if (tx.getTransactionType() == TransactionType.INCOME && tx.getStatus() == TransactionStatus.POSTED) {
                    BigDecimal txAmt = tx.getAmount() != null ? tx.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    projectedIncome = projectedIncome.add(txAmt);
                }
            }
        }

        // 6. Sort items by dueDate ascending, then amount descending
        items.sort(Comparator.comparing(MonthlyObligationItemDto::getDueDate)
                .thenComparing(MonthlyObligationItemDto::getAmount, Comparator.reverseOrder()));

        BigDecimal totalObligations = loanEmisTotal.add(insurancePremiumsTotal)
                .add(recurringBillsTotal)
                .add(tripAllocationsTotal)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSurplusOrDeficit = projectedIncome.subtract(totalObligations).setScale(2, RoundingMode.HALF_UP);

        return MonthlyObligationSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalObligationAmount(totalObligations)
                .loanEmisTotal(loanEmisTotal)
                .insurancePremiumsTotal(insurancePremiumsTotal)
                .recurringBillsTotal(recurringBillsTotal)
                .tripAllocationsTotal(tripAllocationsTotal)
                .projectedIncome(projectedIncome)
                .netSurplusOrDeficit(netSurplusOrDeficit)
                .currency("USD")
                .items(items)
                .build();
    }
}
