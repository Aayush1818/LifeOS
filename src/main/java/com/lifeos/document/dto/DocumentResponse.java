package com.lifeos.document.dto;

import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.entity.IngestionStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;
    private UUID userId;
    private UUID dependentId;
    private String title;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private DocumentCategory category;
    private DocumentType documentType;
    private Integer version;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private List<String> tags;
    private String checksumSha256;
    private IngestionStatus ingestionStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static DocumentResponse fromEntity(DocumentEntity entity) {
        return DocumentResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .dependentId(entity.getDependent() != null ? entity.getDependent().getId() : null)
                .title(entity.getTitle())
                .originalFilename(entity.getOriginalFilename())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .category(entity.getCategory())
                .documentType(entity.getDocumentType())
                .version(entity.getVersion())
                .issueDate(entity.getIssueDate())
                .expiryDate(entity.getExpiryDate())
                .tags(entity.getTags())
                .checksumSha256(entity.getChecksumSha256())
                .ingestionStatus(entity.getIngestionStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
