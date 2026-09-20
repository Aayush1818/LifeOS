package com.lifeos.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantMessageResponse {

    private UUID conversationId;

    private ChatMessageDto userMessage;

    private ChatMessageDto assistantMessage;

    @Builder.Default
    private List<AssistantCitationDto> citations = new ArrayList<>();

    private boolean grounded;

    private boolean hasRelevantContext;

    private TokenUsageDto usage;

    private ModelMetadataDto modelMetadata;
}
