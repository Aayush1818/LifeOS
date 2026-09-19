package com.lifeos.finance.dto;

import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.RecurrencePattern;
import com.lifeos.finance.entity.RecurringStatus;
import com.lifeos.finance.entity.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateRecurringRequest {

    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    private TransactionCategory category;

    private PaymentMethod paymentMethod;

    private RecurrencePattern recurrencePattern;

    @Min(value = 1, message = "Billing day must be between 1 and 31")
    @Max(value = 31, message = "Billing day must be between 1 and 31")
    private Integer billingDay;

    private LocalDate endDate;

    private RecurringStatus status;

    private Boolean autoCreateTransaction;

    private String notes;
}
