package com.lifeos.finance.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.dto.UpdateTransactionRequest;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionStatus;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.service.TransactionService;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Transactions", description = "Personal finance income and expense transaction management")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Record a transaction", description = "Records a new income or expense transaction with BigDecimal precision.")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.createTransaction(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transaction recorded successfully"));
    }

    @GetMapping
    @Operation(summary = "List transactions", description = "Retrieves paginated and filtered transactions belonging to the authenticated user.")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<TransactionResponse> response = transactionService.listTransactions(
                currentUserId, type, category, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details", description = "Retrieves a single transaction by ID. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.getTransaction(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update transaction", description = "Updates transaction details. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.updateTransaction(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction", description = "Soft-deletes a transaction. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        transactionService.deleteTransaction(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction deleted successfully"));
    }
}
