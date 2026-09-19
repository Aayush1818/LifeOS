package com.lifeos.travel.repository;

import com.lifeos.travel.entity.ItineraryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryItemRepository extends JpaRepository<ItineraryItemEntity, UUID> {

    Optional<ItineraryItemEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<ItineraryItemEntity> findByIdAndTripIdAndUserIdAndIsDeletedFalse(UUID id, UUID tripId, UUID userId);

    List<ItineraryItemEntity> findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(UUID tripId, UUID userId);

    List<ItineraryItemEntity> findAllByUserIdAndBookingReferenceAndIsDeletedFalse(UUID userId, String bookingReference);

    List<ItineraryItemEntity> findAllByUserIdAndStartTimeBetweenAndIsDeletedFalseOrderByStartTimeAsc(UUID userId, OffsetDateTime start, OffsetDateTime end);
}
