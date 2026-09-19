package com.lifeos.dependent.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.dto.UpdateDependentRequest;
import com.lifeos.dependent.service.DependentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dependents")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Dependents & Family", description = "Family member and dependent profile management with strict resource isolation")
public class DependentController {

    private final DependentService dependentService;

    @GetMapping
    @Operation(summary = "List all dependents belonging to the current user")
    public ResponseEntity<ApiResponse<List<DependentResponse>>> listDependents() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<DependentResponse> dependents = dependentService.listDependents(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(dependents));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dependent profile by ID")
    public ResponseEntity<ApiResponse<DependentResponse>> getDependent(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DependentResponse response = dependentService.getDependentById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create a new dependent profile")
    public ResponseEntity<ApiResponse<DependentResponse>> createDependent(@Valid @RequestBody CreateDependentRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DependentResponse response = dependentService.createDependent(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Dependent created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing dependent profile")
    public ResponseEntity<ApiResponse<DependentResponse>> updateDependent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDependentRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DependentResponse response = dependentService.updateDependent(id, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Dependent updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a dependent profile")
    public ResponseEntity<ApiResponse<Void>> deleteDependent(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        dependentService.deleteDependent(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Dependent deleted successfully"));
    }
}
