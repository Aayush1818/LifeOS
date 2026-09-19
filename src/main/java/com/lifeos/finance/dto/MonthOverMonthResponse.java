package com.lifeos.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthOverMonthResponse {

    private BigDecimal currentMonthExpenses;
    private BigDecimal priorMonthExpenses;
    private BigDecimal deltaAmount;
    private BigDecimal percentageChange;
    private String direction; // INCREASED, DECREASED, UNCHANGED
}
