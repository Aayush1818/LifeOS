package com.lifeos.asset.dto;

import com.lifeos.asset.entity.InvoicePaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceRequest {

    private String invoiceNumber;
    private String vendorName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate returnDeadline;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    @PositiveOrZero(message = "Subtotal must be positive or zero")
    private BigDecimal subtotal;

    @PositiveOrZero(message = "Tax amount must be positive or zero")
    private BigDecimal taxAmount;

    @PositiveOrZero(message = "Discount amount must be positive or zero")
    private BigDecimal discountAmount;

    @PositiveOrZero(message = "Shipping amount must be positive or zero")
    private BigDecimal shippingAmount;

    @PositiveOrZero(message = "Other charges must be positive or zero")
    private BigDecimal otherCharges;

    @PositiveOrZero(message = "Total amount must be positive or zero")
    private BigDecimal totalAmount;

    private InvoicePaymentStatus paymentStatus;
    private LocalDate paymentDate;
    private String paymentMethod;
    private UUID transactionId;
    private UUID documentId;
    private String notes;

    @Valid
    private List<InvoiceItemDto> items;
}
