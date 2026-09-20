package com.lifeos.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmUsageDto {

    @Builder.Default
    private int promptTokens = 0;

    @Builder.Default
    private int completionTokens = 0;

    @Builder.Default
    private int totalTokens = 0;
}
