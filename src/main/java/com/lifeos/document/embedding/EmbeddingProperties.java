package com.lifeos.document.embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for RAG vector embedding generation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lifeos.rag.embedding")
public class EmbeddingProperties {

    /**
     * Provider type: "mock", "openai", "ollama". Defaults to "mock".
     */
    private String provider = "mock";

    /**
     * Model name identifier.
     */
    private String model = "text-embedding-3-small";

    /**
     * Vector dimension (e.g., 1536).
     */
    private int dimension = 1536;

    /**
     * API key for external providers.
     */
    private String apiKey = "";

    /**
     * Base URL for provider endpoint.
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * Maximum batch size of texts to embed in a single request.
     */
    private int batchSize = 100;

    /**
     * Request timeout in milliseconds.
     */
    private int timeoutMs = 15000;

    /**
     * Maximum retry attempts for transient errors.
     */
    private int maxRetries = 3;
}
