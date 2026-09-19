package com.lifeos.healthcare.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.DocumentEntityLinkEntity;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.repository.DocumentEntityLinkRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.healthcare.dto.MedicalDocumentLinkResponse;
import com.lifeos.healthcare.entity.AppointmentEntity;
import com.lifeos.healthcare.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalDocumentService {

    public static final String ENTITY_TYPE_HEALTH_APPOINTMENT = "HEALTH_APPOINTMENT";

    private final AppointmentRepository appointmentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentEntityLinkRepository documentEntityLinkRepository;

    @Transactional
    public MedicalDocumentLinkResponse attachDocument(UUID appointmentId, UUID documentId, UUID userId) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(appointmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + appointmentId));

        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical document not found with id: " + documentId));

        if (!documentEntityLinkRepository.existsByEntityTypeAndEntityIdAndDocumentId(
                ENTITY_TYPE_HEALTH_APPOINTMENT, appointment.getId(), document.getId())) {
            DocumentEntityLinkEntity link = DocumentEntityLinkEntity.builder()
                    .document(document)
                    .entityType(ENTITY_TYPE_HEALTH_APPOINTMENT)
                    .entityId(appointment.getId())
                    .build();
            documentEntityLinkRepository.save(link);
            log.info("User [{}] attached medical document [{}] to appointment [{}]", userId, documentId, appointmentId);
        }

        return MedicalDocumentLinkResponse.fromEntity(document);
    }

    @Transactional
    public void detachDocument(UUID appointmentId, UUID documentId, UUID userId) {
        // Verify appointment ownership
        appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(appointmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + appointmentId));

        // Verify document ownership
        documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical document not found with id: " + documentId));

        documentEntityLinkRepository.deleteByEntityTypeAndEntityIdAndDocumentId(
                ENTITY_TYPE_HEALTH_APPOINTMENT, appointmentId, documentId);
        log.info("User [{}] detached medical document [{}] from appointment [{}]", userId, documentId, appointmentId);
    }

    @Transactional(readOnly = true)
    public Page<MedicalDocumentLinkResponse> listMedicalDocuments(
            UUID userId, DocumentType type, UUID dependentId, Pageable pageable) {
        Page<DocumentEntity> docs;
        if (dependentId != null) {
            docs = documentRepository.findAllByUserIdAndDependentIdAndCategoryAndIsDeletedFalse(
                    userId, dependentId, DocumentCategory.MEDICAL, pageable);
        } else {
            docs = documentRepository.findAllByUserIdAndCategoryAndIsDeletedFalse(
                    userId, DocumentCategory.MEDICAL, pageable);
        }

        return docs.map(MedicalDocumentLinkResponse::fromEntity);
    }
}
