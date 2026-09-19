package com.lifeos.document.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TikaDocumentTextExtractor implements DocumentTextExtractor {

    private final Tika tika;
    private final AutoDetectParser parser;
    private static final int MAX_TEXT_LIMIT_CHARS = 10 * 1024 * 1024; // 10 Million characters

    public TikaDocumentTextExtractor() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
    }

    @Override
    public String detectMimeType(InputStream inputStream, String originalFilename) {
        try (TikaInputStream tis = TikaInputStream.get(inputStream)) {
            Metadata metadata = new Metadata();
            if (originalFilename != null && !originalFilename.isBlank()) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, originalFilename);
            }
            // Magic-byte detection via Tika
            return tika.detect(tis, metadata);
        } catch (Exception e) {
            log.warn("MIME detection failed, falling back to application/octet-stream: {}", e.getMessage());
            return "application/octet-stream";
        }
    }

    @Override
    public ExtractionResult extract(InputStream inputStream, String originalFilename) {
        Metadata metadata = new Metadata();
        if (originalFilename != null && !originalFilename.isBlank()) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, originalFilename);
        }

        BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LIMIT_CHARS);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        String detectedMimeType = "application/octet-stream";
        Map<String, Object> extractedMetadata = new HashMap<>();

        try (TikaInputStream tis = TikaInputStream.get(inputStream)) {
            parser.parse(tis, handler, metadata, context);

            detectedMimeType = metadata.get(Metadata.CONTENT_TYPE);
            if (detectedMimeType == null || detectedMimeType.isBlank()) {
                detectedMimeType = "application/octet-stream";
            }

            // Populate metadata map
            for (String name : metadata.names()) {
                extractedMetadata.put(name, metadata.get(name));
            }

            String title = metadata.get(TikaCoreProperties.TITLE);
            String author = metadata.get(TikaCoreProperties.CREATOR);

            Integer pageCount = null;
            String pagesStr = metadata.get("xmpTPg:NPages");
            if (pagesStr == null) {
                pagesStr = metadata.get("Page-Count");
            }
            if (pagesStr != null) {
                try {
                    pageCount = Integer.parseInt(pagesStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            OffsetDateTime creationDate = null;
            Date created = metadata.getDate(TikaCoreProperties.CREATED);
            if (created != null) {
                creationDate = created.toInstant().atOffset(ZoneOffset.UTC);
            }

            String text = handler.toString().trim();
            log.info("Extracted {} characters and {} metadata attributes from [{}]",
                    text.length(), extractedMetadata.size(), originalFilename);

            return ExtractionResult.success(detectedMimeType, text, pageCount, title, author, creationDate, extractedMetadata);
        } catch (Throwable t) {
            // Gracefully catch parser, encryption, and corruption exceptions
            log.warn("Text extraction encountered non-fatal failure for [{}]: {}", originalFilename, t.getMessage());
            return ExtractionResult.failure(detectedMimeType, t.getMessage(), extractedMetadata);
        }
    }
}
