package com.lifeos.loan.dto;

import com.lifeos.loan.entity.InterestType;
import com.lifeos.loan.entity.LoanStatus;
import com.lifeos.loan.entity.PaymentFrequency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoanRequest {

    @Size(max = 150, message = "Lender name must not exceed 150 characters")
    private String lenderName;

    @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate;

    private InterestType interestType;

    private PaymentFrequency paymentFrequency;

    private LoanStatus status;

    private UUID documentId;

    private String notes;
}
