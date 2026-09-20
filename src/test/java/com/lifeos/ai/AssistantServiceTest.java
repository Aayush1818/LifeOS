package com.lifeos.ai;

import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.entity.MessageRole;
import com.lifeos.ai.conversation.repository.ChatMessageRepository;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.ai.conversation.repository.MessageCitationRepository;
import com.lifeos.ai.dto.AssistantCitationDto;
import com.lifeos.ai.dto.AssistantMessageResponse;
import com.lifeos.ai.dto.SendMessageRequest;
import com.lifeos.ai.grounding.*;
import com.lifeos.ai.llm.*;
import com.lifeos.ai.service.DefaultAssistantService;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.dto.RetrievedChunkDto;
import com.lifeos.rag.retrieval.processor.QueryProcessor;
import com.lifeos.rag.retrieval.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private MessageCitationRepository messageCitationRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private RetrievalService retrievalService;
    @Mock
    private QueryProcessor queryProcessor;
    @Mock
    private ContextAssembler contextAssembler;
    @Mock
    private PromptBuilder promptBuilder;
    @Mock
    private LlmProvider llmProvider;
    @Mock
    private CitationValidator citationValidator;
    @Mock
    private com.lifeos.ai.agent.service.AgentOrchestrator agentOrchestrator;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private DefaultAssistantService assistantService;

    private UUID userId;
    private UUID convId;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        convId = UUID.randomUUID();
        conversation = Conversation.builder()
                .id(convId)
                .title("Test Conv")
                .createdAt(OffsetDateTime.now())
                .lastMessageAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully process user message, retrieve context, call LLM, and persist citations")
    void shouldProcessMessageWithGroundedCitations() {
        SendMessageRequest request = SendMessageRequest.builder()
                .content("What is my insurance deductible?")
                .retrievalMode(RetrievalMode.HYBRID)
                .topK(5)
                .build();

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conversation));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        when(queryProcessor.process(anyString())).thenReturn(new QueryProcessor.ProcessedQuery(
                request.getContent(),
                "what is my insurance deductible",
                List.of(),
                List.of()
        ));

        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        RetrievedChunkDto chunk = RetrievedChunkDto.builder()
                .documentId(docId)
                .chunkId(chunkId)
                .documentTitle("Policy.pdf")
                .pageNumber(3)
                .sectionTitle("Deductible")
                .content("Deductible is $1000.")
                .relevanceScore(0.9)
                .build();

        RetrievalResponse retrievalResponse = RetrievalResponse.builder()
                .hasRelevantContext(true)
                .results(List.of(chunk))
                .build();
        when(retrievalService.retrieve(eq(userId), any())).thenReturn(retrievalResponse);

        AssembledContext assembledContext = AssembledContext.builder()
                .formattedContext("<untrusted_document_source index=\"1\">Deductible is $1000.</untrusted_document_source>")
                .sourceCount(1)
                .build();
        when(contextAssembler.assemble(any(), any())).thenReturn(assembledContext);

        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(convId)).thenReturn(List.of());

        LlmResponse llmResponse = LlmResponse.builder()
                .content("Your deductible is $1000 [1].")
                .model("mock-gpt-4o-mini")
                .usage(new LlmUsageDto(100, 20, 120))
                .durationMs(15L)
                .build();
        when(llmProvider.getProviderName()).thenReturn("MOCK");

        com.lifeos.ai.agent.service.AgentOrchestrator.OrchestratorResult orchestratorResult =
                com.lifeos.ai.agent.service.AgentOrchestrator.OrchestratorResult.builder()
                        .finalContent("Your deductible is $1000 [1].")
                        .llmResponse(llmResponse)
                        .build();
        when(agentOrchestrator.orchestrate(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(orchestratorResult);

        AssistantCitationDto citationDto = AssistantCitationDto.builder()
                .citationIndex(1)
                .documentId(docId)
                .chunkId(chunkId)
                .documentTitle("Policy.pdf")
                .pageNumber(3)
                .sectionTitle("Deductible")
                .sourceCitation("[Policy.pdf, Page 3]")
                .relevanceScore(0.9)
                .build();

        when(citationValidator.validate(anyString(), any())).thenReturn(ValidatedCitationResult.builder()
                .sanitizedContent("Your deductible is $1000 [1].")
                .validCitations(List.of(citationDto))
                .build());

        DocumentEntity docEntity = DocumentEntity.builder().title("Policy.pdf").build();
        docEntity.setId(docId);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(docEntity));

        AssistantMessageResponse response = assistantService.processMessage(userId, convId, request);

        assertThat(response).isNotNull();
        assertThat(response.getConversationId()).isEqualTo(convId);
        assertThat(response.isGrounded()).isTrue();
        assertThat(response.isHasRelevantContext()).isTrue();
        assertThat(response.getAssistantMessage().getContent()).isEqualTo("Your deductible is $1000 [1].");
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getCitations().getFirst().getDocumentTitle()).isEqualTo("Policy.pdf");

        verify(messageCitationRepository).save(any());
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Should handle insufficient context response gracefully")
    void shouldHandleInsufficientContext() {
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Random unanswerable question")
                .build();

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conversation));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        when(queryProcessor.process(anyString())).thenReturn(new QueryProcessor.ProcessedQuery(
                request.getContent(),
                "random unanswerable question",
                List.of(),
                List.of()
        ));

        RetrievalResponse retrievalResponse = RetrievalResponse.builder()
                .hasRelevantContext(false)
                .results(List.of())
                .build();
        when(retrievalService.retrieve(eq(userId), any())).thenReturn(retrievalResponse);

        AssembledContext emptyContext = AssembledContext.builder().sourceCount(0).build();
        when(contextAssembler.assemble(any(), any())).thenReturn(emptyContext);

        LlmResponse llmResponse = LlmResponse.builder()
                .content("I do not have sufficient information in your uploaded documents or records to answer this question accurately.")
                .model("mock-gpt-4o-mini")
                .build();
        when(llmProvider.getProviderName()).thenReturn("MOCK");

        com.lifeos.ai.agent.service.AgentOrchestrator.OrchestratorResult orchestratorResult =
                com.lifeos.ai.agent.service.AgentOrchestrator.OrchestratorResult.builder()
                        .finalContent(llmResponse.getContent())
                        .llmResponse(llmResponse)
                        .build();
        when(agentOrchestrator.orchestrate(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(orchestratorResult);

        when(citationValidator.validate(anyString(), any())).thenReturn(ValidatedCitationResult.builder()
                .sanitizedContent(llmResponse.getContent())
                .validCitations(List.of())
                .build());

        AssistantMessageResponse response = assistantService.processMessage(userId, convId, request);

        assertThat(response).isNotNull();
        assertThat(response.isGrounded()).isFalse();
        assertThat(response.isHasRelevantContext()).isFalse();
        assertThat(response.getCitations()).isEmpty();
        verify(messageCitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when conversation is unowned or does not exist")
    void shouldThrowWhenConversationUnowned() {
        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.empty());

        SendMessageRequest request = SendMessageRequest.builder().content("Hello").build();

        assertThatThrownBy(() -> assistantService.processMessage(userId, convId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conversation not found");
    }
}
