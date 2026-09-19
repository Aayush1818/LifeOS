package com.lifeos.asset.repository;

import com.lifeos.asset.entity.AssetCategory;
import com.lifeos.asset.entity.AssetEntity;
import com.lifeos.asset.entity.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {

    Optional<AssetEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<AssetEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<AssetEntity> findAllByUserIdAndCategoryAndIsDeletedFalse(UUID userId, AssetCategory category, Pageable pageable);

    Page<AssetEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, AssetStatus status, Pageable pageable);

    Page<AssetEntity> findAllByUserIdAndDependentIdAndIsDeletedFalse(UUID userId, UUID dependentId, Pageable pageable);

    @Query("SELECT a FROM AssetEntity a WHERE a.user.id = :userId AND a.isDeleted = false " +
            "AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AssetEntity> searchAssets(@Param("userId") UUID userId, @Param("search") String search, Pageable pageable);

    boolean existsByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    interface CurrencyCostProjection {
        String getCurrency();
        BigDecimal getTotalAmount();
        Long getAssetCount();
    }

    @Query("SELECT a.currency AS currency, SUM(a.purchasePrice) AS totalAmount, COUNT(a.id) AS assetCount " +
            "FROM AssetEntity a " +
            "WHERE a.user.id = :userId AND a.isDeleted = false AND a.purchasePrice IS NOT NULL " +
            "AND a.status NOT IN (com.lifeos.asset.entity.AssetStatus.RETURNED, com.lifeos.asset.entity.AssetStatus.DISPOSED) " +
            "GROUP BY a.currency")
    List<CurrencyCostProjection> aggregateAcquisitionCostByCurrency(@Param("userId") UUID userId);

    @Query("SELECT COUNT(a.id) FROM AssetEntity a " +
            "WHERE a.user.id = :userId AND a.isDeleted = false AND a.purchasePrice IS NULL " +
            "AND a.status NOT IN (com.lifeos.asset.entity.AssetStatus.RETURNED, com.lifeos.asset.entity.AssetStatus.DISPOSED)")
    long countAssetsWithUnrecordedCost(@Param("userId") UUID userId);
}
