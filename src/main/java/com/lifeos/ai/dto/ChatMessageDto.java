package com.lifeos.ai.dto;

import com.lifeos.ai.conversation.entity.MessageRole;
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
public class ChatMessageDto {

    private UUID id;

    private MessageRole role;

    private String content;

    private OffsetDateTime createdAt;
}
