package com.lifeos.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Representation of a tool/function call requested by the LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallDto {

    private String id;

    @Builder.Default
    private String type = "function";

    private String name;

    private Map<String, Object> arguments;

    private String rawArguments;
}
