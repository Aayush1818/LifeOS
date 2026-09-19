package com.lifeos.loan.dto;

import com.lifeos.loan.entity.InterestType;
import com.lifeos.loan.entity.LoanType;
import com.lifeos.loan.entity.PaymentFrequency;
import jakarta.validation.constraints.*;
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
public class CreateLoanRequest {

    @NotBlank(message = "Loan account number is required")
    @Size(max = 100, message = "Loan account number must not exceed 100 characters")
    private String loanAccountNumber;

    @NotBlank(message = "Lender name is required")
    @Size(max = 150, message = "Lender name must not exceed 150 characters")
    private String lenderName;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "0.01", message = "Principal amount must be strictly positive")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate;

    @Builder.Default
    private InterestType interestType = InterestType.FIXED;

    @Builder.Default
    private PaymentFrequency paymentFrequency = PaymentFrequency.MONTHLY;

    @Min(value = 1, message = "Tenure months must be at least 1")
    @Max(value = 600, message = "Tenure months cannot exceed 600 (50 years)")
    private int tenureMonths;

    @Min(value = 1, message = "EMI due day must be between 1 and 31")
    @Max(value = 31, message = "EMI due day must be between 1 and 31")
    private int emiDueDay;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private UUID documentId;

    private String notes;
}
