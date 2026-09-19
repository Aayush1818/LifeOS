package com.lifeos.loan.dto;

import com.lifeos.loan.entity.PaymentType;
import com.lifeos.loan.entity.PrepaymentStrategy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class RecordLoanPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be strictly positive")
    private BigDecimal paymentAmount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @Builder.Default
    private PaymentType paymentType = PaymentType.REGULAR_EMI;

    @Builder.Default
    private PrepaymentStrategy prepaymentStrategy = PrepaymentStrategy.REDUCE_TENURE;

    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionRef;

    private String notes;
}
