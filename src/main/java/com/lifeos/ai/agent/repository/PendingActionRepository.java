package com.lifeos.ai.agent.repository;

import com.lifeos.ai.agent.entity.PendingAction;
import com.lifeos.ai.agent.entity.PendingActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingActionRepository extends JpaRepository<PendingAction, UUID> {

    Optional<PendingAction> findByIdAndUserId(UUID id, UUID userId);

    List<PendingAction> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, PendingActionStatus status);

    List<PendingAction> findByConversationIdAndStatus(UUID conversationId, PendingActionStatus status);
}
