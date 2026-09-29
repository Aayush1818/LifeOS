package com.lifeos.insight.analyzer;

import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.insurance.entity.InsurancePolicyEntity;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.repository.InsurancePolicyRepository;
import com.lifeos.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsuranceGapAnalyzer implements InsightAnalyzer {

    private final InsurancePolicyRepository insurancePolicyRepository;
    private final DependentRepository dependentRepository;

    @Override
    public List<InsightEntity> analyze(UserEntity user) {
        List<InsightEntity> insights = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate renewalThreshold = now.plusDays(30);

        List<InsurancePolicyEntity> policies = insurancePolicyRepository.findAllByUserIdAndStatusAndIsDeletedFalse(
                user.getId(), PolicyStatus.ACTIVE);

        // 1. Upcoming renewals within 30 days
        for (InsurancePolicyEntity policy : policies) {
            LocalDate renewalDate = policy.getNextRenewalDate();
            if (renewalDate != null && !renewalDate.isBefore(now) && renewalDate.isBefore(renewalThreshold)) {
                BigDecimal premium = policy.getPremiumAmount() != null ? policy.getPremiumAmount() : BigDecimal.ZERO;

                insights.add(InsightEntity.builder()
                        .user(user)
                        .insightType(InsightType.INSURANCE_GAP)
                        .severity(InsightSeverity.WARNING)
                        .title("Insurance Renewal Due: " + policy.getPolicyName())
                        .description(String.format("Your %s (%s) policy is due for renewal on %s with a scheduled premium of $%s. Review terms or compare renewal alternatives.",
                                policy.getProviderName(), policy.getPolicyType(), renewalDate, premium.toPlainString()))
                        .actionType(InsightActionType.RENEW_POLICY)
                        .actionPayload(Map.of(
                                "policyId", policy.getId().toString(),
                                "policyName", policy.getPolicyName(),
                                "renewalDate", renewalDate.toString(),
                                "premiumAmount", premium
                        ))
                        .build());
            }
        }

        // 2. Dependents without active health insurance coverage
        List<DependentEntity> dependents = dependentRepository.findAllByUserIdAndIsDeletedFalse(user.getId());
        if (!dependents.isEmpty()) {
            Set<UUID> coveredDependentIds = new HashSet<>();
            for (InsurancePolicyEntity p : policies) {
                if (p.getPolicyType() == InsuranceType.HEALTH && p.getDependent() != null) {
                    coveredDependentIds.add(p.getDependent().getId());
                }
            }

            for (DependentEntity dep : dependents) {
                if (!coveredDependentIds.contains(dep.getId())) {
                    insights.add(InsightEntity.builder()
                            .user(user)
                            .insightType(InsightType.INSURANCE_GAP)
                            .severity(InsightSeverity.WARNING)
                            .title(String.format("Health Coverage Gap: %s", dep.getFullName()))
                            .description(String.format("No active health insurance policy is currently linked to dependent %s. Ensure critical healthcare coverage is in place.",
                                    dep.getFullName()))
                            .actionType(InsightActionType.RENEW_POLICY)
                            .actionPayload(Map.of(
                                    "dependentId", dep.getId().toString(),
                                    "dependentName", dep.getFullName()
                            ))
                            .build());
                }
            }
        }

        return insights;
    }
}
