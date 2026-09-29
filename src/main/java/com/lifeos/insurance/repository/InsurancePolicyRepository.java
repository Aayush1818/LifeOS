package com.lifeos.insurance.repository;

import com.lifeos.insurance.entity.InsurancePolicyEntity;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicyEntity, UUID> {

    Optional<InsurancePolicyEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<InsurancePolicyEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<InsurancePolicyEntity> findAllByUserIdAndPolicyTypeAndIsDeletedFalse(UUID userId, InsuranceType policyType, Pageable pageable);

    Page<InsurancePolicyEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, PolicyStatus status, Pageable pageable);

    List<InsurancePolicyEntity> findAllByUserIdAndStatusAndIsDeletedFalse(UUID userId, PolicyStatus status);

    List<InsurancePolicyEntity> findAllByUserIdAndStatusAndNextRenewalDateBetweenAndIsDeletedFalse(
            UUID userId, PolicyStatus status, LocalDate startDate, LocalDate endDate);

    boolean existsByUserIdAndPolicyNumberAndIsDeletedFalse(UUID userId, String policyNumber);
}
