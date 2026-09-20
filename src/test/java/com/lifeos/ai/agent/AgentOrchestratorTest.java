package com.lifeos.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.agent.dto.PendingActionDto;
import com.lifeos.ai.agent.entity.PendingActionStatus;
import com.lifeos.ai.agent.service.ActionConfirmationService;
import com.lifeos.ai.agent.service.AgentOrchestrator;
import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.LifeOSToolRegistry;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.grounding.AssembledContext;
import com.lifeos.ai.grounding.PromptBuilder;
import com.lifeos.ai.llm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock
    private LifeOSToolRegistry toolRegistry;
    @Mock
    private ActionConfirmationService actionConfirmationService;
    @Mock
    private LlmProvider llmProvider;
    @Mock
    private PromptBuilder promptBuilder;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AgentOrchestrator orchestrator;

    private UUID userId;
    private UUID conversationId;
    private AssembledContext assembledContext;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        assembledContext = AssembledContext.builder().sourceCount(0).build();

        LlmRequest initialRequest = LlmRequest.builder()
                .messages(new ArrayList<>(List.of(new LlmMessageDto("user", "Hello"))))
                .build();
        when(promptBuilder.buildRequest(any(), anyBoolean(), any(), any())).thenReturn(initialRequest);
        when(toolRegistry.getToolDefinitions()).thenReturn(List.of());
    }

    @Test
    @DisplayName("Should return immediately when LLM provides direct text response without tool calls")
    void shouldReturnDirectTextResponse() {
        LlmResponse directResponse = LlmResponse.builder()
                .content("Hello! How can I assist you today?")
                .toolCalls(List.of())
                .build();
        when(llmProvider.generate(any())).thenReturn(directResponse);

        AgentOrchestrator.OrchestratorResult result = orchestrator.orchestrate(
                userId, conversationId, assembledContext, false, List.of(), "Hello");

        assertThat(result).isNotNull();
        assertThat(result.getFinalContent()).isEqualTo("Hello! How can I assist you today?");
        assertThat(result.getPendingAction()).isNull();
        assertThat(result.getExecutedToolCalls()).isEmpty();
        verify(llmProvider, times(1)).generate(any());
        verify(toolRegistry, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("Should execute read-only tool and feed output to LLM in second turn")
    void shouldExecuteReadOnlyToolAndReturnFinalAnswer() {
        ToolCallDto toolCall = ToolCallDto.builder()
                .id("call_123")
                .name("get_loan_summary")
                .arguments(Map.of())
                .build();

        LlmResponse turn1Response = LlmResponse.builder()
                .content("")
                .toolCalls(List.of(toolCall))
                .build();

        LlmResponse turn2Response = LlmResponse.builder()
                .content("You currently have 1 active loan with a balance of $25,000.")
                .toolCalls(List.of())
                .build();

        when(llmProvider.generate(any()))
                .thenReturn(turn1Response)
                .thenReturn(turn2Response);

        LifeOSTool mockTool = mock(LifeOSTool.class);
        when(mockTool.getName()).thenReturn("get_loan_summary");
        when(mockTool.requiresConfirmation()).thenReturn(false);
        when(toolRegistry.getTool("get_loan_summary")).thenReturn(Optional.of(mockTool));
        when(toolRegistry.execute(eq("get_loan_summary"), eq(userId), any()))
                .thenReturn(ToolExecutionResult.success(Map.of("totalBalance", 25000)));

        AgentOrchestrator.OrchestratorResult result = orchestrator.orchestrate(
                userId, conversationId, assembledContext, false, List.of(), "What are my loans?");

        assertThat(result).isNotNull();
        assertThat(result.getFinalContent()).isEqualTo("You currently have 1 active loan with a balance of $25,000.");
        assertThat(result.getPendingAction()).isNull();
        assertThat(result.getExecutedToolCalls()).hasSize(1);
        assertThat(result.getExecutedToolCalls().getFirst().getName()).isEqualTo("get_loan_summary");

        verify(toolRegistry).execute("get_loan_summary", userId, Map.of());
        verify(llmProvider, times(2)).generate(any());
    }

    @Test
    @DisplayName("Should intercept mutating tool, create PendingAction, and halt without executing")
    void shouldInterceptMutatingToolAndCreatePendingAction() {
        ToolCallDto toolCall = ToolCallDto.builder()
                .id("call_pay_1")
                .name("record_loan_payment")
                .arguments(Map.of("loanId", "a1b2c3d4", "amount", 500.0))
                .build();

        LlmResponse turn1Response = LlmResponse.builder()
                .content("")
                .toolCalls(List.of(toolCall))
                .build();

        when(llmProvider.generate(any())).thenReturn(turn1Response);

        LifeOSTool mockTool = mock(LifeOSTool.class);
        when(mockTool.getName()).thenReturn("record_loan_payment");
        when(mockTool.requiresConfirmation()).thenReturn(true);
        when(mockTool.getConfirmationPrompt(any())).thenReturn("Record a payment of $500.00 for loan a1b2c3d4?");
        when(toolRegistry.getTool("record_loan_payment")).thenReturn(Optional.of(mockTool));

        PendingActionDto pendingActionDto = PendingActionDto.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .toolName("record_loan_payment")
                .parameters(toolCall.getArguments())
                .prompt("Record a payment of $500.00 for loan a1b2c3d4?")
                .status(PendingActionStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();

        when(actionConfirmationService.createPendingAction(
                eq(userId), eq(conversationId), eq("record_loan_payment"), eq(toolCall.getArguments()), anyString()))
                .thenReturn(pendingActionDto);

        AgentOrchestrator.OrchestratorResult result = orchestrator.orchestrate(
                userId, conversationId, assembledContext, false, List.of(), "Pay 500 towards my loan");

        assertThat(result).isNotNull();
        assertThat(result.getPendingAction()).isNotNull();
        assertThat(result.getPendingAction().getToolName()).isEqualTo("record_loan_payment");
        assertThat(result.getFinalContent()).contains("Record a payment of $500.00 for loan a1b2c3d4?");
        assertThat(result.getExecutedToolCalls()).hasSize(1);

        // Crucial security guarantee: Registry execute() MUST NOT be called!
        verify(toolRegistry, never()).execute(any(), any(), any());
        verify(llmProvider, times(1)).generate(any());
    }

    @Test
    @DisplayName("Should prevent infinite loop by capping execution at maximum turns")
    void shouldCapExecutionAtMaxTurns() {
        ToolCallDto infiniteToolCall = ToolCallDto.builder()
                .id("call_loop")
                .name("get_loan_summary")
                .arguments(Map.of())
                .build();

        LlmResponse loopResponse = LlmResponse.builder()
                .content("Still checking...")
                .toolCalls(List.of(infiniteToolCall))
                .build();

        when(llmProvider.generate(any())).thenReturn(loopResponse);

        LifeOSTool mockTool = mock(LifeOSTool.class);
        when(mockTool.getName()).thenReturn("get_loan_summary");
        when(mockTool.requiresConfirmation()).thenReturn(false);
        when(toolRegistry.getTool("get_loan_summary")).thenReturn(Optional.of(mockTool));
        when(toolRegistry.execute(any(), any(), any())).thenReturn(ToolExecutionResult.success("Done"));

        AgentOrchestrator.OrchestratorResult result = orchestrator.orchestrate(
                userId, conversationId, assembledContext, false, List.of(), "Loop forever");

        assertThat(result).isNotNull();
        // Maximum turns is 3
        verify(llmProvider, times(3)).generate(any());
        assertThat(result.getExecutedToolCalls()).hasSize(3);
    }
}
