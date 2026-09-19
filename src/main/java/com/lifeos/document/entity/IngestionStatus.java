package com.lifeos.document.entity;

public enum IngestionStatus {
    UPLOADING,
    STORED,
    PROCESSING,
    PROCESSED,
    EXTRACTION_FAILED,
    EMBEDDING_FAILED,
    FAILED,
    DELETED
}
