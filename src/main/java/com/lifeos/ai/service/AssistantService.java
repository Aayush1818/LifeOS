package com.lifeos.ai.service;

import com.lifeos.ai.dto.AssistantMessageResponse;
import com.lifeos.ai.dto.SendMessageRequest;

import java.util.UUID;

public interface AssistantService {

    /**
     * Executes grounded conversational processing for a user turn.
     *
     * @param userId         authenticated tenant user ID
     * @param conversationId conversation identifier
     * @param request        user message content, retrieval mode, and filters
     * @return grounded assistant response with verified citations
     */
    AssistantMessageResponse processMessage(UUID userId, UUID conversationId, SendMessageRequest request);
}
