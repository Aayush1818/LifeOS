package com.lifeos.document.repository;

import com.lifeos.document.entity.DocumentEntityLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentEntityLinkRepository extends JpaRepository<DocumentEntityLinkEntity, UUID> {

    List<DocumentEntityLinkEntity> findAllByEntityTypeAndEntityId(String entityType, UUID entityId);

    Optional<DocumentEntityLinkEntity> findByEntityTypeAndEntityIdAndDocumentId(String entityType, UUID entityId, UUID documentId);

    boolean existsByEntityTypeAndEntityIdAndDocumentId(String entityType, UUID entityId, UUID documentId);

    void deleteAllByEntityTypeAndEntityId(String entityType, UUID entityId);

    void deleteByEntityTypeAndEntityIdAndDocumentId(String entityType, UUID entityId, UUID documentId);
}
