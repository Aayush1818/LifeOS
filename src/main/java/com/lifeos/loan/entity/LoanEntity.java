package com.lifeos.loan.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    @Column(name = "loan_account_number", nullable = false, length = 100)
    private String loanAccountNumber;

    @Column(name = "lender_name", nullable = false, length = 150)
    private String lenderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 50)
    private LoanType loanType;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 30)
    @Builder.Default
    private InterestType interestType = InterestType.FIXED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false, length = 30)
    @Builder.Default
    private PaymentFrequency paymentFrequency = PaymentFrequency.MONTHLY;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "monthly_emi", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyEmi;

    @Column(name = "emi_due_day", nullable = false)
    private int emiDueDay;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_principal_paid", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

    @Column(name = "total_interest_paid", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
