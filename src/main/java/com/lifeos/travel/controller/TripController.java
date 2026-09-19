package com.lifeos.travel.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.travel.dto.AddTravelerRequest;
import com.lifeos.travel.dto.CreateTripRequest;
import com.lifeos.travel.dto.TravelDocumentLinkResponse;
import com.lifeos.travel.dto.TripDetailResponse;
import com.lifeos.travel.dto.TripResponse;
import com.lifeos.travel.dto.TripTravelerResponse;
import com.lifeos.travel.dto.UpcomingTripsResponse;
import com.lifeos.travel.dto.UpdateTripRequest;
import com.lifeos.travel.entity.TripStatus;
import com.lifeos.travel.service.TravelDocumentService;
import com.lifeos.travel.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/travel/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Travel trip management, destinations, travelers, and multi-currency budgets")
public class TripController {

    private final TripService tripService;
    private final TravelDocumentService travelDocumentService;

    @PostMapping
    @Operation(summary = "Create trip", description = "Creates a new trip with destination, dates, budget, and currency.")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody CreateTripRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TripResponse response = tripService.createTrip(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Trip created successfully"));
    }

    @GetMapping
    @Operation(summary = "List trips", description = "Retrieves paginated trips for the authenticated user.")
    public ResponseEntity<ApiResponse<Page<TripResponse>>> listTrips(
            @RequestParam(required = false) TripStatus status,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<TripResponse> page = tripService.listTrips(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Trips retrieved successfully"));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Upcoming trips", description = "Retrieves upcoming trips within the specified day window (default 30 days).")
    public ResponseEntity<ApiResponse<UpcomingTripsResponse>> getUpcomingTrips(
            @RequestParam(defaultValue = "30") int windowDays) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UpcomingTripsResponse response = tripService.getUpcomingTrips(userId, windowDays);
        return ResponseEntity.ok(ApiResponse.success(response, "Upcoming trips retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trip details", description = "Retrieves full trip details with travelers, itinerary, and spend summary.")
    public ResponseEntity<ApiResponse<TripDetailResponse>> getTrip(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TripDetailResponse response = tripService.getTrip(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update trip", description = "Modifies trip dates, budget, destination, notes, or status.")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TripResponse response = tripService.updateTrip(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Trip updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete trip", description = "Soft-deletes trip and associated itinerary items, dismissing reminders.")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        tripService.deleteTrip(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Trip deleted successfully"));
    }

    @PostMapping("/{id}/travelers")
    @Operation(summary = "Add traveler to trip", description = "Registers a user or dependent as a traveler for this trip.")
    public ResponseEntity<ApiResponse<TripTravelerResponse>> addTraveler(
            @PathVariable UUID id,
            @Valid @RequestBody AddTravelerRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TripTravelerResponse response = tripService.addTraveler(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Traveler added to trip successfully"));
    }

    @DeleteMapping("/{id}/travelers/{travelerId}")
    @Operation(summary = "Remove traveler", description = "Removes a registered traveler from this trip.")
    public ResponseEntity<ApiResponse<Void>> removeTraveler(
            @PathVariable UUID id,
            @PathVariable UUID travelerId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        tripService.removeTraveler(id, travelerId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Traveler removed from trip successfully"));
    }

    @PostMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Link document to trip", description = "Associates a travel visa, insurance policy, or itinerary PDF to the trip.")
    public ResponseEntity<ApiResponse<TravelDocumentLinkResponse>> attachDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TravelDocumentLinkResponse response = travelDocumentService.attachDocumentToTrip(id, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document linked to trip successfully"));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Unlink document from trip", description = "Removes a document link from this trip.")
    public ResponseEntity<ApiResponse<Void>> detachDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        travelDocumentService.detachDocumentFromTrip(id, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document unlinked from trip successfully"));
    }
}
