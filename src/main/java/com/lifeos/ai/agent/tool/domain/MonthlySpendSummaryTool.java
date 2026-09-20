package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.finance.dto.MonthlySummaryResponse;
import com.lifeos.finance.service.FinanceAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for deterministic calculation of user's income, expenses, savings rate, and category breakdowns.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySpendSummaryTool implements LifeOSTool {

    private final FinanceAnalyticsService financeAnalyticsService;

    @Override
    public String getName() {
        return "get_monthly_spend_summary";
    }

    @Override
    public String getDescription() {
        return "Calculates total income, expenses, net savings, and category expense breakdown for a given month and year.";
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
        log.debug("Executing MonthlySpendSummaryTool for user [{}]", userId);
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

            MonthlySummaryResponse summary = financeAnalyticsService.getMonthlySummary(userId, month, year);
            return ToolExecutionResult.success(summary);
        } catch (Exception e) {
            log.error("Failed to calculate monthly spend summary for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to calculate monthly spend summary: " + e.getMessage());
        }
    }
}
