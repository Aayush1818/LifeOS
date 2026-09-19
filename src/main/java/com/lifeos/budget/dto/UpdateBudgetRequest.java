package com.lifeos.budget.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class UpdateBudgetRequest {

    @DecimalMin(value = "0.01", message = "Allocated amount must be strictly positive")
    private BigDecimal allocatedAmount;

    private List<Integer> alertThresholds;
}
