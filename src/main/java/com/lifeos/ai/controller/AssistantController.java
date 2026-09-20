package com.lifeos.ai.controller;

import com.lifeos.ai.dto.*;
import com.lifeos.ai.service.AssistantService;
import com.lifeos.ai.service.ConversationService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for Grounded AI Assistant and Conversation Management.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Endpoints for grounded AI assistant conversations and message generation")
public class AssistantController {

    private final ConversationService conversationService;
    private final AssistantService assistantService;

    @PostMapping("/conversations")
    @Operation(summary = "Create a new conversation session")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody(required = false) CreateConversationRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        ConversationResponse response = conversationService.createConversation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Conversation created successfully"));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversations for current authenticated user")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getConversations(
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Page<ConversationResponse> response = conversationService.getConversations(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversations retrieved successfully"));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation details and message history")
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> getConversationDetails(
            @PathVariable("id") UUID id) {

        UUID userId = SecurityUtils.getCurrentUserId();
        ConversationDetailResponse response = conversationService.getConversationDetails(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversation details retrieved successfully"));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete conversation and all associated messages and citations")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        conversationService.deleteConversation(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation deleted successfully"));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Send a user message, retrieve grounded context via Phase 12 RAG, and generate answer")
    public ResponseEntity<ApiResponse<AssistantMessageResponse>> sendMessage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendMessageRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        AssistantMessageResponse response = assistantService.processMessage(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Message processed successfully"));
    }
}
