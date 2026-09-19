package com.lifeos.asset.controller;

import com.lifeos.asset.dto.CreateWarrantyRequest;
import com.lifeos.asset.dto.UpdateWarrantyRequest;
import com.lifeos.asset.dto.UpdateWarrantyStatusRequest;
import com.lifeos.asset.dto.WarrantyResponse;
import com.lifeos.asset.service.WarrantyService;
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
@Tag(name = "Warranties", description = "Endpoints for managing asset warranties and protection plans")
public class WarrantyController {

    private final WarrantyService warrantyService;

    @PostMapping("/{assetId}/warranties")
    @Operation(summary = "Add warranty to an asset (schedules 09:00 reminder if not LIFETIME)")
    public ResponseEntity<ApiResponse<WarrantyResponse>> createWarranty(@PathVariable UUID assetId,
                                                                         @Valid @RequestBody CreateWarrantyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyResponse response = warrantyService.createWarranty(assetId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Warranty added successfully"));
    }

    @GetMapping("/{assetId}/warranties")
    @Operation(summary = "List all warranties for an asset")
    public ResponseEntity<ApiResponse<List<WarrantyResponse>>> listWarrantiesForAsset(@PathVariable UUID assetId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<WarrantyResponse> list = warrantyService.listWarrantiesForAsset(assetId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Warranties retrieved successfully"));
    }

    @GetMapping("/warranties/expiring")
    @Operation(summary = "Query upcoming expiring warranties across assets within windowDays (default 30)")
    public ResponseEntity<ApiResponse<List<WarrantyResponse>>> listExpiringWarranties(
            @RequestParam(defaultValue = "30") int windowDays) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<WarrantyResponse> list = warrantyService.listExpiringWarranties(windowDays, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Expiring warranties retrieved successfully"));
    }

    @GetMapping("/warranties/{warrantyId}")
    @Operation(summary = "Get warranty by ID")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarranty(@PathVariable UUID warrantyId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyResponse response = warrantyService.getWarranty(warrantyId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Warranty retrieved successfully"));
    }

    @PutMapping("/warranties/{warrantyId}")
    @Operation(summary = "Update warranty details and reschedule reminder")
    public ResponseEntity<ApiResponse<WarrantyResponse>> updateWarranty(@PathVariable UUID warrantyId,
                                                                         @Valid @RequestBody UpdateWarrantyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyResponse response = warrantyService.updateWarranty(warrantyId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Warranty updated successfully"));
    }

    @PatchMapping("/warranties/{warrantyId}/status")
    @Operation(summary = "Update warranty status (dismisses reminder if VOID/EXPIRED/CLAIMED)")
    public ResponseEntity<ApiResponse<WarrantyResponse>> updateWarrantyStatus(@PathVariable UUID warrantyId,
                                                                               @Valid @RequestBody UpdateWarrantyStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        WarrantyResponse response = warrantyService.updateWarrantyStatus(warrantyId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Warranty status updated successfully"));
    }

    @DeleteMapping("/warranties/{warrantyId}")
    @Operation(summary = "Soft delete warranty and dismiss reminder")
    public ResponseEntity<ApiResponse<Void>> deleteWarranty(@PathVariable UUID warrantyId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        warrantyService.deleteWarranty(warrantyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Warranty deleted successfully"));
    }
}
