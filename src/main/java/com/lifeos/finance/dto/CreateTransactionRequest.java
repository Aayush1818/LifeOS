package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.OTHER;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : PaymentMethod.OTHER;
    }

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private String notes;

    private UUID documentId;

    @JsonProperty("isRecurring")
    @JsonAlias({"recurring", "is_recurring"})
    @Builder.Default
    private boolean isRecurring = false;

    @JsonProperty("isRefund")
    @JsonAlias({"refund", "is_refund"})
    @Builder.Default
    private boolean isRefund = false;
}
