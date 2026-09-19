package com.lifeos.asset.repository;

import com.lifeos.asset.entity.InvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, UUID> {

    List<InvoiceItemEntity> findAllByInvoiceIdAndIsDeletedFalse(UUID invoiceId);

    List<InvoiceItemEntity> findAllByAssetIdAndIsDeletedFalse(UUID assetId);
}
