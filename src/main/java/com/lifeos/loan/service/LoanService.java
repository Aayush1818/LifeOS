package com.lifeos.loan.service;

import com.lifeos.common.exception.DuplicateResourceException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.loan.dto.*;
import com.lifeos.loan.engine.AmortizationSchedule;
import com.lifeos.loan.engine.LoanAmortizationEngine;
import com.lifeos.loan.entity.*;
import com.lifeos.loan.repository.LoanPaymentRepository;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final LoanAmortizationEngine amortizationEngine;

    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request, UUID userId) {
        if (loanRepository.existsByUserIdAndLoanAccountNumberAndIsDeletedFalse(userId, request.getLoanAccountNumber().trim())) {
            throw new DuplicateResourceException("A loan with account number '" + request.getLoanAccountNumber() + "' already exists");
        }

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DocumentEntity document = null;
        if (request.getDocumentId() != null) {
            document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
        }

        BigDecimal monthlyEmi = amortizationEngine.calculateEmi(
                request.getPrincipalAmount(),
                request.getInterestRate(),
                request.getTenureMonths()
        );

        LocalDate endDate = request.getStartDate().plusMonths(request.getTenureMonths());

        LoanEntity loan = LoanEntity.builder()
                .user(user)
                .document(document)
                .loanAccountNumber(request.getLoanAccountNumber().trim())
                .lenderName(request.getLenderName().trim())
                .loanType(request.getLoanType())
                .principalAmount(request.getPrincipalAmount())
                .outstandingBalance(request.getPrincipalAmount())
                .interestRate(request.getInterestRate())
                .interestType(request.getInterestType() != null ? request.getInterestType() : InterestType.FIXED)
                .paymentFrequency(request.getPaymentFrequency() != null ? request.getPaymentFrequency() : PaymentFrequency.MONTHLY)
                .tenureMonths(request.getTenureMonths())
                .monthlyEmi(monthlyEmi)
                .emiDueDay(request.getEmiDueDay())
                .startDate(request.getStartDate())
                .endDate(endDate)
                .totalPrincipalPaid(BigDecimal.ZERO)
                .totalInterestPaid(BigDecimal.ZERO)
                .status(LoanStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        LoanEntity saved = loanRepository.save(loan);
        log.info("User [{}] created loan [{}] for {} (principal: {}, EMI: {})",
                userId, saved.getId(), saved.getLenderName(), saved.getPrincipalAmount(), saved.getMonthlyEmi());

        return LoanResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(UUID id, UUID userId) {
        LoanEntity loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));
        return LoanResponse.fromEntity(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> listLoans(UUID userId, LoanStatus status, Pageable pageable) {
        Page<LoanEntity> page = (status != null)
                ? loanRepository.findAllByUserIdAndStatusAndIsDeletedFalse(userId, status, pageable)
                : loanRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
        return page.map(LoanResponse::fromEntity);
    }

    @Transactional
    public LoanResponse updateLoan(UUID id, UpdateLoanRequest request, UUID userId) {
        LoanEntity loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));

        if (request.getLenderName() != null && !request.getLenderName().isBlank()) {
            loan.setLenderName(request.getLenderName().trim());
        }
        if (request.getInterestType() != null) {
            loan.setInterestType(request.getInterestType());
        }
        if (request.getPaymentFrequency() != null) {
            loan.setPaymentFrequency(request.getPaymentFrequency());
        }
        if (request.getNotes() != null) {
            loan.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            loan.setStatus(request.getStatus());
        }
        if (request.getDocumentId() != null) {
            DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
            loan.setDocument(doc);
        }

        // If interest rate updated on an active loan, recompute EMI for remaining balance and tenure
        if (request.getInterestRate() != null && request.getInterestRate().compareTo(loan.getInterestRate()) != 0) {
            loan.setInterestRate(request.getInterestRate());
            if (loan.getStatus() == LoanStatus.ACTIVE && loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
                int remainingMonths = Math.max(1, (int) ChronoUnit.MONTHS.between(LocalDate.now(), loan.getEndDate()));
                BigDecimal recomputedEmi = amortizationEngine.calculateEmi(
                        loan.getOutstandingBalance(),
                        request.getInterestRate(),
                        remainingMonths
                );
                loan.setMonthlyEmi(recomputedEmi);
            }
        }

        LoanEntity updated = loanRepository.save(loan);
        log.info("User [{}] updated loan [{}]", userId, id);
        return LoanResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteLoan(UUID id, UUID userId) {
        LoanEntity loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));

        loan.setDeleted(true);
        loan.setStatus(LoanStatus.CLOSED);
        loanRepository.save(loan);
        log.info("User [{}] soft-deleted loan [{}]", userId, id);
    }

    @Transactional(readOnly = true)
    public AmortizationScheduleResponse getAmortizationSchedule(UUID id, UUID userId) {
        LoanEntity loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));

        AmortizationSchedule schedule = amortizationEngine.generateSchedule(
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTenureMonths(),
                loan.getStartDate(),
                loan.getEmiDueDay()
        );

        return AmortizationScheduleResponse.fromModel(loan.getId(), schedule);
    }

    @Transactional
    public LoanPaymentResponse recordPayment(UUID id, RecordLoanPaymentRequest request, UUID userId) {
        LoanEntity loan = loanRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));

        if (loan.getStatus() == LoanStatus.CLOSED || loan.getStatus() == LoanStatus.PAID_OFF) {
            throw new IllegalStateException("Cannot record payment on a closed or paid-off loan");
        }

        BigDecimal paymentAmount = request.getPaymentAmount();
        BigDecimal currentBalance = loan.getOutstandingBalance();
        PaymentType paymentType = request.getPaymentType() != null ? request.getPaymentType() : PaymentType.REGULAR_EMI;

        BigDecimal principalComponent;
        BigDecimal interestComponent;

        if (paymentType == PaymentType.FULL_CLOSURE) {
            // Full early closure: pay exact remaining balance
            principalComponent = currentBalance;
            interestComponent = paymentAmount.compareTo(currentBalance) > 0
                    ? paymentAmount.subtract(currentBalance)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            loan.setOutstandingBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            loan.setStatus(LoanStatus.CLOSED);
        } else if (paymentType == PaymentType.PARTIAL_PREPAYMENT) {
            // Partial prepayment applied 100% to principal
            if (paymentAmount.compareTo(currentBalance) >= 0) {
                // Prepayment clears the entire loan
                principalComponent = currentBalance;
                interestComponent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                loan.setOutstandingBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                loan.setStatus(LoanStatus.CLOSED);
            } else {
                principalComponent = paymentAmount;
                interestComponent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal newBalance = currentBalance.subtract(principalComponent).setScale(2, RoundingMode.HALF_UP);
                loan.setOutstandingBalance(newBalance);

                // If strategy is REDUCE_EMI, lower future monthly EMI for remaining tenure
                if (request.getPrepaymentStrategy() == PrepaymentStrategy.REDUCE_EMI) {
                    int remainingMonths = Math.max(1, (int) ChronoUnit.MONTHS.between(request.getPaymentDate(), loan.getEndDate()));
                    BigDecimal recomputedEmi = amortizationEngine.calculateEmi(newBalance, loan.getInterestRate(), remainingMonths);
                    loan.setMonthlyEmi(recomputedEmi);
                    log.info("Loan [{}] EMI recomputed to {} after partial prepayment under REDUCE_EMI strategy", id, recomputedEmi);
                } else {
                    log.info("Loan [{}] tenure effectively shortened under REDUCE_TENURE strategy", id);
                }
            }
        } else {
            // REGULAR_EMI payment
            BigDecimal monthlyRate = loan.getInterestRate().compareTo(BigDecimal.ZERO) > 0
                    ? loan.getInterestRate().divide(BigDecimal.valueOf(1200), MathContext.DECIMAL128)
                    : BigDecimal.ZERO;

            interestComponent = currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal rawPrincipal = paymentAmount.subtract(interestComponent);

            if (rawPrincipal.compareTo(currentBalance) >= 0) {
                principalComponent = currentBalance;
                loan.setOutstandingBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                loan.setStatus(LoanStatus.CLOSED);
            } else {
                principalComponent = rawPrincipal.max(BigDecimal.ZERO);
                BigDecimal newBalance = currentBalance.subtract(principalComponent).setScale(2, RoundingMode.HALF_UP);
                loan.setOutstandingBalance(newBalance);
            }
        }

        loan.setTotalPrincipalPaid(loan.getTotalPrincipalPaid().add(principalComponent));
        loan.setTotalInterestPaid(loan.getTotalInterestPaid().add(interestComponent));
        loanRepository.save(loan);

        LoanPaymentEntity payment = LoanPaymentEntity.builder()
                .loan(loan)
                .paymentAmount(paymentAmount)
                .principalComponent(principalComponent)
                .interestComponent(interestComponent)
                .paymentDate(request.getPaymentDate())
                .paymentType(paymentType)
                .transactionRef(request.getTransactionRef())
                .notes(request.getNotes())
                .build();

        LoanPaymentEntity savedPayment = paymentRepository.save(payment);
        log.info("User [{}] recorded payment [{}] on loan [{}] (amount: {}, type: {}, remaining balance: {})",
                userId, savedPayment.getId(), id, paymentAmount, paymentType, loan.getOutstandingBalance());

        return LoanPaymentResponse.fromEntity(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<LoanPaymentResponse> listPayments(UUID loanId, UUID userId) {
        // Ensure loan exists and belongs to user
        loanRepository.findByIdAndUserIdAndIsDeletedFalse(loanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        return paymentRepository.findAllByLoanIdAndLoanUserIdOrderByPaymentDateDesc(loanId, userId)
                .stream()
                .map(LoanPaymentResponse::fromEntity)
                .toList();
    }
}
