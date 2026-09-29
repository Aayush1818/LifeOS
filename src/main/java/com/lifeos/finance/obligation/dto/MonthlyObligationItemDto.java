package com.lifeos.finance.obligation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyObligationItemDto {
    private UUID id;
    private String title;
    private String category;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String currency;
    private String providerOrLender;
    private UUID sourceEntityId;
    private String status;
}
