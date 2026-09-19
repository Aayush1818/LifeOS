package com.lifeos.asset.dto;

import com.lifeos.asset.entity.InvoicePaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateInvoiceRequest {

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;
    private LocalDate returnDeadline;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Builder.Default
    private String currency = "USD";

    @PositiveOrZero(message = "Subtotal must be positive or zero")
    private BigDecimal subtotal;

    @PositiveOrZero(message = "Tax amount must be positive or zero")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2);

    @PositiveOrZero(message = "Discount amount must be positive or zero")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);

    @PositiveOrZero(message = "Shipping amount must be positive or zero")
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO.setScale(2);

    @PositiveOrZero(message = "Other charges must be positive or zero")
    @Builder.Default
    private BigDecimal otherCharges = BigDecimal.ZERO.setScale(2);

    @PositiveOrZero(message = "Total amount must be positive or zero")
    private BigDecimal totalAmount;

    @Builder.Default
    private InvoicePaymentStatus paymentStatus = InvoicePaymentStatus.PAID;

    private LocalDate paymentDate;
    private String paymentMethod;
    private UUID transactionId;
    private UUID documentId;
    private String notes;

    @NotEmpty(message = "Invoice must contain at least one line item")
    @Valid
    private List<InvoiceItemDto> items;
}
