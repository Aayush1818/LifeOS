package com.lifeos.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingTripsResponse {

    private int windowDays;
    private int upcomingCount;
    private List<TripResponse> trips;
}
