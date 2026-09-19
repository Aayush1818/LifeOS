package com.lifeos.healthcare.repository;

import com.lifeos.healthcare.entity.AppointmentEntity;
import com.lifeos.healthcare.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID>, JpaSpecificationExecutor<AppointmentEntity> {

    Optional<AppointmentEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<AppointmentEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<AppointmentEntity> findAllByUserIdAndDependentIdAndIsDeletedFalse(UUID userId, UUID dependentId, Pageable pageable);

    Page<AppointmentEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, AppointmentStatus status, Pageable pageable);

    List<AppointmentEntity> findAllByUserIdAndStatusAndAppointmentTimeBetweenAndIsDeletedFalse(
            UUID userId, AppointmentStatus status, OffsetDateTime start, OffsetDateTime end);

    List<AppointmentEntity> findAllByFollowUpToIdAndIsDeletedFalse(UUID followUpToId);
}
