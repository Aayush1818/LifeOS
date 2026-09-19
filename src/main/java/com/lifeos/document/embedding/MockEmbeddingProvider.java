package com.lifeos.document.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic pseudo-random embedding provider for local testing and offline CI/CD.
 * Generates unit-normalized 1536-dimensional vectors using SHA-256 seed hashing.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lifeos.rag.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String getModelName() {
        return properties.getModel() != null ? properties.getModel() : "mock-text-embedding-3-small";
    }

    @Override
    public int getDimension() {
        return properties.getDimension() > 0 ? properties.getDimension() : 1536;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        int dim = getDimension();
        List<float[]> result = new ArrayList<>(texts.size());

        for (String text : texts) {
            long seed = computeSeed(text);
            Random random = new Random(seed);
            float[] vector = new float[dim];
            double normSq = 0.0;

            for (int i = 0; i < dim; i++) {
                float val = (float) random.nextGaussian();
                vector[i] = val;
                normSq += val * val;
            }

            // Normalize vector to unit length (L2 norm = 1.0)
            double norm = Math.sqrt(normSq);
            if (norm > 1e-9) {
                for (int i = 0; i < dim; i++) {
                    vector[i] = (float) (vector[i] / norm);
                }
            }

            result.add(vector);
        }

        log.debug("MockEmbeddingProvider generated {} vectors of dimension {}", texts.size(), dim);
        return result;
    }

    private long computeSeed(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((text != null ? text : "").getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }
            return seed;
        } catch (Exception e) {
            return (text != null ? text.hashCode() : 0L);
        }
    }
}
