package com.lifeos.travel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSpendSummary {

    private String baseCurrency;

    @Builder.Default
    private Map<String, BigDecimal> totalsByCurrency = new HashMap<>();

    /**
     * Consolidated total in baseCurrency.
     * ONLY populated if all items are in baseCurrency or an explicit exchange rate
     * is recorded for every foreign currency item. Never uses guessed/invented rates.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private BigDecimal consolidatedTotal;

    private boolean hasUnconvertedCurrencies;

    private String conversionPolicyNotice;
}
