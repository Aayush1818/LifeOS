package com.lifeos.ai.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-Schema compatible parameter schema for LLM function calling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterSchema {

    @Builder.Default
    private List<ToolParameter> parameters = new ArrayList<>();

    public static ToolParameterSchema empty() {
        return new ToolParameterSchema(new ArrayList<>());
    }

    public static ToolParameterSchema of(ToolParameter... params) {
        return new ToolParameterSchema(new ArrayList<>(List.of(params)));
    }

    /**
     * Converts to standard OpenAI function parameters JSON map structure.
     */
    public Map<String, Object> toOpenAiSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        List<String> requiredList = new ArrayList<>();

        for (ToolParameter p : parameters) {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", p.getType());
            prop.put("description", p.getDescription());
            if (p.getEnumValues() != null && !p.getEnumValues().isEmpty()) {
                prop.put("enum", p.getEnumValues());
            }
            properties.put(p.getName(), prop);

            if (p.isRequired()) {
                requiredList.add(p.getName());
            }
        }

        schema.put("properties", properties);
        schema.put("required", requiredList);
        return schema;
    }
}
