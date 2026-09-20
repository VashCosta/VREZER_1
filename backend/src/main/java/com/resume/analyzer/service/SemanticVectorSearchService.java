package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.analyzer.model.ResumeEmbedding;
import com.resume.analyzer.repository.ResumeEmbeddingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Phase 5: RAG + Vector Semantic Search Service.
 * Computes semantic matching using vector embeddings fetched from the Gemini embedding API (models/gemini-embedding-001)
 * or the OpenAI embedding API (text-embedding-3-small).
 * Supports optional indexing and retrieval via Qdrant or PostgreSQL pgvector.
 * Fallbacks to in-memory cosine similarity if database connections are unavailable.
 */
@Service
public class SemanticVectorSearchService {

    @Value("${app.vector-store.type:none}")
    private String vectorStoreType;

    @Value("${app.vector-store.qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    @Value("${app.vector-store.qdrant.collection:job_postings}")
    private String qdrantCollection;

    @Value("${app.vector-store.pgvector.url:jdbc:postgresql://localhost:5432/careerforgedb}")
    private String pgvectorUrl;

    @Value("${app.vector-store.pgvector.username:postgres}")
    private String pgvectorUsername;

    @Value("${app.vector-store.pgvector.password:password123}")
    private String pgvectorPassword;

    @Autowired(required = false)
    private EmbeddingCacheService embeddingCacheService;

    @Autowired(required = false)
    private ResumeEmbeddingRepository resumeEmbeddingRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public SemanticVectorSearchService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Overloaded method to calculate vector similarity using TF-IDF fallback when no API key is present.
     */
    public double calculateVectorSimilarity(String resumeText, String jobDescription) {
        return calculateTfIdfSimilarityFallback(resumeText, jobDescription);
    }

    /**
     * Entry point to calculate similarity score (30.0 to 99.0) using Embeddings and cosine similarity.
     */
    public double calculateVectorSimilarity(String resumeText, String jobDescription, String apiKey) {
        if (resumeText == null || resumeText.trim().isEmpty() || jobDescription == null || jobDescription.trim().isEmpty()) {
            return 50.0;
        }

        try {
            double[] resumeVec = getEmbedding(resumeText, apiKey);
            double[] jobVec = getEmbedding(jobDescription, apiKey);

            if (resumeVec != null && jobVec != null) {
                double cosineSim = calculateCosineSimilarity(resumeVec, jobVec);
                // Cosine similarity ranges from -1.0 to 1.0. Scale it to a percentage (30.0 - 99.0)
                double percentage = 30.0 + (cosineSim + 1.0) / 2.0 * 69.0;
                return Math.min(99.0, Math.max(30.0, percentage));
            }
        } catch (Exception e) {
            System.err.println("[SEMANTIC SEARCH] Cosine similarity error: " + e.getMessage());
        }

        // Fallback to TF-IDF local calculation
        return calculateTfIdfSimilarityFallback(resumeText, jobDescription);
    }

    /**
     * Generates vector embeddings via Gemini (text-embedding-004) or OpenAI (text-embedding-3-small).
     * Results are cached by EmbeddingCacheService (Caffeine, 24h TTL) when available.
     */
    public double[] getEmbedding(String text, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty() || text == null || text.trim().isEmpty()) {
            return null;
        }
        // Compute cache key
        String cacheKey = EmbeddingCacheService.sha256(text + "|" + (apiKey != null ? apiKey : ""));
        // Try cache first
        if (embeddingCacheService != null) {
            double[] cached = embeddingCacheService.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        // Compute embedding via appropriate API
        double[] computed;
        if (apiKey.trim().startsWith("sk-")) {
            computed = getOpenAiEmbedding(text, apiKey);
        } else {
            computed = getGeminiEmbedding(text, apiKey);
        }
        // Store in cache if available
        if (embeddingCacheService != null && computed != null) {
            embeddingCacheService.put(cacheKey, computed);
        }
        return computed;
    }

    /**
     * Persist a resume embedding to PostgreSQL (resume_embeddings table via JPA).
     * Uses SHA-256 of text as dedup key — same resume won't be re-indexed.
     *
     * @param resumeText   Raw resume text to embed and persist.
     * @param apiKey       Gemini or OpenAI API key.
     * @param userId       User ID (nullable for anonymous uploads).
     * @param candidateName Candidate name extracted from resume.
     * @param targetRole   Target job role extracted from resume.
     * @param skillsJson   JSON array string of extracted skills.
     * @param atsScore     Computed ATS score (0-100).
     */
    public void persistResumeEmbedding(String resumeText, String apiKey, Long userId,
                                        String candidateName, String targetRole,
                                        String skillsJson, Double atsScore) {
        if (resumeEmbeddingRepository == null || resumeText == null || resumeText.isBlank()) return;
        try {
            String hash = sha256(resumeText);
            if (resumeEmbeddingRepository.existsByResumeHash(hash)) {
                System.out.println("[VECTOR STORE] Resume embedding already indexed (hash=" + hash.substring(0, 12) + "...). Skipping.");
                return;
            }
            double[] vec = getEmbedding(resumeText, apiKey);
            if (vec == null) {
                System.out.println("[VECTOR STORE] Embedding returned null — pgvector persistence skipped.");
                return;
            }
            ResumeEmbedding entity = new ResumeEmbedding();
            entity.setUserId(userId);
            entity.setResumeHash(hash);
            entity.setEmbeddingJson(ResumeEmbedding.serializeEmbedding(vec));
            entity.setResumeSnippet(resumeText.substring(0, Math.min(500, resumeText.length())));
            entity.setCandidateName(candidateName);
            entity.setTargetRole(targetRole);
            entity.setSkillsJson(skillsJson);
            entity.setAtsScore(atsScore);
            resumeEmbeddingRepository.save(entity);
            System.out.println("[VECTOR STORE] Resume embedding persisted to PostgreSQL. dim=" + vec.length
                + " | hash=" + hash.substring(0, 12) + "...");
        } catch (Exception e) {
            System.err.println("[VECTOR STORE] Failed to persist resume embedding: " + e.getMessage());
        }
    }

    private static String sha256(String input) {
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

    private double[] getGeminiEmbedding(String text, String apiKey) {
        try {
            String cleanText = text.substring(0, Math.min(text.length(), 4000)).trim();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + apiKey.trim();

            Map<String, Object> textPart = Map.of("text", cleanText);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> requestBody = Map.of(
                "model", "models/text-embedding-004",
                "content", content
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> embeddingNode = (Map<?, ?>) response.getBody().get("embedding");
                if (embeddingNode != null) {
                    List<?> values = (List<?>) embeddingNode.get("values");
                    if (values != null && !values.isEmpty()) {
                        double[] arr = new double[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = ((Number) values.get(i)).doubleValue();
                        }
                        return arr;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SEMANTIC SEARCH] Failed to get Gemini embedding: " + e.getMessage());
        }
        return null;
    }

    private double[] getOpenAiEmbedding(String text, String apiKey) {
        try {
            String cleanText = text.substring(0, Math.min(text.length(), 4000)).trim();
            String url = "https://api.openai.com/v1/embeddings";
            Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-small",
                "input", cleanText
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> data = (List<?>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) data.get(0);
                    List<?> values = (List<?>) first.get("embedding");
                    double[] arr = new double[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        arr[i] = ((Number) values.get(i)).doubleValue();
                    }
                    return arr;
                }
            }
        } catch (Exception e) {
            System.err.println("[SEMANTIC SEARCH] Failed to get OpenAI embedding: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cosine similarity helper
     */
    public double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Index retrieved jobs and perform Vector Search using Qdrant or pgvector if enabled.
     */
    public List<Map<String, String>> searchSemanticJobs(List<Map<String, String>> rawJobs, String resumeText, String apiKey) {
        if (rawJobs == null || rawJobs.isEmpty() || resumeText == null || resumeText.trim().isEmpty() || apiKey == null) {
            return rawJobs;
        }

        double[] resumeEmb = getEmbedding(resumeText, apiKey);
        if (resumeEmb == null) {
            return rawJobs;
        }

        if ("qdrant".equalsIgnoreCase(vectorStoreType)) {
            return searchQdrant(rawJobs, resumeEmb, apiKey);
        } else if ("pgvector".equalsIgnoreCase(vectorStoreType)) {
            return searchPgVector(rawJobs, resumeEmb, apiKey);
        }

        // Default: compute similarity in-memory
        for (Map<String, String> job : rawJobs) {
            String combinedText = job.getOrDefault("title", "") + " " + job.getOrDefault("description", "") + " " + job.getOrDefault("requiredSkills", "");
            double score = calculateVectorSimilarity(resumeText, combinedText, apiKey);
            job.put("semanticScore", String.format(Locale.US, "%.1f", score));
        }
        return rawJobs;
    }

    private List<Map<String, String>> searchQdrant(List<Map<String, String>> rawJobs, double[] resumeEmb, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String createUrl = qdrantUrl + "/collections/" + qdrantCollection;
            Map<String, Object> createBody = Map.of(
                "vectors", Map.of("size", resumeEmb.length, "distance", "Cosine")
            );
            try {
                restTemplate.exchange(createUrl, HttpMethod.PUT, new HttpEntity<>(createBody, headers), String.class);
            } catch (Exception ignored) {}

            List<Map<String, Object>> points = new ArrayList<>();
            for (int i = 0; i < rawJobs.size(); i++) {
                Map<String, String> job = rawJobs.get(i);
                String combinedText = job.getOrDefault("title", "") + " " + job.getOrDefault("description", "") + " " + job.getOrDefault("requiredSkills", "");
                double[] jobEmb = getEmbedding(combinedText, apiKey);
                if (jobEmb != null) {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("id", i);
                    point.put("vector", jobEmb);
                    point.put("payload", job);
                    points.add(point);
                }
            }

            if (!points.isEmpty()) {
                String upsertUrl = qdrantUrl + "/collections/" + qdrantCollection + "/points";
                Map<String, Object> upsertBody = Map.of("points", points);
                restTemplate.exchange(upsertUrl, HttpMethod.PUT, new HttpEntity<>(upsertBody, headers), String.class);

                String searchUrl = qdrantUrl + "/collections/" + qdrantCollection + "/points/search";
                Map<String, Object> searchBody = Map.of(
                    "vector", resumeEmb,
                    "limit", rawJobs.size(),
                    "with_payload", true
                );
                ResponseEntity<Map> searchResp = restTemplate.postForEntity(searchUrl, new HttpEntity<>(searchBody, headers), Map.class);
                if (searchResp.getStatusCode().is2xxSuccessful() && searchResp.getBody() != null) {
                    List<?> resultList = (List<?>) searchResp.getBody().get("result");
                    List<Map<String, String>> ranked = new ArrayList<>();
                    for (Object res : resultList) {
                        Map<?, ?> node = (Map<?, ?>) res;
                        Map<String, String> payload = (Map<String, String>) node.get("payload");
                        double score = ((Number) node.get("score")).doubleValue();
                        double scaledScore = 30.0 + (score + 1.0) / 2.0 * 69.0;
                        payload.put("semanticScore", String.format(Locale.US, "%.1f", scaledScore));
                        ranked.add(payload);
                    }
                    return ranked;
                }
            }
        } catch (Exception e) {
            System.err.println("[SEMANTIC SEARCH] Qdrant connector warning: " + e.getMessage());
        }
        return rawJobs;
    }

    private List<Map<String, String>> searchPgVector(List<Map<String, String>> rawJobs, double[] resumeEmb, String apiKey) {
        try (Connection conn = DriverManager.getConnection(pgvectorUrl, pgvectorUsername, pgvectorPassword)) {
            try (var s = conn.createStatement()) {
                s.execute("CREATE EXTENSION IF NOT EXISTS vector");
                s.execute("CREATE TABLE IF NOT EXISTS job_embeddings (id VARCHAR(255) PRIMARY KEY, title TEXT, company TEXT, description TEXT, location TEXT, url TEXT, salary TEXT, source TEXT, vector vector(" + resumeEmb.length + "))");
                s.execute("DELETE FROM job_embeddings");
            }

            String insertSql = "INSERT INTO job_embeddings (id, title, company, description, location, url, salary, source, vector) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::vector)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < rawJobs.size(); i++) {
                    Map<String, String> job = rawJobs.get(i);
                    String combinedText = job.getOrDefault("title", "") + " " + job.getOrDefault("description", "") + " " + job.getOrDefault("requiredSkills", "");
                    double[] jobEmb = getEmbedding(combinedText, apiKey);
                    if (jobEmb != null) {
                        ps.setString(1, String.valueOf(i));
                        ps.setString(2, job.getOrDefault("title", ""));
                        ps.setString(3, job.getOrDefault("name", ""));
                        ps.setString(4, job.getOrDefault("description", ""));
                        ps.setString(5, job.getOrDefault("location", ""));
                        ps.setString(6, job.getOrDefault("url", ""));
                        ps.setString(7, job.getOrDefault("salary", ""));
                        ps.setString(8, job.getOrDefault("source", ""));
                        ps.setString(9, Arrays.toString(jobEmb));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            String selectSql = "SELECT title, company, description, location, url, salary, source, (vector <=> ?::vector) as distance FROM job_embeddings ORDER BY distance ASC";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, Arrays.toString(resumeEmb));
                try (ResultSet rs = ps.executeQuery()) {
                    List<Map<String, String>> ranked = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, String> job = new LinkedHashMap<>();
                        job.put("title", rs.getString("title"));
                        job.put("name", rs.getString("company"));
                        job.put("description", rs.getString("description"));
                        job.put("location", rs.getString("location"));
                        job.put("url", rs.getString("url"));
                        job.put("salary", rs.getString("salary"));
                        job.put("source", rs.getString("source"));
                        double distance = rs.getDouble("distance");
                        double cosineSim = 1.0 - distance;
                        double score = 30.0 + (cosineSim + 1.0) / 2.0 * 69.0;
                        job.put("semanticScore", String.format(Locale.US, "%.1f", score));
                        ranked.add(job);
                    }
                    if (!ranked.isEmpty()) {
                        return ranked;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SEMANTIC SEARCH] pgvector connector warning: " + e.getMessage());
        }
        return rawJobs;
    }

    private double calculateTfIdfSimilarityFallback(String resumeText, String jobDescription) {
        Map<String, Integer> resumeFreq = buildTermFrequencyVector(resumeText);
        Map<String, Integer> jobFreq = buildTermFrequencyVector(jobDescription);

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(resumeFreq.keySet());
        allTerms.addAll(jobFreq.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String term : allTerms) {
            int freqA = resumeFreq.getOrDefault(term, 0);
            int freqB = jobFreq.getOrDefault(term, 0);
            dotProduct += freqA * freqB;
            normA += freqA * freqA;
            normB += freqB * freqB;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 50.0;
        }

        double cosineSimilarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        double score = cosineSimilarity * 100.0 * 2.5;
        return Math.min(98.0, Math.max(30.0, score));
    }

    private Map<String, Integer> buildTermFrequencyVector(String text) {
        Map<String, Integer> freq = new HashMap<>();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9+#.\\s]", " ").split("\\s+");
        Set<String> stopWords = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "by", "about",
            "from", "up", "down", "of", "off", "over", "under", "again", "further", "then", "once",
            "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"
        );

        for (String w : words) {
            String term = w.trim();
            if (term.length() >= 2 && !stopWords.contains(term)) {
                freq.put(term, freq.getOrDefault(term, 0) + 1);
            }
        }
        return freq;
    }
}
