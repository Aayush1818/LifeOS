package com.lifeos.loan.engine;

import com.lifeos.loan.entity.PrepaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure deterministic domain engine for loan amortization calculations.
 * <p>
 * Evaluates standard reducing-balance installments using {@link MathContext#DECIMAL128}
 * for intermediate ratio precision and {@link RoundingMode#HALF_UP} with scale 2
 * for all monetary boundaries.
 * <p>
 * Guarantees zero float/double usage and absolute mathematical reconciliation:
 * <ul>
 *     <li>Sum of principal components == original principal</li>
 *     <li>Closing principal after final installment == 0.00</li>
 *     <li>Total payments == principal + total interest</li>
 * </ul>
 */
@Component
public class LoanAmortizationEngine {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    /**
     * Computes the Equated Monthly Installment (EMI) for a fixed-rate reducing-balance loan.
     *
     * @param principal          Initial borrowed principal amount (> 0)
     * @param annualInterestRate Annual interest rate percentage [0, 100]
     * @param tenureMonths       Total loan duration in months (> 0)
     * @return Monthly EMI rounded to 2 decimal places using HALF_UP
     */
    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        validateInputs(principal, annualInterestRate, tenureMonths);

        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), MONETARY_SCALE, ROUNDING);
        }

        // r = annualInterestRate / (12 * 100)
        BigDecimal monthlyRate = annualInterestRate.divide(TWELVE_HUNDRED, MC);

        // (1 + r)^n
        BigDecimal onePlusRPowN = BigDecimal.ONE.add(monthlyRate, MC).pow(tenureMonths, MC);

        // Numerator: P * r * (1 + r)^n
        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(onePlusRPowN, MC);

        // Denominator: (1 + r)^n - 1
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);

        return numerator.divide(denominator, MC).setScale(MONETARY_SCALE, ROUNDING);
    }

    /**
     * Generates a complete mathematical amortization schedule with final installment reconciliation.
     *
     * @param principal          Initial principal amount
     * @param annualInterestRate Annual interest rate percentage
     * @param tenureMonths       Total loan duration in months
     * @param startDate          Loan start date
     * @param emiDueDay          Preferred day of the month for installment due dates (1-31)
     * @return Fully reconciled AmortizationSchedule
     */
    public AmortizationSchedule generateSchedule(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            int tenureMonths,
            LocalDate startDate,
            int emiDueDay
    ) {
        validateInputs(principal, annualInterestRate, tenureMonths);
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (emiDueDay < 1 || emiDueDay > 31) {
            throw new IllegalArgumentException("EMI due day must be between 1 and 31");
        }

        BigDecimal monthlyRate = annualInterestRate.compareTo(BigDecimal.ZERO) > 0
                ? annualInterestRate.divide(TWELVE_HUNDRED, MC)
                : BigDecimal.ZERO;

        BigDecimal emi = calculateEmi(principal, annualInterestRate, tenureMonths);

        List<AmortizationInstallment> installments = new ArrayList<>(tenureMonths);
        BigDecimal openingPrincipal = principal.setScale(MONETARY_SCALE, ROUNDING);
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);
        BigDecimal totalPrincipalAccounted = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);

        for (int k = 1; k <= tenureMonths; k++) {
            LocalDate monthTarget = startDate.plusMonths(k);
            int validDay = Math.min(emiDueDay, monthTarget.lengthOfMonth());
            LocalDate dueDate = monthTarget.withDayOfMonth(validDay);

            BigDecimal interestComponent = openingPrincipal.multiply(monthlyRate, MC).setScale(MONETARY_SCALE, ROUNDING);

            BigDecimal principalComponent;
            BigDecimal payment;
            BigDecimal closingPrincipal;

            if (k < tenureMonths) {
                principalComponent = emi.subtract(interestComponent);

                // If scheduled principal exceeds remaining balance, cap it
                if (principalComponent.compareTo(openingPrincipal) >= 0) {
                    principalComponent = openingPrincipal;
                    payment = principalComponent.add(interestComponent);
                    closingPrincipal = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);
                } else {
                    payment = emi;
                    closingPrincipal = openingPrincipal.subtract(principalComponent).setScale(MONETARY_SCALE, ROUNDING);
                }
            } else {
                // Final installment reconciliation: exact remaining principal is paid off
                principalComponent = openingPrincipal;
                payment = principalComponent.add(interestComponent);
                closingPrincipal = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);
            }

            installments.add(AmortizationInstallment.builder()
                    .installmentNumber(k)
                    .dueDate(dueDate)
                    .openingPrincipal(openingPrincipal)
                    .payment(payment)
                    .principalComponent(principalComponent)
                    .interestComponent(interestComponent)
                    .closingPrincipal(closingPrincipal)
                    .build());

            totalInterest = totalInterest.add(interestComponent);
            totalPrincipalAccounted = totalPrincipalAccounted.add(principalComponent);
            openingPrincipal = closingPrincipal;

            if (closingPrincipal.compareTo(BigDecimal.ZERO) == 0 && k < tenureMonths) {
                // Loan paid off early
                break;
            }
        }

        BigDecimal totalPayment = totalPrincipalAccounted.add(totalInterest);

        // Strict Invariant Verification
        validateInvariants(principal, installments, totalPrincipalAccounted, totalInterest, totalPayment);

        return AmortizationSchedule.builder()
                .principal(principal.setScale(MONETARY_SCALE, ROUNDING))
                .annualInterestRate(annualInterestRate.setScale(MONETARY_SCALE, ROUNDING))
                .totalTenureMonths(installments.size())
                .monthlyEmi(emi)
                .totalInterest(totalInterest)
                .totalPayment(totalPayment)
                .installments(installments)
                .build();
    }

    /**
     * Recalculates remaining amortization installments following an ad-hoc partial prepayment.
     *
     * @param remainingPrincipal   Principal remaining immediately after prepayment is applied (> 0)
     * @param annualInterestRate   Annual interest rate percentage
     * @param remainingTenureMonths Months remaining under original agreement
     * @param currentEmi           Current regular monthly EMI
     * @param strategy             Strategy for recalculation (REDUCE_TENURE or REDUCE_EMI)
     * @param nextDueDate          Next upcoming installment due date
     * @param startInstallmentNo   The installment sequence number to begin counting from
     * @return Recalculated schedule for remaining loan duration
     */
    public AmortizationSchedule recalculateAfterPrepayment(
            BigDecimal remainingPrincipal,
            BigDecimal annualInterestRate,
            int remainingTenureMonths,
            BigDecimal currentEmi,
            PrepaymentStrategy strategy,
            LocalDate nextDueDate,
            int startInstallmentNo
    ) {
        validateInputs(remainingPrincipal, annualInterestRate, remainingTenureMonths);
        if (currentEmi == null || currentEmi.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Current EMI must be positive");
        }
        if (strategy == null) {
            strategy = PrepaymentStrategy.REDUCE_TENURE;
        }

        BigDecimal monthlyRate = annualInterestRate.compareTo(BigDecimal.ZERO) > 0
                ? annualInterestRate.divide(TWELVE_HUNDRED, MC)
                : BigDecimal.ZERO;

        BigDecimal effectiveEmi;
        int effectiveTenure;

        if (strategy == PrepaymentStrategy.REDUCE_EMI) {
            effectiveEmi = calculateEmi(remainingPrincipal, annualInterestRate, remainingTenureMonths);
            effectiveTenure = remainingTenureMonths;
        } else {
            effectiveEmi = currentEmi;
            // With fixed EMI and reduced principal, tenure naturally shortens
            effectiveTenure = remainingTenureMonths;
        }

        List<AmortizationInstallment> installments = new ArrayList<>();
        BigDecimal openingPrincipal = remainingPrincipal.setScale(MONETARY_SCALE, ROUNDING);
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);
        BigDecimal totalPrincipalAccounted = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);

        int k = 0;
        while (openingPrincipal.compareTo(BigDecimal.ZERO) > 0 && k < 1200) { // Safety ceiling of 100 years
            k++;
            LocalDate dueDate = nextDueDate.plusMonths(k - 1);
            BigDecimal interestComponent = openingPrincipal.multiply(monthlyRate, MC).setScale(MONETARY_SCALE, ROUNDING);

            BigDecimal principalComponent;
            BigDecimal payment;
            BigDecimal closingPrincipal;

            if (strategy == PrepaymentStrategy.REDUCE_EMI && k == effectiveTenure) {
                // Final month under REDUCE_EMI
                principalComponent = openingPrincipal;
                payment = principalComponent.add(interestComponent);
                closingPrincipal = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);
            } else {
                BigDecimal candidatePrincipal = effectiveEmi.subtract(interestComponent);
                if (candidatePrincipal.compareTo(openingPrincipal) >= 0) {
                    // Payoff reached
                    principalComponent = openingPrincipal;
                    payment = principalComponent.add(interestComponent);
                    closingPrincipal = BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING);
                } else {
                    principalComponent = candidatePrincipal;
                    payment = effectiveEmi;
                    closingPrincipal = openingPrincipal.subtract(principalComponent).setScale(MONETARY_SCALE, ROUNDING);
                }
            }

            installments.add(AmortizationInstallment.builder()
                    .installmentNumber(startInstallmentNo + k - 1)
                    .dueDate(dueDate)
                    .openingPrincipal(openingPrincipal)
                    .payment(payment)
                    .principalComponent(principalComponent)
                    .interestComponent(interestComponent)
                    .closingPrincipal(closingPrincipal)
                    .build());

            totalInterest = totalInterest.add(interestComponent);
            totalPrincipalAccounted = totalPrincipalAccounted.add(principalComponent);
            openingPrincipal = closingPrincipal;

            if (closingPrincipal.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
        }

        BigDecimal totalPayment = totalPrincipalAccounted.add(totalInterest);

        validateInvariants(remainingPrincipal, installments, totalPrincipalAccounted, totalInterest, totalPayment);

        return AmortizationSchedule.builder()
                .principal(remainingPrincipal.setScale(MONETARY_SCALE, ROUNDING))
                .annualInterestRate(annualInterestRate.setScale(MONETARY_SCALE, ROUNDING))
                .totalTenureMonths(installments.size())
                .monthlyEmi(effectiveEmi)
                .totalInterest(totalInterest)
                .totalPayment(totalPayment)
                .installments(installments)
                .build();
    }

    private void validateInputs(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan principal must be strictly positive");
        }
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) < 0 || annualInterestRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Annual interest rate must be between 0% and 100%");
        }
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Loan tenure months must be strictly positive");
        }
    }

    private void validateInvariants(
            BigDecimal expectedPrincipal,
            List<AmortizationInstallment> installments,
            BigDecimal sumPrincipal,
            BigDecimal sumInterest,
            BigDecimal sumPayment
    ) {
        if (installments.isEmpty()) {
            throw new IllegalStateException("Amortization schedule produced zero installments");
        }

        // Invariant 1: Sum of principal components == original principal
        if (sumPrincipal.compareTo(expectedPrincipal.setScale(MONETARY_SCALE, ROUNDING)) != 0) {
            throw new IllegalStateException(String.format(
                    "Mathematical invariant failure: sum of principal components (%s) != expected principal (%s)",
                    sumPrincipal, expectedPrincipal));
        }

        // Invariant 2: Closing principal of final installment == 0.00
        AmortizationInstallment last = installments.get(installments.size() - 1);
        if (last.getClosingPrincipal().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException(String.format(
                    "Mathematical invariant failure: closing principal of final installment (%s) != 0.00",
                    last.getClosingPrincipal()));
        }

        // Invariant 3: Sum of all payments == total principal + total interest
        BigDecimal expectedTotalPayment = sumPrincipal.add(sumInterest);
        if (sumPayment.compareTo(expectedTotalPayment) != 0) {
            throw new IllegalStateException(String.format(
                    "Mathematical invariant failure: sum of payments (%s) != principal + interest (%s)",
                    sumPayment, expectedTotalPayment));
        }
    }
}
