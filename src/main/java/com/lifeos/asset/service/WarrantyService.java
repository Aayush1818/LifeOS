package com.lifeos.asset.service;

import com.lifeos.asset.dto.CreateWarrantyRequest;
import com.lifeos.asset.dto.UpdateWarrantyRequest;
import com.lifeos.asset.dto.UpdateWarrantyStatusRequest;
import com.lifeos.asset.dto.WarrantyResponse;
import com.lifeos.asset.entity.AssetEntity;
import com.lifeos.asset.entity.WarrantyEntity;
import com.lifeos.asset.entity.WarrantyStatus;
import com.lifeos.asset.entity.WarrantyType;
import com.lifeos.asset.repository.AssetRepository;
import com.lifeos.asset.repository.WarrantyRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;

    @Transactional
    public WarrantyResponse createWarranty(UUID assetId, CreateWarrantyRequest request, UUID userId) {
        log.info("User [{}] creating warranty for asset [{}] with provider [{}]", userId, assetId, request.getProvider());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AssetEntity asset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        boolean isLifetime = request.getWarrantyType() == WarrantyType.LIFETIME;
        LocalDate expiryDate = isLifetime ? null : request.getExpiryDate();

        WarrantyStatus initialStatus = WarrantyStatus.ACTIVE;
        if (!isLifetime && expiryDate != null && !expiryDate.isAfter(LocalDate.now())) {
            initialStatus = WarrantyStatus.EXPIRED;
        }

        BigDecimal deductible = request.getDeductibleAmount() != null
                ? request.getDeductibleAmount().setScale(2, RoundingMode.HALF_UP) : null;

        WarrantyEntity warranty = WarrantyEntity.builder()
                .user(user)
                .asset(asset)
                .provider(request.getProvider().trim())
                .warrantyType(request.getWarrantyType())
                .policyNumber(request.getPolicyNumber() != null ? request.getPolicyNumber().trim() : null)
                .startDate(request.getStartDate())
                .expiryDate(expiryDate)
                .status(initialStatus)
                .coverageDetails(request.getCoverageDetails())
                .deductibleAmount(deductible)
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "USD")
                .reminderOffsetDays(request.getReminderOffsetDays() != null ? request.getReminderOffsetDays() : 30)
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .notes(request.getNotes())
                .build();

        WarrantyEntity saved = warrantyRepository.save(warranty);

        // Schedule reminder: LIFETIME warranties NEVER generate expiry reminders
        if (!isLifetime && saved.getExpiryDate() != null && saved.getStatus() == WarrantyStatus.ACTIVE) {
            syncWarrantyReminder(saved, user, asset);
        }

        return WarrantyResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public WarrantyResponse getWarranty(UUID warrantyId, UUID userId) {
        WarrantyEntity warranty = warrantyRepository.findByIdAndUserIdAndIsDeletedFalse(warrantyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found with id: " + warrantyId));
        return WarrantyResponse.fromEntity(warranty);
    }

    @Transactional(readOnly = true)
    public List<WarrantyResponse> listWarrantiesForAsset(UUID assetId, UUID userId) {
        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        return warrantyRepository.findAllByAssetIdAndUserIdAndIsDeletedFalse(assetId, userId).stream()
                .map(WarrantyResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarrantyResponse> listExpiringWarranties(int windowDays, UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(windowDays);
        return warrantyRepository.findAllByUserIdAndStatusAndExpiryDateBetweenAndIsDeletedFalse(
                userId, WarrantyStatus.ACTIVE, today, cutoff).stream()
                .map(WarrantyResponse::fromEntity)
                .toList();
    }

    @Transactional
    public WarrantyResponse updateWarranty(UUID warrantyId, UpdateWarrantyRequest request, UUID userId) {
        WarrantyEntity warranty = warrantyRepository.findByIdAndUserIdAndIsDeletedFalse(warrantyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found with id: " + warrantyId));

        if (request.getProvider() != null && !request.getProvider().isBlank()) {
            warranty.setProvider(request.getProvider().trim());
        }
        if (request.getWarrantyType() != null) {
            warranty.setWarrantyType(request.getWarrantyType());
            if (request.getWarrantyType() == WarrantyType.LIFETIME) {
                warranty.setExpiryDate(null);
                dismissWarrantyReminder(warranty);
            }
        }
        if (request.getPolicyNumber() != null) {
            warranty.setPolicyNumber(request.getPolicyNumber().trim());
        }
        if (request.getStartDate() != null) {
            warranty.setStartDate(request.getStartDate());
        }
        if (request.getExpiryDate() != null && warranty.getWarrantyType() != WarrantyType.LIFETIME) {
            warranty.setExpiryDate(request.getExpiryDate());
        }
        if (request.getCoverageDetails() != null) {
            warranty.setCoverageDetails(request.getCoverageDetails());
        }
        if (request.getDeductibleAmount() != null) {
            warranty.setDeductibleAmount(request.getDeductibleAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            warranty.setCurrency(request.getCurrency().toUpperCase().trim());
        }
        if (request.getReminderOffsetDays() != null) {
            warranty.setReminderOffsetDays(request.getReminderOffsetDays());
        }
        if (request.getContactPhone() != null) {
            warranty.setContactPhone(request.getContactPhone());
        }
        if (request.getContactEmail() != null) {
            warranty.setContactEmail(request.getContactEmail());
        }
        if (request.getNotes() != null) {
            warranty.setNotes(request.getNotes());
        }

        // Reschedule reminder if active and not LIFETIME
        if (warranty.getWarrantyType() != WarrantyType.LIFETIME &&
                warranty.getExpiryDate() != null &&
                warranty.getStatus() == WarrantyStatus.ACTIVE) {
            syncWarrantyReminder(warranty, warranty.getUser(), warranty.getAsset());
        }

        WarrantyEntity saved = warrantyRepository.save(warranty);
        return WarrantyResponse.fromEntity(saved);
    }

    @Transactional
    public WarrantyResponse updateWarrantyStatus(UUID warrantyId, UpdateWarrantyStatusRequest request, UUID userId) {
        WarrantyEntity warranty = warrantyRepository.findByIdAndUserIdAndIsDeletedFalse(warrantyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found with id: " + warrantyId));

        warranty.setStatus(request.getStatus());

        if (request.getStatus() == WarrantyStatus.EXPIRED ||
                request.getStatus() == WarrantyStatus.VOID ||
                request.getStatus() == WarrantyStatus.CLAIMED) {
            dismissWarrantyReminder(warranty);
        }

        WarrantyEntity saved = warrantyRepository.save(warranty);
        return WarrantyResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteWarranty(UUID warrantyId, UUID userId) {
        WarrantyEntity warranty = warrantyRepository.findByIdAndUserIdAndIsDeletedFalse(warrantyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found with id: " + warrantyId));

        warranty.setDeleted(true);
        dismissWarrantyReminder(warranty);
        warrantyRepository.save(warranty);
        log.info("Soft deleted warranty [{}] for user [{}]", warrantyId, userId);
    }

    private void syncWarrantyReminder(WarrantyEntity warranty, UserEntity user, AssetEntity asset) {
        if (warranty.getExpiryDate() == null) return;

        int offsetDays = warranty.getReminderOffsetDays() != null ? warranty.getReminderOffsetDays() : 30;
        LocalDate targetDate = warranty.getExpiryDate().minusDays(offsetDays);
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

        String title = "Warranty Expiring: " + asset.getName() + " (" + warranty.getProvider() + ")";
        String desc = "Warranty #" + (warranty.getPolicyNumber() != null ? warranty.getPolicyNumber() : "N/A")
                + " for " + asset.getName() + " expires on " + warranty.getExpiryDate();

        ReminderEntity reminder;
        if (warranty.getReminderId() != null) {
            Optional<ReminderEntity> opt = reminderRepository.findById(warranty.getReminderId());
            if (opt.isPresent()) {
                reminder = opt.get();
                reminder.setTitle(title);
                reminder.setDescription(desc);
                reminder.setDueAt(dueAt);
                reminder.setStatus(ReminderStatus.ACTIVE);
            } else {
                reminder = createNewReminder(user, warranty.getId(), title, desc, dueAt);
            }
        } else {
            reminder = createNewReminder(user, warranty.getId(), title, desc, dueAt);
        }

        ReminderEntity savedReminder = reminderRepository.save(reminder);
        warranty.setReminderId(savedReminder.getId());
        warrantyRepository.save(warranty);
    }

    private ReminderEntity createNewReminder(UserEntity user, UUID warrantyId, String title, String desc, OffsetDateTime dueAt) {
        return ReminderEntity.builder()
                .user(user)
                .title(title)
                .description(desc)
                .dueAt(dueAt)
                .reminderType("WARRANTY_EXPIRY")
                .targetEntityId(warrantyId)
                .status(ReminderStatus.ACTIVE)
                .build();
    }

    private void dismissWarrantyReminder(WarrantyEntity warranty) {
        if (warranty.getReminderId() != null) {
            reminderRepository.findById(warranty.getReminderId()).ifPresent(r -> {
                r.setStatus(ReminderStatus.DISMISSED);
                reminderRepository.save(r);
            });
            warranty.setReminderId(null);
        }
    }
}
