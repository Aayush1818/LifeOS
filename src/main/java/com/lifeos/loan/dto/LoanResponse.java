package com.lifeos.loan.dto;

import com.lifeos.loan.entity.*;
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
public class LoanResponse {

    private UUID id;
    private String loanAccountNumber;
    private String lenderName;
    private LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal interestRate;
    private InterestType interestType;
    private PaymentFrequency paymentFrequency;
    private int tenureMonths;
    private BigDecimal monthlyEmi;
    private int emiDueDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPrincipalPaid;
    private BigDecimal totalInterestPaid;
    private LoanStatus status;
    private UUID documentId;
    private String documentTitle;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static LoanResponse fromEntity(LoanEntity entity) {
        return LoanResponse.builder()
                .id(entity.getId())
                .loanAccountNumber(entity.getLoanAccountNumber())
                .lenderName(entity.getLenderName())
                .loanType(entity.getLoanType())
                .principalAmount(entity.getPrincipalAmount())
                .outstandingBalance(entity.getOutstandingBalance())
                .interestRate(entity.getInterestRate())
                .interestType(entity.getInterestType())
                .paymentFrequency(entity.getPaymentFrequency())
                .tenureMonths(entity.getTenureMonths())
                .monthlyEmi(entity.getMonthlyEmi())
                .emiDueDay(entity.getEmiDueDay())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalPrincipalPaid(entity.getTotalPrincipalPaid())
                .totalInterestPaid(entity.getTotalInterestPaid())
                .status(entity.getStatus())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .documentTitle(entity.getDocument() != null ? entity.getDocument().getTitle() : null)
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
