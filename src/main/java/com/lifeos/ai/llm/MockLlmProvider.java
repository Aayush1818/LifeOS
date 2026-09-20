package com.lifeos.ai.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Mock LLM Provider for offline automated tests and local CI/CD.
 * Produces structured answers with citations, simulates insufficiency and transient failures.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lifeos.assistant.llm", name = "provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockLlmProvider implements LlmProvider {

    private final LlmProperties properties;
    private static final Pattern SOURCE_PATTERN = Pattern.compile("<untrusted_document_source\\s+index=\"(\\d+)\"");

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String getModelName() {
        return properties.getModel() != null ? properties.getModel() : "mock-gpt-4o-mini";
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new LlmException("LlmRequest must contain at least one message");
        }

        // Find system message and user message
        String systemContent = "";
        String userContent = "";

        for (LlmMessageDto msg : request.getMessages()) {
            if ("system".equalsIgnoreCase(msg.getRole())) {
                systemContent = msg.getContent() != null ? msg.getContent() : "";
            } else if ("user".equalsIgnoreCase(msg.getRole())) {
                userContent = msg.getContent() != null ? msg.getContent() : "";
            }
        }

        // Simulate test trigger conditions
        if (userContent.contains("TRIGGER_TIMEOUT")) {
            throw new LlmException("Simulated provider timeout", new TimeoutException("Connection timed out after 20000ms"), true);
        }
        if (userContent.contains("TRIGGER_RATE_LIMIT")) {
            throw new LlmException("Simulated HTTP 429 rate limit exceeded", null, true);
        }
        if (userContent.contains("TRIGGER_SERVER_ERROR")) {
            throw new LlmException("Simulated HTTP 500 internal provider failure", null, false);
        }
        if (userContent.contains("TRIGGER_INVALID_CITATION")) {
            return LlmResponse.builder()
                    .content("According to your uploaded records, this coverage applies under special terms [99].")
                    .model(getModelName())
                    .usage(new LlmUsageDto(120, 25, 145))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Medical safety check
        if (userContent.toLowerCase().contains("diagnose") || userContent.toLowerCase().contains("prescribe")
                || userContent.toLowerCase().contains("chest pain") || userContent.toLowerCase().contains("what medicine should i take")) {
            return LlmResponse.builder()
                    .content("I am an AI assistant, not a doctor or healthcare professional. I cannot diagnose conditions or prescribe treatments. If you are experiencing serious symptoms, please consult a healthcare professional or contact emergency services immediately.")
                    .model(getModelName())
                    .usage(new LlmUsageDto(100, 45, 145))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Tool calling simulation (Phase 14)
        boolean hasToolResults = request.getMessages().stream()
                .anyMatch(m -> "tool".equalsIgnoreCase(m.getRole()));

        if (!hasToolResults && request.getTools() != null && !request.getTools().isEmpty()) {
            if (userContent.toLowerCase().contains("total loan emi")
                    || userContent.toLowerCase().contains("active loans")
                    || userContent.contains("TRIGGER_TOOL_LOAN")) {
                return LlmResponse.builder()
                        .content("")
                        .model(getModelName())
                        .finishReason("tool_calls")
                        .toolCalls(java.util.List.of(ToolCallDto.builder()
                                .id("call_loan_123")
                                .name("get_loan_summary")
                                .arguments(java.util.Map.of())
                                .build()))
                        .usage(new LlmUsageDto(150, 15, 165))
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            if (userContent.toLowerCase().contains("record payment")
                    || userContent.toLowerCase().contains("pay loan")
                    || userContent.contains("TRIGGER_TOOL_PAYMENT")) {
                return LlmResponse.builder()
                        .content("I will prepare the loan payment for your confirmation.")
                        .model(getModelName())
                        .finishReason("tool_calls")
                        .toolCalls(java.util.List.of(ToolCallDto.builder()
                                .id("call_pay_456")
                                .name("record_loan_payment")
                                .arguments(java.util.Map.of(
                                        "loanId", "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                                        "amount", 500.00,
                                        "paymentType", "REGULAR_EMI"
                                ))
                                .build()))
                        .usage(new LlmUsageDto(160, 20, 180))
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            if (userContent.toLowerCase().contains("reminder")
                    || userContent.toLowerCase().contains("remind")
                    || userContent.contains("TRIGGER_TOOL_REMINDER")) {
                return LlmResponse.builder()
                        .content("I will prepare the scheduled reminder for your confirmation.")
                        .model(getModelName())
                        .finishReason("tool_calls")
                        .toolCalls(java.util.List.of(ToolCallDto.builder()
                                .id("call_rem_789")
                                .name("create_reminder")
                                .arguments(java.util.Map.of(
                                        "title", "Dentist appointment reminder",
                                        "dueAt", "2026-10-15T14:00:00Z"
                                ))
                                .build()))
                        .usage(new LlmUsageDto(160, 20, 180))
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            if (userContent.toLowerCase().contains("upcoming renewals")
                    || userContent.contains("TRIGGER_TOOL_INSURANCE")) {
                return LlmResponse.builder()
                        .content("")
                        .model(getModelName())
                        .finishReason("tool_calls")
                        .toolCalls(java.util.List.of(ToolCallDto.builder()
                                .id("call_ins_101")
                                .name("get_insurance_renewals")
                                .arguments(java.util.Map.of("daysAhead", 60))
                                .build()))
                        .usage(new LlmUsageDto(150, 15, 165))
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }
        }

        if (hasToolResults) {
            String toolSummary = "Based on your real-time records, I've calculated and retrieved the requested domain details.";
            if (userContent.toLowerCase().contains("loan") || userContent.contains("TRIGGER_TOOL_LOAN")) {
                toolSummary = "Based on your active loans, your total monthly EMI is calculated and confirmed by your records.";
            } else if (userContent.toLowerCase().contains("renewal") || userContent.contains("TRIGGER_TOOL_INSURANCE")) {
                toolSummary = "Here are your upcoming insurance renewals as retrieved from your active policies.";
            }
            return LlmResponse.builder()
                    .content(toolSummary)
                    .model(getModelName())
                    .usage(new LlmUsageDto(220, 30, 250))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Check for reference sources in system prompt
        Matcher matcher = SOURCE_PATTERN.matcher(systemContent);
        StringBuilder citations = new StringBuilder();
        int count = 0;
        while (matcher.find() && count < 2) {
            String index = matcher.group(1);
            citations.append("[").append(index).append("]");
            count++;
        }

        String answerContent;
        if (count > 0) {
            answerContent = "Based on your uploaded documents, " + citations + " confirms that your request is documented under your policy guidelines.";
        } else {
            answerContent = "I do not have sufficient information in your uploaded documents or records to answer this question accurately.";
        }

        long duration = System.currentTimeMillis() - startTime;
        log.debug("MockLlmProvider generated response in {}ms (sources cited: {})", duration, count);

        return LlmResponse.builder()
                .content(answerContent)
                .model(getModelName())
                .usage(new LlmUsageDto(250, 40, 290))
                .finishReason("stop")
                .durationMs(duration)
                .build();
    }
}
