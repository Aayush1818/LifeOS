package com.lifeos.ai.service;

import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.entity.MessageCitation;
import com.lifeos.ai.conversation.entity.MessageRole;
import com.lifeos.ai.conversation.repository.ChatMessageRepository;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.ai.conversation.repository.MessageCitationRepository;
import com.lifeos.ai.dto.*;
import com.lifeos.ai.grounding.AssembledContext;
import com.lifeos.ai.grounding.CitationValidator;
import com.lifeos.ai.grounding.ContextAssembler;
import com.lifeos.ai.grounding.PromptBuilder;
import com.lifeos.ai.grounding.ValidatedCitationResult;
import com.lifeos.ai.llm.LlmProvider;
import com.lifeos.ai.llm.LlmRequest;
import com.lifeos.ai.llm.LlmResponse;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import com.lifeos.rag.retrieval.dto.RetrievalRequest;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.processor.QueryProcessor;
import com.lifeos.rag.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAssistantService implements AssistantService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageCitationRepository messageCitationRepository;
    private final DocumentRepository documentRepository;
    private final RetrievalService retrievalService;
    private final QueryProcessor queryProcessor;
    private final ContextAssembler contextAssembler;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final CitationValidator citationValidator;

    @Override
    @Transactional
    public AssistantMessageResponse processMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        // 1. Verify Conversation Ownership
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        // 2. Persist User Message
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderRole(MessageRole.USER)
                .content(request.getContent().trim())
                .createdAt(OffsetDateTime.now())
                .build();
        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

        // 3. Clean and normalize query
        QueryProcessor.ProcessedQuery processedQuery = queryProcessor.process(request.getContent());

        // 4. Execute Phase 12 Hybrid Retrieval
        RetrievalRequest retrievalRequest = RetrievalRequest.builder()
                .query(processedQuery.normalizedQuery())
                .mode(request.getRetrievalMode() != null ? request.getRetrievalMode() : RetrievalMode.HYBRID)
                .topK(request.getTopK() > 0 ? request.getTopK() : 5)
                .filters(request.getFilters())
                .build();

        RetrievalResponse retrievalResponse = retrievalService.retrieve(userId, retrievalRequest);
        log.debug("Phase 12 retrieval returned {} chunks (hasRelevantContext: {}) for user [{}]",
                retrievalResponse.getResults().size(), retrievalResponse.isHasRelevantContext(), userId);

        // 5. Assemble Context
        AssembledContext assembledContext = contextAssembler.assemble(
                retrievalResponse.getResults(),
                retrievalResponse.getCitations()
        );

        // 6. Fetch Conversation History
        List<ChatMessage> history = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessage> priorHistory = history.isEmpty() ? List.of() : history.subList(0, Math.max(0, history.size() - 1));

        // 7. Build Versioned Prompt
        LlmRequest llmRequest = promptBuilder.buildRequest(
                assembledContext,
                retrievalResponse.isHasRelevantContext(),
                priorHistory,
                request.getContent()
        );

        // 8. Invoke Pluggable LLM Provider
        LlmResponse llmResponse = llmProvider.generate(llmRequest);

        // 9. Validate Inline Citations
        ValidatedCitationResult citationResult = citationValidator.validate(
                llmResponse.getContent(),
                assembledContext
        );

        // 10. Persist Assistant Message
        ChatMessage assistantMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderRole(MessageRole.ASSISTANT)
                .content(citationResult.getSanitizedContent())
                .promptTokens(llmResponse.getUsage() != null ? llmResponse.getUsage().getPromptTokens() : 0)
                .completionTokens(llmResponse.getUsage() != null ? llmResponse.getUsage().getCompletionTokens() : 0)
                .modelName(llmResponse.getModel())
                .createdAt(OffsetDateTime.now())
                .build();
        ChatMessage savedAssistantMessage = chatMessageRepository.save(assistantMessage);

        // 11. Persist Validated Citations
        for (AssistantCitationDto cit : citationResult.getValidCitations()) {
            DocumentEntity doc = null;
            if (cit.getDocumentId() != null) {
                doc = documentRepository.findById(cit.getDocumentId()).orElse(null);
            }

            if (doc != null) {
                MessageCitation messageCitation = MessageCitation.builder()
                        .message(savedAssistantMessage)
                        .document(doc)
                        .chunkId(cit.getChunkId())
                        .pageNumber(cit.getPageNumber() != null ? cit.getPageNumber() : 1)
                        .snippet(cit.getSnippet() != null ? cit.getSnippet() : "")
                        .confidenceScore(BigDecimal.valueOf(cit.getRelevanceScore() != null ? cit.getRelevanceScore() : 0.0))
                        .citationIndex(cit.getCitationIndex() != null ? cit.getCitationIndex() : 1)
                        .sectionTitle(cit.getSectionTitle())
                        .sourceCitation(cit.getSourceCitation())
                        .createdAt(OffsetDateTime.now())
                        .build();
                messageCitationRepository.save(messageCitation);
            }
        }

        // 12. Update Conversation Timestamp
        conversation.setLastMessageAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        // 13. Construct Response DTO
        TokenUsageDto usageDto = new TokenUsageDto();
        if (llmResponse.getUsage() != null) {
            usageDto.setPromptTokens(llmResponse.getUsage().getPromptTokens());
            usageDto.setCompletionTokens(llmResponse.getUsage().getCompletionTokens());
            usageDto.setTotalTokens(llmResponse.getUsage().getTotalTokens());
        }

        ModelMetadataDto modelMetadata = ModelMetadataDto.builder()
                .provider(llmProvider.getProviderName())
                .model(llmResponse.getModel())
                .durationMs(llmResponse.getDurationMs())
                .build();

        return AssistantMessageResponse.builder()
                .conversationId(conversationId)
                .userMessage(ChatMessageDto.builder()
                        .id(savedUserMessage.getId())
                        .role(savedUserMessage.getSenderRole())
                        .content(savedUserMessage.getContent())
                        .createdAt(savedUserMessage.getCreatedAt())
                        .build())
                .assistantMessage(ChatMessageDto.builder()
                        .id(savedAssistantMessage.getId())
                        .role(savedAssistantMessage.getSenderRole())
                        .content(savedAssistantMessage.getContent())
                        .createdAt(savedAssistantMessage.getCreatedAt())
                        .build())
                .citations(citationResult.getValidCitations())
                .grounded(retrievalResponse.isHasRelevantContext() && !citationResult.getValidCitations().isEmpty())
                .hasRelevantContext(retrievalResponse.isHasRelevantContext())
                .usage(usageDto)
                .modelMetadata(modelMetadata)
                .build();
    }
}
