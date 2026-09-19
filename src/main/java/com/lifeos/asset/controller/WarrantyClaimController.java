package com.lifeos.asset.controller;

import com.lifeos.asset.dto.CreateWarrantyClaimRequest;
import com.lifeos.asset.dto.UpdateClaimStatusRequest;
import com.lifeos.asset.dto.UpdateWarrantyClaimRequest;
import com.lifeos.asset.dto.WarrantyClaimResponse;
import com.lifeos.asset.service.WarrantyClaimService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Warranty Claims", description = "Endpoints for tracking formal warranty claims and resolutions")
public class WarrantyClaimController {

    private final WarrantyClaimService warrantyClaimService;

    @PostMapping("/warranties/{warrantyId}/claims")
    @Operation(summary = "File a warranty claim under a warranty")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> createClaim(@PathVariable UUID warrantyId,
                                                                           @Valid @RequestBody CreateWarrantyClaimRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyClaimResponse response = warrantyClaimService.createClaim(warrantyId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Warranty claim filed successfully"));
    }

    @GetMapping("/warranties/{warrantyId}/claims")
    @Operation(summary = "List claims under a specific warranty")
    public ResponseEntity<ApiResponse<List<WarrantyClaimResponse>>> listClaimsForWarranty(@PathVariable UUID warrantyId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<WarrantyClaimResponse> list = warrantyClaimService.listClaimsForWarranty(warrantyId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Claims retrieved successfully"));
    }

    @GetMapping("/{assetId}/claims")
    @Operation(summary = "List all historical claims for an asset (queryable post-expiry)")
    public ResponseEntity<ApiResponse<List<WarrantyClaimResponse>>> listClaimsForAsset(@PathVariable UUID assetId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<WarrantyClaimResponse> list = warrantyClaimService.listClaimsForAsset(assetId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Asset claims retrieved successfully"));
    }

    @GetMapping("/warranties/claims/{claimId}")
    @Operation(summary = "Get warranty claim by ID")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> getClaim(@PathVariable UUID claimId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyClaimResponse response = warrantyClaimService.getClaim(claimId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Warranty claim retrieved successfully"));
    }

    @PutMapping("/warranties/claims/{claimId}")
    @Operation(summary = "Update warranty claim details")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> updateClaim(@PathVariable UUID claimId,
                                                                           @Valid @RequestBody UpdateWarrantyClaimRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyClaimResponse response = warrantyClaimService.updateClaim(claimId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Warranty claim updated successfully"));
    }

    @PatchMapping("/warranties/claims/{claimId}/status")
    @Operation(summary = "Transition claim status with resolution details and costs")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> updateClaimStatus(@PathVariable UUID claimId,
                                                                                 @Valid @RequestBody UpdateClaimStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyClaimResponse response = warrantyClaimService.updateClaimStatus(claimId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Warranty claim status updated successfully"));
    }

    @DeleteMapping("/warranties/claims/{claimId}")
    @Operation(summary = "Soft delete warranty claim")
    public ResponseEntity<ApiResponse<Void>> deleteClaim(@PathVariable UUID claimId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        warrantyClaimService.deleteClaim(claimId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Warranty claim deleted successfully"));
    }
}
