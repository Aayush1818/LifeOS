package com.lifeos.document.dto;

import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentRequest {

    @NotBlank(message = "Document title is required")
    @Size(max = 255, message = "Document title must not exceed 255 characters")
    private String title;

    @NotNull(message = "Document category is required")
    private DocumentCategory category;

    private DocumentType documentType;

    private UUID dependentId;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
