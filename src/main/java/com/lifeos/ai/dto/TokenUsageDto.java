package com.lifeos.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageDto {

    @Builder.Default
    private int promptTokens = 0;

    @Builder.Default
    private int completionTokens = 0;

    @Builder.Default
    private int totalTokens = 0;
}
