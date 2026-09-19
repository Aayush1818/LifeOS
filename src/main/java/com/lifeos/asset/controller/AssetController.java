package com.lifeos.asset.controller;

import com.lifeos.asset.dto.*;
import com.lifeos.asset.entity.AssetCategory;
import com.lifeos.asset.entity.AssetStatus;
import com.lifeos.asset.service.AssetService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Endpoints for managing personal, household, and vehicle assets")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    @Operation(summary = "Create a new asset")
    public ResponseEntity<ApiResponse<AssetResponse>> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetResponse response = assetService.createAsset(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Asset created successfully"));
    }

    @GetMapping
    @Operation(summary = "List user assets with optional filters")
    public ResponseEntity<ApiResponse<Page<AssetResponse>>> listAssets(
            @RequestParam(required = false) AssetCategory category,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) UUID dependentId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<AssetResponse> page = assetService.listAssets(userId, category, status, dependentId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Assets retrieved successfully"));
    }

    @GetMapping("/acquisition-summary")
    @Operation(summary = "Get multi-currency deterministic acquisition cost aggregation")
    public ResponseEntity<ApiResponse<AcquisitionCostSummaryDto>> getAcquisitionSummary() {
        UUID userId = SecurityUtils.getCurrentUserId();
        AcquisitionCostSummaryDto summary = assetService.getAcquisitionSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Acquisition summary retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset by ID")
    public ResponseEntity<ApiResponse<AssetResponse>> getAsset(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetResponse response = assetService.getAsset(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update asset details")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAsset(@PathVariable UUID id,
                                                                  @Valid @RequestBody UpdateAssetRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetResponse response = assetService.updateAsset(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition asset status with state validation and audit log")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAssetStatus(@PathVariable UUID id,
                                                                        @Valid @RequestBody UpdateAssetStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetResponse response = assetService.updateAssetStatus(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset status updated successfully"));
    }

    @GetMapping("/{id}/status-history")
    @Operation(summary = "Get immutable lifecycle audit history for asset")
    public ResponseEntity<ApiResponse<List<AssetStatusHistoryResponse>>> getStatusHistory(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AssetStatusHistoryResponse> history = assetService.getStatusHistory(id, userId);
        return ResponseEntity.ok(ApiResponse.success(history, "Asset status history retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an asset")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        assetService.deleteAsset(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Asset deleted successfully"));
    }
}
