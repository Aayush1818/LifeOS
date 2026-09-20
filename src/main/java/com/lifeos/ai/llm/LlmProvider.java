package com.lifeos.ai.llm;

/**
 * Service Provider Interface for Language Model generation.
 * Decouples conversation orchestration and prompt building from specific AI vendors.
 */
public interface LlmProvider {

    /**
     * Provider identifier (e.g. "MOCK", "OPENAI", "ANTHROPIC", "OLLAMA").
     */
    String getProviderName();

    /**
     * Active model identifier (e.g. "gpt-4o-mini", "mock-gpt-4o-mini").
     */
    String getModelName();

    /**
     * Whether the provider is healthy and configured.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Executes generation based on structured request.
     *
     * @param request input messages, temperature, and tokens
     * @return generated text, usage metrics, and latency
     * @throws LlmException on provider failure or timeout
     */
    LlmResponse generate(LlmRequest request);
}
