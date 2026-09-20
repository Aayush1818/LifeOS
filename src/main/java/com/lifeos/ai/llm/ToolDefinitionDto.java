package com.lifeos.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * OpenAI-compatible tool/function definition for LLM tool declaration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinitionDto {

    @Builder.Default
    private String type = "function";

    private FunctionDefinition function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }

    public static ToolDefinitionDto of(String name, String description, Map<String, Object> parameters) {
        return ToolDefinitionDto.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name(name)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }
}
