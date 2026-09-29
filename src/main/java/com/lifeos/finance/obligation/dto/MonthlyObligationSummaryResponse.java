package com.lifeos.finance.obligation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyObligationSummaryResponse {
    private int month;
    private int year;
    private BigDecimal totalObligationAmount;
    private BigDecimal loanEmisTotal;
    private BigDecimal insurancePremiumsTotal;
    private BigDecimal recurringBillsTotal;
    private BigDecimal tripAllocationsTotal;
    private BigDecimal projectedIncome;
    private BigDecimal netSurplusOrDeficit;
    @Builder.Default
    private String currency = "USD";
    @Builder.Default
    private List<MonthlyObligationItemDto> items = new ArrayList<>();
}
