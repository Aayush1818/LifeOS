package com.lifeos.insurance.comparison.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.insurance.comparison.dto.PolicyComparisonRequest;
import com.lifeos.insurance.comparison.dto.PolicyComparisonResponse;
import com.lifeos.insurance.comparison.service.PolicyComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insurance/policies")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Insurance Policy Comparison", description = "Automated policy clause comparison and differential analysis")
public class PolicyComparisonController {

    private final PolicyComparisonService policyComparisonService;

    @PostMapping("/compare")
    @Operation(summary = "Compare two policy document versions and generate structured clause diffs")
    public ResponseEntity<ApiResponse<PolicyComparisonResponse>> comparePolicies(
            @Valid @RequestBody PolicyComparisonRequest request) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PolicyComparisonResponse response = policyComparisonService.comparePolicies(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Policy comparison completed successfully"));
    }
}
