package com.lifeos.travel.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.DocumentEntityLinkEntity;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.repository.DocumentEntityLinkRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.travel.dto.TravelDocumentLinkResponse;
import com.lifeos.travel.repository.ItineraryItemRepository;
import com.lifeos.travel.repository.TripRepository;
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
public class TravelDocumentService {

    private final TripRepository tripRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final DocumentRepository documentRepository;
    private final DocumentEntityLinkRepository documentEntityLinkRepository;

    @Transactional
    public TravelDocumentLinkResponse attachDocumentToTrip(UUID tripId, UUID documentId, UUID userId) {
        log.info("Attaching document [{}] to trip [{}] for user [{}]", documentId, tripId, userId);

        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!documentEntityLinkRepository.existsByEntityTypeAndEntityIdAndDocumentId("TRIP", tripId, documentId)) {
            DocumentEntityLinkEntity link = DocumentEntityLinkEntity.builder()
                    .document(doc)
                    .entityType("TRIP")
                    .entityId(tripId)
                    .build();
            documentEntityLinkRepository.save(link);
        }

        return TravelDocumentLinkResponse.fromEntity(doc);
    }

    @Transactional
    public void detachDocumentFromTrip(UUID tripId, UUID documentId, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        documentEntityLinkRepository.deleteByEntityTypeAndEntityIdAndDocumentId("TRIP", tripId, documentId);
        log.info("Detached document [{}] from trip [{}]", documentId, tripId);
    }

    @Transactional
    public TravelDocumentLinkResponse attachDocumentToItineraryItem(UUID tripId, UUID itemId, UUID documentId, UUID userId) {
        log.info("Attaching document [{}] to itinerary item [{}] on trip [{}]", documentId, itemId, tripId);

        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        itineraryItemRepository.findByIdAndTripIdAndUserIdAndIsDeletedFalse(itemId, tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found with id: " + itemId));

        DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!documentEntityLinkRepository.existsByEntityTypeAndEntityIdAndDocumentId("ITINERARY_ITEM", itemId, documentId)) {
            DocumentEntityLinkEntity link = DocumentEntityLinkEntity.builder()
                    .document(doc)
                    .entityType("ITINERARY_ITEM")
                    .entityId(itemId)
                    .build();
            documentEntityLinkRepository.save(link);
        }

        return TravelDocumentLinkResponse.fromEntity(doc);
    }

    @Transactional
    public void detachDocumentFromItineraryItem(UUID tripId, UUID itemId, UUID documentId, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        itineraryItemRepository.findByIdAndTripIdAndUserIdAndIsDeletedFalse(itemId, tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found with id: " + itemId));

        documentEntityLinkRepository.deleteByEntityTypeAndEntityIdAndDocumentId("ITINERARY_ITEM", itemId, documentId);
        log.info("Detached document [{}] from itinerary item [{}]", documentId, itemId);
    }

    @Transactional(readOnly = true)
    public Page<TravelDocumentLinkResponse> listTravelDocuments(UUID userId, DocumentType type, Pageable pageable) {
        Page<DocumentEntity> page;
        if (type != null) {
            page = documentRepository.findAllByUserIdAndCategoryAndDocumentTypeAndIsDeletedFalse(
                    userId, DocumentCategory.TRAVEL, type, pageable);
        } else {
            page = documentRepository.findAllByUserIdAndCategoryAndIsDeletedFalse(
                    userId, DocumentCategory.TRAVEL, pageable);
        }
        return page.map(TravelDocumentLinkResponse::fromEntity);
    }
}
