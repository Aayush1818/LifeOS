package com.lifeos.finance.dto;

import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.RecurrencePattern;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecurringRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Recurrence pattern is required")
    private RecurrencePattern recurrencePattern;

    @Min(value = 1, message = "Billing day must be between 1 and 31")
    @Max(value = 31, message = "Billing day must be between 1 and 31")
    private int billingDay;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private boolean autoCreateTransaction = false;

    private String notes;
}
