package com.lifeos.ai.grounding;

import com.lifeos.ai.dto.AssistantCitationDto;
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
public class ValidatedCitationResult {

    private String sanitizedContent;

    @Builder.Default
    private List<AssistantCitationDto> validCitations = new ArrayList<>();

    @Builder.Default
    private int invalidCitationsRemoved = 0;
}
