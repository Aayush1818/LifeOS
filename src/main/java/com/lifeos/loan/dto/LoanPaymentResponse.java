package com.lifeos.loan.dto;

import com.lifeos.loan.entity.LoanPaymentEntity;
import com.lifeos.loan.entity.PaymentType;
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
public class LoanPaymentResponse {

    private UUID id;
    private UUID loanId;
    private BigDecimal paymentAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private LocalDate paymentDate;
    private PaymentType paymentType;
    private String transactionRef;
    private String notes;
    private OffsetDateTime createdAt;

    public static LoanPaymentResponse fromEntity(LoanPaymentEntity entity) {
        return LoanPaymentResponse.builder()
                .id(entity.getId())
                .loanId(entity.getLoan().getId())
                .paymentAmount(entity.getPaymentAmount())
                .principalComponent(entity.getPrincipalComponent())
                .interestComponent(entity.getInterestComponent())
                .paymentDate(entity.getPaymentDate())
                .paymentType(entity.getPaymentType())
                .transactionRef(entity.getTransactionRef())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
