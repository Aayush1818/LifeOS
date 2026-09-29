package com.lifeos.insurance.comparison.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyComparisonRequest {

    @NotNull(message = "documentId1 is required")
    private UUID documentId1;

    @NotNull(message = "documentId2 is required")
    private UUID documentId2;
}
