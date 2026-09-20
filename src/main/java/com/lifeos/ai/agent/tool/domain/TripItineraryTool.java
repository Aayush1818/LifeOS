package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.travel.dto.TripDetailResponse;
import com.lifeos.travel.dto.TripResponse;
import com.lifeos.travel.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Tool for retrieving user's active/upcoming trips and itinerary bookings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripItineraryTool implements LifeOSTool {

    private final TripService tripService;

    @Override
    public String getName() {
        return "get_trip_itinerary";
    }

    @Override
    public String getDescription() {
        return "Retrieves user's planned/upcoming trips, destinations, dates, budgets, and detailed itinerary bookings.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("tripId")
                        .type("string")
                        .description("Optional trip UUID to get detailed bookings/itinerary. If omitted, lists all trips.")
                        .required(false)
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.debug("Executing TripItineraryTool for user [{}]", userId);
        try {
            if (parameters != null && parameters.containsKey("tripId") && parameters.get("tripId") != null) {
                String tripIdStr = parameters.get("tripId").toString().trim();
                if (!tripIdStr.isBlank()) {
                    try {
                        UUID tripId = UUID.fromString(tripIdStr);
                        TripDetailResponse details = tripService.getTrip(tripId, userId);
                        return ToolExecutionResult.success(details);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            Page<TripResponse> trips = tripService.listTrips(userId, null, PageRequest.of(0, 20));
            return ToolExecutionResult.success(trips.getContent());
        } catch (Exception e) {
            log.error("Failed to fetch trips for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to fetch trips: " + e.getMessage());
        }
    }
}
