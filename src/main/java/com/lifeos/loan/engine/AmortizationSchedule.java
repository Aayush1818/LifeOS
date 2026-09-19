package com.lifeos.loan.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationSchedule {

    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private int totalTenureMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal totalInterest;
    private BigDecimal totalPayment;
    private List<AmortizationInstallment> installments;
}
