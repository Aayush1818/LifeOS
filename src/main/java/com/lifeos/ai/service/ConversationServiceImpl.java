package com.lifeos.ai.service;

import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.repository.ChatMessageRepository;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.ai.dto.ChatMessageDto;
import com.lifeos.ai.dto.ConversationDetailResponse;
import com.lifeos.ai.dto.ConversationResponse;
import com.lifeos.ai.dto.CreateConversationRequest;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ConversationResponse createConversation(UUID userId, CreateConversationRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String title = (request != null && request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim()
                : "New Conversation";

        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title)
                .createdAt(OffsetDateTime.now())
                .lastMessageAt(OffsetDateTime.now())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("Created conversation [{}] for user [{}]", saved.getId(), userId);

        return mapToResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(UUID userId, Pageable pageable) {
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId, pageable)
                .map(c -> {
                    long count = chatMessageRepository.countByConversationId(c.getId());
                    return mapToResponse(c, count);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetails(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(m -> ChatMessageDto.builder()
                        .id(m.getId())
                        .role(m.getSenderRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ConversationDetailResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .messages(messageDtos)
                .build();
    }

    @Override
    @Transactional
    public void deleteConversation(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        conversationRepository.delete(conversation);
        log.info("Deleted conversation [{}] for user [{}]", conversationId, userId);
    }

    private ConversationResponse mapToResponse(Conversation c, long messageCount) {
        return ConversationResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .createdAt(c.getCreatedAt())
                .lastMessageAt(c.getLastMessageAt())
                .messageCount(messageCount)
                .build();
    }
}
