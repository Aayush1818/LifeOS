package com.lifeos.asset.dto;

import com.lifeos.asset.entity.InvoiceEntity;
import com.lifeos.asset.entity.InvoicePaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;
    private UUID userId;
    private String invoiceNumber;
    private String vendorName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate returnDeadline;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;
    private BigDecimal otherCharges;
    private BigDecimal totalAmount;
    private InvoicePaymentStatus paymentStatus;
    private LocalDate paymentDate;
    private String paymentMethod;
    private UUID transactionId;
    private UUID documentId;
    private String notes;
    @Builder.Default
    private List<InvoiceItemDto> items = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static InvoiceResponse fromEntity(InvoiceEntity entity) {
        if (entity == null) return null;
        List<InvoiceItemDto> itemDtos = new ArrayList<>();
        if (entity.getItems() != null) {
            itemDtos = entity.getItems().stream()
                    .filter(item -> !item.isDeleted())
                    .map(InvoiceItemDto::fromEntity)
                    .toList();
        }

        return InvoiceResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .invoiceNumber(entity.getInvoiceNumber())
                .vendorName(entity.getVendorName())
                .invoiceDate(entity.getInvoiceDate())
                .dueDate(entity.getDueDate())
                .returnDeadline(entity.getReturnDeadline())
                .currency(entity.getCurrency())
                .subtotal(entity.getSubtotal())
                .taxAmount(entity.getTaxAmount())
                .discountAmount(entity.getDiscountAmount())
                .shippingAmount(entity.getShippingAmount())
                .otherCharges(entity.getOtherCharges())
                .totalAmount(entity.getTotalAmount())
                .paymentStatus(entity.getPaymentStatus())
                .paymentDate(entity.getPaymentDate())
                .paymentMethod(entity.getPaymentMethod())
                .transactionId(entity.getTransactionId())
                .documentId(entity.getDocumentId())
                .notes(entity.getNotes())
                .items(itemDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
