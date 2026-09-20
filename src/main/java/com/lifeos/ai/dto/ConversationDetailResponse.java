package com.lifeos.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {

    private UUID id;

    private String title;

    private OffsetDateTime createdAt;

    private OffsetDateTime lastMessageAt;

    @Builder.Default
    private List<ChatMessageDto> messages = new ArrayList<>();
}
