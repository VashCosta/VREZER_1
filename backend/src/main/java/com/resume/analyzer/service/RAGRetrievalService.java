package com.resume.analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG Retrieval Service for VREZER.
 * Integrates JobAggregatorService to retrieve structured jobs driven entirely
 * by the candidate's unique profile. All hardcoded placeholder fallbacks are removed.
 * Google snippet search for company recommendations is completely replaced with
 * structured API retrieval.
 */
@Service
public class RAGRetrievalService {

    @Autowired
    private JobAggregatorService jobAggregatorService;

    @org.springframework.beans.factory.annotation.Value("${app.rag.cache-ttl-minutes:10}")
    private long cacheTtlMinutes;

    private final java.util.concurrent.ConcurrentHashMap<String, CachedRagEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class CachedRagEntry {
        final Map<String, Object> context;
        final long expiresAt;
        CachedRagEntry(Map<String, Object> context, long expiresAt) {
            this.context = context;
            this.expiresAt = expiresAt;
        }
    }

    private String buildCacheKey(String careerDomain, List<String> skills, String experienceLevel,
                                  String education, String location, String resumeText) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            String source = String.join("|",
                    String.valueOf(careerDomain).trim().toLowerCase(Locale.ROOT),
                    String.valueOf(skills).trim().toLowerCase(Locale.ROOT),
                    String.valueOf(experienceLevel).trim().toLowerCase(Locale.ROOT),
                    String.valueOf(education).trim().toLowerCase(Locale.ROOT),
                    String.valueOf(location).trim().toLowerCase(Locale.ROOT),
                    String.valueOf(resumeText).trim());
            byte[] digest = md.digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString((String.valueOf(careerDomain) + skills + experienceLevel + resumeText).hashCode());
        }
    }

    public Map<String, Object> retrieveMarketData(
            String careerDomain,
            List<String> skills,
            String experienceLevel,
            String education,
            String location
    ) {
        return retrieveMarketData(careerDomain, skills, experienceLevel, education, location, "", "");
    }

    public Map<String, Object> retrieveMarketData(
            String careerDomain,
            List<String> skills,
            String experienceLevel,
            String education,
            String location,
            String resumeText
    ) {
        return retrieveMarketData(careerDomain, skills, experienceLevel, education, location, resumeText, "");
    }

    /**
     * Master retrieval method. Given a candidate profile and resume text,
     * queries structured job APIs via JobAggregatorService.
     */
    public Map<String, Object> retrieveMarketData(
            String careerDomain,
            List<String> skills,
            String experienceLevel,
            String education,
            String location,
            String resumeText,
            String apiKey
    ) {
        String cacheKey = buildCacheKey(careerDomain, skills, experienceLevel, education, location, resumeText);
        CachedRagEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
            Map<String, Object> cachedContext = new LinkedHashMap<>(cached.context);
            cachedContext.put("cacheStatus", "HIT");
            return cachedContext;
        }

        Map<String, Object> ragContext = new LinkedHashMap<>();
        ragContext.put("retrievalTimestamp", "stable-retrieval-window");
        ragContext.put("candidateDomain", careerDomain);
        ragContext.put("candidateSkills", skills);
        ragContext.put("retrievalStatus", "EXECUTING");

        // Execute dynamic job retrieval & 7-part similarity ranking
        List<Map<String, String>> liveApiJobs = new ArrayList<>();
        if (jobAggregatorService != null) {
            try {
                liveApiJobs = jobAggregatorService.aggregateAndRankJobs(careerDomain, skills, location, resumeText, experienceLevel, apiKey);
            } catch (Exception e) {
                System.err.println("[RAG RETRIEVAL] Job aggregator error: " + e.getMessage());
                System.err.println("[RAG RETRIEVAL] Provider failure isolated: " + e.getMessage());
                liveApiJobs = new ArrayList<>();
            }
        }
        ragContext.put("liveApiJobs", liveApiJobs);

        // Populate retrieved companies strictly from structured live jobs (no snippet extraction)
        List<Map<String, String>> retrievedCompanies = new ArrayList<>();
        for (Map<String, String> job : liveApiJobs) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", job.getOrDefault("name", ""));
            entry.put("title", job.getOrDefault("title", ""));
            entry.put("domain", careerDomain);
            entry.put("requiredSkills", job.getOrDefault("requiredSkills", ""));
            entry.put("location", job.getOrDefault("location", ""));
            entry.put("salary", job.getOrDefault("salary", ""));
            entry.put("url", job.getOrDefault("url", ""));
            entry.put("source", job.getOrDefault("source", "Structured Job API"));
            entry.put("similarityScore", job.getOrDefault("similarityScore", "0.0"));
            if (!entry.get("name").isEmpty()) {
                retrievedCompanies.add(entry);
            }
        }
        ragContext.put("retrievedCompanies", retrievedCompanies);

        int totalRetrieved = liveApiJobs.size();
        ragContext.put("totalRetrievedDocuments", totalRetrieved);
        ragContext.put("retrievalStatus", totalRetrieved > 0 ? "SUCCESS" : "NO_RETRIEVED_JOBS_FOUND");
        ragContext.put("cacheStatus", "MISS");
        cache.put(cacheKey, new CachedRagEntry(
                new LinkedHashMap<>(ragContext),
                System.currentTimeMillis() + Math.max(1L, cacheTtlMinutes) * 60_000L));

        System.out.println("[VREZER RAG] Retrieved " + totalRetrieved + " structured jobs passing similarity threshold for: " + careerDomain);
        return ragContext;
    }

    /**
     * Formats RAG retrieval results into a structured text block for AI prompt injection.
     */
    public String formatRAGContextForPrompt(Map<String, Object> ragContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║  RAG RETRIEVAL CONTEXT — STRUCTURED API MARKET DATA          ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        sb.append("CRITICAL INSTRUCTION: Recommend companies and job roles ONLY from\n");
        sb.append("the retrieved structured API market data below. Do NOT generate companies\n");
        sb.append("from internal knowledge or invent placeholders. If no structured jobs\n");
        sb.append("passed the similarity threshold, report that no matching live postings\n");
        sb.append("met the criteria.\n\n");

        sb.append("▸ Retrieval Timestamp: ").append(ragContext.getOrDefault("retrievalTimestamp", "N/A")).append("\n");
        sb.append("▸ Candidate Domain: ").append(ragContext.getOrDefault("candidateDomain", "")).append("\n");
        sb.append("▸ Total Retrieved Jobs: ").append(ragContext.getOrDefault("totalRetrievedDocuments", 0)).append("\n\n");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> companies = (List<Map<String, String>>) ragContext.getOrDefault("retrievedCompanies", List.of());
        sb.append("── RETRIEVED STRUCTURED JOBS (RANKED BY SIMILARITY SCORE) ──\n");
        if (companies.isEmpty()) {
            sb.append("  [No matching structured jobs meeting similarity threshold retrieved from live sources]\n");
        } else {
            for (int i = 0; i < companies.size(); i++) {
                Map<String, String> c = companies.get(i);
                sb.append("  ").append(i + 1).append(". ")
                  .append(c.getOrDefault("name", ""))
                  .append(" | Title: ").append(c.getOrDefault("title", ""))
                  .append(" | SimilarityScore: ").append(c.getOrDefault("similarityScore", "0.0")).append("%")
                  .append(" | Location: ").append(c.getOrDefault("location", ""))
                  .append(" | Salary: ").append(c.getOrDefault("salary", ""))
                  .append(" | AppLink: ").append(c.getOrDefault("url", ""))
                  .append(" | Source: ").append(c.getOrDefault("source", ""))
                  .append("\n");
            }
        }

        return sb.toString();
    }
}
