package com.lifeos.budget.dto;

import com.lifeos.budget.entity.BudgetEntity;
import com.lifeos.finance.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private UUID id;
    private TransactionCategory category;
    private int budgetMonth;
    private int budgetYear;
    private BigDecimal allocatedAmount;
    private List<Integer> alertThresholds;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static BudgetResponse fromEntity(BudgetEntity entity) {
        return BudgetResponse.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .budgetMonth(entity.getBudgetMonth())
                .budgetYear(entity.getBudgetYear())
                .allocatedAmount(entity.getAllocatedAmount())
                .alertThresholds(entity.getAlertThresholds())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
