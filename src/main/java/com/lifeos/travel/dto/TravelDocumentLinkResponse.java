package com.lifeos.travel.dto;

import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelDocumentLinkResponse {

    private UUID id;
    private String title;
    private String originalFilename;
    private String mimeType;
    private DocumentType documentType;
    private OffsetDateTime createdAt;

    public static TravelDocumentLinkResponse fromEntity(DocumentEntity entity) {
        return TravelDocumentLinkResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .originalFilename(entity.getOriginalFilename())
                .mimeType(entity.getMimeType())
                .documentType(entity.getDocumentType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
