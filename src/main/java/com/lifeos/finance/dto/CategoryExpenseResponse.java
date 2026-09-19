package com.lifeos.finance.dto;

import com.lifeos.finance.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseResponse {

    private TransactionCategory category;
    private BigDecimal amount;
    private BigDecimal percentageOfTotal;
    private long transactionCount;
}
