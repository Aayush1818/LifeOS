package com.lifeos.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkTransactionRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
}
