package com.lifeos.asset.controller;

import com.lifeos.asset.dto.AssetServiceRecordResponse;
import com.lifeos.asset.dto.CreateAssetServiceRecordRequest;
import com.lifeos.asset.dto.UpdateAssetServiceRecordRequest;
import com.lifeos.asset.service.AssetServiceRecordService;
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
@Tag(name = "Asset Service Records", description = "Endpoints for maintenance, repair, and service history")
public class AssetServiceRecordController {

    private final AssetServiceRecordService serviceRecordService;

    @PostMapping("/{assetId}/services")
    @Operation(summary = "Record service/maintenance on an asset")
    public ResponseEntity<ApiResponse<AssetServiceRecordResponse>> createServiceRecord(
            @PathVariable UUID assetId,
            @Valid @RequestBody CreateAssetServiceRecordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetServiceRecordResponse response = serviceRecordService.createServiceRecord(assetId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Service record created successfully"));
    }

    @GetMapping("/{assetId}/services")
    @Operation(summary = "List service and repair history for an asset")
    public ResponseEntity<ApiResponse<List<AssetServiceRecordResponse>>> listServiceRecordsForAsset(
            @PathVariable UUID assetId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AssetServiceRecordResponse> list = serviceRecordService.listServiceRecordsForAsset(assetId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Service records retrieved successfully"));
    }

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Get service record by ID")
    public ResponseEntity<ApiResponse<AssetServiceRecordResponse>> getServiceRecord(@PathVariable UUID serviceId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetServiceRecordResponse response = serviceRecordService.getServiceRecord(serviceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Service record retrieved successfully"));
    }

    @PutMapping("/services/{serviceId}")
    @Operation(summary = "Update service record")
    public ResponseEntity<ApiResponse<AssetServiceRecordResponse>> updateServiceRecord(
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateAssetServiceRecordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetServiceRecordResponse response = serviceRecordService.updateServiceRecord(serviceId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Service record updated successfully"));
    }

    @DeleteMapping("/services/{serviceId}")
    @Operation(summary = "Soft delete service record")
    public ResponseEntity<ApiResponse<Void>> deleteServiceRecord(@PathVariable UUID serviceId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        serviceRecordService.deleteServiceRecord(serviceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Service record deleted successfully"));
    }
}
