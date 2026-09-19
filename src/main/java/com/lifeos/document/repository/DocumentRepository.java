package com.lifeos.document.repository;

import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Page<DocumentEntity> findAllByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<DocumentEntity> findAllByUserIdAndCategoryAndIsDeletedFalse(UUID userId, DocumentCategory category, Pageable pageable);

    Page<DocumentEntity> findAllByUserIdAndDependentIdAndIsDeletedFalse(UUID userId, UUID dependentId, Pageable pageable);

    Page<DocumentEntity> findAllByUserIdAndDependentIdAndCategoryAndIsDeletedFalse(UUID userId, UUID dependentId, DocumentCategory category, Pageable pageable);

    boolean existsByUserIdAndChecksumSha256AndIsDeletedFalse(UUID userId, String checksumSha256);

    long countByUserIdAndIsDeletedFalse(UUID userId);
}
