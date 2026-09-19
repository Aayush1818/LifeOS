package com.lifeos.travel.repository;

import com.lifeos.travel.entity.TripTravelerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripTravelerRepository extends JpaRepository<TripTravelerEntity, UUID> {

    List<TripTravelerEntity> findAllByTripIdOrderByCreatedAtAsc(UUID tripId);

    Optional<TripTravelerEntity> findByIdAndTripId(UUID id, UUID tripId);

    Optional<TripTravelerEntity> findByTripIdAndDependentId(UUID tripId, UUID dependentId);

    void deleteAllByTripId(UUID tripId);
}
