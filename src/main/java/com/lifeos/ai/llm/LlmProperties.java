package com.lifeos.ai.llm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI Assistant LLM Provider and context boundaries.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lifeos.assistant.llm")
public class LlmProperties {

    /**
     * Provider implementation: "mock", "openai". Defaults to "mock".
     */
    private String provider = "mock";

    /**
     * Target model name identifier.
     */
    private String model = "gpt-4o-mini";

    /**
     * API key for commercial/remote providers.
     */
    private String apiKey = "";

    /**
     * Base URL for OpenAI-compatible completions endpoint.
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * Sampling temperature. Defaults to 0.1 for grounded determinism.
     */
    private double temperature = 0.1;

    /**
     * Maximum output completion tokens.
     */
    private int maxTokens = 1024;

    /**
     * Request timeout in milliseconds.
     */
    private int timeoutMs = 20000;

    /**
     * Maximum retry attempts for transient errors (e.g. 429, 503).
     */
    private int maxRetries = 2;
}
