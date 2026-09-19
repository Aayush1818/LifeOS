package com.lifeos.asset.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquisitionCostSummaryDto {

    private Map<String, BigDecimal> totalsByCurrency;
    private Map<String, Long> assetCountByCurrency;
    private Long assetsWithUnrecordedCost;
    private BigDecimal consolidatedTotal;
}
