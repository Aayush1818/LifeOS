package com.lifeos.ai.agent.tool;

import java.util.Map;
import java.util.UUID;

/**
 * Service Provider Interface (SPI) for all LifeOS domain agent tools.
 * Tools are discovered automatically by LifeOSToolRegistry as Spring components.
 */
public interface LifeOSTool {

    /**
     * Unique identifier of the tool (e.g., 'get_loan_summary', 'record_loan_payment').
     */
    String getName();

    /**
     * Human and LLM readable description of what the tool accomplishes.
     */
    String getDescription();

    /**
     * JSON Schema of input parameters accepted by this tool.
     */
    ToolParameterSchema getParameterSchema();

    /**
     * Declares whether this tool performs state mutations.
     * If true, execution is intercepted by the agent orchestrator and requires
     * explicit user confirmation via an action card.
     */
    boolean requiresConfirmation();

    /**
     * Human-friendly confirmation prompt to display on the action card.
     *
     * @param parameters parsed tool arguments
     * @return prompt text, e.g., "Would you like me to record this payment of $500?"
     */
    default String getConfirmationPrompt(Map<String, Object> parameters) {
        return "Confirm execution of action: " + getName();
    }

    /**
     * Executes the domain logic for the specified authenticated user.
     *
     * @param userId     authenticated caller ID (enforced server-side)
     * @param parameters validated tool parameters
     * @return standardized execution result
     */
    ToolExecutionResult execute(UUID userId, Map<String, Object> parameters);
}
