package com.lifeos.finance.obligation.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.finance.obligation.dto.MonthlyObligationSummaryResponse;
import com.lifeos.finance.obligation.service.FinancialObligationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/obligations")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Financial Obligations", description = "Multi-domain aggregation of loan EMIs, insurance renewals, recurring bills, and travel budgets")
public class FinancialObligationController {

    private final FinancialObligationService financialObligationService;

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly financial obligations", description = "Synthesizes loans, insurance, recurring expenses, and trips into a single monthly timeline and cash flow forecast.")
    public ResponseEntity<ApiResponse<MonthlyObligationSummaryResponse>> getMonthlyObligations(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        MonthlyObligationSummaryResponse response = financialObligationService.getMonthlyObligations(
                currentUserId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
