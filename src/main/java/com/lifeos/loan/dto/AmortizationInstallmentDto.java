package com.lifeos.loan.dto;

import com.lifeos.loan.engine.AmortizationInstallment;
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
public class AmortizationInstallmentDto {

    private int installmentNumber;
    private LocalDate dueDate;
    private BigDecimal openingPrincipal;
    private BigDecimal payment;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal closingPrincipal;

    public static AmortizationInstallmentDto fromModel(AmortizationInstallment m) {
        return AmortizationInstallmentDto.builder()
                .installmentNumber(m.getInstallmentNumber())
                .dueDate(m.getDueDate())
                .openingPrincipal(m.getOpeningPrincipal())
                .payment(m.getPayment())
                .principalComponent(m.getPrincipalComponent())
                .interestComponent(m.getInterestComponent())
                .closingPrincipal(m.getClosingPrincipal())
                .build();
    }
}
