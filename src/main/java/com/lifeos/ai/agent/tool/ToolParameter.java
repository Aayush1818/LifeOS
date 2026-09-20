package com.lifeos.ai.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Definition of a single parameter in a LifeOSTool schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameter {

    private String name;

    private String type; // string, number, integer, boolean, array, object

    private String description;

    private boolean required;

    private List<String> enumValues;

    public static ToolParameter string(String name, String description, boolean required) {
        return ToolParameter.builder()
                .name(name)
                .type("string")
                .description(description)
                .required(required)
                .build();
    }

    public static ToolParameter integer(String name, String description, boolean required) {
        return ToolParameter.builder()
                .name(name)
                .type("integer")
                .description(description)
                .required(required)
                .build();
    }
}
