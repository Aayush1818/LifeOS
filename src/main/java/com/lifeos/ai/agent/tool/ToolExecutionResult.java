package com.lifeos.ai.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized execution result from a LifeOSTool invocation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionResult {

    private boolean success;

    private Object output;

    private String errorMessage;

    public static ToolExecutionResult success(Object output) {
        return ToolExecutionResult.builder()
                .success(true)
                .output(output)
                .build();
    }

    public static ToolExecutionResult failure(String errorMessage) {
        return ToolExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
