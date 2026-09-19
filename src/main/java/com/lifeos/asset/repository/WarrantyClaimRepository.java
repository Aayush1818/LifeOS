package com.lifeos.asset.repository;

import com.lifeos.asset.entity.ClaimStatus;
import com.lifeos.asset.entity.WarrantyClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaimEntity, UUID> {

    Optional<WarrantyClaimEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    List<WarrantyClaimEntity> findAllByWarrantyIdAndUserIdAndIsDeletedFalse(UUID warrantyId, UUID userId);

    List<WarrantyClaimEntity> findAllByAssetIdAndUserIdAndIsDeletedFalse(UUID assetId, UUID userId);

    List<WarrantyClaimEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, ClaimStatus status);
}
