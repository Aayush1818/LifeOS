package com.lifeos.asset.service;

import com.lifeos.asset.dto.*;
import com.lifeos.asset.entity.*;
import com.lifeos.asset.repository.AssetRepository;
import com.lifeos.asset.repository.InvoiceItemRepository;
import com.lifeos.asset.repository.InvoiceRepository;
import com.lifeos.common.exception.DuplicateResourceException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionEntity;
import com.lifeos.finance.entity.TransactionStatus;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.repository.TransactionRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, UUID userId) {
        log.info("User [{}] creating invoice [{}] from vendor [{}]", userId, request.getInvoiceNumber(), request.getVendorName());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate optional transaction linkage
        if (request.getTransactionId() != null) {
            transactionRepository.findByIdAndUserIdAndIsDeletedFalse(request.getTransactionId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + request.getTransactionId()));

            if (invoiceRepository.existsByTransactionIdAndIsDeletedFalse(request.getTransactionId())) {
                throw new DuplicateResourceException("Transaction is already linked to another invoice");
            }
        }

        BigDecimal subtotal = request.getSubtotal();
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            for (InvoiceItemDto itemDto : request.getItems()) {
                BigDecimal itemTotal = itemDto.getUnitPrice()
                        .multiply(BigDecimal.valueOf(itemDto.getQuantity() != null ? itemDto.getQuantity() : 1))
                        .setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(itemTotal);
            }
        }

        BigDecimal tax = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = request.getShippingAmount() != null ? request.getShippingAmount() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal other = request.getOtherCharges() != null ? request.getOtherCharges() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = request.getTotalAmount();
        if (total == null) {
            total = subtotal.add(tax).add(shipping).add(other).subtract(discount).setScale(2, RoundingMode.HALF_UP);
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }

        InvoiceEntity invoice = InvoiceEntity.builder()
                .user(user)
                .invoiceNumber(request.getInvoiceNumber().trim())
                .vendorName(request.getVendorName().trim())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .returnDeadline(request.getReturnDeadline())
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "USD")
                .subtotal(subtotal)
                .taxAmount(tax)
                .discountAmount(discount)
                .shippingAmount(shipping)
                .otherCharges(other)
                .totalAmount(total)
                .paymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : InvoicePaymentStatus.PAID)
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .transactionId(request.getTransactionId())
                .documentId(request.getDocumentId())
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        for (InvoiceItemDto itemDto : request.getItems()) {
            AssetEntity linkedAsset = null;
            if (itemDto.getAssetId() != null) {
                linkedAsset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(itemDto.getAssetId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + itemDto.getAssetId()));
            }

            BigDecimal unitPrice = itemDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
            int qty = itemDto.getQuantity() != null ? itemDto.getQuantity() : 1;
            BigDecimal lineTotal = itemDto.getTotalPrice() != null ? itemDto.getTotalPrice().setScale(2, RoundingMode.HALF_UP)
                    : unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            InvoiceItemEntity itemEntity = InvoiceItemEntity.builder()
                    .asset(linkedAsset)
                    .itemDescription(itemDto.getItemDescription().trim())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .notes(itemDto.getNotes())
                    .build();

            invoice.addItem(itemEntity);
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(UUID userId, String vendor, InvoicePaymentStatus status,
                                             LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<InvoiceEntity> page;
        if (vendor != null && !vendor.isBlank()) {
            page = invoiceRepository.findAllByUserIdAndVendorNameContainingIgnoreCaseAndIsDeletedFalse(userId, vendor.trim(), pageable);
        } else if (status != null) {
            page = invoiceRepository.findAllByUserIdAndPaymentStatusAndIsDeletedFalse(userId, status, pageable);
        } else if (startDate != null && endDate != null) {
            page = invoiceRepository.findAllByUserIdAndInvoiceDateBetweenAndIsDeletedFalse(userId, startDate, endDate, pageable);
        } else {
            page = invoiceRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
        }
        return page.map(InvoiceResponse::fromEntity);
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID id, UpdateInvoiceRequest request, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(request.getInvoiceNumber().trim());
        }
        if (request.getVendorName() != null && !request.getVendorName().isBlank()) {
            invoice.setVendorName(request.getVendorName().trim());
        }
        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getReturnDeadline() != null) {
            invoice.setReturnDeadline(request.getReturnDeadline());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            invoice.setCurrency(request.getCurrency().toUpperCase().trim());
        }
        if (request.getSubtotal() != null) {
            invoice.setSubtotal(request.getSubtotal().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getTaxAmount() != null) {
            invoice.setTaxAmount(request.getTaxAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getDiscountAmount() != null) {
            invoice.setDiscountAmount(request.getDiscountAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getShippingAmount() != null) {
            invoice.setShippingAmount(request.getShippingAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getOtherCharges() != null) {
            invoice.setOtherCharges(request.getOtherCharges().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getTotalAmount() != null) {
            invoice.setTotalAmount(request.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getPaymentStatus() != null) {
            invoice.setPaymentStatus(request.getPaymentStatus());
        }
        if (request.getPaymentDate() != null) {
            invoice.setPaymentDate(request.getPaymentDate());
        }
        if (request.getPaymentMethod() != null) {
            invoice.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getDocumentId() != null) {
            invoice.setDocumentId(request.getDocumentId());
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        if (request.getItems() != null) {
            invoice.getItems().clear();
            for (InvoiceItemDto itemDto : request.getItems()) {
                AssetEntity linkedAsset = null;
                if (itemDto.getAssetId() != null) {
                    linkedAsset = assetRepository.findByIdAndUserIdAndIsDeletedFalse(itemDto.getAssetId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + itemDto.getAssetId()));
                }

                BigDecimal unitPrice = itemDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
                int qty = itemDto.getQuantity() != null ? itemDto.getQuantity() : 1;
                BigDecimal lineTotal = itemDto.getTotalPrice() != null ? itemDto.getTotalPrice().setScale(2, RoundingMode.HALF_UP)
                        : unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

                InvoiceItemEntity itemEntity = InvoiceItemEntity.builder()
                        .asset(linkedAsset)
                        .itemDescription(itemDto.getItemDescription().trim())
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .totalPrice(lineTotal)
                        .notes(itemDto.getNotes())
                        .build();

                invoice.addItem(itemEntity);
            }
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteInvoice(UUID id, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        invoice.setDeleted(true);
        invoiceRepository.save(invoice);
        log.info("Soft deleted invoice [{}] for user [{}]", id, userId);
    }

    @Transactional
    public InvoiceResponse linkTransaction(UUID invoiceId, UUID transactionId, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        // Ownership validation: transaction must exist and belong to the same authenticated user
        transactionRepository.findByIdAndUserIdAndIsDeletedFalse(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Idempotency: if already linked to the exact same transaction, return successfully
        if (invoice.getTransactionId() != null) {
            if (invoice.getTransactionId().equals(transactionId)) {
                return InvoiceResponse.fromEntity(invoice);
            }
            throw new DuplicateResourceException("Invoice is already linked to transaction: " + invoice.getTransactionId());
        }

        // Validate that no other invoice has claimed this transaction
        Optional<InvoiceEntity> other = invoiceRepository.findByTransactionIdAndIsDeletedFalse(transactionId);
        if (other.isPresent() && !other.get().getId().equals(invoiceId)) {
            throw new DuplicateResourceException("Transaction is already linked to another invoice: " + other.get().getId());
        }

        invoice.setTransactionId(transactionId);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("User [{}] linked invoice [{}] to transaction [{}]", userId, invoiceId, transactionId);
        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional
    public InvoiceResponse unlinkTransaction(UUID invoiceId, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        invoice.setTransactionId(null);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("User [{}] unlinked transaction from invoice [{}]", userId, invoiceId);
        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional
    public InvoiceResponse convertToTransaction(UUID invoiceId, ConvertInvoiceToTransactionRequest request, UUID userId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndUserIdAndIsDeletedFalse(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        // Duplicate prevention: cannot convert an invoice that is already linked to a transaction
        if (invoice.getTransactionId() != null) {
            throw new DuplicateResourceException("Invoice already has a linked transaction with ID: " + invoice.getTransactionId());
        }

        TransactionCategory category = request != null && request.getCategory() != null
                ? request.getCategory() : TransactionCategory.SHOPPING;

        PaymentMethod paymentMethod = request != null && request.getPaymentMethod() != null
                ? request.getPaymentMethod() : PaymentMethod.OTHER;

        LocalDate txDate = request != null && request.getPaymentDate() != null
                ? request.getPaymentDate()
                : (invoice.getPaymentDate() != null ? invoice.getPaymentDate() : invoice.getInvoiceDate());

        String description = request != null && request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : "Invoice: " + invoice.getVendorName() + " (" + invoice.getInvoiceNumber() + ")";

        TransactionEntity newTx = TransactionEntity.builder()
                .user(invoice.getUser())
                .amount(invoice.getTotalAmount())
                .transactionType(TransactionType.EXPENSE)
                .category(category)
                .paymentMethod(paymentMethod)
                .transactionDate(txDate)
                .description(description)
                .status(TransactionStatus.POSTED)
                .notes("Generated from Invoice #" + invoice.getInvoiceNumber())
                .build();

        TransactionEntity savedTx = transactionRepository.save(newTx);

        invoice.setTransactionId(savedTx.getId());
        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("User [{}] converted invoice [{}] to new expense transaction [{}] of amount [{}]",
                userId, invoiceId, savedTx.getId(), savedTx.getAmount());

        return InvoiceResponse.fromEntity(saved);
    }
}
