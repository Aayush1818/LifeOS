package com.lifeos.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    private String content;

    private String model;

    @Builder.Default
    private LlmUsageDto usage = new LlmUsageDto();

    @Builder.Default
    private String finishReason = "stop";

    @Builder.Default
    private Long durationMs = 0L;

    @Builder.Default
    private java.util.List<ToolCallDto> toolCalls = new java.util.ArrayList<>();

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
