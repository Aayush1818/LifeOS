package com.lifeos.rag.retrieval.processor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic query processor for RAG retrieval requests.
 * Normalizes user queries, strips malicious control characters, extracts
 * exact quoted phrases and identifiers for high-precision downstream reranking.
 */
@Component
public class QueryProcessor {

    private static final int MAX_QUERY_LENGTH = 1000;
    private static final Pattern QUOTED_PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b(?=[A-Za-z0-9-_]*[0-9-_])[A-Za-z0-9-_]{4,}\\b");

    /**
     * Encapsulates the output of deterministic query processing.
     */
    public record ProcessedQuery(
            String rawQuery,
            String normalizedQuery,
            List<String> exactPhrases,
            List<String> identifiers
    ) {}

    /**
     * Processes and validates an inbound query string.
     *
     * @param rawQuery inbound raw user query
     * @return structured processed query
     * @throws IllegalArgumentException if query is blank or invalid
     */
    public ProcessedQuery process(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text must not be blank");
        }

        // 1. Strip null bytes and control characters (except standard tabs and newlines)
        String sanitized = rawQuery.replace("\u0000", "")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // 2. Collapse whitespace
        String normalized = sanitized.trim().replaceAll("\\s+", " ");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Query text must not be blank");
        }

        if (normalized.length() > MAX_QUERY_LENGTH) {
            normalized = normalized.substring(0, MAX_QUERY_LENGTH).trim();
        }

        // 3. Extract quoted phrases for exact-phrase match boosting
        List<String> exactPhrases = new ArrayList<>();
        Matcher quoteMatcher = QUOTED_PHRASE_PATTERN.matcher(normalized);
        while (quoteMatcher.find()) {
            String phrase = quoteMatcher.group(1).trim();
            if (!phrase.isEmpty()) {
                exactPhrases.add(phrase.toLowerCase());
            }
        }

        // 4. Extract potential identifiers (e.g., policy numbers, invoice numbers, codes)
        List<String> identifiers = new ArrayList<>();
        Matcher idMatcher = IDENTIFIER_PATTERN.matcher(normalized);
        while (idMatcher.find()) {
            String id = idMatcher.group().trim();
            if (!id.isEmpty() && !identifiers.contains(id.toLowerCase())) {
                identifiers.add(id.toLowerCase());
            }
        }

        return new ProcessedQuery(rawQuery, normalized, exactPhrases, identifiers);
    }
}
