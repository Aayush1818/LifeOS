package com.lifeos.insurance.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.insurance.dto.*;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.service.InsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Insurance Policies", description = "Insurance policy portfolio management, renewal tracking, and reminder synchronization")
public class InsuranceController {

    private final InsuranceService insuranceService;

    @PostMapping
    @Operation(summary = "Create an insurance policy", description = "Creates a new insurance policy and automatically synchronizes a renewal reminder.")
    public ResponseEntity<ApiResponse<InsuranceResponse>> createPolicy(
            @Valid @RequestBody CreateInsuranceRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsuranceResponse response = insuranceService.createPolicy(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Insurance policy created successfully"));
    }

    @GetMapping
    @Operation(summary = "List insurance policies", description = "Retrieves paginated insurance policies for authenticated user.")
    public ResponseEntity<ApiResponse<Page<InsuranceResponse>>> listPolicies(
            @RequestParam(required = false) InsuranceType type,
            @RequestParam(required = false) PolicyStatus status,
            @PageableDefault(size = 20, sort = "nextRenewalDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<InsuranceResponse> response = insuranceService.listPolicies(currentUserId, type, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy details", description = "Retrieves insurance policy details by ID. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<InsuranceResponse>> getPolicy(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsuranceResponse response = insuranceService.getPolicy(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update policy", description = "Updates policy details and resynchronizes the renewal reminder.")
    public ResponseEntity<ApiResponse<InsuranceResponse>> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInsuranceRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsuranceResponse response = insuranceService.updatePolicy(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Insurance policy updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete policy", description = "Soft-deletes policy and dismisses linked renewal reminder.")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        insuranceService.deletePolicy(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Insurance policy deleted successfully"));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renew insurance policy", description = "Renews policy to new expiry date, updates status, and advances renewal reminder.")
    public ResponseEntity<ApiResponse<InsuranceResponse>> renewPolicy(
            @PathVariable UUID id,
            @Valid @RequestBody RenewPolicyRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsuranceResponse response = insuranceService.renewPolicy(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Policy renewed successfully"));
    }

    @GetMapping("/renewals/upcoming")
    @Operation(summary = "Upcoming policy renewals", description = "Retrieves policies due for renewal within the specified day window (default: 30 days).")
    public ResponseEntity<ApiResponse<UpcomingRenewalsResponse>> getUpcomingRenewals(
            @RequestParam(defaultValue = "30") int windowDays
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UpcomingRenewalsResponse response = insuranceService.getUpcomingRenewals(currentUserId, windowDays);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
