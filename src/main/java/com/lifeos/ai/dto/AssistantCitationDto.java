package com.lifeos.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Client-facing citation DTO representing a verified source reference.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantCitationDto {

    private Integer citationIndex;

    private UUID documentId;

    private String documentTitle;

    private Integer documentVersion;

    private UUID chunkId;

    private Integer pageNumber;

    private String sectionTitle;

    private String snippet;

    private String sourceCitation;

    private Double relevanceScore;
}
