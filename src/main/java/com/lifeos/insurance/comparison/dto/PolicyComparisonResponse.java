package com.lifeos.insurance.comparison.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyComparisonResponse {
    private UUID documentId1;
    private String documentTitle1;
    private UUID documentId2;
    private String documentTitle2;
    private String summary;
    private List<ClauseDiffDto> clauseDifferences;
    private List<String> addedBenefits;
    private List<String> removedBenefits;
    private String premiumAnalysis;
    private String overallRecommendation;
    private OffsetDateTime comparedAt;
}
