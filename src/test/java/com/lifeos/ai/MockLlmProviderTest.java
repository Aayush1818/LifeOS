package com.lifeos.ai;

import com.lifeos.ai.llm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockLlmProviderTest {

    private MockLlmProvider provider;
    private LlmProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        properties.setModel("mock-gpt-4o-mini");
        provider = new MockLlmProvider(properties);
    }

    @Test
    @DisplayName("Should generate grounded response citing sources when sources are present in system prompt")
    void shouldGenerateGroundedResponseWhenSourcesPresent() {
        String system = "You are an assistant.\n" +
                "<untrusted_document_source index=\"1\" document_title=\"Policy.pdf\" page=\"1\" section=\"General\">\n" +
                "Policy coverage is active.\n" +
                "</untrusted_document_source>";

        LlmRequest request = LlmRequest.builder()
                .messages(List.of(
                        LlmMessageDto.system(system),
                        LlmMessageDto.user("What is my policy status?")
                ))
                .build();

        LlmResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("[1]");
        assertThat(response.getModel()).isEqualTo("mock-gpt-4o-mini");
        assertThat(response.getUsage().getTotalTokens()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return insufficient information message when no sources are present")
    void shouldReturnInsufficientInfoWhenNoSourcesPresent() {
        LlmRequest request = LlmRequest.builder()
                .messages(List.of(
                        LlmMessageDto.system("You are an assistant. No sources provided."),
                        LlmMessageDto.user("When is my dentist appointment?")
                ))
                .build();

        LlmResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("I do not have sufficient information in your uploaded documents");
    }

    @Test
    @DisplayName("Should return medical safety disclaimer when user asks for diagnosis or symptoms")
    void shouldReturnMedicalDisclaimerForDiagnosis() {
        LlmRequest request = LlmRequest.builder()
                .messages(List.of(
                        LlmMessageDto.system("System prompt."),
                        LlmMessageDto.user("I have severe chest pain, can you diagnose and tell me what medicine should I take?")
                ))
                .build();

        LlmResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("I am an AI assistant, not a doctor or healthcare professional");
    }

    @Test
    @DisplayName("Should simulate timeout when TRIGGER_TIMEOUT keyword is supplied")
    void shouldSimulateTimeout() {
        LlmRequest request = LlmRequest.builder()
                .messages(List.of(LlmMessageDto.user("Test TRIGGER_TIMEOUT")))
                .build();

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Simulated provider timeout");
    }

    @Test
    @DisplayName("Should simulate rate limit when TRIGGER_RATE_LIMIT keyword is supplied")
    void shouldSimulateRateLimit() {
        LlmRequest request = LlmRequest.builder()
                .messages(List.of(LlmMessageDto.user("Test TRIGGER_RATE_LIMIT")))
                .build();

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Simulated HTTP 429");
    }

    @Test
    @DisplayName("Should simulate server error when TRIGGER_SERVER_ERROR keyword is supplied")
    void shouldSimulateServerError() {
        LlmRequest request = LlmRequest.builder()
                .messages(List.of(LlmMessageDto.user("Test TRIGGER_SERVER_ERROR")))
                .build();

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Simulated HTTP 500");
    }

    @Test
    @DisplayName("Should simulate invalid citation [99] when TRIGGER_INVALID_CITATION is requested")
    void shouldSimulateInvalidCitation() {
        LlmRequest request = LlmRequest.builder()
                .messages(List.of(LlmMessageDto.user("Test TRIGGER_INVALID_CITATION")))
                .build();

        LlmResponse response = provider.generate(request);
        assertThat(response.getContent()).contains("[99]");
    }
}
