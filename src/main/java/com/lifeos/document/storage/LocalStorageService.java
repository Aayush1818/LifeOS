package com.lifeos.document.storage;

import com.lifeos.common.exception.FileStorageException;
import com.lifeos.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Local filesystem implementation of DocumentStorageService.
 * Enforces directory confinement, random UUID storage filenames,
 * and path traversal mitigations.
 */
@Slf4j
@Service
public class LocalStorageService implements DocumentStorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${storage.local.upload-dir:./storage/uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Initialized local document storage directory at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, UUID userId) {
        if (userId == null) {
            throw new FileStorageException("User ID must not be null when storing document");
        }

        try {
            // Sanitize original filename and extract extension
            String sanitizedExtension = extractExtension(originalFilename);
            String storageFilename = UUID.randomUUID() + (sanitizedExtension.isEmpty() ? "" : "." + sanitizedExtension);

            Path userDir = rootLocation.resolve(userId.toString()).normalize();
            if (!userDir.startsWith(rootLocation)) {
                throw new FileStorageException("Security error: User directory escapes storage root");
            }
            Files.createDirectories(userDir);

            Path destinationFile = userDir.resolve(storageFilename).normalize();
            if (!destinationFile.startsWith(userDir)) {
                throw new FileStorageException("Security error: Cannot store file outside user storage directory");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored document on disk at: {}", destinationFile);

            // Store path relative to root: "{userId}/{storageFilename}"
            return userId + "/" + storageFilename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file on disk", e);
        }
    }

    @Override
    public Resource loadAsResource(String storagePath, UUID userId) {
        Path filePath = resolveAndValidatePath(storagePath, userId);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Document file not found on storage: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Error reading document file", e);
        }
    }

    @Override
    public void delete(String storagePath, UUID userId) {
        Path filePath = resolveAndValidatePath(storagePath, userId);
        try {
            Files.deleteIfExists(filePath);
            log.debug("Deleted document file from disk: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete document file from disk: {}", filePath, e);
            throw new FileStorageException("Could not delete physical file: " + storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath, UUID userId) {
        try {
            Path filePath = resolveAndValidatePath(storagePath, userId);
            return Files.exists(filePath) && Files.isRegularFile(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compensating action to clean up an orphaned file without throwing exceptions.
     */
    public void deleteQuietly(String storagePath, UUID userId) {
        try {
            delete(storagePath, userId);
        } catch (Exception e) {
            log.warn("Compensation deletion failed for [{}]: {}", storagePath, e.getMessage());
        }
    }

    private Path resolveAndValidatePath(String storagePath, UUID userId) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new FileStorageException("Storage path must not be null or blank");
        }

        Path resolved = rootLocation.resolve(storagePath).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new FileStorageException("Security error: Path traversal attempt detected");
        }

        // Validate that the storage path strictly belongs to the requesting user directory
        if (userId != null) {
            Path userDir = rootLocation.resolve(userId.toString()).normalize();
            if (!resolved.startsWith(userDir)) {
                throw new FileStorageException("Security error: Access to file outside user scope forbidden");
            }
        }

        return resolved;
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        // Normalize filename, removing any directory path elements
        String cleanName = Paths.get(filename).getFileName().toString();
        int lastDotIndex = cleanName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < cleanName.length() - 1) {
            String ext = cleanName.substring(lastDotIndex + 1).toLowerCase();
            // Sanitize extension to alphanumeric only
            return ext.replaceAll("[^a-zA-Z0-9]", "");
        }
        return "";
    }
}
