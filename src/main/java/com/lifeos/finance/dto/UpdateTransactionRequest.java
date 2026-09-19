package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionStatus;
import com.lifeos.finance.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
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
public class UpdateTransactionRequest {

    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    private TransactionType transactionType;

    private TransactionCategory category;

    private LocalDate transactionDate;

    private PaymentMethod paymentMethod;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private String notes;

    private TransactionStatus status;

    private UUID documentId;

    @JsonProperty("isRefund")
    @JsonAlias({"refund", "is_refund"})
    private Boolean isRefund;
}
