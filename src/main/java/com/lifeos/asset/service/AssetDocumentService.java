package com.lifeos.asset.service;

import com.lifeos.asset.dto.AssetDocumentLinkResponse;
import com.lifeos.asset.entity.AssetEntity;
import com.lifeos.asset.entity.InvoiceEntity;
import com.lifeos.asset.entity.WarrantyClaimEntity;
import com.lifeos.asset.repository.AssetRepository;
import com.lifeos.asset.repository.InvoiceRepository;
import com.lifeos.asset.repository.WarrantyClaimRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.DocumentEntityLinkEntity;
import com.lifeos.document.repository.DocumentEntityLinkRepository;
import com.lifeos.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetDocumentService {

    private final AssetRepository assetRepository;
    private final InvoiceRepository invoiceRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final DocumentRepository documentRepository;
    private final DocumentEntityLinkRepository documentEntityLinkRepository;

    @Transactional
    public AssetDocumentLinkResponse attachDocumentToAsset(UUID assetId, UUID documentId, UUID userId) {
        log.info("Attaching document [{}] to asset [{}] for user [{}]", documentId, assetId, userId);

        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!documentEntityLinkRepository.existsByEntityTypeAndEntityIdAndDocumentId("ASSET", assetId, documentId)) {
            DocumentEntityLinkEntity link = DocumentEntityLinkEntity.builder()
                    .document(doc)
                    .entityType("ASSET")
                    .entityId(assetId)
                    .build();
            documentEntityLinkRepository.save(link);
        }

        return AssetDocumentLinkResponse.fromEntity(doc);
    }

    @Transactional
    public void detachDocumentFromAsset(UUID assetId, UUID documentId, UUID userId) {
        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        documentEntityLinkRepository.deleteByEntityTypeAndEntityIdAndDocumentId("ASSET", assetId, documentId);
        log.info("Detached document [{}] from asset [{}]", documentId, assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetDocumentLinkResponse> listDocumentsForAsset(UUID assetId, UUID userId) {
        assetRepository.findByIdAndUserIdAndIsDeletedFalse(assetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        return documentEntityLinkRepository.findAllByEntityTypeAndEntityId("ASSET", assetId).stream()
                .map(DocumentEntityLinkEntity::getDocument)
                .filter(doc -> !doc.isDeleted())
                .map(AssetDocumentLinkResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AssetDocumentLinkResponse attachDocumentToInvoice(UUID invoiceId, UUID documentId, UUID userId) {
        log.info("Attaching document [{}] to invoice [{}] for user [{}]", documentId, invoiceId, userId);

        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        invoice.setDocumentId(documentId);
        invoiceRepository.save(invoice);

        if (!documentEntityLinkRepository.existsByEntityTypeAndEntityIdAndDocumentId("INVOICE", invoiceId, documentId)) {
            DocumentEntityLinkEntity link = DocumentEntityLinkEntity.builder()
                    .document(doc)
                    .entityType("INVOICE")
                    .entityId(invoiceId)
                    .build();
            documentEntityLinkRepository.save(link);
        }

        return AssetDocumentLinkResponse.fromEntity(doc);
    }

    @Transactional
    public void detachDocumentFromInvoice(UUID invoiceId, UUID documentId, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (documentId.equals(invoice.getDocumentId())) {
            invoice.setDocumentId(null);
            invoiceRepository.save(invoice);
        }

        documentEntityLinkRepository.deleteByEntityTypeAndEntityIdAndDocumentId("INVOICE", invoiceId, documentId);
        log.info("Detached document [{}] from invoice [{}]", documentId, invoiceId);
    }

    @Transactional(readOnly = true)
    public List<AssetDocumentLinkResponse> listDocumentsForInvoice(UUID invoiceId, UUID userId) {
        invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        return documentEntityLinkRepository.findAllByEntityTypeAndEntityId("INVOICE", invoiceId).stream()
                .map(DocumentEntityLinkEntity::getDocument)
                .filter(doc -> !doc.isDeleted())
                .map(AssetDocumentLinkResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AssetDocumentLinkResponse attachDocumentToWarrantyClaim(UUID claimId, UUID documentId, UUID userId) {
        log.info("Attaching document [{}] to warranty claim [{}] for user [{}]", documentId, claimId, userId);

        warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));

        DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!documentEntityLinkRepository.existsByEntityTypeAndEntityIdAndDocumentId("WARRANTY_CLAIM", claimId, documentId)) {
            DocumentEntityLinkEntity link = DocumentEntityLinkEntity.builder()
                    .document(doc)
                    .entityType("WARRANTY_CLAIM")
                    .entityId(claimId)
                    .build();
            documentEntityLinkRepository.save(link);
        }

        return AssetDocumentLinkResponse.fromEntity(doc);
    }

    @Transactional
    public void detachDocumentFromWarrantyClaim(UUID claimId, UUID documentId, UUID userId) {
        warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));

        documentEntityLinkRepository.deleteByEntityTypeAndEntityIdAndDocumentId("WARRANTY_CLAIM", claimId, documentId);
        log.info("Detached document [{}] from warranty claim [{}]", documentId, claimId);
    }

    @Transactional(readOnly = true)
    public List<AssetDocumentLinkResponse> listDocumentsForWarrantyClaim(UUID claimId, UUID userId) {
        warrantyClaimRepository.findByIdAndUserIdAndIsDeletedFalse(claimId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found with id: " + claimId));

        return documentEntityLinkRepository.findAllByEntityTypeAndEntityId("WARRANTY_CLAIM", claimId).stream()
                .map(DocumentEntityLinkEntity::getDocument)
                .filter(doc -> !doc.isDeleted())
                .map(AssetDocumentLinkResponse::fromEntity)
                .toList();
    }
}
