package com.lifeos.ai.agent.dto;

import com.lifeos.ai.agent.entity.PendingActionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionDto {

    private UUID id;

    private UUID conversationId;

    private String toolName;

    private Map<String, Object> parameters;

    private String prompt;

    private PendingActionStatus status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;
}
