package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import com.lifeos.rag.retrieval.dto.RetrievalRequest;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Tool allowing the agent to perform targeted semantic/hybrid document search.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSearchTool implements LifeOSTool {

    private final RetrievalService retrievalService;

    @Override
    public String getName() {
        return "search_documents";
    }

    @Override
    public String getDescription() {
        return "Performs hybrid semantic and keyword search across stored PDF/Word/Text documents for specific clauses, policies, or receipts.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("query")
                        .type("string")
                        .description("Search query terms or questions to find in documents")
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("topK")
                        .type("integer")
                        .description("Number of chunks to retrieve (default 5)")
                        .required(false)
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.debug("Executing DocumentSearchTool for user [{}]", userId);
        if (parameters == null || !parameters.containsKey("query") || parameters.get("query") == null) {
            return ToolExecutionResult.failure("Parameter 'query' is required");
        }

        String query = parameters.get("query").toString().trim();
        int topK = 5;
        if (parameters.containsKey("topK") && parameters.get("topK") != null) {
            try {
                topK = Integer.parseInt(parameters.get("topK").toString());
            } catch (NumberFormatException ignored) {}
        }

        try {
            RetrievalRequest req = RetrievalRequest.builder()
                    .query(query)
                    .mode(RetrievalMode.HYBRID)
                    .topK(topK)
                    .build();

            RetrievalResponse resp = retrievalService.retrieve(userId, req);
            return ToolExecutionResult.success(resp.getResults());
        } catch (Exception e) {
            log.error("Document search tool failed for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to search documents: " + e.getMessage());
        }
    }
}
