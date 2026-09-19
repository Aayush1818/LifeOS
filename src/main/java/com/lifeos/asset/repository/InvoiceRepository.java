package com.lifeos.asset.repository;

import com.lifeos.asset.entity.InvoiceEntity;
import com.lifeos.asset.entity.InvoicePaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<InvoiceEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<InvoiceEntity> findAllByUserIdAndVendorNameContainingIgnoreCaseAndIsDeletedFalse(UUID userId, String vendorName, Pageable pageable);

    Page<InvoiceEntity> findAllByUserIdAndPaymentStatusAndIsDeletedFalse(UUID userId, InvoicePaymentStatus paymentStatus, Pageable pageable);

    Page<InvoiceEntity> findAllByUserIdAndInvoiceDateBetweenAndIsDeletedFalse(UUID userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    boolean existsByTransactionIdAndIsDeletedFalse(UUID transactionId);

    Optional<InvoiceEntity> findByTransactionIdAndIsDeletedFalse(UUID transactionId);
}
