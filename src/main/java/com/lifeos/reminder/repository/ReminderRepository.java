package com.lifeos.reminder.repository;

import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
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

    List<ReminderEntity> findAllByStatusAndDueAtLessThanEqualAndIsDeletedFalse(ReminderStatus status, OffsetDateTime cutoff);
}
