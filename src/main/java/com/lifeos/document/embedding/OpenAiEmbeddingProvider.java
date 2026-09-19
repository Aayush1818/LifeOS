package com.lifeos.document.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Production-grade OpenAI-compatible vector embedding provider adapter.
 * Supports OpenAI text-embedding-3-small, Azure OpenAI, and compatible local endpoints.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lifeos.rag.embedding", name = "provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    public OpenAiEmbeddingProvider(EmbeddingProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public String getModelName() {
        return properties.getModel();
    }

    @Override
    public int getDimension() {
        return properties.getDimension();
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> allEmbeddings = new ArrayList<>(texts.size());
        int batchSize = Math.max(1, properties.getBatchSize());

        for (int i = 0; i < texts.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, toIndex);
            List<float[]> batchEmbeddings = executeWithRetry(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        return allEmbeddings;
    }

    @SuppressWarnings("unchecked")
    private List<float[]> executeWithRetry(List<String> batch) {
        int maxRetries = Math.max(1, properties.getMaxRetries());
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> requestBody = Map.of(
                        "input", batch,
                        "model", properties.getModel()
                );

                Map<String, Object> response = restClient.post()
                        .uri("/embeddings")
                        .body(requestBody)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (response == null || !response.containsKey("data")) {
                    throw new IllegalStateException("Invalid response from OpenAI embedding endpoint");
                }

                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                List<float[]> result = new ArrayList<>(data.size());

                for (Map<String, Object> item : data) {
                    List<Number> embeddingNumbers = (List<Number>) item.get("embedding");
                    float[] vec = new float[embeddingNumbers.size()];
                    for (int j = 0; j < embeddingNumbers.size(); j++) {
                        vec[j] = embeddingNumbers.get(j).floatValue();
                    }
                    result.add(vec);
                }

                return result;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status == 500 || status == 502 || status == 503 || status == 504;

                if (retryable && attempt < maxRetries) {
                    log.warn("Embedding call failed with status {} (attempt {}/{}). Retrying in {} ms...",
                            status, attempt, maxRetries, backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during embedding retry backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    log.error("OpenAI embedding call failed permanently with status {}: {}", status, e.getResponseBodyAsString());
                    throw new RuntimeException("OpenAI embedding request failed: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    log.warn("Embedding call exception: {} (attempt {}/{}). Retrying in {} ms...",
                            e.getMessage(), attempt, maxRetries, backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during embedding retry backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw new RuntimeException("Failed to generate OpenAI embeddings after " + maxRetries + " attempts", e);
                }
            }
        }

        throw new RuntimeException("Failed to generate OpenAI embeddings: retries exhausted");
    }
}
