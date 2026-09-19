package com.lifeos.asset.service;

import com.lifeos.asset.dto.*;
import com.lifeos.asset.entity.*;
import com.lifeos.asset.repository.*;
import com.lifeos.common.exception.InvalidStateTransitionException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final DependentRepository dependentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AssetStatusHistoryRepository assetStatusHistoryRepository;
    private final ReminderRepository reminderRepository;

    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request, UUID userId) {
        log.info("User [{}] creating asset [{}] in category [{}]", userId, request.getName(), request.getCategory());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DependentEntity dependent = null;
        if (request.getDependentId() != null) {
            dependent = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));
        }

        InvoiceEntity primaryInvoice = null;
        if (request.getPrimaryInvoiceId() != null) {
            primaryInvoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(request.getPrimaryInvoiceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getPrimaryInvoiceId()));
        }

        AssetEntity asset = AssetEntity.builder()
                .user(user)
                .dependent(dependent)
                .name(request.getName().trim())
                .category(request.getCategory())
                .brand(request.getBrand() != null ? request.getBrand().trim() : null)
                .modelNumber(request.getModelNumber() != null ? request.getModelNumber().trim() : null)
                .serialNumber(request.getSerialNumber() != null ? request.getSerialNumber().trim() : null)
                .purchaseDate(request.getPurchaseDate())
                .returnDeadline(request.getReturnDeadline())
                .purchasePrice(request.getPurchasePrice())
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "USD")
                .primaryInvoice(primaryInvoice)
                .location(request.getLocation() != null ? request.getLocation().trim() : null)
                .status(AssetStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        AssetEntity saved = assetRepository.save(asset);

        // Record initial status creation in audit trail
        AssetStatusHistoryEntity initialAudit = AssetStatusHistoryEntity.builder()
                .asset(saved)
                .userId(userId)
                .fromStatus("NEW")
                .toStatus(AssetStatus.ACTIVE.name())
                .reason("Initial asset registration")
                .build();
        assetStatusHistoryRepository.save(initialAudit);

        // Optional return deadline reminder
        if (saved.getReturnDeadline() != null && saved.getReturnDeadline().isAfter(LocalDate.now())) {
            syncReturnReminder(saved, user);
        }

        return AssetResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AssetResponse getAsset(UUID id, UUID userId) {
        AssetEntity asset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
        return AssetResponse.fromEntity(asset);
    }

    @Transactional(readOnly = true)
    public Page<AssetResponse> listAssets(UUID userId, AssetCategory category, AssetStatus status,
                                          UUID dependentId, String search, Pageable pageable) {
        Page<AssetEntity> page;
        if (search != null && !search.isBlank()) {
            page = assetRepository.searchAssets(userId, search.trim(), pageable);
        } else if (category != null) {
            page = assetRepository.findAllByUserIdAndCategoryAndIsDeletedFalse(userId, category, pageable);
        } else if (status != null) {
            page = assetRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, status, pageable);
        } else if (dependentId != null) {
            page = assetRepository.findAllByUserIdAndDependentIdAndIsDeletedFalse(userId, dependentId, pageable);
        } else {
            page = assetRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
        }
        return page.map(AssetResponse::fromEntity);
    }

    @Transactional
    public AssetResponse updateAsset(UUID id, UpdateAssetRequest request, UUID userId) {
        AssetEntity asset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            asset.setName(request.getName().trim());
        }
        if (request.getCategory() != null) {
            asset.setCategory(request.getCategory());
        }
        if (request.getBrand() != null) {
            asset.setBrand(request.getBrand().trim());
        }
        if (request.getModelNumber() != null) {
            asset.setModelNumber(request.getModelNumber().trim());
        }
        if (request.getSerialNumber() != null) {
            asset.setSerialNumber(request.getSerialNumber().trim());
        }
        if (request.getPurchaseDate() != null) {
            asset.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getReturnDeadline() != null) {
            asset.setReturnDeadline(request.getReturnDeadline());
            if (asset.getReturnDeadline().isAfter(LocalDate.now())) {
                syncReturnReminder(asset, asset.getUser());
            }
        }
        if (request.getPurchasePrice() != null) {
            asset.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            asset.setCurrency(request.getCurrency().toUpperCase().trim());
        }
        if (request.getDependentId() != null) {
            DependentEntity dep = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));
            asset.setDependent(dep);
        }
        if (request.getPrimaryInvoiceId() != null) {
            InvoiceEntity inv = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(request.getPrimaryInvoiceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getPrimaryInvoiceId()));
            asset.setPrimaryInvoice(inv);
        }
        if (request.getLocation() != null) {
            asset.setLocation(request.getLocation().trim());
        }
        if (request.getNotes() != null) {
            asset.setNotes(request.getNotes());
        }

        AssetEntity saved = assetRepository.save(asset);
        return AssetResponse.fromEntity(saved);
    }

    @Transactional
    public AssetResponse updateAssetStatus(UUID id, UpdateAssetStatusRequest request, UUID userId) {
        AssetEntity asset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        AssetStatus fromStatus = asset.getStatus();
        AssetStatus toStatus = request.getStatus();

        if (fromStatus != toStatus) {
            validateAssetStatusTransition(fromStatus, toStatus);

            asset.setStatus(toStatus);
            AssetEntity saved = assetRepository.save(asset);

            // Record status audit record
            AssetStatusHistoryEntity audit = AssetStatusHistoryEntity.builder()
                    .asset(saved)
                    .userId(userId)
                    .fromStatus(fromStatus.name())
                    .toStatus(toStatus.name())
                    .reason(request.getReason() != null ? request.getReason().trim() : "Status updated by user")
                    .build();
            assetStatusHistoryRepository.save(audit);

            // If moving to inactive / no longer possessed state, dismiss active return reminders
            if (toStatus == AssetStatus.SOLD || toStatus == AssetStatus.DISPOSED ||
                    toStatus == AssetStatus.LOST || toStatus == AssetStatus.STOLEN ||
                    toStatus == AssetStatus.GIFTED || toStatus == AssetStatus.RETURNED) {
                dismissAssetReminders(saved.getId());
            }

            return AssetResponse.fromEntity(saved);
        }

        return AssetResponse.fromEntity(asset);
    }

    @Transactional(readOnly = true)
    public List<AssetStatusHistoryResponse> getStatusHistory(UUID assetId, UUID userId) {
        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        return assetStatusHistoryRepository.findAllByAssetIdOrderByChangedAtDesc(assetId).stream()
                .map(AssetStatusHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteAsset(UUID id, UUID userId) {
        AssetEntity asset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        asset.setDeleted(true);
        assetRepository.save(asset);
        dismissAssetReminders(id);
        log.info("Soft deleted asset [{}] for user [{}]", id, userId);
    }

    @Transactional(readOnly = true)
    public AcquisitionCostSummaryDto getAcquisitionSummary(UUID userId) {
        List<AssetRepository.CurrencyCostProjection> projections =
                assetRepository.aggregateAcquisitionCostByCurrency(userId);
        long unrecorded = assetRepository.countAssetsWithUnrecordedCost(userId);

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();

        for (AssetRepository.CurrencyCostProjection p : projections) {
            totals.put(p.getCurrency(), p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO.setScale(2));
            counts.put(p.getCurrency(), p.getAssetCount() != null ? p.getAssetCount() : 0L);
        }

        BigDecimal consolidated = null;
        if (totals.size() == 1) {
            consolidated = totals.values().iterator().next();
        }

        return AcquisitionCostSummaryDto.builder()
                .totalsByCurrency(totals)
                .assetCountByCurrency(counts)
                .assetsWithUnrecordedCost(unrecorded)
                .consolidatedTotal(consolidated)
                .build();
    }

    private void validateAssetStatusTransition(AssetStatus from, AssetStatus to) {
        boolean valid = switch (from) {
            case ACTIVE -> true;
            case UNDER_REPAIR -> (to == AssetStatus.ACTIVE || to == AssetStatus.RETIRED || to == AssetStatus.SOLD
                    || to == AssetStatus.DISPOSED || to == AssetStatus.LOST || to == AssetStatus.STOLEN || to == AssetStatus.RETURNED);
            case RETIRED -> (to == AssetStatus.ACTIVE || to == AssetStatus.SOLD || to == AssetStatus.DISPOSED
                    || to == AssetStatus.LOST || to == AssetStatus.STOLEN || to == AssetStatus.GIFTED);
            case LOST -> (to == AssetStatus.ACTIVE || to == AssetStatus.DISPOSED || to == AssetStatus.RETIRED);
            case STOLEN -> (to == AssetStatus.ACTIVE || to == AssetStatus.DISPOSED || to == AssetStatus.RETIRED);
            case SOLD, DISPOSED, GIFTED, RETURNED -> (to == AssetStatus.ACTIVE);
        };

        if (!valid) {
            throw new InvalidStateTransitionException("Cannot transition asset from status [" + from + "] to [" + to + "]");
        }
    }

    private void syncReturnReminder(AssetEntity asset, UserEntity user) {
        LocalDate targetDate = asset.getReturnDeadline().minusDays(3);
        if (targetDate.isBefore(LocalDate.now())) {
            targetDate = LocalDate.now();
        }

        ZoneId zoneId = ZoneOffset.UTC;
        if (user.getPreferences() != null && user.getPreferences().containsKey("timezone")) {
            try {
                zoneId = ZoneId.of(user.getPreferences().get("timezone").toString());
            } catch (Exception ignored) {}
        }

        ZonedDateTime zdt = targetDate.atTime(9, 0).atZone(zoneId);
        OffsetDateTime dueAt = zdt.toOffsetDateTime();

        Optional<ReminderEntity> existing = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(asset.getId(), "ASSET_RETURN_DEADLINE");

        ReminderEntity reminder;
        if (existing.isPresent()) {
            reminder = existing.get();
            reminder.setDueAt(dueAt);
            reminder.setStatus(ReminderStatus.ACTIVE);
        } else {
            reminder = ReminderEntity.builder()
                    .user(user)
                    .title("Return Window Expiring: " + asset.getName())
                    .description("Return deadline for asset " + asset.getName() + " is " + asset.getReturnDeadline())
                    .dueAt(dueAt)
                    .reminderType("ASSET_RETURN_DEADLINE")
                    .targetEntityId(asset.getId())
                    .status(ReminderStatus.ACTIVE)
                    .build();
        }
        reminderRepository.save(reminder);
    }

    private void dismissAssetReminders(UUID assetId) {
        Optional<ReminderEntity> opt = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(assetId, "ASSET_RETURN_DEADLINE");
        opt.ifPresent(r -> {
            r.setStatus(ReminderStatus.DISMISSED);
            reminderRepository.save(r);
        });
    }
}
