package com.lifeos.ai;

import com.lifeos.ai.conversation.entity.ChatMessage;
import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.entity.MessageRole;
import com.lifeos.ai.conversation.repository.ChatMessageRepository;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.ai.dto.ConversationDetailResponse;
import com.lifeos.ai.dto.ConversationResponse;
import com.lifeos.ai.dto.CreateConversationRequest;
import com.lifeos.ai.service.ConversationServiceImpl;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private UUID userId;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEntity = UserEntity.builder()
                .email("user@example.com")
                .build();
        userEntity.setId(userId);
    }

    @Test
    @DisplayName("Should create conversation with custom title")
    void shouldCreateConversationWithCustomTitle() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CreateConversationRequest request = new CreateConversationRequest("Insurance Review");
        ConversationResponse response = conversationService.createConversation(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Insurance Review");
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Should create conversation with default title when title is blank")
    void shouldCreateConversationWithDefaultTitle() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ConversationResponse response = conversationService.createConversation(userId, null);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("New Conversation");
    }

    @Test
    @DisplayName("Should get paginated conversations for user")
    void shouldGetConversations() {
        Conversation c1 = Conversation.builder()
                .id(UUID.randomUUID())
                .user(userEntity)
                .title("Conv 1")
                .createdAt(OffsetDateTime.now())
                .lastMessageAt(OffsetDateTime.now())
                .build();

        when(conversationRepository.findByUserIdOrderByLastMessageAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(c1)));
        when(chatMessageRepository.countByConversationId(c1.getId())).thenReturn(4L);

        Page<ConversationResponse> page = conversationService.getConversations(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Conv 1");
        assertThat(page.getContent().getFirst().getMessageCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Should return conversation details with ordered messages")
    void shouldReturnConversationDetailsWithMessages() {
        UUID convId = UUID.randomUUID();
        Conversation c = Conversation.builder()
                .id(convId)
                .user(userEntity)
                .title("Trip Planning")
                .createdAt(OffsetDateTime.now())
                .lastMessageAt(OffsetDateTime.now())
                .build();

        ChatMessage msg1 = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversation(c)
                .senderRole(MessageRole.USER)
                .content("Hello")
                .createdAt(OffsetDateTime.now())
                .build();

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(c));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(convId)).thenReturn(List.of(msg1));

        ConversationDetailResponse details = conversationService.getConversationDetails(userId, convId);

        assertThat(details).isNotNull();
        assertThat(details.getTitle()).isEqualTo("Trip Planning");
        assertThat(details.getMessages()).hasSize(1);
        assertThat(details.getMessages().getFirst().getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when conversation not found or owned by another user")
    void shouldThrowWhenConversationNotFound() {
        UUID convId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getConversationDetails(userId, convId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    @DisplayName("Should delete conversation when owned by user")
    void shouldDeleteConversation() {
        UUID convId = UUID.randomUUID();
        Conversation c = Conversation.builder().id(convId).user(userEntity).build();

        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(c));

        conversationService.deleteConversation(userId, convId);

        verify(conversationRepository).delete(c);
    }
}
