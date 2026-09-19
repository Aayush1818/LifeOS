package com.lifeos.asset.dto;

import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertInvoiceToTransactionRequest {

    private TransactionCategory category;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String description;
}
