package com.lifeos.asset.repository;

import com.lifeos.asset.entity.AssetStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetStatusHistoryRepository extends JpaRepository<AssetStatusHistoryEntity, UUID> {

    List<AssetStatusHistoryEntity> findAllByAssetIdOrderByChangedAtDesc(UUID assetId);
}
