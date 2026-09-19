package com.lifeos.budget.controller;

import com.lifeos.budget.dto.BudgetResponse;
import com.lifeos.budget.dto.BudgetStatusResponse;
import com.lifeos.budget.dto.CreateBudgetRequest;
import com.lifeos.budget.dto.UpdateBudgetRequest;
import com.lifeos.budget.service.BudgetService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Budgets", description = "Monthly budget allocation, tracking, threshold alerts, and utilization")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Set or update monthly budget", description = "Creates or updates budget allocation for a category and month/year.")
    public ResponseEntity<ApiResponse<BudgetResponse>> setBudget(
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.setBudget(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Budget set successfully"));
    }

    @GetMapping
    @Operation(summary = "List budgets for period", description = "Retrieves all category budgets for a specific month and year.")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> listBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        List<BudgetResponse> response = budgetService.listBudgets(currentUserId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget details", description = "Retrieves budget allocation by ID. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudget(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.getBudget(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget allocation", description = "Updates budget allocation or alert thresholds. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.updateBudget(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Budget updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget", description = "Soft-deletes a budget allocation. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Budget deleted successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get budget vs actual status", description = "Calculates actual spend, remaining balance, utilization %, projected spend, and threshold alerts.")
    public ResponseEntity<ApiResponse<BudgetStatusResponse>> getBudgetStatus(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        BudgetStatusResponse response = budgetService.getBudgetStatus(currentUserId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
