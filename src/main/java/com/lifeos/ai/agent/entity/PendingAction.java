package com.lifeos.ai.agent.entity;

import com.lifeos.ai.conversation.entity.Conversation;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted interactive confirmation card for state-mutating agent actions.
 */
@Getter
@Setter
@Entity
@Table(name = "pending_actions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String parameters = "{}";

    @Column(name = "prompt", nullable = false, columnDefinition = "text")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PendingActionStatus status = PendingActionStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusMinutes(10);
        }
        if (status == null) {
            status = PendingActionStatus.PENDING;
        }
        if (parameters == null) {
            parameters = "{}";
        }
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }
}
