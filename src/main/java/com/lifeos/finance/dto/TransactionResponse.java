package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifeos.finance.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionCategory category;
    private LocalDate transactionDate;
    private PaymentMethod paymentMethod;
    private String description;
    private String notes;
    private TransactionStatus status;
    private UUID documentId;
    private UUID recurringId;

    @JsonProperty("isRecurring")
    private boolean isRecurring;

    @JsonProperty("isRefund")
    private boolean isRefund;

    @JsonProperty("possibleDuplicateWarning")
    private boolean possibleDuplicateWarning;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TransactionResponse fromEntity(TransactionEntity entity) {
        return fromEntity(entity, false);
    }

    public static TransactionResponse fromEntity(TransactionEntity entity, boolean possibleDuplicateWarning) {
        return TransactionResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .transactionType(entity.getTransactionType())
                .category(entity.getCategory())
                .transactionDate(entity.getTransactionDate())
                .paymentMethod(entity.getPaymentMethod())
                .description(entity.getDescription())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .recurringId(entity.getRecurringTransaction() != null ? entity.getRecurringTransaction().getId() : null)
                .isRecurring(entity.isRecurring())
                .isRefund(entity.isRefund())
                .possibleDuplicateWarning(possibleDuplicateWarning)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
