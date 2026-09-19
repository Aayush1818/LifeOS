package com.lifeos.asset.repository;

import com.lifeos.asset.entity.AssetServiceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetServiceRecordRepository extends JpaRepository<AssetServiceRecordEntity, UUID> {

    Optional<AssetServiceRecordEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    List<AssetServiceRecordEntity> findAllByAssetIdAndUserIdAndIsDeletedFalseOrderByServiceDateDesc(UUID assetId, UUID userId);

    List<AssetServiceRecordEntity> findAllByInvoiceIdAndUserIdAndIsDeletedFalse(UUID invoiceId, UUID userId);
}
