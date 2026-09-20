package com.lifeos.ai.agent.tool;

import com.lifeos.ai.llm.ToolDefinitionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry that auto-discovers and manages all LifeOSTool implementations.
 */
@Slf4j
@Component
public class LifeOSToolRegistry {

    private final Map<String, LifeOSTool> tools = new ConcurrentHashMap<>();

    public LifeOSToolRegistry(List<LifeOSTool> toolBeans) {
        if (toolBeans != null) {
            for (LifeOSTool tool : toolBeans) {
                tools.put(tool.getName(), tool);
                log.info("Registered LifeOS agent tool: [{}] (requiresConfirmation: {})",
                        tool.getName(), tool.requiresConfirmation());
            }
        }
    }

    public Optional<LifeOSTool> getTool(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name.trim()));
    }

    public Collection<LifeOSTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Generates OpenAI-compatible tool declarations for prompt construction.
     */
    public List<ToolDefinitionDto> getToolDefinitions() {
        List<ToolDefinitionDto> definitions = new ArrayList<>();
        for (LifeOSTool tool : tools.values()) {
            definitions.add(ToolDefinitionDto.of(
                    tool.getName(),
                    tool.getDescription(),
                    tool.getParameterSchema().toOpenAiSchema()
            ));
        }
        return definitions;
    }

    /**
     * Executes a tool with the authenticated caller's userId.
     */
    public ToolExecutionResult execute(String toolName, UUID userId, Map<String, Object> parameters) {
        LifeOSTool tool = tools.get(toolName);
        if (tool == null) {
            log.warn("Tool [{}] not found in registry", toolName);
            return ToolExecutionResult.failure("Unknown tool: " + toolName);
        }

        try {
            return tool.execute(userId, parameters != null ? parameters : Map.of());
        } catch (Exception e) {
            log.error("Execution error in tool [{}] for user [{}]: {}", toolName, userId, e.getMessage(), e);
            return ToolExecutionResult.failure("Error executing tool " + toolName + ": " + e.getMessage());
        }
    }
}
