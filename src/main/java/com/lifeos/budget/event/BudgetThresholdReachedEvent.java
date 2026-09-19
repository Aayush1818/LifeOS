package com.lifeos.budget.event;

import com.lifeos.finance.entity.TransactionCategory;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Decoupled domain event published when a category budget crosses a defined alert threshold.
 * Asynchronous notification listeners can subscribe to this event without coupling
 * the budget domain to any specific notification delivery channel.
 */
@Getter
public class BudgetThresholdReachedEvent extends ApplicationEvent {

    private final UUID userId;
    private final UUID budgetId;
    private final TransactionCategory category;
    private final int thresholdPercentage;
    private final BigDecimal currentUtilization;
    private final BigDecimal allocatedAmount;
    private final BigDecimal actualSpent;

    public BudgetThresholdReachedEvent(
            Object source,
            UUID userId,
            UUID budgetId,
            TransactionCategory category,
            int thresholdPercentage,
            BigDecimal currentUtilization,
            BigDecimal allocatedAmount,
            BigDecimal actualSpent
    ) {
        super(source);
        this.userId = userId;
        this.budgetId = budgetId;
        this.category = category;
        this.thresholdPercentage = thresholdPercentage;
        this.currentUtilization = currentUtilization;
        this.allocatedAmount = allocatedAmount;
        this.actualSpent = actualSpent;
    }
}
