package com.lifeos.ai.dto;

import com.lifeos.rag.retrieval.dto.RetrievalFilters;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Message content must not be blank")
    @Size(max = 2000, message = "Message content must not exceed 2000 characters")
    private String content;

    @Builder.Default
    private RetrievalMode retrievalMode = RetrievalMode.HYBRID;

    @Builder.Default
    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 20, message = "topK must not exceed 20")
    private int topK = 5;

    private RetrievalFilters filters;
}
