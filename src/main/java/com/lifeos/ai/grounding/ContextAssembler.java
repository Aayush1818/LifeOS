package com.lifeos.ai.grounding;

import com.lifeos.rag.retrieval.dto.RetrievalCitationDto;
import com.lifeos.rag.retrieval.dto.RetrievedChunkDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms authorized Phase 12 retrieval candidates into an isolated,
 * XML-wrapped reference context block while stripping internal database UUIDs.
 */
@Slf4j
@Component
public class ContextAssembler {

    private static final int DEFAULT_MAX_CHUNKS = 5;
    private static final int DEFAULT_MAX_TOTAL_CHARS = 12000;

    /**
     * Assembles retrieval chunks into structured context.
     *
     * @param chunks    retrieved chunks from Phase 12
     * @param citations corresponding provenance citations from Phase 12
     * @return assembled context with lookup maps for citation validation
     */
    public AssembledContext assemble(List<RetrievedChunkDto> chunks, List<RetrievalCitationDto> citations) {
        if (chunks == null || chunks.isEmpty()) {
            return AssembledContext.builder()
                    .formattedContext("")
                    .sourceCount(0)
                    .totalChars(0)
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- REFERENCE SOURCES ---\n");

        Map<Integer, RetrievedChunkDto> sourceMap = new HashMap<>();
        Map<Integer, RetrievalCitationDto> citationMap = new HashMap<>();

        int sourceIndex = 1;
        int totalChars = 0;
        int limit = Math.min(chunks.size(), DEFAULT_MAX_CHUNKS);

        for (int i = 0; i < limit; i++) {
            RetrievedChunkDto chunk = chunks.get(i);
            RetrievalCitationDto citation = (citations != null && i < citations.size()) ? citations.get(i) : null;

            String docTitle = sanitize(chunk.getDocumentTitle() != null ? chunk.getDocumentTitle() : "Document");
            int page = chunk.getPageNumber() != null ? chunk.getPageNumber() : 1;
            int version = chunk.getDocumentVersion() != null ? chunk.getDocumentVersion() : 1;
            String section = chunk.getSectionTitle() != null ? sanitize(chunk.getSectionTitle()) : "General";
            String rawContent = chunk.getContent() != null ? chunk.getContent() : "";

            // Prompt Injection Defense: Escape any closing XML delimiter tags inside untrusted content
            String safeContent = rawContent
                    .replace("</untrusted_document_source>", "[escaped_tag]")
                    .replace("<untrusted_document_source", "[escaped_tag]");

            StringBuilder chunkBlock = new StringBuilder();
            chunkBlock.append(String.format("<untrusted_document_source index=\"%d\" document_title=\"%s\" page=\"%d\" section=\"%s\">\n",
                    sourceIndex, docTitle, page, section));
            chunkBlock.append("Document: ").append(docTitle).append("\n");
            chunkBlock.append("Version: ").append(version).append("\n");
            chunkBlock.append("Page: ").append(page).append("\n");
            chunkBlock.append("Section: ").append(section).append("\n");
            chunkBlock.append("Content:\n").append(safeContent.trim()).append("\n");
            chunkBlock.append("</untrusted_document_source>\n\n");

            String blockStr = chunkBlock.toString();
            if (totalChars + blockStr.length() > DEFAULT_MAX_TOTAL_CHARS && sourceIndex > 1) {
                log.info("Context length reached max character threshold ({} chars). Clamping at {} sources.",
                        DEFAULT_MAX_TOTAL_CHARS, sourceIndex - 1);
                break;
            }

            sb.append(blockStr);
            sourceMap.put(sourceIndex, chunk);
            if (citation != null) {
                citationMap.put(sourceIndex, citation);
            }

            totalChars += blockStr.length();
            sourceIndex++;
        }

        sb.append("--- END REFERENCE SOURCES ---");

        return AssembledContext.builder()
                .formattedContext(sb.toString())
                .sourceMap(sourceMap)
                .citationMap(citationMap)
                .sourceCount(sourceMap.size())
                .totalChars(totalChars)
                .build();
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "'").replace("\n", " ").trim();
    }
}
