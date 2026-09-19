package com.lifeos.asset.controller;

import com.lifeos.asset.dto.AssetDocumentLinkResponse;
import com.lifeos.asset.service.AssetDocumentService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Asset Documents", description = "Endpoints for linking documents to assets, invoices, and warranty claims")
public class AssetDocumentController {

    private final AssetDocumentService assetDocumentService;

    @PostMapping("/{assetId}/documents/{documentId}")
    @Operation(summary = "Attach document to an asset")
    public ResponseEntity<ApiResponse<AssetDocumentLinkResponse>> attachDocumentToAsset(@PathVariable UUID assetId,
                                                                                        @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetDocumentLinkResponse response = assetDocumentService.attachDocumentToAsset(assetId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document attached to asset successfully"));
    }

    @DeleteMapping("/{assetId}/documents/{documentId}")
    @Operation(summary = "Detach document from an asset")
    public ResponseEntity<ApiResponse<Void>> detachDocumentFromAsset(@PathVariable UUID assetId,
                                                                     @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        assetDocumentService.detachDocumentFromAsset(assetId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document detached from asset successfully"));
    }

    @GetMapping("/{assetId}/documents")
    @Operation(summary = "List documents attached to an asset")
    public ResponseEntity<ApiResponse<List<AssetDocumentLinkResponse>>> listDocumentsForAsset(@PathVariable UUID assetId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AssetDocumentLinkResponse> list = assetDocumentService.listDocumentsForAsset(assetId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Asset documents retrieved successfully"));
    }

    @PostMapping("/invoices/{invoiceId}/documents/{documentId}")
    @Operation(summary = "Attach document to an invoice")
    public ResponseEntity<ApiResponse<AssetDocumentLinkResponse>> attachDocumentToInvoice(@PathVariable UUID invoiceId,
                                                                                          @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetDocumentLinkResponse response = assetDocumentService.attachDocumentToInvoice(invoiceId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document attached to invoice successfully"));
    }

    @DeleteMapping("/invoices/{invoiceId}/documents/{documentId}")
    @Operation(summary = "Detach document from an invoice")
    public ResponseEntity<ApiResponse<Void>> detachDocumentFromInvoice(@PathVariable UUID invoiceId,
                                                                       @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        assetDocumentService.detachDocumentFromInvoice(invoiceId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document detached from invoice successfully"));
    }

    @GetMapping("/invoices/{invoiceId}/documents")
    @Operation(summary = "List documents attached to an invoice")
    public ResponseEntity<ApiResponse<List<AssetDocumentLinkResponse>>> listDocumentsForInvoice(@PathVariable UUID invoiceId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AssetDocumentLinkResponse> list = assetDocumentService.listDocumentsForInvoice(invoiceId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Invoice documents retrieved successfully"));
    }

    @PostMapping("/warranties/claims/{claimId}/documents/{documentId}")
    @Operation(summary = "Attach evidence document to a warranty claim")
    public ResponseEntity<ApiResponse<AssetDocumentLinkResponse>> attachDocumentToWarrantyClaim(@PathVariable UUID claimId,
                                                                                                @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AssetDocumentLinkResponse response = assetDocumentService.attachDocumentToWarrantyClaim(claimId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document attached to warranty claim successfully"));
    }

    @DeleteMapping("/warranties/claims/{claimId}/documents/{documentId}")
    @Operation(summary = "Detach evidence document from a warranty claim")
    public ResponseEntity<ApiResponse<Void>> detachDocumentFromWarrantyClaim(@PathVariable UUID claimId,
                                                                             @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        assetDocumentService.detachDocumentFromWarrantyClaim(claimId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document detached from warranty claim successfully"));
    }

    @GetMapping("/warranties/claims/{claimId}/documents")
    @Operation(summary = "List evidence documents attached to a warranty claim")
    public ResponseEntity<ApiResponse<List<AssetDocumentLinkResponse>>> listDocumentsForWarrantyClaim(@PathVariable UUID claimId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AssetDocumentLinkResponse> list = assetDocumentService.listDocumentsForWarrantyClaim(claimId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Warranty claim documents retrieved successfully"));
    }
}
