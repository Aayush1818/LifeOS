package com.lifeos.ai.llm;

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
public class LlmRequest {

    @Builder.Default
    private List<LlmMessageDto> messages = new ArrayList<>();

    @Builder.Default
    private Double temperature = 0.1;

    @Builder.Default
    private Integer maxTokens = 1024;

    @Builder.Default
    private Double topP = 1.0;

    @Builder.Default
    private List<ToolDefinitionDto> tools = new ArrayList<>();
}
