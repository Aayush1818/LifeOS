package com.lifeos.finance.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.finance.dto.CreateRecurringRequest;
import com.lifeos.finance.dto.RecurringResponse;
import com.lifeos.finance.dto.UpdateRecurringRequest;
import com.lifeos.finance.service.RecurringTransactionService;
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
@RequestMapping("/api/v1/finance/recurring")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Recurring Transactions", description = "Subscriptions, recurring bills, and scheduled financial obligations")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;

    @PostMapping
    @Operation(summary = "Create recurring transaction rule", description = "Creates a new recurring subscription or bill.")
    public ResponseEntity<ApiResponse<RecurringResponse>> createRecurring(
            @Valid @RequestBody CreateRecurringRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        RecurringResponse response = recurringService.createRecurring(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Recurring transaction created successfully"));
    }

    @GetMapping
    @Operation(summary = "List recurring transactions", description = "Lists all recurring transactions for authenticated user.")
    public ResponseEntity<ApiResponse<List<RecurringResponse>>> listRecurring() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<RecurringResponse> response = recurringService.listRecurring(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring transaction details", description = "Retrieves recurring rule details. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<RecurringResponse>> getRecurring(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        RecurringResponse response = recurringService.getRecurring(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update recurring transaction", description = "Updates recurring rule. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<RecurringResponse>> updateRecurring(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        RecurringResponse response = recurringService.updateRecurring(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Recurring transaction updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete recurring transaction", description = "Soft-deletes a recurring transaction rule.")
    public ResponseEntity<ApiResponse<Void>> deleteRecurring(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        recurringService.deleteRecurring(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Recurring transaction deleted successfully"));
    }
}
