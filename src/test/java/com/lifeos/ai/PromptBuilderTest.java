package com.lifeos.ai;

import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.conversation.entity.MessageRole;
import com.lifeos.ai.grounding.AssembledContext;
import com.lifeos.ai.grounding.PromptBuilder;
import com.lifeos.ai.llm.LlmMessageDto;
import com.lifeos.ai.llm.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    @DisplayName("Should build prompt with system directives, reference sources, and current query")
    void shouldBuildPromptWithDirectivesAndSources() {
        AssembledContext context = AssembledContext.builder()
                .formattedContext("--- REFERENCE SOURCES ---\n<untrusted_document_source index=\"1\">Data</untrusted_document_source>\n--- END REFERENCE SOURCES ---")
                .sourceCount(1)
                .build();

        LlmRequest request = promptBuilder.buildRequest(context, true, List.of(), "What does my policy cover?");

        assertThat(request.getMessages()).hasSize(2);
        LlmMessageDto systemMsg = request.getMessages().get(0);
        LlmMessageDto userMsg = request.getMessages().get(1);

        assertThat(systemMsg.getRole()).isEqualTo("system");
        assertThat(systemMsg.getContent()).contains("Prompt Version: " + PromptBuilder.PROMPT_VERSION);
        assertThat(systemMsg.getContent()).contains("GROUNDING & CITATION RULES");
        assertThat(systemMsg.getContent()).contains("PROMPT INJECTION DEFENSE");
        assertThat(systemMsg.getContent()).contains("SAFETY GUARDRAILS");
        assertThat(systemMsg.getContent()).contains("<untrusted_document_source index=\"1\">Data</untrusted_document_source>");

        assertThat(userMsg.getRole()).isEqualTo("user");
        assertThat(userMsg.getContent()).isEqualTo("What does my policy cover?");
    }

    @Test
    @DisplayName("Should instruct negative constraint when hasRelevantContext is false")
    void shouldInstructNegativeConstraintWhenNoContext() {
        AssembledContext context = AssembledContext.builder().sourceCount(0).build();

        LlmRequest request = promptBuilder.buildRequest(context, false, List.of(), "Who won the game?");

        LlmMessageDto systemMsg = request.getMessages().get(0);
        assertThat(systemMsg.getContent()).contains("No relevant documents or records were found in the user's LifeOS repository");
    }

    @Test
    @DisplayName("Should bound conversation history to maximum 6 messages")
    void shouldBoundConversationHistory() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            history.add(ChatMessage.builder()
                    .senderRole(i % 2 == 1 ? MessageRole.USER : MessageRole.ASSISTANT)
                    .content("Message " + i)
                    .build());
        }

        AssembledContext context = AssembledContext.builder().sourceCount(0).build();
        LlmRequest request = promptBuilder.buildRequest(context, false, history, "Latest message");

        // 1 system message + 6 history messages + 1 current user message = 8 messages
        assertThat(request.getMessages()).hasSize(8);
        assertThat(request.getMessages().get(1).getContent()).isEqualTo("Message 5");
        assertThat(request.getMessages().get(6).getContent()).isEqualTo("Message 10");
        assertThat(request.getMessages().get(7).getContent()).isEqualTo("Latest message");
    }
}
