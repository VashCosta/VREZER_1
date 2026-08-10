package com.resume.analyzer.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * Simple Caffeine-backed in‑memory cache for embedding vectors.
 *
 * It stores a double[] vector keyed by a SHA‑256 hash of the text+apiKey.
 * No direct dependency on SemanticVectorSearchService – the service that
 * computes embeddings will handle cache lookup/insert.
 */
@Service
public class EmbeddingCacheService {

    private final Cache<String, double[]> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats()
            .build();

    /** Retrieve a cached embedding if present. */
    public double[] get(String key) {
        return cache.getIfPresent(key);
    }

    /** Store an embedding in the cache. */
    public void put(String key, double[] value) {
        cache.put(key, value);
    }

    /** Human‑readable stats for debugging. */
    public String getStats() {
        var s = cache.stats();
        return String.format(
                "EmbeddingCache[size=%d, hitRate=%.1f%%, missRate=%.1f%%, evictions=%d]",
                cache.estimatedSize(), s.hitRate() * 100, s.missRate() * 100, s.evictionCount());
    }

    /** Utility to produce the same cache key used elsewhere. */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
