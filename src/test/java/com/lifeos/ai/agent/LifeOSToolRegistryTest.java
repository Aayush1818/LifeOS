package com.lifeos.ai.agent;

import com.lifeos.ai.agent.tool.*;
import com.lifeos.ai.llm.ToolDefinitionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LifeOSToolRegistryTest {

    private static class DummyTool implements LifeOSTool {
        private final String name;
        private final boolean requiresConfirmation;

        DummyTool(String name, boolean requiresConfirmation) {
            this.name = name;
            this.requiresConfirmation = requiresConfirmation;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "Test tool description for " + name;
        }

        @Override
        public ToolParameterSchema getParameterSchema() {
            return ToolParameterSchema.of(
                    ToolParameter.string("query", "Search query", true),
                    ToolParameter.integer("limit", "Max results", false)
            );
        }

        @Override
        public boolean requiresConfirmation() {
            return requiresConfirmation;
        }

        @Override
        public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
            if ("fail".equals(parameters.get("query"))) {
                throw new RuntimeException("Simulated tool crash");
            }
            return ToolExecutionResult.success("Executed " + name + " with query: " + parameters.get("query"));
        }
    }

    @Test
    @DisplayName("Should register all provided tools and retrieve by name")
    void shouldRegisterAndRetrieveTools() {
        DummyTool tool1 = new DummyTool("test_tool_1", false);
        DummyTool tool2 = new DummyTool("test_tool_2", true);

        LifeOSToolRegistry registry = new LifeOSToolRegistry(List.of(tool1, tool2));

        assertThat(registry.getAllTools()).hasSize(2);
        assertThat(registry.getTool("test_tool_1")).isPresent();
        assertThat(registry.getTool("test_tool_2")).isPresent();
        assertThat(registry.getTool("non_existent")).isEmpty();
        assertThat(registry.getTool(null)).isEmpty();
    }

    @Test
    @DisplayName("Should generate valid OpenAI tool definitions from registered tools")
    void shouldGenerateToolDefinitions() {
        DummyTool tool1 = new DummyTool("search_items", false);
        LifeOSToolRegistry registry = new LifeOSToolRegistry(List.of(tool1));

        List<ToolDefinitionDto> definitions = registry.getToolDefinitions();

        assertThat(definitions).hasSize(1);
        ToolDefinitionDto def = definitions.getFirst();
        assertThat(def.getType()).isEqualTo("function");
        assertThat(def.getFunction().getName()).isEqualTo("search_items");
        assertThat(def.getFunction().getDescription()).isEqualTo("Test tool description for search_items");
        assertThat(def.getFunction().getParameters()).isNotNull();

        Map<String, Object> params = def.getFunction().getParameters();
        assertThat(params.get("type")).isEqualTo("object");
        assertThat(params.get("required")).asList().containsExactly("query");
    }

    @Test
    @DisplayName("Should execute registered tool successfully")
    void shouldExecuteToolSuccessfully() {
        DummyTool tool1 = new DummyTool("search_items", false);
        LifeOSToolRegistry registry = new LifeOSToolRegistry(List.of(tool1));
        UUID userId = UUID.randomUUID();

        ToolExecutionResult result = registry.execute("search_items", userId, Map.of("query", "hello"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo("Executed search_items with query: hello");
    }

    @Test
    @DisplayName("Should return failure result when executing unknown tool")
    void shouldReturnFailureForUnknownTool() {
        LifeOSToolRegistry registry = new LifeOSToolRegistry(List.of());
        UUID userId = UUID.randomUUID();

        ToolExecutionResult result = registry.execute("unknown_tool", userId, Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unknown tool: unknown_tool");
    }

    @Test
    @DisplayName("Should handle tool exception gracefully without crashing")
    void shouldHandleToolExceptionGracefully() {
        DummyTool tool1 = new DummyTool("search_items", false);
        LifeOSToolRegistry registry = new LifeOSToolRegistry(List.of(tool1));
        UUID userId = UUID.randomUUID();

        ToolExecutionResult result = registry.execute("search_items", userId, Map.of("query", "fail"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Simulated tool crash");
    }
}
