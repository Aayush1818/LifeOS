package com.lifeos.asset.repository;

import com.lifeos.asset.entity.WarrantyEntity;
import com.lifeos.asset.entity.WarrantyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarrantyRepository extends JpaRepository<WarrantyEntity, UUID> {

    Optional<WarrantyEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    List<WarrantyEntity> findAllByAssetIdAndUserIdAndIsDeletedFalse(UUID assetId, UUID userId);

    List<WarrantyEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, WarrantyStatus status);

    List<WarrantyEntity> findAllByUserIdAndStatusAndExpiryDateBetweenAndIsDeletedFalse(
            UUID userId, WarrantyStatus status, LocalDate startDate, LocalDate endDate);

    Optional<WarrantyEntity> findByReminderId(UUID reminderId);
}
