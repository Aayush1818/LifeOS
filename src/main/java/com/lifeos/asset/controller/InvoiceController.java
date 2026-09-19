package com.lifeos.asset.controller;

import com.lifeos.asset.dto.*;
import com.lifeos.asset.entity.InvoicePaymentStatus;
import com.lifeos.asset.service.InvoiceService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Endpoints for managing purchase invoices, receipts, and line items")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @Operation(summary = "Create an invoice with line items")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.createInvoice(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invoice created successfully"));
    }

    @GetMapping
    @Operation(summary = "List user invoices with optional filters")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> listInvoices(
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) InvoicePaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<InvoiceResponse> page = invoiceService.listInvoices(userId, vendor, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Invoices retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.getInvoice(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update invoice and line items")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(@PathVariable UUID id,
                                                                      @Valid @RequestBody UpdateInvoiceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.updateInvoice(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an invoice")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        invoiceService.deleteInvoice(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoice deleted successfully"));
    }

    @PostMapping("/{id}/link-transaction")
    @Operation(summary = "Link existing finance transaction to invoice (idempotent, validates ownership)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> linkTransaction(@PathVariable UUID id,
                                                                        @Valid @RequestBody LinkTransactionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.linkTransaction(id, request.getTransactionId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction linked successfully"));
    }

    @DeleteMapping("/{id}/link-transaction")
    @Operation(summary = "Unlink finance transaction from invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> unlinkTransaction(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.unlinkTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction unlinked successfully"));
    }

    @PostMapping("/{id}/convert-to-transaction")
    @Operation(summary = "Convert invoice into an expense transaction (duplicate prevention)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> convertToTransaction(@PathVariable UUID id,
                                                                             @RequestBody(required = false) ConvertInvoiceToTransactionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.convertToTransaction(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invoice converted to transaction successfully"));
    }
}
