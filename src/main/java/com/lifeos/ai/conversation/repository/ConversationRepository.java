package com.lifeos.ai.conversation.repository;

import com.lifeos.ai.conversation.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    Page<Conversation> findByUserIdOrderByLastMessageAtDesc(UUID userId, Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
