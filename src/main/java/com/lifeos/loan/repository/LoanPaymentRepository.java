package com.lifeos.loan.repository;

import com.lifeos.loan.entity.LoanPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanPaymentRepository extends JpaRepository<LoanPaymentEntity, UUID> {

    List<LoanPaymentEntity> findAllByLoanIdAndLoanUserIdOrderByPaymentDateDesc(UUID loanId, UUID userId);
}
