package com.lifeos.travel.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.travel.dto.CreateItineraryItemRequest;
import com.lifeos.travel.dto.ItineraryItemResponse;
import com.lifeos.travel.dto.TravelDocumentLinkResponse;
import com.lifeos.travel.dto.UpdateBookingStatusRequest;
import com.lifeos.travel.dto.UpdateItineraryItemRequest;
import com.lifeos.travel.service.ItineraryService;
import com.lifeos.travel.service.TravelDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/travel/trips/{tripId}/itinerary")
@RequiredArgsConstructor
@Tag(name = "Itinerary", description = "Trip itinerary planning, flights, hotels, activities, and timezone handling")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final TravelDocumentService travelDocumentService;

    @PostMapping
    @Operation(summary = "Add itinerary item", description = "Adds a flight, hotel, activity, or custom item to the trip itinerary.")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> createItineraryItem(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateItineraryItemRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ItineraryItemResponse response = itineraryService.createItineraryItem(tripId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Itinerary item created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get trip itinerary", description = "Retrieves chronological itinerary items for the specified trip.")
    public ResponseEntity<ApiResponse<List<ItineraryItemResponse>>> getItinerary(@PathVariable UUID tripId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<ItineraryItemResponse> list = itineraryService.getItinerary(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Itinerary retrieved successfully"));
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Get itinerary item details", description = "Retrieves details of a specific itinerary item with linked documents.")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> getItineraryItem(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ItineraryItemResponse response = itineraryService.getItineraryItem(tripId, itemId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Itinerary item retrieved successfully"));
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update itinerary item", description = "Updates item times, locations, timezones, booking reference, or cost.")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> updateItineraryItem(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItineraryItemRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ItineraryItemResponse response = itineraryService.updateItineraryItem(tripId, itemId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Itinerary item updated successfully"));
    }

    @PostMapping("/{itemId}/status")
    @Operation(summary = "Update booking status", description = "Transitions status (e.g. CANCELLED, COMPLETED) and dismisses linked reminders.")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> updateBookingStatus(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ItineraryItemResponse response = itineraryService.updateBookingStatus(tripId, itemId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking status updated successfully"));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete itinerary item", description = "Soft-deletes itinerary item and dismisses associated reminder.")
    public ResponseEntity<ApiResponse<Void>> deleteItineraryItem(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        itineraryService.deleteItineraryItem(tripId, itemId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Itinerary item deleted successfully"));
    }

    @PostMapping("/{itemId}/documents/{documentId}")
    @Operation(summary = "Link document to itinerary item", description = "Associates a flight boarding pass, train ticket, or hotel confirmation.")
    public ResponseEntity<ApiResponse<TravelDocumentLinkResponse>> attachDocument(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId,
            @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TravelDocumentLinkResponse response = travelDocumentService.attachDocumentToItineraryItem(tripId, itemId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document linked to itinerary item successfully"));
    }

    @DeleteMapping("/{itemId}/documents/{documentId}")
    @Operation(summary = "Unlink document from itinerary item", description = "Removes a document link from this itinerary item.")
    public ResponseEntity<ApiResponse<Void>> detachDocument(
            @PathVariable UUID tripId,
            @PathVariable UUID itemId,
            @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        travelDocumentService.detachDocumentFromItineraryItem(tripId, itemId, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document unlinked from itinerary item successfully"));
    }
}
