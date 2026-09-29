package com.lifeos.document.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.UUID;

/**
 * Interface-driven document storage abstraction.
 * Allows seamless drop-in replacements (e.g. AWS S3, Azure Blob, GCS)
 * without altering business logic.
 */
public interface DocumentStorageService {

    /**
     * Stores a file stream for the given user.
     *
     * @param inputStream the file content stream
     * @param originalFilename original client filename for extension preservation
     * @param userId owner of the document
     * @return the relative or internal storage path reference
     */
    String store(InputStream inputStream, String originalFilename, UUID userId);

    /**
     * Loads the stored document as a Spring Resource for download streaming.
     *
     * @param storagePath the internal storage reference
     * @param userId owner of the document
     * @return Resource representing the file
     */
    Resource loadAsResource(String storagePath, UUID userId);

    /**
     * Deletes the stored document from physical storage.
     *
     * @param storagePath the internal storage reference
     * @param userId owner of the document
     */
    void delete(String storagePath, UUID userId);

    /**
     * Checks if the physical document exists.
     *
     * @param storagePath the internal storage reference
     * @param userId owner of the document
     * @return true if file exists physically
     */
    boolean exists(String storagePath, UUID userId);
}
