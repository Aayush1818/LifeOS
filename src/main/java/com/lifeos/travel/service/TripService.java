package com.lifeos.travel.service;

import com.lifeos.common.exception.DuplicateResourceException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.entity.DocumentEntityLinkEntity;
import com.lifeos.document.repository.DocumentEntityLinkRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.travel.dto.AddTravelerRequest;
import com.lifeos.travel.dto.CreateTripRequest;
import com.lifeos.travel.dto.ItineraryItemResponse;
import com.lifeos.travel.dto.TravelDocumentLinkResponse;
import com.lifeos.travel.dto.TripDetailResponse;
import com.lifeos.travel.dto.TripResponse;
import com.lifeos.travel.dto.TripSpendSummary;
import com.lifeos.travel.dto.TripTravelerResponse;
import com.lifeos.travel.dto.UpcomingTripsResponse;
import com.lifeos.travel.dto.UpdateTripRequest;
import com.lifeos.travel.entity.BookingStatus;
import com.lifeos.travel.entity.ItineraryItemEntity;
import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.entity.TripStatus;
import com.lifeos.travel.entity.TripTravelerEntity;
import com.lifeos.travel.repository.ItineraryItemRepository;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.travel.repository.TripTravelerRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripTravelerRepository tripTravelerRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final UserRepository userRepository;
    private final DependentRepository dependentRepository;
    private final ReminderRepository reminderRepository;
    private final DocumentEntityLinkRepository documentEntityLinkRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, UUID userId) {
        log.info("Creating trip [{}] for user [{}]", request.getTripTitle(), userId);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Trip end date cannot be before start date");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        TripEntity trip = TripEntity.builder()
                .user(user)
                .destination(request.getDestination().trim())
                .tripTitle(request.getTripTitle().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalBudget(request.getTotalBudget() != null ? request.getTotalBudget() : BigDecimal.ZERO)
                .actualSpend(BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD")
                .status(TripStatus.PLANNED)
                .notes(request.getNotes())
                .coverImageUrl(request.getCoverImageUrl())
                .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
                .build();

        TripEntity savedTrip = tripRepository.save(trip);

        // Automatically add primary user as organizer/traveler
        String primaryName = (user.getFirstName() != null ? user.getFirstName() + " " : "")
                + (user.getLastName() != null ? user.getLastName() : "Primary Traveler");
        TripTravelerEntity traveler = TripTravelerEntity.builder()
                .trip(savedTrip)
                .travelerName(primaryName.trim().isEmpty() ? "Primary Traveler" : primaryName.trim())
                .isPrimaryUser(true)
                .notes("Trip Organizer")
                .build();
        tripTravelerRepository.save(traveler);

        return TripResponse.fromEntity(savedTrip, 1, 0);
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTrip(UUID tripId, UUID userId) {
        TripEntity trip = tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        List<TripTravelerEntity> travelers = tripTravelerRepository.findAllByTripIdOrderByCreatedAtAsc(tripId);
        List<TripTravelerResponse> travelerResponses = travelers.stream()
                .map(TripTravelerResponse::fromEntity)
                .collect(Collectors.toList());

        List<ItineraryItemEntity> itineraryItems = itineraryItemRepository
                .findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(tripId, userId);

        List<ItineraryItemResponse> itineraryResponses = itineraryItems.stream()
                .map(item -> {
                    List<TravelDocumentLinkResponse> itemDocs = getLinkedDocumentsForEntity("ITINERARY_ITEM", item.getId(), userId);
                    return ItineraryItemResponse.fromEntity(item, itemDocs);
                })
                .collect(Collectors.toList());

        TripSpendSummary spendSummary = calculateSpendSummary(trip, itineraryItems);
        List<TravelDocumentLinkResponse> tripDocs = getLinkedDocumentsForEntity("TRIP", trip.getId(), userId);

        return TripDetailResponse.fromEntity(trip, travelerResponses, itineraryResponses, spendSummary, tripDocs);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(UUID userId, TripStatus status, Pageable pageable) {
        Page<TripEntity> page;
        if (status != null) {
            page = tripRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, status, pageable);
        } else {
            page = tripRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
        }

        return page.map(trip -> {
            List<TripTravelerEntity> travelers = tripTravelerRepository.findAllByTripIdOrderByCreatedAtAsc(trip.getId());
            List<ItineraryItemEntity> items = itineraryItemRepository.findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(trip.getId(), userId);
            return TripResponse.fromEntity(trip, travelers.size(), items.size());
        });
    }

    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, UUID userId) {
        TripEntity trip = tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : trip.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : trip.getEndDate();

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Trip end date cannot be before start date");
        }

        if (request.getDestination() != null && !request.getDestination().isBlank()) {
            trip.setDestination(request.getDestination().trim());
        }
        if (request.getTripTitle() != null && !request.getTripTitle().isBlank()) {
            trip.setTripTitle(request.getTripTitle().trim());
        }
        trip.setStartDate(startDate);
        trip.setEndDate(endDate);

        if (request.getTotalBudget() != null) {
            trip.setTotalBudget(request.getTotalBudget());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            trip.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        if (request.getStatus() != null) {
            trip.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            trip.setNotes(request.getNotes());
        }
        if (request.getCoverImageUrl() != null) {
            trip.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getMetadata() != null) {
            trip.setMetadata(request.getMetadata());
        }

        TripEntity updated = tripRepository.save(trip);
        int travelersCount = tripTravelerRepository.findAllByTripIdOrderByCreatedAtAsc(tripId).size();
        int itemsCount = itineraryItemRepository.findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(tripId, userId).size();

        return TripResponse.fromEntity(updated, travelersCount, itemsCount);
    }

    @Transactional
    public void deleteTrip(UUID tripId, UUID userId) {
        TripEntity trip = tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        trip.setDeleted(true);
        tripRepository.save(trip);

        // Soft-delete all itinerary items and dismiss associated reminders
        List<ItineraryItemEntity> items = itineraryItemRepository
                .findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(tripId, userId);

        for (ItineraryItemEntity item : items) {
            item.setDeleted(true);
            itineraryItemRepository.save(item);

            reminderRepository.findByTargetEntityIdAndIsDeletedFalse(item.getId())
                    .ifPresent(reminder -> {
                        reminder.setStatus(ReminderStatus.DISMISSED);
                        reminderRepository.save(reminder);
                    });
        }
        log.info("Soft-deleted trip [{}] and [{}] associated itinerary items", tripId, items.size());
    }

    @Transactional(readOnly = true)
    public UpcomingTripsResponse getUpcomingTrips(UUID userId, int windowDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(windowDays > 0 ? windowDays : 30);

        List<TripEntity> upcoming = tripRepository
                .findAllByUserIdAndEndDateGreaterThanEqualAndIsDeletedFalseOrderByStartDateAsc(userId, today)
                .stream()
                .filter(t -> !t.getStartDate().isAfter(cutoff))
                .filter(t -> t.getStatus() != TripStatus.CANCELLED && t.getStatus() != TripStatus.COMPLETED)
                .collect(Collectors.toList());

        List<TripResponse> tripResponses = upcoming.stream()
                .map(t -> {
                    int trav = tripTravelerRepository.findAllByTripIdOrderByCreatedAtAsc(t.getId()).size();
                    int items = itineraryItemRepository.findAllByTripIdAndUserIdAndIsDeletedFalseOrderByStartTimeAsc(t.getId(), userId).size();
                    return TripResponse.fromEntity(t, trav, items);
                })
                .collect(Collectors.toList());

        return UpcomingTripsResponse.builder()
                .windowDays(windowDays > 0 ? windowDays : 30)
                .upcomingCount(tripResponses.size())
                .trips(tripResponses)
                .build();
    }

    @Transactional
    public TripTravelerResponse addTraveler(UUID tripId, AddTravelerRequest request, UUID userId) {
        TripEntity trip = tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        DependentEntity dependent = null;
        if (request.getDependentId() != null) {
            dependent = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));

            if (tripTravelerRepository.findByTripIdAndDependentId(tripId, request.getDependentId()).isPresent()) {
                throw new DuplicateResourceException("Dependent is already registered as a traveler for this trip");
            }
        }

        TripTravelerEntity traveler = TripTravelerEntity.builder()
                .trip(trip)
                .dependent(dependent)
                .travelerName(request.getTravelerName().trim())
                .isPrimaryUser(request.isPrimaryUser())
                .notes(request.getNotes())
                .build();

        TripTravelerEntity saved = tripTravelerRepository.save(traveler);
        return TripTravelerResponse.fromEntity(saved);
    }

    @Transactional
    public void removeTraveler(UUID tripId, UUID travelerId, UUID userId) {
        tripRepository.findByIdAndUserIdAndIsDeletedFalse(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        TripTravelerEntity traveler = tripTravelerRepository.findByIdAndTripId(travelerId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found with id: " + travelerId));

        if (traveler.isPrimaryUser()) {
            throw new IllegalArgumentException("Cannot remove the primary organizer from the trip");
        }

        tripTravelerRepository.delete(traveler);
    }

    /**
     * Strict multi-currency aggregation:
     * Never silently mixes amounts in different currencies. Groups totals by currency.
     * Consolidated total is ONLY computed if all items are in base currency or explicit exchange rates are provided.
     */
    public TripSpendSummary calculateSpendSummary(TripEntity trip, List<ItineraryItemEntity> items) {
        String baseCurrency = trip.getCurrency() != null ? trip.getCurrency() : "USD";
        Map<String, BigDecimal> totalsByCurrency = new HashMap<>();

        boolean allInBase = true;
        boolean allForeignHaveRates = true;
        BigDecimal consolidated = BigDecimal.ZERO;

        for (ItineraryItemEntity item : items) {
            if (item.getCost() != null && item.getStatus() != BookingStatus.CANCELLED) {
                String itemCurrency = item.getCurrency() != null ? item.getCurrency() : "USD";
                totalsByCurrency.merge(itemCurrency, item.getCost(), BigDecimal::add);

                if (itemCurrency.equalsIgnoreCase(baseCurrency)) {
                    consolidated = consolidated.add(item.getCost());
                } else {
                    allInBase = false;
                    if (item.getExchangeRateToBase() != null && item.getExchangeRateToBase().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal converted = item.getCost().multiply(item.getExchangeRateToBase())
                                .setScale(2, RoundingMode.HALF_UP);
                        consolidated = consolidated.add(converted);
                    } else {
                        allForeignHaveRates = false;
                    }
                }
            }
        }

        if (allInBase) {
            return TripSpendSummary.builder()
                    .baseCurrency(baseCurrency)
                    .totalsByCurrency(totalsByCurrency)
                    .consolidatedTotal(consolidated.setScale(2, RoundingMode.HALF_UP))
                    .hasUnconvertedCurrencies(false)
                    .conversionPolicyNotice("All expenses recorded in base currency [" + baseCurrency + "].")
                    .build();
        } else if (allForeignHaveRates) {
            return TripSpendSummary.builder()
                    .baseCurrency(baseCurrency)
                    .totalsByCurrency(totalsByCurrency)
                    .consolidatedTotal(consolidated.setScale(2, RoundingMode.HALF_UP))
                    .hasUnconvertedCurrencies(false)
                    .conversionPolicyNotice("Foreign currencies converted to [" + baseCurrency + "] using explicitly recorded exchange rates.")
                    .build();
        } else {
            return TripSpendSummary.builder()
                    .baseCurrency(baseCurrency)
                    .totalsByCurrency(totalsByCurrency)
                    .consolidatedTotal(null)
                    .hasUnconvertedCurrencies(true)
                    .conversionPolicyNotice("Trip contains mixed currencies (" + totalsByCurrency.keySet()
                            + ") without explicit exchange rates. Consolidated total is strictly omitted to prevent erroneous calculation.")
                    .build();
        }
    }

    private List<TravelDocumentLinkResponse> getLinkedDocumentsForEntity(String entityType, UUID entityId, UUID userId) {
        List<DocumentEntityLinkEntity> links = documentEntityLinkRepository.findAllByEntityTypeAndEntityId(entityType, entityId);
        List<TravelDocumentLinkResponse> docs = new ArrayList<>();
        for (DocumentEntityLinkEntity link : links) {
            documentRepository.findByIdAndUserIdAndIsDeletedFalse(link.getDocument().getId(), userId)
                    .ifPresent(doc -> docs.add(TravelDocumentLinkResponse.fromEntity(doc)));
        }
        return docs;
    }
}
