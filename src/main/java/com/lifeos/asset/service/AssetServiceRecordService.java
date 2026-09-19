package com.lifeos.asset.service;

import com.lifeos.asset.dto.AssetServiceRecordResponse;
import com.lifeos.asset.dto.CreateAssetServiceRecordRequest;
import com.lifeos.asset.dto.UpdateAssetServiceRecordRequest;
import com.lifeos.asset.entity.*;
import com.lifeos.asset.repository.AssetRepository;
import com.lifeos.asset.repository.AssetServiceRecordRepository;
import com.lifeos.asset.repository.InvoiceRepository;
import com.lifeos.asset.repository.WarrantyClaimRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceRecordService {

    private final AssetServiceRecordRepository serviceRecordRepository;
    private final AssetRepository assetRepository;
    private final InvoiceRepository invoiceRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final UserRepository userRepository;

    @Transactional
    public AssetServiceRecordResponse createServiceRecord(UUID assetId, CreateAssetServiceRecordRequest request, UUID userId) {
        log.info("User [{}] creating service record for asset [{}]", userId, assetId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AssetEntity asset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        InvoiceEntity invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(request.getInvoiceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));
        }

        WarrantyClaimEntity claim = null;
        if (request.getWarrantyClaimId() != null) {
            claim = warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(request.getWarrantyClaimId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + request.getWarrantyClaimId()));
        }

        BigDecimal cost = request.getCost() != null
                ? request.getCost().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);

        AssetServiceRecordEntity record = AssetServiceRecordEntity.builder()
                .user(user)
                .asset(asset)
                .serviceDate(request.getServiceDate())
                .serviceType(request.getServiceType())
                .serviceProvider(request.getServiceProvider().trim())
                .description(request.getDescription().trim())
                .cost(cost)
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "USD")
                .status(request.getStatus() != null ? request.getStatus() : ServiceStatus.COMPLETED)
                .invoice(invoice)
                .warrantyClaim(claim)
                .notes(request.getNotes())
                .build();

        AssetServiceRecordEntity saved = serviceRecordRepository.save(record);
        return AssetServiceRecordResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AssetServiceRecordResponse getServiceRecord(UUID serviceId, UUID userId) {
        AssetServiceRecordEntity record = serviceRecordRepository.findByIdAndUserIdAndIsDeletedFalse(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + serviceId));
        return AssetServiceRecordResponse.fromEntity(record);
    }

    @Transactional(readOnly = true)
    public List<AssetServiceRecordResponse> listServiceRecordsForAsset(UUID assetId, UUID userId) {
        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        return serviceRecordRepository.findAllByAssetIdAndUserIdAndIsDeletedFalseOrderByServiceDateDesc(assetId, userId).stream()
                .map(AssetServiceRecordResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AssetServiceRecordResponse updateServiceRecord(UUID serviceId, UpdateAssetServiceRecordRequest request, UUID userId) {
        AssetServiceRecordEntity record = serviceRecordRepository.findByIdAndUserIdAndIsDeletedFalse(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + serviceId));

        if (request.getServiceDate() != null) {
            record.setServiceDate(request.getServiceDate());
        }
        if (request.getServiceType() != null) {
            record.setServiceType(request.getServiceType());
        }
        if (request.getServiceProvider() != null && !request.getServiceProvider().isBlank()) {
            record.setServiceProvider(request.getServiceProvider().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            record.setDescription(request.getDescription().trim());
        }
        if (request.getCost() != null) {
            record.setCost(request.getCost().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            record.setCurrency(request.getCurrency().toUpperCase().trim());
        }
        if (request.getStatus() != null) {
            record.setStatus(request.getStatus());
        }
        if (request.getInvoiceId() != null) {
            InvoiceEntity inv = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(request.getInvoiceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));
            record.setInvoice(inv);
        }
        if (request.getWarrantyClaimId() != null) {
            WarrantyClaimEntity claim = warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(request.getWarrantyClaimId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + request.getWarrantyClaimId()));
            record.setWarrantyClaim(claim);
        }
        if (request.getNotes() != null) {
            record.setNotes(request.getNotes());
        }

        AssetServiceRecordEntity saved = serviceRecordRepository.save(record);
        return AssetServiceRecordResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteServiceRecord(UUID serviceId, UUID userId) {
        AssetServiceRecordEntity record = serviceRecordRepository.findByIdAndUserIdAndIsDeletedFalse(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + serviceId));

        record.setDeleted(true);
        serviceRecordRepository.save(record);
        log.info("Soft deleted service record [{}] for user [{}]", serviceId, userId);
    }
}
