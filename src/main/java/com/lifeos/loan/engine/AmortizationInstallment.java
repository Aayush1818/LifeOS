package com.lifeos.loan.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationInstallment {

    private int installmentNumber;
    private LocalDate dueDate;
    private BigDecimal openingPrincipal;
    private BigDecimal payment;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal closingPrincipal;
}
