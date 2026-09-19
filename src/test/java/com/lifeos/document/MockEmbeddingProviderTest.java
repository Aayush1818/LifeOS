package com.lifeos.document;

import com.lifeos.document.embedding.EmbeddingProperties;
import com.lifeos.document.embedding.MockEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class MockEmbeddingProviderTest {

    private MockEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setDimension(1536);
        properties.setModel("mock-text-embedding-3-small");
        provider = new MockEmbeddingProvider(properties);
    }

    @Test
    @DisplayName("Should generate 1536-dimensional unit-normalized vectors")
    void shouldGenerateNormalizedVectors() {
        String text = "Sample LifeOS policy clause for unit testing.";
        float[] vector = provider.generateEmbedding(text);

        assertThat(vector).hasSize(1536);

        // Verify L2 norm is approximately 1.0
        double normSq = 0.0;
        for (float f : vector) {
            normSq += f * f;
        }
        double norm = Math.sqrt(normSq);
        assertThat(norm).isCloseTo(1.0, offset(0.01));
    }

    @Test
    @DisplayName("Should produce deterministic vectors for identical text")
    void shouldProduceDeterministicVectors() {
        String text = "LifeOS deterministic embedding check.";
        float[] vec1 = provider.generateEmbedding(text);
        float[] vec2 = provider.generateEmbedding(text);

        assertThat(vec1).isEqualTo(vec2);
    }

    @Test
    @DisplayName("Should handle batch generation")
    void shouldHandleBatchGeneration() {
        List<String> texts = List.of(
                "First chunk text for embedding.",
                "Second chunk text for embedding.",
                "Third chunk text for embedding."
        );

        List<float[]> embeddings = provider.generateEmbeddings(texts);
        assertThat(embeddings).hasSize(3);
        for (float[] emb : embeddings) {
            assertThat(emb).hasSize(1536);
        }
    }
}
