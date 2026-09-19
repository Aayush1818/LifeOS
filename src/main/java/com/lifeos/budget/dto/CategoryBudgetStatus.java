package com.lifeos.budget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifeos.finance.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBudgetStatus {

    private UUID budgetId;
    private TransactionCategory category;
    private BigDecimal allocatedAmount;
    private BigDecimal actualSpent;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercentage;
    private BigDecimal projectedSpend;

    @JsonProperty("isOverBudget")
    private boolean isOverBudget;

    private Integer highestTriggeredThreshold;
}
