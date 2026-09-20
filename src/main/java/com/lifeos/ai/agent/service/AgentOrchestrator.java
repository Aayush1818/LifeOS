package com.lifeos.ai.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.agent.dto.PendingActionDto;
import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.LifeOSToolRegistry;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.grounding.AssembledContext;
import com.lifeos.ai.grounding.PromptBuilder;
import com.lifeos.ai.llm.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates multi-turn agent tool execution with safe iteration bounding (max 3 turns)
 * and Human-in-the-Loop confirmation interruption for state-mutating actions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final int MAX_AGENT_TURNS = 3;

    private final LifeOSToolRegistry toolRegistry;
    private final ActionConfirmationService actionConfirmationService;
    private final LlmProvider llmProvider;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    public static class OrchestratorResult {
        private String finalContent;
        private LlmResponse llmResponse;
        private PendingActionDto pendingAction;
        @Builder.Default
        private List<ToolCallDto> executedToolCalls = new ArrayList<>();
    }

    /**
     * Executes the agent loop.
     */
    public OrchestratorResult orchestrate(
            UUID userId,
            UUID conversationId,
            AssembledContext assembledContext,
            boolean hasRelevantContext,
            List<ChatMessage> conversationHistory,
            String currentQuery) {

        log.debug("Starting agent orchestration for user [{}] in conversation [{}]", userId, conversationId);

        // 1. Build initial LLM request with tool definitions
        LlmRequest request = promptBuilder.buildRequest(
                assembledContext,
                hasRelevantContext,
                conversationHistory,
                currentQuery
        );
        request.setTools(toolRegistry.getToolDefinitions());

        List<ToolCallDto> accumulatedToolCalls = new ArrayList<>();
        PendingActionDto pendingAction = null;
        LlmResponse lastResponse = null;

        int turn = 0;
        while (turn < MAX_AGENT_TURNS) {
            turn++;
            log.debug("Agent turn [{}] for user [{}]", turn, userId);

            lastResponse = llmProvider.generate(request);

            if (!lastResponse.hasToolCalls()) {
                // Model synthesized an answer without requesting further tools
                break;
            }

            // Inspect requested tool calls
            boolean requiresInterruption = false;
            for (ToolCallDto call : lastResponse.getToolCalls()) {
                accumulatedToolCalls.add(call);
                Optional<LifeOSTool> toolOpt = toolRegistry.getTool(call.getName());

                if (toolOpt.isEmpty()) {
                    log.warn("Model requested non-existent tool: [{}]", call.getName());
                    continue;
                }

                LifeOSTool tool = toolOpt.get();

                if (tool.requiresConfirmation()) {
                    // State mutation detected! Intercept and produce confirmation card
                    log.info("Mutating tool [{}] called. Intercepting for user confirmation.", tool.getName());
                    String prompt = tool.getConfirmationPrompt(call.getArguments());
                    pendingAction = actionConfirmationService.createPendingAction(
                            userId, conversationId, tool.getName(), call.getArguments(), prompt);
                    requiresInterruption = true;
                    break;
                } else {
                    // Read-only tool: execute immediately
                    log.info("Executing read-only tool [{}] in agent turn [{}]", tool.getName(), turn);
                    ToolExecutionResult execResult = toolRegistry.execute(tool.getName(), userId, call.getArguments());

                    String outputJson;
                    try {
                        outputJson = objectMapper.writeValueAsString(execResult.getOutput());
                    } catch (Exception e) {
                        outputJson = String.valueOf(execResult.getOutput());
                    }

                    // Feed tool output back to conversation for synthesis
                    request.getMessages().add(LlmMessageDto.assistant(
                            "Tool call requested: " + call.getName() + " with args: " + call.getArguments()));
                    request.getMessages().add(new LlmMessageDto("tool", outputJson));
                }
            }

            if (requiresInterruption) {
                break;
            }
        }

        String finalContent = lastResponse != null && lastResponse.getContent() != null
                ? lastResponse.getContent()
                : "";

        if (pendingAction != null && finalContent.isBlank()) {
            finalContent = "I've prepared this action for your review: " + pendingAction.getPrompt();
        }

        return OrchestratorResult.builder()
                .finalContent(finalContent)
                .llmResponse(lastResponse)
                .pendingAction(pendingAction)
                .executedToolCalls(accumulatedToolCalls)
                .build();
    }
}
