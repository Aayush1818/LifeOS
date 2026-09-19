package com.lifeos.finance.dto;

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
public class RecurringResponse {

    private UUID id;
    private String title;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionCategory category;
    private PaymentMethod paymentMethod;
    private RecurrencePattern recurrencePattern;
    private int billingDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextDueDate;
    private LocalDate lastGeneratedDate;
    private RecurringStatus status;
    private boolean autoCreateTransaction;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static RecurringResponse fromEntity(RecurringTransactionEntity entity) {
        return RecurringResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .amount(entity.getAmount())
                .transactionType(entity.getTransactionType())
                .category(entity.getCategory())
                .paymentMethod(entity.getPaymentMethod())
                .recurrencePattern(entity.getRecurrencePattern())
                .billingDay(entity.getBillingDay())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .nextDueDate(entity.getNextDueDate())
                .lastGeneratedDate(entity.getLastGeneratedDate())
                .status(entity.getStatus())
                .autoCreateTransaction(entity.isAutoCreateTransaction())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
