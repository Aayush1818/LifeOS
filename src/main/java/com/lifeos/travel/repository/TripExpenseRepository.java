package com.lifeos.travel.repository;

import com.lifeos.travel.entity.TripExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripExpenseRepository extends JpaRepository<TripExpenseEntity, UUID> {

    List<TripExpenseEntity> findAllByTripIdAndIsDeletedFalseOrderByExpenseDateDesc(UUID tripId);

    Optional<TripExpenseEntity> findByIdAndTripIdAndIsDeletedFalse(UUID id, UUID tripId);
}
