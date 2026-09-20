package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.insurance.dto.InsuranceResponse;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for retrieving user's active insurance policies and upcoming renewal dates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsuranceRenewalsTool implements LifeOSTool {

    private final InsuranceService insuranceService;

    @Override
    public String getName() {
        return "get_insurance_policies";
    }

    @Override
    public String getDescription() {
        return "Retrieves user's active insurance policies, provider names, coverage amounts, and upcoming renewal/expiry dates.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("windowDays")
                        .type("integer")
                        .description("Optional window in days to filter upcoming renewals (default 60)")
                        .required(false)
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.debug("Executing InsuranceRenewalsTool for user [{}]", userId);
        try {
            int windowDays = 60;
            if (parameters != null && parameters.containsKey("windowDays") && parameters.get("windowDays") != null) {
                try {
                    windowDays = Integer.parseInt(parameters.get("windowDays").toString());
                } catch (NumberFormatException ignored) {}
            }

            Page<InsuranceResponse> policies = insuranceService.listPolicies(
                    userId, null, PolicyStatus.ACTIVE, PageRequest.of(0, 50));

            LocalDate threshold = LocalDate.now().plusDays(windowDays);
            List<InsuranceResponse> filtered = policies.getContent().stream()
                    .filter(p -> p.getNextRenewalDate() == null || !p.getNextRenewalDate().isAfter(threshold))
                    .toList();

            return ToolExecutionResult.success(filtered.isEmpty() ? policies.getContent() : filtered);
        } catch (Exception e) {
            log.error("Failed to fetch insurance policies for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to fetch insurance policies: " + e.getMessage());
        }
    }
}
