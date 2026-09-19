package com.lifeos.budget.dto;

import com.lifeos.finance.entity.TransactionCategory;
import jakarta.validation.constraints.*;
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
public class CreateBudgetRequest {

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private int budgetMonth;

    @Min(value = 2020, message = "Year must be 2020 or later")
    private int budgetYear;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.01", message = "Allocated amount must be strictly positive")
    private BigDecimal allocatedAmount;

    @Builder.Default
    private List<Integer> alertThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
}
