package com.lifeos.budget.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_cat_month_year", columnNames = {"user_id", "category", "budget_month", "budget_year"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private TransactionCategory category;

    @Column(name = "budget_month", nullable = false)
    private int budgetMonth;

    @Column(name = "budget_year", nullable = false)
    private int budgetYear;

    @Column(name = "allocated_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alert_thresholds", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Integer> alertThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
}
