package com.lifeos.insurance.comparison.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClauseDiffDto {
    private String category;
    private String clauseName;
    private String changeType;
    private String document1Value;
    private String document2Value;
    private String impact;
}
