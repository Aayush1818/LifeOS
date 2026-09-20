package com.lifeos.ai.service;

import com.lifeos.ai.dto.ConversationDetailResponse;
import com.lifeos.ai.dto.ConversationResponse;
import com.lifeos.ai.dto.CreateConversationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConversationService {

    ConversationResponse createConversation(UUID userId, CreateConversationRequest request);

    Page<ConversationResponse> getConversations(UUID userId, Pageable pageable);

    ConversationDetailResponse getConversationDetails(UUID userId, UUID conversationId);

    void deleteConversation(UUID userId, UUID conversationId);
}
