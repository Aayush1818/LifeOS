package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.finance.obligation.dto.MonthlyObligationSummaryResponse;
import com.lifeos.finance.obligation.service.FinancialObligationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for synthesizing multi-domain monthly financial obligations (loans, insurance, recurring bills, travel).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyObligationsTool implements LifeOSTool {

    private final FinancialObligationService financialObligationService;

    @Override
    public String getName() {
        return "get_monthly_obligations";
    }

    @Override
    public String getDescription() {
        return "Aggregates upcoming financial obligations for a specific month and year, including active loan EMIs, insurance premiums, recurring bills, and planned travel budgets, with projected cash flow.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("month")
                        .type("integer")
                        .description("Month of the year (1-12). Defaults to current month.")
                        .required(false)
                        .build(),
                ToolParameter.builder()
                        .name("year")
                        .type("integer")
                        .description("Calendar year (e.g. 2026). Defaults to current year.")
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
        log.debug("Executing MonthlyObligationsTool for user [{}]", userId);
        try {
            LocalDate now = LocalDate.now();
            int month = now.getMonthValue();
            int year = now.getYear();

            if (parameters != null) {
                if (parameters.get("month") != null) {
                    try {
                        month = Integer.parseInt(parameters.get("month").toString());
                    } catch (NumberFormatException ignored) {}
                }
                if (parameters.get("year") != null) {
                    try {
                        year = Integer.parseInt(parameters.get("year").toString());
                    } catch (NumberFormatException ignored) {}
                }
            }

            MonthlyObligationSummaryResponse response = financialObligationService.getMonthlyObligations(userId, month, year);
            return ToolExecutionResult.success(response);
        } catch (Exception e) {
            log.error("Failed to aggregate monthly obligations for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to aggregate monthly obligations: " + e.getMessage());
        }
    }
}
