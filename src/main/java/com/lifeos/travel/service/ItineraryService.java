package com.lifeos.travel.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentEntityLinkEntity;
import com.lifeos.document.repository.DocumentEntityLinkRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.travel.dto.CreateItineraryItemRequest;
import com.lifeos.travel.dto.ItineraryItemResponse;
import com.lifeos.travel.dto.TravelDocumentLinkResponse;
import com.lifeos.travel.dto.UpdateBookingStatusRequest;
import com.lifeos.travel.dto.UpdateItineraryItemRequest;
import com.lifeos.travel.entity.BookingStatus;
import com.lifeos.travel.entity.ItineraryItemEntity;
import com.lifeos.travel.entity.ItineraryItemType;
import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.repository.ItineraryItemRepository;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryItemRepository itineraryItemRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;
    private final DocumentEntityLinkRepository documentEntityLinkRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public ItineraryItemResponse createItineraryItem(UUID tripId, CreateItineraryItemRequest request, UUID userId) {
        log.info("Creating itinerary item [{}] for trip [{}] user [{}]", request.getTitle(), tripId, userId);

        TripEntity trip = tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Temporal validation
        if (request.getEndTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Itinerary end time cannot be before start time");
        }

        // IANA Timezone validation
        validateTimeZone(request.getStartTimeZone());
        if (request.getEndTimeZone() != null) {
            validateTimeZone(request.getEndTimeZone());
        }

        ItineraryItemEntity item = ItineraryItemEntity.builder()
                .trip(trip)
                .user(user)
                .itemType(request.getItemType())
                .customTypeName(request.getCustomTypeName())
                .title(request.getTitle().trim())
                .provider(request.getProvider() != null ? request.getProvider().trim() : null)
                .bookingReference(request.getBookingReference() != null ? request.getBookingReference().trim() : null)
                .confirmationDetails(request.getConfirmationDetails())
                .startTime(request.getStartTime())
                .startTimeZone(request.getStartTimeZone() != null ? request.getStartTimeZone().trim() : "UTC")
                .startLocation(request.getStartLocation())
                .endTime(request.getEndTime())
                .endTimeZone(request.getEndTimeZone() != null ? request.getEndTimeZone().trim() : "UTC")
                .endLocation(request.getEndLocation())
                .status(request.getStatus() != null ? request.getStatus() : BookingStatus.CONFIRMED)
                .cost(request.getCost())
                .currency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : trip.getCurrency())
                .exchangeRateToBase(request.getExchangeRateToBase())
                .reminderOffsetMinutes(request.getReminderOffsetMinutes())
                .notes(request.getNotes())
                .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
                .build();

        ItineraryItemEntity savedItem = itineraryItemRepository.save(item);

        // Synchronize Reminder
        if (request.getReminderOffsetMinutes() != null && request.getReminderOffsetMinutes() > 0) {
            syncReminderForItem(savedItem, user);
        }

        return ItineraryItemResponse.fromEntity(savedItem);
    }

    @Transactional(readOnly = true)
    public List<ItineraryItemResponse> getItinerary(UUID tripId, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        List<ItineraryItemEntity> items = itineraryItemRepository
                .findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(tripId, userId);

        return items.stream()
                .map(item -> {
                    List<TravelDocumentLinkResponse> docs = getLinkedDocuments(item.getId(), userId);
                    return ItineraryItemResponse.fromEntity(item, docs);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItineraryItemResponse getItineraryItem(UUID tripId, UUID itemId, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        ItineraryItemEntity item = itineraryItemRepository
                .findByIdAndTripIdAndUserIdAndIsDeletedFalse(itemId, tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found with id: " + itemId));

        List<TravelDocumentLinkResponse> docs = getLinkedDocuments(itemId, userId);
        return ItineraryItemResponse.fromEntity(item, docs);
    }

    @Transactional
    public ItineraryItemResponse updateItineraryItem(UUID tripId, UUID itemId, UpdateItineraryItemRequest request, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        ItineraryItemEntity item = itineraryItemRepository
                .findByIdAndTripIdAndUserIdAndIsDeletedFalse(itemId, tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found with id: " + itemId));

        OffsetDateTime start = request.getStartTime() != null ? request.getStartTime() : item.getStartTime();
        OffsetDateTime end = request.getEndTime() != null ? request.getEndTime() : item.getEndTime();

        if (end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Itinerary end time cannot be before start time");
        }

        if (request.getStartTimeZone() != null) {
            validateTimeZone(request.getStartTimeZone());
            item.setStartTimeZone(request.getStartTimeZone().trim());
        }
        if (request.getEndTimeZone() != null) {
            validateTimeZone(request.getEndTimeZone());
            item.setEndTimeZone(request.getEndTimeZone().trim());
        }

        if (request.getItemType() != null) {
            item.setItemType(request.getItemType());
        }
        if (request.getCustomTypeName() != null) {
            item.setCustomTypeName(request.getCustomTypeName());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getProvider() != null) {
            item.setProvider(request.getProvider().trim());
        }
        if (request.getBookingReference() != null) {
            item.setBookingReference(request.getBookingReference().trim());
        }
        if (request.getConfirmationDetails() != null) {
            item.setConfirmationDetails(request.getConfirmationDetails());
        }
        item.setStartTime(start);
        item.setEndTime(end);

        if (request.getStartLocation() != null) {
            item.setStartLocation(request.getStartLocation());
        }
        if (request.getEndLocation() != null) {
            item.setEndLocation(request.getEndLocation());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        if (request.getCost() != null) {
            item.setCost(request.getCost());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            item.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        if (request.getExchangeRateToBase() != null) {
            item.setExchangeRateToBase(request.getExchangeRateToBase());
        }
        if (request.getReminderOffsetMinutes() != null) {
            item.setReminderOffsetMinutes(request.getReminderOffsetMinutes());
        }
        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }
        if (request.getMetadata() != null) {
            item.setMetadata(request.getMetadata());
        }

        ItineraryItemEntity updated = itineraryItemRepository.save(item);

        // Update reminder due date if start time or offset changed
        if (updated.getReminderOffsetMinutes() != null && updated.getReminderOffsetMinutes() > 0) {
            syncReminderForItem(updated, updated.getUser());
        }

        List<TravelDocumentLinkResponse> docs = getLinkedDocuments(itemId, userId);
        return ItineraryItemResponse.fromEntity(updated, docs);
    }

    @Transactional
    public ItineraryItemResponse updateBookingStatus(UUID tripId, UUID itemId, UpdateBookingStatusRequest request, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        ItineraryItemEntity item = itineraryItemRepository
                .findByIdAndTripIdAndUserIdAndIsDeletedFalse(itemId, tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found with id: " + itemId));

        item.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }

        ItineraryItemEntity updated = itineraryItemRepository.save(item);

        // If status changed to CANCELLED or COMPLETED, dismiss active reminder
        if (request.getStatus() == BookingStatus.CANCELLED || request.getStatus() == BookingStatus.COMPLETED) {
            reminderRepository.findByTargetEntityIdAndIsDeletedFalse(itemId)
                    .ifPresent(reminder -> {
                        reminder.setStatus(ReminderStatus.DISMISSED);
                        reminderRepository.save(reminder);
                        log.info("Dismissed reminder for itinerary item [{}] due to status [{}]", itemId, request.getStatus());
                    });
        }

        List<TravelDocumentLinkResponse> docs = getLinkedDocuments(itemId, userId);
        return ItineraryItemResponse.fromEntity(updated, docs);
    }

    @Transactional
    public void deleteItineraryItem(UUID tripId, UUID itemId, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        ItineraryItemEntity item = itineraryItemRepository
                .findByIdAndTripIdAndUserIdAndIsDeletedFalse(itemId, tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary item not found with id: " + itemId));

        item.setDeleted(true);
        itineraryItemRepository.save(item);

        // Dismiss associated reminder
        reminderRepository.findByTargetEntityIdAndIsDeletedFalse(itemId)
                .ifPresent(reminder -> {
                    reminder.setStatus(ReminderStatus.DISMISSED);
                    reminderRepository.save(reminder);
                });

        log.info("Soft-deleted itinerary item [{}] and dismissed reminder", itemId);
    }

    private void syncReminderForItem(ItineraryItemEntity item, UserEntity user) {
        OffsetDateTime dueAt = item.getStartTime().minusMinutes(item.getReminderOffsetMinutes());
        String reminderType = determineReminderType(item.getItemType());

        ReminderEntity reminder = reminderRepository
                .findByTargetEntityIdAndIsDeletedFalse(item.getId())
                .orElse(ReminderEntity.builder()
                        .user(user)
                        .targetEntityId(item.getId())
                        .recurrencePattern("ONCE")
                        .status(ReminderStatus.ACTIVE)
                        .build());

        reminder.setTitle("Upcoming " + item.getItemType() + ": " + item.getTitle());
        reminder.setDescription("Scheduled for " + item.getStartTime() + " (" + item.getStartTimeZone() + ") at " + (item.getStartLocation() != null ? item.getStartLocation() : ""));
        reminder.setDueAt(dueAt);
        reminder.setReminderType(reminderType);
        reminder.setStatus(ReminderStatus.ACTIVE);

        reminderRepository.save(reminder);
        log.info("Synchronized [{}] reminder for itinerary item [{}] due at [{}]", reminderType, item.getId(), dueAt);
    }

    private String determineReminderType(ItineraryItemType type) {
        return switch (type) {
            case FLIGHT, TRAIN, BUS, TRANSFER -> "TRAVEL_DEPARTURE";
            case LODGING -> "TRAVEL_CHECKIN";
            case ACTIVITY, RESTAURANT -> "TRAVEL_ACTIVITY";
            default -> "TRAVEL_GENERAL";
        };
    }

    private void validateTimeZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return;
        }
        try {
            ZoneId.of(zoneId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IANA timezone identifier: " + zoneId);
        }
    }

    private List<TravelDocumentLinkResponse> getLinkedDocuments(UUID itemId, UUID userId) {
        List<DocumentEntityLinkEntity> links = documentEntityLinkRepository.findAllByEntityTypeAndEntityId("ITINERARY_ITEM", itemId);
        List<TravelDocumentLinkResponse> docs = new ArrayList<>();
        for (DocumentEntityLinkEntity link : links) {
            documentRepository.findByIdAndUserIdAndIsDeletedFalse(link.getDocument().getId(), userId)
                    .ifPresent(doc -> docs.add(TravelDocumentLinkResponse.fromEntity(doc)));
        }
        return docs;
    }
}
