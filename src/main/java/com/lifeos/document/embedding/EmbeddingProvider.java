package com.lifeos.document.embedding;

import java.util.List;

/**
 * Pluggable service provider interface for vector embedding generation.
 * Decouples domain logic from AI providers (OpenAI, Ollama, HuggingFace, Mock).
 */
public interface EmbeddingProvider {

    /**
     * Returns the name of the provider (e.g., "OPENAI", "OLLAMA", "MOCK").
     */
    String getProviderName();

    /**
     * Returns the specific model identifier (e.g., "text-embedding-3-small").
     */
    String getModelName();

    /**
     * Returns the vector dimension produced by this model (e.g., 1536).
     */
    int getDimension();

    /**
     * Generates dense vector embeddings for a list of texts.
     *
     * @param texts list of input text strings to embed
     * @return list of float arrays, each of length {@link #getDimension()}
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Generates a single vector embedding.
     */
    default float[] generateEmbedding(String text) {
        return generateEmbeddings(List.of(text)).getFirst();
    }
}
