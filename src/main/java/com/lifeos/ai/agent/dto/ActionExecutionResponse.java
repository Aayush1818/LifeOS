package com.lifeos.ai.agent.dto;

import com.lifeos.ai.agent.entity.PendingActionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionExecutionResponse {

    private UUID actionId;

    private String toolName;

    private PendingActionStatus status;

    private boolean success;

    private Object result;

    private String message;

    private OffsetDateTime executedAt;
}
