package com.lifeos.reminder.repository;

import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<ReminderEntity, UUID> {

    Optional<ReminderEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<ReminderEntity> findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(UUID targetEntityId, String reminderType);

    Optional<ReminderEntity> findByTargetEntityIdAndIsDeletedFalse(UUID targetEntityId);

    List<ReminderEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, ReminderStatus status);

    Page<ReminderEntity> findAllByUserIdAndIsDeletedFalseOrderByDueAtAsc(UUID userId, Pageable pageable);

    Page<ReminderEntity> findAllByUserIdAndStatusAndIsDeletedFalseOrderByDueAtAsc(UUID userId, ReminderStatus status, Pageable pageable);

    List<ReminderEntity> findAllByUserIdAndDueAtBetweenAndStatusAndIsDeletedFalseOrderByDueAtAsc(
            UUID userId,
            OffsetDateTime start,
            OffsetDateTime end,
            ReminderStatus status
    );

    List<ReminderEntity> findAllByUserIdAndDueAtBeforeAndStatusAndIsDeletedFalseOrderByDueAtAsc(
            UUID userId,
            OffsetDateTime cutoff,
            ReminderStatus status
    );

    List<ReminderEntity> findAllByStatusAndDueAtLessThanEqualAndIsDeletedFalse(ReminderStatus status, OffsetDateTime cutoff);
}
