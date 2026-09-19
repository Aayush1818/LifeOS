package com.lifeos.document.extractor;

import java.io.InputStream;

/**
 * Pluggable document text and metadata extraction abstraction.
 * Decouples Apache Tika from the document storage and business logic,
 * permitting supplementary OCR or dedicated parsers in the future.
 */
public interface DocumentTextExtractor {

    /**
     * Inspects magic bytes from the stream to accurately detect true content MIME type.
     * Does NOT rely on client headers or file extensions.
     *
     * @param inputStream the binary input stream (supports mark/reset or buffered)
     * @param originalFilename client filename for hint purposes only
     * @return the detected MIME type
     */
    String detectMimeType(InputStream inputStream, String originalFilename);

    /**
     * Extracts text, structural properties, and metadata from the document stream.
     * Implementations must handle corrupted or encrypted files gracefully without
     * throwing unhandled exceptions.
     *
     * @param inputStream the document binary stream
     * @param originalFilename original filename
     * @return structured ExtractionResult
     */
    ExtractionResult extract(InputStream inputStream, String originalFilename);
}
