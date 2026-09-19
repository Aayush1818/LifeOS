package com.lifeos.finance.repository;

import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionEntity;
import com.lifeos.finance.entity.TransactionStatus;
import com.lifeos.finance.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {

    Optional<TransactionEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<TransactionEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE t.user.id = :userId
          AND t.isDeleted = false
          AND (:type IS NULL OR t.transactionType = :type)
          AND (:category IS NULL OR t.category = :category)
          AND (:status IS NULL OR t.status = :status)
          AND (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
    """)
    Page<TransactionEntity> findFiltered(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("category") TransactionCategory category,
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    boolean existsByUserIdAndAmountAndCategoryAndTransactionDateAndIsDeletedFalse(
            UUID userId,
            BigDecimal amount,
            TransactionCategory category,
            LocalDate transactionDate
    );

    List<TransactionEntity> findAllByUserIdAndTransactionDateBetweenAndIsDeletedFalse(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
