package com.lifeos.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessageDto {

    /**
     * Standard message role: "system", "user", "assistant".
     */
    private String role;

    /**
     * Message content.
     */
    private String content;

    public static LlmMessageDto system(String content) {
        return new LlmMessageDto("system", content);
    }

    public static LlmMessageDto user(String content) {
        return new LlmMessageDto("user", content);
    }

    public static LlmMessageDto assistant(String content) {
        return new LlmMessageDto("assistant", content);
    }
}
