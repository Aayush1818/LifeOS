package com.lifeos.asset.service;

import com.lifeos.asset.dto.CreateWarrantyClaimRequest;
import com.lifeos.asset.dto.UpdateClaimStatusRequest;
import com.lifeos.asset.dto.UpdateWarrantyClaimRequest;
import com.lifeos.asset.dto.WarrantyClaimResponse;
import com.lifeos.asset.entity.ClaimStatus;
import com.lifeos.asset.entity.WarrantyClaimEntity;
import com.lifeos.asset.entity.WarrantyEntity;
import com.lifeos.asset.repository.AssetRepository;
import com.lifeos.asset.repository.WarrantyClaimRepository;
import com.lifeos.asset.repository.WarrantyRepository;
import com.lifeos.common.exception.InvalidStateTransitionException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyClaimService {

    private final WarrantyClaimRepository warrantyClaimRepository;
    private final WarrantyRepository warrantyRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    @Transactional
    public WarrantyClaimResponse createClaim(UUID warrantyId, CreateWarrantyClaimRequest request, UUID userId) {
        log.info("User [{}] creating claim on warranty [{}]", userId, warrantyId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        WarrantyEntity warranty = warrantyRepository.findByIdAndUserIdAndIsDeletedFalse(warrantyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found with id: " + warrantyId));

        WarrantyClaimEntity claim = WarrantyClaimEntity.builder()
                .user(user)
                .warranty(warranty)
                .asset(warranty.getAsset())
                .claimNumber(request.getClaimNumber() != null ? request.getClaimNumber().trim() : null)
                .claimDate(request.getClaimDate())
                .claimType(request.getClaimType())
                .status(ClaimStatus.FILED)
                .description(request.getDescription().trim())
                .resolution(request.getResolution())
                .resolvedDate(request.getResolvedDate())
                .claimCostCovered(request.getClaimCostCovered() != null ? request.getClaimCostCovered().setScale(2, RoundingMode.HALF_UP) : null)
                .outOfPocketCost(request.getOutOfPocketCost() != null ? request.getOutOfPocketCost().setScale(2, RoundingMode.HALF_UP) : null)
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "USD")
                .build();

        WarrantyClaimEntity saved = warrantyClaimRepository.save(claim);
        return WarrantyClaimResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public WarrantyClaimResponse getClaim(UUID claimId, UUID userId) {
        WarrantyClaimEntity claim = warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));
        return WarrantyClaimResponse.fromEntity(claim);
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> listClaimsForWarranty(UUID warrantyId, UUID userId) {
        warrantyRepository.findByIdAndUserIdAndIsDeletedFalse(warrantyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found with id: " + warrantyId));

        return warrantyClaimRepository.findAllByWarrantyIdAndUserIdAndIsDeletedFalse(warrantyId, userId).stream()
                .map(WarrantyClaimResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> listClaimsForAsset(UUID assetId, UUID userId) {
        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        return warrantyClaimRepository.findAllByAssetIdAndUserIdAndIsDeletedFalse(assetId, userId).stream()
                .map(WarrantyClaimResponse::fromEntity)
                .toList();
    }

    @Transactional
    public WarrantyClaimResponse updateClaim(UUID claimId, UpdateWarrantyClaimRequest request, UUID userId) {
        WarrantyClaimEntity claim = warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));

        if (request.getClaimNumber() != null) {
            claim.setClaimNumber(request.getClaimNumber().trim());
        }
        if (request.getClaimDate() != null) {
            claim.setClaimDate(request.getClaimDate());
        }
        if (request.getClaimType() != null) {
            claim.setClaimType(request.getClaimType());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            claim.setDescription(request.getDescription().trim());
        }
        if (request.getResolution() != null) {
            claim.setResolution(request.getResolution());
        }
        if (request.getResolvedDate() != null) {
            claim.setResolvedDate(request.getResolvedDate());
        }
        if (request.getClaimCostCovered() != null) {
            claim.setClaimCostCovered(request.getClaimCostCovered().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getOutOfPocketCost() != null) {
            claim.setOutOfPocketCost(request.getOutOfPocketCost().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            claim.setCurrency(request.getCurrency().toUpperCase().trim());
        }

        WarrantyClaimEntity saved = warrantyClaimRepository.save(claim);
        return WarrantyClaimResponse.fromEntity(saved);
    }

    @Transactional
    public WarrantyClaimResponse updateClaimStatus(UUID claimId, UpdateClaimStatusRequest request, UUID userId) {
        WarrantyClaimEntity claim = warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));

        ClaimStatus fromStatus = claim.getStatus();
        ClaimStatus toStatus = request.getStatus();

        if (fromStatus != toStatus) {
            validateClaimStatusTransition(fromStatus, toStatus);
            claim.setStatus(toStatus);

            if (toStatus == ClaimStatus.RESOLVED) {
                claim.setResolvedDate(request.getResolvedDate() != null ? request.getResolvedDate() : LocalDate.now());
            }
        }

        if (request.getResolution() != null) {
            claim.setResolution(request.getResolution());
        }
        if (request.getClaimCostCovered() != null) {
            claim.setClaimCostCovered(request.getClaimCostCovered().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getOutOfPocketCost() != null) {
            claim.setOutOfPocketCost(request.getOutOfPocketCost().setScale(2, RoundingMode.HALF_UP));
        }

        WarrantyClaimEntity saved = warrantyClaimRepository.save(claim);
        return WarrantyClaimResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteClaim(UUID claimId, UUID userId) {
        WarrantyClaimEntity claim = warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));

        claim.setDeleted(true);
        warrantyClaimRepository.save(claim);
        log.info("Soft deleted warranty claim [{}] for user [{}]", claimId, userId);
    }

    private void validateClaimStatusTransition(ClaimStatus from, ClaimStatus to) {
        boolean valid = switch (from) {
            case FILED -> (to == ClaimStatus.UNDER_REVIEW || to == ClaimStatus.APPROVED
                    || to == ClaimStatus.REJECTED || to == ClaimStatus.CANCELLED);
            case UNDER_REVIEW -> (to == ClaimStatus.APPROVED || to == ClaimStatus.REJECTED
                    || to == ClaimStatus.CANCELLED);
            case APPROVED -> (to == ClaimStatus.RESOLVED || to == ClaimStatus.CANCELLED);
            case REJECTED -> (to == ClaimStatus.FILED); // Appeal
            case RESOLVED, CANCELLED -> false; // Terminal states
        };

        if (!valid) {
            throw new InvalidStateTransitionException("Cannot transition claim from status [" + from + "] to [" + to + "]");
        }
    }
}
