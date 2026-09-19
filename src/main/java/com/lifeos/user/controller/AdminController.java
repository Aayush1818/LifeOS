package com.lifeos.user.controller;

import com.lifeos.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin Operations", description = "Administrative operations requiring ROLE_ADMIN authority")
public class AdminController {

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "System status for administrators", description = "Requires ROLE_ADMIN authority")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStatus() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("adminAccess", true, "message", "Admin authority confirmed"),
                "Admin access granted"
        ));
    }
}
