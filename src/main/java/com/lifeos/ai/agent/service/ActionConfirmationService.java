package com.lifeos.ai.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.agent.dto.ActionExecutionResponse;
import com.lifeos.ai.agent.dto.PendingActionDto;
import com.lifeos.ai.agent.entity.PendingAction;
import com.lifeos.ai.agent.entity.PendingActionStatus;
import com.lifeos.ai.agent.repository.PendingActionRepository;
import com.lifeos.ai.agent.tool.LifeOSToolRegistry;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.entity.MessageRole;
import com.lifeos.ai.conversation.repository.ChatMessageRepository;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing Human-in-the-Loop (HITL) confirmation lifecycle for mutating agent tools.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionConfirmationService {

    private final PendingActionRepository pendingActionRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final LifeOSToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    @Transactional
    public PendingActionDto createPendingAction(
            UUID userId, UUID conversationId, String toolName, Map<String, Object> parameters, String prompt) {

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Conversation conversation = null;
        if (conversationId != null) {
            conversation = conversationRepository.findByIdAndUserId(conversationId, userId).orElse(null);
        }

        String paramsJson = "{}";
        try {
            if (parameters != null) {
                paramsJson = objectMapper.writeValueAsString(parameters);
            }
        } catch (Exception e) {
            log.warn("Failed to serialize tool parameters: {}", e.getMessage());
        }

        PendingAction action = PendingAction.builder()
                .user(user)
                .conversation(conversation)
                .toolName(toolName)
                .parameters(paramsJson)
                .prompt(prompt)
                .status(PendingActionStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();

        PendingAction saved = pendingActionRepository.save(action);
        log.info("Created pending action [{}] for user [{}] (tool: {})", saved.getId(), userId, toolName);

        return mapToDto(saved, parameters);
    }

    @Transactional(readOnly = true)
    public List<PendingActionDto> getPendingActions(UUID userId) {
        List<PendingAction> actions = pendingActionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, PendingActionStatus.PENDING);
        return actions.stream().map(a -> mapToDto(a, parseParams(a.getParameters()))).toList();
    }

    @Transactional
    public ActionExecutionResponse confirmAction(UUID userId, UUID actionId) {
        PendingAction action = pendingActionRepository.findByIdAndUserId(actionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found with id: " + actionId));

        if (action.getStatus() != PendingActionStatus.PENDING) {
            throw new IllegalStateException("Action is already " + action.getStatus());
        }

        if (action.isExpired()) {
            action.setStatus(PendingActionStatus.EXPIRED);
            pendingActionRepository.save(action);
            throw new IllegalStateException("Action has expired");
        }

        Map<String, Object> params = parseParams(action.getParameters());
        log.info("User [{}] confirmed action [{}] ({})", userId, actionId, action.getToolName());

        ToolExecutionResult result = toolRegistry.execute(action.getToolName(), userId, params);

        action.setStatus(PendingActionStatus.CONFIRMED);
        action.setExecutedAt(OffsetDateTime.now());
        pendingActionRepository.save(action);

        // Record execution outcome in conversation if associated
        if (action.getConversation() != null) {
            String messageText = result.isSuccess()
                    ? "Action '" + action.getToolName() + "' confirmed and executed successfully: " + result.getOutput()
                    : "Action '" + action.getToolName() + "' failed: " + result.getErrorMessage();

            ChatMessage executionNote = ChatMessage.builder()
                    .conversation(action.getConversation())
                    .senderRole(MessageRole.ASSISTANT)
                    .content(messageText)
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(executionNote);
        }

        return ActionExecutionResponse.builder()
                .actionId(actionId)
                .toolName(action.getToolName())
                .status(PendingActionStatus.CONFIRMED)
                .success(result.isSuccess())
                .result(result.getOutput())
                .message(result.isSuccess() ? "Action executed successfully" : result.getErrorMessage())
                .executedAt(action.getExecutedAt())
                .build();
    }

    @Transactional
    public ActionExecutionResponse rejectAction(UUID userId, UUID actionId) {
        PendingAction action = pendingActionRepository.findByIdAndUserId(actionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found with id: " + actionId));

        if (action.getStatus() != PendingActionStatus.PENDING) {
            throw new IllegalStateException("Action is already " + action.getStatus());
        }

        action.setStatus(PendingActionStatus.REJECTED);
        pendingActionRepository.save(action);
        log.info("User [{}] rejected action [{}] ({})", userId, actionId, action.getToolName());

        if (action.getConversation() != null) {
            ChatMessage cancelNote = ChatMessage.builder()
                    .conversation(action.getConversation())
                    .senderRole(MessageRole.ASSISTANT)
                    .content("Action '" + action.getToolName() + "' was rejected by user.")
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(cancelNote);
        }

        return ActionExecutionResponse.builder()
                .actionId(actionId)
                .toolName(action.getToolName())
                .status(PendingActionStatus.REJECTED)
                .success(true)
                .message("Action was cancelled by user")
                .build();
    }

    private Map<String, Object> parseParams(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private PendingActionDto mapToDto(PendingAction action, Map<String, Object> params) {
        return PendingActionDto.builder()
                .id(action.getId())
                .conversationId(action.getConversation() != null ? action.getConversation().getId() : null)
                .toolName(action.getToolName())
                .parameters(params)
                .prompt(action.getPrompt())
                .status(action.getStatus())
                .expiresAt(action.getExpiresAt())
                .createdAt(action.getCreatedAt())
                .build();
    }
}
