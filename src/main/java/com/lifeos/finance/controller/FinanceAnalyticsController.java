package com.lifeos.finance.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.finance.dto.CategoryExpenseResponse;
import com.lifeos.finance.dto.MonthOverMonthResponse;
import com.lifeos.finance.dto.MonthlySummaryResponse;
import com.lifeos.finance.service.FinanceAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Finance Analytics", description = "Deterministic aggregation, month-over-month trends, and category breakdowns")
public class FinanceAnalyticsController {

    private final FinanceAnalyticsService analyticsService;

    @GetMapping("/monthly-summary")
    @Operation(summary = "Monthly financial summary", description = "Calculates total income, expenses, savings rate, and category breakdowns.")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        MonthlySummaryResponse response = analyticsService.getMonthlySummary(currentUserId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category-breakdown")
    @Operation(summary = "Category expense breakdown", description = "Retrieves spending breakdown grouped by expense category.")
    public ResponseEntity<ApiResponse<List<CategoryExpenseResponse>>> getCategoryBreakdown(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        List<CategoryExpenseResponse> response = analyticsService.getCategoryBreakdown(currentUserId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/month-over-month")
    @Operation(summary = "Month-over-month comparison", description = "Calculates spending delta and percentage change compared to previous month.")
    public ResponseEntity<ApiResponse<MonthOverMonthResponse>> getMonthOverMonth(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        MonthOverMonthResponse response = analyticsService.getMonthOverMonthComparison(currentUserId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
