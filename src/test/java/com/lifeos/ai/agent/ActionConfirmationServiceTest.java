package com.lifeos.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.agent.dto.ActionExecutionResponse;
import com.lifeos.ai.agent.dto.PendingActionDto;
import com.lifeos.ai.agent.entity.PendingAction;
import com.lifeos.ai.agent.entity.PendingActionStatus;
import com.lifeos.ai.agent.repository.PendingActionRepository;
import com.lifeos.ai.agent.service.ActionConfirmationService;
import com.lifeos.ai.agent.tool.LifeOSToolRegistry;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.ai.conversation.repository.ChatMessageRepository;
import com.lifeos.ai.conversation.repository.ConversationRepository;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionConfirmationServiceTest {

    @Mock
    private PendingActionRepository pendingActionRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LifeOSToolRegistry toolRegistry;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ActionConfirmationService confirmationService;

    private UUID userId;
    private UUID conversationId;
    private UserEntity user;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        user = UserEntity.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        user.setId(userId);

        conversation = Conversation.builder()
                .id(conversationId)
                .title("Financial Planning")
                .build();
    }

    @Test
    @DisplayName("Should create pending action successfully with PENDING status and 10 min expiration")
    void shouldCreatePendingAction() {
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> {
            PendingAction pa = inv.getArgument(0);
            pa.setId(UUID.randomUUID());
            return pa;
        });

        PendingActionDto result = confirmationService.createPendingAction(
                userId, conversationId, "create_reminder", Map.of("title", "Renew Passport"), "Create a reminder for 'Renew Passport'?");

        assertThat(result).isNotNull();
        assertThat(result.getToolName()).isEqualTo("create_reminder");
        assertThat(result.getStatus()).isEqualTo(PendingActionStatus.PENDING);
        assertThat(result.getExpiresAt()).isAfter(OffsetDateTime.now());
        assertThat(result.getPrompt()).contains("Renew Passport");
        verify(pendingActionRepository).save(any(PendingAction.class));
    }

    @Test
    @DisplayName("Should list pending actions for authenticated user")
    void shouldGetPendingActions() {
        PendingAction action = PendingAction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .toolName("create_reminder")
                .parameters("{\"title\":\"Buy Milk\"}")
                .prompt("Create reminder?")
                .status(PendingActionStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .createdAt(OffsetDateTime.now())
                .build();

        when(pendingActionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PendingActionStatus.PENDING))
                .thenReturn(List.of(action));

        List<PendingActionDto> actions = confirmationService.getPendingActions(userId);

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().getToolName()).isEqualTo("create_reminder");
        assertThat(actions.getFirst().getParameters()).containsEntry("title", "Buy Milk");
    }

    @Test
    @DisplayName("Should confirm pending action, execute tool via registry, and update status to CONFIRMED")
    void shouldConfirmActionSuccessfully() {
        UUID actionId = UUID.randomUUID();
        PendingAction action = PendingAction.builder()
                .id(actionId)
                .user(user)
                .conversation(conversation)
                .toolName("create_reminder")
                .parameters("{\"title\":\"Schedule Dentist Appointment\"}")
                .prompt("Create reminder?")
                .status(PendingActionStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .createdAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(pendingActionRepository.findByIdAndUserId(actionId, userId)).thenReturn(Optional.of(action));
        when(toolRegistry.execute(eq("create_reminder"), eq(userId), any()))
                .thenReturn(ToolExecutionResult.success(Map.of("reminderId", "rem-123")));

        ActionExecutionResponse response = confirmationService.confirmAction(userId, actionId);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.CONFIRMED);
        assertThat(action.getStatus()).isEqualTo(PendingActionStatus.CONFIRMED);
        assertThat(action.getExecutedAt()).isNotNull();

        verify(toolRegistry).execute(eq("create_reminder"), eq(userId), any());
        verify(chatMessageRepository).save(any());
        verify(pendingActionRepository).save(action);
    }

    @Test
    @DisplayName("Should reject pending action without executing tool and update status to REJECTED")
    void shouldRejectActionSuccessfully() {
        UUID actionId = UUID.randomUUID();
        PendingAction action = PendingAction.builder()
                .id(actionId)
                .user(user)
                .conversation(conversation)
                .toolName("create_reminder")
                .parameters("{\"title\":\"Buy Grocery\"}")
                .prompt("Create reminder?")
                .status(PendingActionStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .createdAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(pendingActionRepository.findByIdAndUserId(actionId, userId)).thenReturn(Optional.of(action));

        ActionExecutionResponse response = confirmationService.rejectAction(userId, actionId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.REJECTED);
        assertThat(action.getStatus()).isEqualTo(PendingActionStatus.REJECTED);

        verify(toolRegistry, never()).execute(any(), any(), any());
        verify(chatMessageRepository).save(any());
        verify(pendingActionRepository).save(action);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when confirming non-PENDING action")
    void shouldThrowWhenConfirmingAlreadyProcessedAction() {
        UUID actionId = UUID.randomUUID();
        PendingAction action = PendingAction.builder()
                .id(actionId)
                .user(user)
                .toolName("create_reminder")
                .status(PendingActionStatus.CONFIRMED)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .build();

        when(pendingActionRepository.findByIdAndUserId(actionId, userId)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> confirmationService.confirmAction(userId, actionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Action is already CONFIRMED");
    }

    @Test
    @DisplayName("Should expire action and throw IllegalStateException when confirming expired action")
    void shouldExpireAndThrowWhenConfirmingExpiredAction() {
        UUID actionId = UUID.randomUUID();
        PendingAction action = PendingAction.builder()
                .id(actionId)
                .user(user)
                .toolName("create_reminder")
                .status(PendingActionStatus.PENDING)
                .expiresAt(OffsetDateTime.now().minusMinutes(1)) // Expired
                .build();

        when(pendingActionRepository.findByIdAndUserId(actionId, userId)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> confirmationService.confirmAction(userId, actionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Action has expired");

        assertThat(action.getStatus()).isEqualTo(PendingActionStatus.EXPIRED);
        verify(pendingActionRepository).save(action);
        verify(toolRegistry, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("Should enforce cross-tenant isolation and throw ResourceNotFoundException for alien actions")
    void shouldEnforceCrossTenantIsolation() {
        UUID actionId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        when(pendingActionRepository.findByIdAndUserId(actionId, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmationService.confirmAction(otherUserId, actionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pending action not found with id");

        assertThatThrownBy(() -> confirmationService.rejectAction(otherUserId, actionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pending action not found with id");
    }
}
