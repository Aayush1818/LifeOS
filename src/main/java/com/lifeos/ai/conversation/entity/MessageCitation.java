package com.lifeos.ai.conversation.entity;

import com.lifeos.document.entity.DocumentEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Verified provenance citation associated with an assistant message.
 */
@Getter
@Setter
@Entity
@Table(name = "message_citations")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "snippet", nullable = false, columnDefinition = "text")
    private String snippet;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "citation_index", nullable = false)
    @Builder.Default
    private Integer citationIndex = 1;

    @Column(name = "section_title", length = 255)
    private String sectionTitle;

    @Column(name = "source_citation", length = 500)
    private String sourceCitation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (citationIndex == null) {
            citationIndex = 1;
        }
    }
}
