package com.lifeos.budget.repository;

import com.lifeos.budget.entity.BudgetEntity;
import com.lifeos.finance.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {

    Optional<BudgetEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<BudgetEntity> findByUserIdAndCategoryAndBudgetMonthAndBudgetYearAndIsDeletedFalse(
            UUID userId,
            TransactionCategory category,
            int budgetMonth,
            int budgetYear
    );

    List<BudgetEntity> findAllByUserIdAndBudgetMonthAndBudgetYearAndIsDeletedFalse(
            UUID userId,
            int budgetMonth,
            int budgetYear
    );

    List<BudgetEntity> findAllByUserIdAndIsDeletedFalse(UUID userId);
}
