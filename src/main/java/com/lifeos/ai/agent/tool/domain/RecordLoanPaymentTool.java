package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.loan.dto.LoanPaymentResponse;
import com.lifeos.loan.dto.RecordLoanPaymentRequest;
import com.lifeos.loan.entity.PaymentType;
import com.lifeos.loan.entity.PrepaymentStrategy;
import com.lifeos.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mutating tool for recording loan payments.
 * Requires human-in-the-loop confirmation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordLoanPaymentTool implements LifeOSTool {

    private final LoanService loanService;

    @Override
    public String getName() {
        return "record_loan_payment";
    }

    @Override
    public String getDescription() {
        return "Records an EMI or partial prepayment against an active loan, reducing outstanding balance.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("loanId")
                        .type("string")
                        .description("UUID of the active loan to record payment against")
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("amount")
                        .type("number")
                        .description("Payment amount to record (e.g., 500.00)")
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("paymentType")
                        .type("string")
                        .description("Payment type: REGULAR_EMI, PARTIAL_PREPAYMENT, FULL_CLOSURE")
                        .required(false)
                        .enumValues(List.of("REGULAR_EMI", "PARTIAL_PREPAYMENT", "FULL_CLOSURE"))
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }

    @Override
    public String getConfirmationPrompt(Map<String, Object> parameters) {
        String amount = parameters.getOrDefault("amount", "specified amount").toString();
        String loanId = parameters.getOrDefault("loanId", "").toString();
        return "Would you like me to record a payment of $" + amount + " against loan " + loanId + "?";
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.info("Executing RecordLoanPaymentTool for user [{}] with params: {}", userId, parameters);
        try {
            if (!parameters.containsKey("loanId") || parameters.get("loanId") == null) {
                return ToolExecutionResult.failure("Parameter 'loanId' is required");
            }
            if (!parameters.containsKey("amount") || parameters.get("amount") == null) {
                return ToolExecutionResult.failure("Parameter 'amount' is required");
            }

            UUID loanId = UUID.fromString(parameters.get("loanId").toString().trim());
            BigDecimal amount = new BigDecimal(parameters.get("amount").toString().trim());

            PaymentType pType = PaymentType.REGULAR_EMI;
            if (parameters.containsKey("paymentType") && parameters.get("paymentType") != null) {
                try {
                    pType = PaymentType.valueOf(parameters.get("paymentType").toString().trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            RecordLoanPaymentRequest req = RecordLoanPaymentRequest.builder()
                    .paymentAmount(amount)
                    .paymentDate(LocalDate.now())
                    .paymentType(pType)
                    .prepaymentStrategy(PrepaymentStrategy.REDUCE_TENURE)
                    .notes("Recorded via LifeOS Agent Tool")
                    .build();

            LoanPaymentResponse resp = loanService.recordPayment(loanId, req, userId);
            return ToolExecutionResult.success(resp);
        } catch (Exception e) {
            log.error("Failed to record loan payment for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to record loan payment: " + e.getMessage());
        }
    }
}
