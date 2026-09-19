package com.lifeos.loan.service;

import com.lifeos.loan.dto.LoanPortfolioSummaryResponse;
import com.lifeos.loan.repository.LoanAnalyticsJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanAnalyticsService {

    private final LoanAnalyticsJdbcRepository analyticsRepository;

    @Transactional(readOnly = true)
    public LoanPortfolioSummaryResponse getPortfolioSummary(UUID userId) {
        return analyticsRepository.calculatePortfolioSummary(userId);
    }
}
