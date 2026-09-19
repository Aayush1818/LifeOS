package com.lifeos.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingRenewalsResponse {

    private int windowDays;
    private int upcomingCount;
    private List<InsuranceResponse> policies;
}
