package com.lifeos.loan.dto;

import com.lifeos.loan.engine.AmortizationSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationScheduleResponse {

    private UUID loanId;
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private int totalTenureMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal totalInterest;
    private BigDecimal totalPayment;
    private List<AmortizationInstallmentDto> installments;

    public static AmortizationScheduleResponse fromModel(UUID loanId, AmortizationSchedule schedule) {
        return AmortizationScheduleResponse.builder()
                .loanId(loanId)
                .principal(schedule.getPrincipal())
                .annualInterestRate(schedule.getAnnualInterestRate())
                .totalTenureMonths(schedule.getTotalTenureMonths())
                .monthlyEmi(schedule.getMonthlyEmi())
                .totalInterest(schedule.getTotalInterest())
                .totalPayment(schedule.getTotalPayment())
                .installments(schedule.getInstallments().stream()
                        .map(AmortizationInstallmentDto::fromModel)
                        .toList())
                .build();
    }
}
