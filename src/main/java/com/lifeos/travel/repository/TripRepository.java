package com.lifeos.travel.repository;

import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.entity.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<TripEntity, UUID> {

    Optional<TripEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<TripEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<TripEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, TripStatus status, Pageable pageable);

    List<TripEntity> findAllByUserIdAndStartDateBetweenAndIsDeletedFalse(UUID userId, LocalDate start, LocalDate end);

    List<TripEntity> findAllByUserIdAndEndDateGreaterThanEqualAndIsDeletedFalseOrderByStartDateAsc(UUID userId, LocalDate today);

    List<TripEntity> findAllByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsDeletedFalse(
            UUID userId, LocalDate endOfMonth, LocalDate startOfMonth);
}
