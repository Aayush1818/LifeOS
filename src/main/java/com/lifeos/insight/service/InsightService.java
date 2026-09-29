package com.lifeos.insight.service;

import com.lifeos.insight.dto.InsightResponse;
import com.lifeos.insight.dto.InsightSummaryResponse;
import com.lifeos.insight.entity.InsightSeverity;

import java.util.UUID;

public interface InsightService {
    InsightSummaryResponse generateInsights(UUID userId);
    InsightSummaryResponse getActiveInsights(UUID userId, InsightSeverity minSeverity);
    InsightResponse dismissInsight(UUID userId, UUID insightId);
    InsightResponse actionInsight(UUID userId, UUID insightId);
}
