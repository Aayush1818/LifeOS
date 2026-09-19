package com.lifeos.loan.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.loan.dto.*;
import com.lifeos.loan.entity.LoanStatus;
import com.lifeos.loan.service.LoanAnalyticsService;
import com.lifeos.loan.service.LoanService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Loans & Amortization", description = "Loan management, mathematical amortization schedule, payments, and prepayments")
public class LoanController {

    private final LoanService loanService;
    private final LoanAnalyticsService analyticsService;

    @PostMapping
    @Operation(summary = "Create a new loan", description = "Records a new loan obligation and auto-computes regular monthly EMI.")
    public ResponseEntity<ApiResponse<LoanResponse>> createLoan(
            @Valid @RequestBody CreateLoanRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LoanResponse response = loanService.createLoan(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Loan created successfully"));
    }

    @GetMapping
    @Operation(summary = "List loans", description = "Retrieves paginated loans belonging to the authenticated user.")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> listLoans(
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<LoanResponse> response = loanService.listLoans(currentUserId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan details", description = "Retrieves loan details by ID. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoan(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LoanResponse response = loanService.getLoan(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update loan", description = "Updates loan details. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<LoanResponse>> updateLoan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLoanRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LoanResponse response = loanService.updateLoan(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete loan", description = "Soft-deletes a loan. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<Void>> deleteLoan(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        loanService.deleteLoan(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Loan deleted successfully"));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Generate amortization schedule", description = "Generates complete mathematical reducing-balance amortization schedule with final installment reconciliation.")
    public ResponseEntity<ApiResponse<AmortizationScheduleResponse>> getAmortizationSchedule(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        AmortizationScheduleResponse response = loanService.getAmortizationSchedule(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record loan payment / prepayment", description = "Records a regular EMI payment, partial prepayment (with tenure or EMI reduction), or full early closure.")
    public ResponseEntity<ApiResponse<LoanPaymentResponse>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordLoanPaymentRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LoanPaymentResponse response = loanService.recordPayment(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment recorded successfully"));
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "List loan payments", description = "Retrieves payment history for a specific loan.")
    public ResponseEntity<ApiResponse<List<LoanPaymentResponse>>> listPayments(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<LoanPaymentResponse> response = loanService.listPayments(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/analytics/summary")
    @Operation(summary = "Loan portfolio summary", description = "Calculates total active loans, outstanding balance, monthly EMI commitment, and paid interest via direct JDBC aggregation.")
    public ResponseEntity<ApiResponse<LoanPortfolioSummaryResponse>> getPortfolioSummary() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LoanPortfolioSummaryResponse response = analyticsService.getPortfolioSummary(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
