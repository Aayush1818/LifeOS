package com.lifeos.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production HTTP adapter for OpenAI-compatible chat completion endpoints.
 * Compatible with OpenAI (gpt-4o, gpt-4o-mini), Ollama, Azure OpenAI, Groq, and vLLM.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lifeos.assistant.llm", name = "provider", havingValue = "openai")
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final LlmProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleLlmProvider(LlmProperties properties) {
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
    public LlmResponse generate(LlmRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new LlmException("LlmRequest must contain at least one message");
        }

        long startTime = System.currentTimeMillis();
        Map<String, Object> payload = buildPayload(request);
        Map<String, Object> responseMap = executeWithRetry(payload);

        long durationMs = System.currentTimeMillis() - startTime;
        return parseResponse(responseMap, durationMs);
    }

    private Map<String, Object> buildPayload(LlmRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.getModel());
        payload.put("temperature", request.getTemperature() != null ? request.getTemperature() : properties.getTemperature());
        payload.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : properties.getMaxTokens());

        List<Map<String, String>> messagesList = new ArrayList<>();
        for (LlmMessageDto msg : request.getMessages()) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            messagesList.add(m);
        }
        payload.put("messages", messagesList);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(Map<String, Object> responseMap, long durationMs) {
        if (responseMap == null || !responseMap.containsKey("choices")) {
            throw new LlmException("Malformed response from OpenAI provider: missing 'choices' field");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new LlmException("OpenAI provider returned empty choices list");
        }

        Map<String, Object> firstChoice = choices.getFirst();
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        String content = message != null ? (String) message.get("content") : "";
        String finishReason = (String) firstChoice.getOrDefault("finish_reason", "stop");

        LlmUsageDto usage = new LlmUsageDto();
        if (responseMap.containsKey("usage") && responseMap.get("usage") instanceof Map<?, ?> usageMap) {
            usage.setPromptTokens(getInt(usageMap.get("prompt_tokens")));
            usage.setCompletionTokens(getInt(usageMap.get("completion_tokens")));
            usage.setTotalTokens(getInt(usageMap.get("total_tokens")));
        }

        String model = (String) responseMap.getOrDefault("model", getModelName());

        return LlmResponse.builder()
                .content(content)
                .model(model)
                .usage(usage)
                .finishReason(finishReason)
                .durationMs(durationMs)
                .build();
    }

    private int getInt(Object val) {
        if (val instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private Map<String, Object> executeWithRetry(Map<String, Object> payload) {
        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        long backoffMs = 1000L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return restClient.post()
                        .uri("/chat/completions")
                        .body(payload)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean isTransient = (status == 429 || status == 500 || status == 503 || status == 502);

                if (isTransient && attempt < maxAttempts) {
                    log.warn("Transient error from LLM provider (HTTP {}). Retrying attempt {}/{} in {}ms...",
                            status, attempt, maxAttempts, backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LlmException("LLM request interrupted during backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    log.error("Fatal or non-retryable error from LLM provider: HTTP {} - {}", status, e.getResponseBodyAsString());
                    throw new LlmException("LLM provider returned HTTP " + status + ": " + e.getResponseBodyAsString(), e, isTransient);
                }
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    log.warn("Network error during LLM call. Retrying attempt {}/{}...", attempt, maxAttempts, e);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LlmException("LLM request interrupted during retry", ie);
                    }
                    backoffMs *= 2;
                } else {
                    log.error("Failed to execute LLM request after {} attempts", maxAttempts, e);
                    throw new LlmException("Failed to reach LLM provider: " + e.getMessage(), e, true);
                }
            }
        }
        throw new LlmException("Exhausted retry attempts calling LLM provider");
    }
}
