package com.lifeos.finance.obligation.service;

import com.lifeos.finance.obligation.dto.MonthlyObligationSummaryResponse;

import java.util.UUID;

public interface FinancialObligationService {
    MonthlyObligationSummaryResponse getMonthlyObligations(UUID userId, int month, int year);
}
