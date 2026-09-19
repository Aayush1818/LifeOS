package com.lifeos.document.repository;

import com.lifeos.document.entity.DocumentChunkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    Page<DocumentChunkEntity> findAllByDocumentIdAndUserIdOrderByChunkIndexAsc(
            UUID documentId, UUID userId, Pageable pageable
    );

    List<DocumentChunkEntity> findAllByDocumentIdAndUserIdAndIsActiveTrueOrderByChunkIndexAsc(
            UUID documentId, UUID userId
    );

    long countByDocumentIdAndUserIdAndIsActiveTrue(UUID documentId, UUID userId);

    @Modifying
    @Query("DELETE FROM DocumentChunkEntity c WHERE c.document.id = :documentId AND c.documentVersion = :version")
    void deleteAllByDocumentIdAndDocumentVersion(
            @Param("documentId") UUID documentId,
            @Param("version") Integer version
    );

    @Modifying
    @Query("UPDATE DocumentChunkEntity c SET c.isActive = false, c.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE c.document.id = :documentId AND c.documentVersion < :newVersion")
    void deactivateOldVersions(
            @Param("documentId") UUID documentId,
            @Param("newVersion") Integer newVersion
    );
}
