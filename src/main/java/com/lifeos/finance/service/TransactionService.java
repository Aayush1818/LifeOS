package com.lifeos.finance.service;

import com.lifeos.common.exception.InvalidDocumentException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.dto.UpdateTransactionRequest;
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
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, UUID userId) {
        validateAmount(request.getAmount());

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DocumentEntity document = null;
        if (request.getDocumentId() != null) {
            document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
        }

        // Duplicate submission heuristic check
        boolean duplicate = transactionRepository.existsByUserIdAndAmountAndCategoryAndTransactionDateAndIsDeletedFalse(
                userId, request.getAmount(), request.getCategory(), request.getTransactionDate());
        if (duplicate) {
            log.warn("Potential duplicate transaction detected for user [{}]: amount={}, category={}, date={}",
                    userId, request.getAmount(), request.getCategory(), request.getTransactionDate());
        }

        TransactionEntity transaction = TransactionEntity.builder()
                .user(user)
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .category(request.getCategory())
                .transactionDate(request.getTransactionDate())
                .paymentMethod(request.getPaymentMethod())
                .description(request.getDescription().trim())
                .notes(request.getNotes())
                .status(TransactionStatus.POSTED)
                .document(document)
                .isRecurring(request.isRecurring())
                .isRefund(request.isRefund())
                .build();

        TransactionEntity saved = transactionRepository.save(transaction);
        log.info("User [{}] recorded transaction [{}] (amount: {}, category: {}, type: {})",
                userId, saved.getId(), saved.getAmount(), saved.getCategory(), saved.getTransactionType());

        return TransactionResponse.fromEntity(saved, duplicate);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id, UUID userId) {
        TransactionEntity entity = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return TransactionResponse.fromEntity(entity);
    }

    @Transactional
    public TransactionResponse updateTransaction(UUID id, UpdateTransactionRequest request, UUID userId) {
        TransactionEntity entity = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (request.getAmount() != null) {
            validateAmount(request.getAmount());
            entity.setAmount(request.getAmount());
        }
        if (request.getTransactionType() != null) {
            entity.setTransactionType(request.getTransactionType());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getTransactionDate() != null) {
            entity.setTransactionDate(request.getTransactionDate());
        }
        if (request.getPaymentMethod() != null) {
            entity.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getIsRefund() != null) {
            entity.setRefund(request.getIsRefund());
        }
        if (request.getDocumentId() != null) {
            DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getDocumentId()));
            entity.setDocument(doc);
        }

        TransactionEntity updated = transactionRepository.save(entity);
        log.info("User [{}] updated transaction [{}]", userId, id);
        return TransactionResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteTransaction(UUID id, UUID userId) {
        TransactionEntity entity = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        entity.setDeleted(true);
        transactionRepository.save(entity);
        log.info("User [{}] soft-deleted transaction [{}]", userId, id);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(
            UUID userId,
            TransactionType type,
            TransactionCategory category,
            TransactionStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return transactionRepository.findFiltered(userId, type, category, status, startDate, endDate, pageable)
                .map(TransactionResponse::fromEntity);
    }

    /**
     * Clean programmatic tool contract for future AI Agent invocation.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchTransactions(
            UUID userId,
            TransactionCategory category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return transactionRepository.findFiltered(userId, null, category, TransactionStatus.POSTED, startDate, endDate, pageable)
                .map(TransactionResponse::fromEntity);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly greater than zero");
        }
    }
}
