package com.lifeos.loan.repository;

import com.lifeos.loan.entity.LoanEntity;
import com.lifeos.loan.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {

    Optional<LoanEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<LoanEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<LoanEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, LoanStatus status, Pageable pageable);

    List<LoanEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, LoanStatus status);

    boolean existsByUserIdAndLoanAccountNumberAndIsDeletedFalse(UUID userId, String loanAccountNumber);
}
