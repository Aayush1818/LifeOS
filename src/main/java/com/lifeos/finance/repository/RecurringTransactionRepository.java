package com.lifeos.finance.repository;

import com.lifeos.finance.entity.RecurringStatus;
import com.lifeos.finance.entity.RecurringTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {

    Optional<RecurringTransactionEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    List<RecurringTransactionEntity> findAllByUserIdAndIsDeletedFalse(UUID userId);

    List<RecurringTransactionEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, RecurringStatus status);

    List<RecurringTransactionEntity> findAllByNextDueDateLessThanEqualAndStatusAndIsDeletedFalse(
            LocalDate date,
            RecurringStatus status
    );
}
