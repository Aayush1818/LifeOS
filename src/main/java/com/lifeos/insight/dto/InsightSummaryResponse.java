package com.lifeos.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightSummaryResponse {
    private long totalActive;
    private long criticalCount;
    private long warningCount;
    private long infoCount;
    @Builder.Default
    private List<InsightResponse> insights = new ArrayList<>();
}
