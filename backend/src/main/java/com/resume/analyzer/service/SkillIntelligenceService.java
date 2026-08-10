package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * SkillIntelligenceService — Integrates live external skill taxonomy APIs to
 * enrich the resume analysis with evidence-based, industry-standard skill data.
 *
 *  1. ESCO Skills API (EU Open Skills Taxonomy)
 *     - Finds related skills and broader/narrower competencies
 *     - URL: https://ec.europa.eu/esco/api
 *     - Auth: No auth required for basic lookup
 *
 *  2. O*NET Web Services (US Occupational Data)
 *     - Maps candidate's skills to standardized occupation codes
 *     - URL: https://services.onetcenter.org/ws
 *     - Auth: Free registration at https://services.onetcenter.org/
 *
 *  3. LanguageTool API (Grammar & Writing Quality)
 *     - Analyzes resume text quality, grammar, and readability
 *     - URL: https://api.languagetool.org/v2/check (public, rate-limited)
 *
 * All integrations are OPTIONAL. If any API is unavailable or returns errors,
 * the service degrades gracefully and returns empty enrichment data.
 */
@Service
public class SkillIntelligenceService {

    @Value("${app.esco.api-url:https://ec.europa.eu/esco/api}")
    private String escoApiUrl;

    @Value("${app.esco.enabled:true}")
    private boolean escoEnabled;

    @Value("${app.onet.api-url:https://services.onetcenter.org/ws}")
    private String onetApiUrl;

    @Value("${app.onet.username:}")
    private String onetUsername;

    @Value("${app.onet.enabled:true}")
    private boolean onetEnabled;

    @Value("${app.languagetool.api-url:https://api.languagetool.org/v2/check}")
    private String languageToolUrl;

    @Value("${app.languagetool.enabled:true}")
    private boolean languageToolEnabled;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public SkillIntelligenceService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Master enrichment method. Given candidate's detected skills and career domain,
     * queries ESCO, O*NET and LanguageTool in parallel and returns consolidated enrichment.
     */
    public Map<String, Object> enrichSkillIntelligence(
            List<String> detectedSkills,
            String careerDomain,
            String resumeText,
            String experienceLevel) {

        Map<String, Object> enrichment = new LinkedHashMap<>();
        enrichment.put("enrichmentTimestamp", new Date().toString());
        enrichment.put("inputSkillsCount", detectedSkills.size());
        enrichment.put("careerDomain", careerDomain);

        List<CompletableFuture<?>> futures = new ArrayList<>();

        // ── ESCO Skills Enrichment ──
        CompletableFuture<Map<String, Object>> escoFuture = CompletableFuture.supplyAsync(
            () -> queryEscoSkills(detectedSkills, careerDomain), executor);

        // ── O*NET Occupation Mapping ──
        CompletableFuture<Map<String, Object>> onetFuture = CompletableFuture.supplyAsync(
            () -> queryOnetOccupations(careerDomain, detectedSkills, experienceLevel), executor);

        // ── LanguageTool Grammar Check ──
        CompletableFuture<Map<String, Object>> grammarFuture = CompletableFuture.supplyAsync(
            () -> analyzeGrammarQuality(resumeText), executor);

        futures.add(escoFuture);
        futures.add(onetFuture);
        futures.add(grammarFuture);

        // Wait up to 10 seconds for all enrichments
        try {
            enrichment.put("escoEnrichment", escoFuture.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            System.err.println("[SKILL INTELLIGENCE] ESCO query failed: " + e.getMessage());
            enrichment.put("escoEnrichment", buildFallbackEsco(detectedSkills));
        }

        try {
            enrichment.put("onetOccupations", onetFuture.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            System.err.println("[SKILL INTELLIGENCE] O*NET query failed: " + e.getMessage());
            enrichment.put("onetOccupations", buildFallbackOnet(careerDomain));
        }

        try {
            enrichment.put("grammarAnalysis", grammarFuture.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            System.err.println("[SKILL INTELLIGENCE] LanguageTool query failed: " + e.getMessage());
            enrichment.put("grammarAnalysis", buildFallbackGrammar());
        }

        System.out.println("[SKILL INTELLIGENCE] Enrichment complete for domain: " + careerDomain);
        return enrichment;
    }

    // ── ESCO Skills API ──────────────────────────────────────────────────────

    /**
     * Queries ESCO API to find related skills and broader competencies for
     * each of the candidate's detected skills. This reveals missing but related
     * skills the candidate should develop.
     */
    private Map<String, Object> queryEscoSkills(List<String> detectedSkills, String careerDomain) {
        Map<String, Object> escoData = new LinkedHashMap<>();
        escoData.put("source", "ESCO Skills API v1.1.0 (EU Taxonomy)");
        escoData.put("status", "UNAVAILABLE");

        if (!escoEnabled || detectedSkills.isEmpty()) {
            escoData.put("status", "DISABLED");
            return escoData;
        }

        List<Map<String, Object>> enrichedSkills = new ArrayList<>();
        List<String> relatedSkillsDiscovered = new ArrayList<>();

        // Query top 5 skills to keep within rate limits
        int queryCount = Math.min(5, detectedSkills.size());
        for (int i = 0; i < queryCount; i++) {
            String skill = detectedSkills.get(i);
            try {
                String encoded = URLEncoder.encode(skill, StandardCharsets.UTF_8);
                String url = escoApiUrl + "/search?text=" + encoded
                        + "&type=skill&language=en&limit=5&full=true";

                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    JsonNode root = mapper.readTree(resp.getBody());
                    JsonNode embedded = root.path("_embedded").path("results");

                    Map<String, Object> skillEntry = new LinkedHashMap<>();
                    skillEntry.put("skill", skill);
                    List<String> related = new ArrayList<>();
                    List<String> broaderSkills = new ArrayList<>();

                    if (embedded.isArray()) {
                        for (JsonNode node : embedded) {
                            String title = node.path("title").asText("");
                            String type = node.path("className").asText("");
                            if (!title.isEmpty() && !title.equalsIgnoreCase(skill)) {
                                related.add(title);
                                if (!relatedSkillsDiscovered.contains(title)) {
                                    relatedSkillsDiscovered.add(title);
                                }
                            }
                            // Fetch broader skills from hasbroader links
                            JsonNode broader = node.path("_links").path("hasParent");
                            if (!broader.isMissingNode()) {
                                String broaderTitle = broader.path("title").asText("");
                                if (!broaderTitle.isEmpty()) {
                                    broaderSkills.add(broaderTitle);
                                }
                            }
                        }
                    }
                    skillEntry.put("relatedEscoSkills", related.subList(0, Math.min(3, related.size())));
                    skillEntry.put("broaderCompetencies", broaderSkills);
                    enrichedSkills.add(skillEntry);

                    // Small delay to respect rate limits
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                System.err.println("[ESCO] Skill lookup failed for '" + skill + "': " + e.getMessage());
            }
        }

        // Compute missing skills = related skills NOT already in candidate's skill set
        List<String> missingRelated = new ArrayList<>();
        String lowerSkillsJoined = String.join(" ", detectedSkills).toLowerCase();
        for (String related : relatedSkillsDiscovered) {
            if (!lowerSkillsJoined.contains(related.toLowerCase()) && !missingRelated.contains(related)) {
                missingRelated.add(related);
            }
        }

        escoData.put("status", "SUCCESS");
        escoData.put("enrichedSkills", enrichedSkills);
        escoData.put("escoRelatedSkillsDiscovered", relatedSkillsDiscovered.subList(0, Math.min(10, relatedSkillsDiscovered.size())));
        escoData.put("escoIdentifiedMissingSkills", missingRelated.subList(0, Math.min(8, missingRelated.size())));
        escoData.put("queriedSkillsCount", queryCount);
        escoData.put("taxonomyVersion", "ESCO v1.1.0");

        return escoData;
    }

    // ── O*NET Occupation Mapping API ─────────────────────────────────────────

    /**
     * Queries O*NET Web Services to find matching occupation codes and titles
     * for the candidate's career domain. Returns suitable career paths and
     * occupation-level skill requirements.
     */
    private Map<String, Object> queryOnetOccupations(String careerDomain, List<String> skills, String experienceLevel) {
        Map<String, Object> onetData = new LinkedHashMap<>();
        onetData.put("source", "O*NET Web Services (US Occupational Data)");
        onetData.put("status", "UNAVAILABLE");

        if (!onetEnabled) {
            onetData.put("status", "DISABLED");
            return onetData;
        }

        try {
            String encoded = URLEncoder.encode(careerDomain, StandardCharsets.UTF_8);
            String url = onetApiUrl + "/mnm/search?keyword=" + encoded + "&end=10";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");

            // O*NET allows anonymous access with limited rate — add username if available
            if (onetUsername != null && !onetUsername.isEmpty()) {
                String auth = Base64.getEncoder().encodeToString((onetUsername + ":onet").getBytes());
                headers.set("Authorization", "Basic " + auth);
            }

            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = mapper.readTree(resp.getBody());
                JsonNode occupation = root.path("occupation");

                List<Map<String, Object>> occupations = new ArrayList<>();
                if (occupation.isArray()) {
                    for (JsonNode occ : occupation) {
                        Map<String, Object> occMap = new LinkedHashMap<>();
                        occMap.put("code", occ.path("code").asText(""));
                        occMap.put("title", occ.path("title").asText(""));
                        occMap.put("tags", extractOnetTags(occ));
                        occupations.add(occMap);
                        if (occupations.size() >= 5) break;
                    }
                }

                onetData.put("status", "SUCCESS");
                onetData.put("matchingOccupations", occupations);
                onetData.put("searchKeyword", careerDomain);
                onetData.put("dataSource", "O*NET Online");
            }
        } catch (Exception e) {
            System.err.println("[O*NET] Query failed: " + e.getMessage());
            // Return static O*NET-inspired career paths as fallback
            return buildFallbackOnet(careerDomain);
        }

        return onetData;
    }

    private List<String> extractOnetTags(JsonNode occ) {
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = occ.path("tags");
        if (tagsNode.isObject()) {
            tagsNode.fieldNames().forEachRemaining(field -> {
                if (tagsNode.path(field).asBoolean(false)) {
                    tags.add(field);
                }
            });
        }
        return tags;
    }

    // ── LanguageTool Grammar Analysis ────────────────────────────────────────

    /**
     * Sends a sample of the resume text to LanguageTool to detect grammar,
     * spelling, and style issues that negatively impact resume quality.
     */
    private Map<String, Object> analyzeGrammarQuality(String resumeText) {
        Map<String, Object> grammarData = new LinkedHashMap<>();
        grammarData.put("source", "LanguageTool API");
        grammarData.put("status", "UNAVAILABLE");

        if (!languageToolEnabled || resumeText == null || resumeText.trim().length() < 50) {
            grammarData.put("status", "DISABLED");
            return grammarData;
        }

        try {
            // Analyze first 2000 chars to keep within public API limits
            String sample = resumeText.substring(0, Math.min(2000, resumeText.length()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "text=" + URLEncoder.encode(sample, StandardCharsets.UTF_8)
                    + "&language=en-US"
                    + "&enabledOnly=false";

            ResponseEntity<String> resp = restTemplate.postForEntity(
                languageToolUrl, new HttpEntity<>(body, headers), String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = mapper.readTree(resp.getBody());
                JsonNode matches = root.path("matches");

                List<Map<String, Object>> issues = new ArrayList<>();
                int grammarErrors = 0, spellingErrors = 0, styleErrors = 0;

                if (matches.isArray()) {
                    for (JsonNode match : matches) {
                        String ruleId = match.path("rule").path("id").asText("");
                        String category = match.path("rule").path("category").path("id").asText("");
                        String message = match.path("message").asText("");
                        String context = match.path("context").path("text").asText("");

                        if (category.equalsIgnoreCase("GRAMMAR")) grammarErrors++;
                        else if (category.equalsIgnoreCase("TYPOS")) spellingErrors++;
                        else if (category.equalsIgnoreCase("STYLE")) styleErrors++;

                        if (issues.size() < 8) {
                            Map<String, Object> issue = new LinkedHashMap<>();
                            issue.put("ruleId", ruleId);
                            issue.put("category", category);
                            issue.put("message", message);
                            issue.put("context", context.length() > 80 ? context.substring(0, 80) + "..." : context);

                            // Get suggestions
                            JsonNode replacements = match.path("replacements");
                            List<String> suggestions = new ArrayList<>();
                            if (replacements.isArray()) {
                                for (JsonNode r : replacements) {
                                    suggestions.add(r.path("value").asText(""));
                                    if (suggestions.size() >= 3) break;
                                }
                            }
                            issue.put("suggestions", suggestions);
                            issues.add(issue);
                        }
                    }
                }

                int totalErrors = grammarErrors + spellingErrors + styleErrors;
                int qualityScore = Math.max(60, 100 - (totalErrors * 3));

                grammarData.put("status", "SUCCESS");
                grammarData.put("grammarErrors", grammarErrors);
                grammarData.put("spellingErrors", spellingErrors);
                grammarData.put("styleErrors", styleErrors);
                grammarData.put("totalIssues", totalErrors);
                grammarData.put("writingQualityScore", qualityScore);
                grammarData.put("writingQualityRating",
                    qualityScore >= 90 ? "Excellent" : qualityScore >= 75 ? "Good" : qualityScore >= 60 ? "Average" : "Needs Improvement");
                grammarData.put("issues", issues);
                grammarData.put("analysisNote",
                    totalErrors == 0
                        ? "No grammar or spelling issues detected in the analyzed resume section."
                        : "Found " + totalErrors + " writing issue(s) in the resume. Fixing these will improve ATS readability.");
            }
        } catch (Exception e) {
            System.err.println("[LANGUAGETOOL] Grammar analysis failed: " + e.getMessage());
            return buildFallbackGrammar();
        }

        return grammarData;
    }

    // ── Graceful Fallback Methods ────────────────────────────────────────────

    private Map<String, Object> buildFallbackEsco(List<String> skills) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("source", "ESCO Skills API v1.1.0 (EU Taxonomy)");
        fallback.put("status", "API_UNAVAILABLE");
        fallback.put("note", "ESCO API was not reachable. Skill enrichment is based on resume text analysis only.");
        fallback.put("enrichedSkills", List.of());
        fallback.put("escoRelatedSkillsDiscovered", List.of());
        fallback.put("escoIdentifiedMissingSkills", List.of());
        return fallback;
    }

    private Map<String, Object> buildFallbackOnet(String careerDomain) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("source", "O*NET Web Services (US Occupational Data)");
        fallback.put("status", "API_UNAVAILABLE");
        fallback.put("note", "O*NET API was not reachable. Career path data is based on AI domain inference.");
        fallback.put("matchingOccupations", List.of());
        fallback.put("searchKeyword", careerDomain);
        return fallback;
    }

    private Map<String, Object> buildFallbackGrammar() {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("source", "LanguageTool API");
        fallback.put("status", "API_UNAVAILABLE");
        fallback.put("note", "LanguageTool API was not reachable. Grammar analysis is unavailable.");
        fallback.put("totalIssues", 0);
        fallback.put("writingQualityScore", 0);
        fallback.put("writingQualityRating", "Unable to assess");
        fallback.put("issues", List.of());
        return fallback;
    }

    /**
     * Standalone grammar check for a given text sample.
     * Returns grammar issues as a structured list.
     */
    public Map<String, Object> checkGrammar(String text) {
        return analyzeGrammarQuality(text);
    }
}
