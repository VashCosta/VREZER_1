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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarketIntelligenceService — Live Job Board API Connector & Market Crawler.
 * Connects to live job market APIs:
 *  - Adzuna Job Search API
 *  - Remotive Remote Jobs API
 *  - Greenhouse Board API Search
 *  - Lever Board API Search
 *  - Live Web Search (DuckDuckGo Fallback)
 *
 * Executes requests in parallel with short-term caching to optimize speed
 * and avoid rate limiting.
 */
@Service
public class MarketIntelligenceService {

    @Value("${app.job-api.adzuna.app-id:}")
    private String adzunaAppId;

    @Value("${app.job-api.adzuna.app-key:}")
    private String adzunaAppKey;

    @Value("${app.job-api.jooble.api-key:}")
    private String joobleApiKey;

    @Value("${app.job-api.jsearch.api-key:}")
    private String jsearchApiKey;

    @Value("${app.job-api.google.api-key:}")
    private String googleApiKey;

    @Value("${app.job-api.google.cx:}")
    private String googleCx;


    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    // Simple in-memory search cache with TTL (5 minutes)
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private final Map<String, CacheEntry> searchCache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final List<Map<String, String>> data;
        final long timestamp;

        CacheEntry(List<Map<String, String>> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public MarketIntelligenceService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Master method to fetch live postings from all connected job APIs in parallel.
     */
    public List<Map<String, String>> fetchLiveMarketJobs(String careerDomain, List<String> skills, String location) {
        return fetchLiveMarketJobs(careerDomain, skills, location, "mid-level");
    }

    public List<Map<String, String>> fetchLiveMarketJobs(String careerDomain, List<String> skills, String location, String experienceLevel) {
        String searchKeyword = buildJobKeyword(careerDomain, skills, experienceLevel);
        String skillContext = (skills != null && !skills.isEmpty())
            ? String.join(" ", skills.subList(0, Math.min(3, skills.size())))
            : "";
        int fullSkillsHash = (skills == null || skills.isEmpty()) ? 0 : String.join("_", skills).hashCode();
        String cacheKey = (searchKeyword + "_" + fullSkillsHash + "_" + (location != null ? location : "")).toLowerCase();
        CacheEntry cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            System.out.println("[MARKET INTELLIGENCE] Cache hit: " + cached.data.size() + " postings for keyword: " + searchKeyword);
            return cached.data;
        }

        List<CompletableFuture<List<Map<String, String>>>> futures = new ArrayList<>();

        // 1. Remotive API
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchRemotiveJobs(searchKeyword), "Remotive"), executor));

        // 2. Adzuna API with dynamic country localization
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchAdzunaJobs(searchKeyword, skillContext, location), "Adzuna"), executor));

        // 3. Greenhouse — domain-specific boards
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchGreenhouseJobs(careerDomain, searchKeyword), "Greenhouse"), executor));

        // 4. Lever — domain-specific boards
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchLeverJobs(careerDomain, searchKeyword), "Lever"), executor));

        // 5. Wellfound Startup Jobs Crawler
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchWellfoundJobs(searchKeyword), "Wellfound"), executor));

        // 6. Jooble Job Search API
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchJoobleJobs(searchKeyword, location), "Jooble"), executor));

        // 7. JSearch RapidAPI Connector
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchJSearchJobs(searchKeyword, location), "JSearch"), executor));

        // 8. Google Official Career Discovery (Custom Search API)
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchGoogleJobs(searchKeyword, location), "GoogleDiscovery"), executor));

        // 9. DuckDuckGo Live Search Crawler
        futures.add(CompletableFuture.supplyAsync(() -> fetchWithRetry(() -> fetchDuckDuckGoJobs(searchKeyword, skillContext), "DuckDuckGo"), executor));

        // Use one global market-retrieval deadline. The old implementation waited
        // up to 6 seconds on each future sequentially, so nine providers could
        // stall the resume analysis for roughly 54 seconds even though they were
        // launched in parallel. A single deadline keeps the dashboard responsive.
        List<Map<String, String>> combined = new ArrayList<>();
        CompletableFuture<Void> allProviders = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        try {
            allProviders.get(6500, TimeUnit.MILLISECONDS);
        } catch (Exception timeout) {
            System.err.println("[MARKET INTELLIGENCE] Global provider deadline reached; using completed provider results.");
        }

        for (CompletableFuture<List<Map<String, String>>> future : futures) {
            if (!future.isDone() || future.isCompletedExceptionally() || future.isCancelled()) {
                if (!future.isDone()) future.cancel(true);
                continue;
            }
            try {
                List<Map<String, String>> res = future.getNow(List.of());
                if (res != null) combined.addAll(res);
            } catch (Exception e) {
                System.err.println("[MARKET INTELLIGENCE] Completed provider result unavailable: " + e.getMessage());
            }
        }

        List<Map<String, String>> deduped = deduplicateAndRankJobs(combined, skills, careerDomain);
        if (!deduped.isEmpty()) {
            searchCache.put(cacheKey, new CacheEntry(deduped));
        }

        System.out.println("[MARKET INTELLIGENCE] " + deduped.size() + " unique postings for domain=" + careerDomain + " exp=" + experienceLevel);
        return deduped;
    }

    /**
     * Build a rich, skill-embedded search keyword so different candidate profiles get different job results.
     * A bare "Digital Marketing" query returns the same jobs for everyone; this creates specific queries.
     */
    public String buildJobKeyword(String domain, List<String> skills, String experienceLevel) {
        String base = "Software Engineer";
        if (domain != null && !domain.trim().isEmpty()) {
            String dLow = domain.toLowerCase();
            if (dLow.contains("digital marketing")) {
                base = "Digital Marketing";
            } else if (dLow.contains("backend")) {
                base = "Backend Developer";
            } else if (dLow.contains("frontend")) {
                base = "Frontend Developer";
            } else if (dLow.contains("full-stack") || dLow.contains("full stack")) {
                base = "Full Stack Engineer";
            } else if (dLow.contains("ai") || dLow.contains("machine learning")) {
                base = "Machine Learning Engineer";
            } else if (dLow.contains("mechanical")) {
                base = "Mechanical Engineer";
            } else if (dLow.contains("finance")) {
                base = "Financial Analyst";
            } else {
                base = domain.split("[&|,]")[0].trim();
            }
        }

        String expPrefix = "";
        if (experienceLevel != null) {
            String lowerExp = experienceLevel.toLowerCase();
            if (lowerExp.contains("fresher") || lowerExp.contains("junior") || lowerExp.contains("entry")) {
                expPrefix = "Junior ";
            } else if (lowerExp.contains("senior") || lowerExp.contains("lead")) {
                expPrefix = "Senior ";
            }
        }

        List<String> cleanSkills = new ArrayList<>();
        if (skills != null) {
            Set<String> stops = Set.of(
                "SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS", "PROJECTS", "CERTIFICATIONS", "ACHIEVEMENTS",
                "DECLARATION", "INTERNSHIPS", "PROFILE", "CONTACT", "INTERNSHIP", "PRESENT", "MONTH", "YEAR",
                "NAME", "EMAIL", "PHONE", "LINKEDIN", "GITHUB", "PORTFOLIO", "WORK", "DETAILS", "RESPONSIBILITIES",
                "DESCRIPTION", "DUTIES", "CURRICULUM", "VITAE", "RESUME", "REVIEWS", "DEVELOPER", "ENGINEER",
                "ANALYST", "SPECIALIST", "MANAGER", "LEAD", "SENIOR", "JUNIOR", "EXECUTIVE", "TITLES", "HEADING",
                "SECTION", "PAGE", "ADDRESS", "CITY", "STATE", "COUNTRY", "INDIA", "GLOBAL"
            );
            for (String s : skills) {
                if (s != null && s.trim().length() >= 2) {
                    String clean = s.trim();
                    if (!stops.contains(clean.toUpperCase())) {
                        cleanSkills.add(clean);
                    }
                }
            }
        }

        String skillSuffix = "";
        if (cleanSkills.size() >= 2) {
            skillSuffix = " " + cleanSkills.get(0) + " " + cleanSkills.get(1);
        } else if (cleanSkills.size() == 1) {
            skillSuffix = " " + cleanSkills.get(0);
        }
        return (expPrefix + base + skillSuffix).trim();
    }

    // ── Remotive API ─────────────────────────────────────────────────────────────
    public List<Map<String, String>> fetchRemotiveJobs(String category) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String url = "https://remotive.com/api/remote-jobs?search=" + URLEncoder.encode(category, StandardCharsets.UTF_8) + "&limit=10";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode jobs = root.path("jobs");
                if (jobs.isArray()) {
                    for (JsonNode job : jobs) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("name", job.path("company_name").asText("Remotive Employer"));
                        item.put("title", job.path("title").asText(category + " Specialist"));
                        item.put("location", job.path("candidate_required_location").asText("Remote"));
                        item.put("salary", job.path("salary").asText("Salary not disclosed"));
                        item.put("url", job.path("url").asText("https://remotive.com"));
                        item.put("requiredSkills", job.path("category").asText(category));
                        item.put("source", "Remotive Job API");
                        results.add(item);
                        if (results.size() >= 6) break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] Remotive API fallback: " + e.getMessage());
        }
        return results;
    }

    // ── Adzuna Job Search API ───────────────────────────────────────────────────
    public List<Map<String, String>> fetchAdzunaJobs(String keyword, String skills, String location) {
        List<Map<String, String>> results = new ArrayList<>();
        if (adzunaAppId == null || adzunaAppId.isEmpty() || adzunaAppKey == null || adzunaAppKey.isEmpty()) {
            return results; // Skipped if no key configured
        }
        try {
            String country = resolveAdzunaCountryCode(location);
            String query = URLEncoder.encode(keyword + (skills != null && !skills.isEmpty() ? " " + skills : ""), StandardCharsets.UTF_8);
            String url = "https://api.adzuna.com/v1/api/jobs/" + country + "/search/1?app_id=" + adzunaAppId 
                    + "&app_key=" + adzunaAppKey + "&results_per_page=8&what=" + query;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode resultsNode = root.path("results");
                if (resultsNode.isArray()) {
                    for (JsonNode job : resultsNode) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("name", job.path("company").path("display_name").asText("Adzuna Partner"));
                        item.put("title", job.path("title").asText(keyword));
                        item.put("location", job.path("location").path("display_name").asText(location != null && !location.isEmpty() ? location : "Global / Remote"));
                        double minSal = job.path("salary_min").asDouble(0);
                        double maxSal = job.path("salary_max").asDouble(0);
                        String salText = (minSal > 0 && maxSal > 0) 
                            ? ("in".equals(country) ? String.format("₹%.0f - ₹%.0f INR", minSal, maxSal) : String.format("$%.0f - $%.0f USD", minSal, maxSal))
                            : "Salary not disclosed";
                        item.put("salary", salText);
                        item.put("url", job.path("redirect_url").asText("https://www.adzuna.com"));
                        item.put("requiredSkills", skills != null ? skills : "");
                        item.put("source", "Adzuna Live Job API");
                        results.add(item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] Adzuna API warning: " + e.getMessage());
        }
        return results;
    }

    public List<Map<String, String>> fetchAdzunaJobs(String keyword, String skills) {
        return fetchAdzunaJobs(keyword, skills, "India");
    }

    private String resolveAdzunaCountryCode(String location) {
        if (location == null || location.trim().isEmpty()) return "in";
        String l = location.toLowerCase();
        if (l.contains("india") || l.contains("bengaluru") || l.contains("bangalore") || l.contains("hyderabad") ||
            l.contains("pune") || l.contains("mumbai") || l.contains("delhi") || l.contains("noida") ||
            l.contains("gurgaon") || l.contains("gurugram") || l.contains("chennai") || l.contains("kolkata")) {
            return "in";
        }
        if (l.contains("uk") || l.contains("london") || l.contains("manchester") || l.contains("united kingdom")) return "gb";
        if (l.contains("canada") || l.contains("toronto") || l.contains("vancouver")) return "ca";
        if (l.contains("australia") || l.contains("sydney") || l.contains("melbourne")) return "au";
        if (l.contains("germany") || l.contains("berlin") || l.contains("munich")) return "de";
        if (l.contains("singapore")) return "sg";
        if (l.contains("uae") || l.contains("dubai")) return "ae";
        if (l.contains("us") || l.contains("united states") || l.contains("new york") || l.contains("california") || l.contains("san francisco")) return "us";
        return "in";
    }

    // ── Greenhouse Public Jobs Crawler ─────────────────────────────────────────
    private static final Map<String, String[]> GREENHOUSE_DOMAIN_BOARDS = Map.ofEntries(
        Map.entry("ai",               new String[]{"anthropic", "cohere", "mistral", "huggingface", "openai"}),
        Map.entry("machine learning", new String[]{"anthropic", "cohere", "databricks", "huggingface", "scale"}),
        Map.entry("data science",     new String[]{"databricks", "datadog", "snowflake", "airbnb", "lyft"}),
        Map.entry("devops",           new String[]{"hashicorp", "cloudflare", "datadog", "fastly", "pagerduty"}),
        Map.entry("cloud",            new String[]{"hashicorp", "cloudflare", "fastly", "datadog", "stripe"}),
        Map.entry("cybersecurity",    new String[]{"crowdstrike", "cloudflare", "pagerduty", "hashicorp", "snyk"}),
        Map.entry("fintech",          new String[]{"stripe", "plaid", "robinhood", "coinbase", "brex"}),
        Map.entry("finance",          new String[]{"stripe", "plaid", "robinhood", "coinbase", "brex"}),
        Map.entry("marketing",        new String[]{"hubspot", "sprinklr", "yotpo", "attentive", "klaviyo"}),
        Map.entry("sales",            new String[]{"hubspot", "outreach", "gong", "salesloft", "drift"}),
        Map.entry("design",           new String[]{"figma", "notion", "canva", "miro", "loom"}),
        Map.entry("ui",               new String[]{"figma", "notion", "canva", "miro", "loom"}),
        Map.entry("ux",               new String[]{"figma", "notion", "canva", "miro", "loom"}),
        Map.entry("healthtech",       new String[]{"tempus", "doordash", "hims", "ro", "nuvation"}),
        Map.entry("edtech",           new String[]{"coursera", "duolingo", "chegg", "brainly", "kahoot"}),
        Map.entry("ecommerce",        new String[]{"shopify", "affirm", "bolt", "yotpo", "klaviyo"}),
        Map.entry("full stack",       new String[]{"stripe", "notion", "linear", "vercel", "supabase"}),
        Map.entry("web",              new String[]{"vercel", "netlify", "linear", "notion", "stripe"})
    );

    private String[] getGreenhouseBoards(String domain) {
        if (domain == null) return new String[]{"stripe", "airbnb", "datadog", "notion", "linear"};
        String d = domain.toLowerCase();
        for (Map.Entry<String, String[]> entry : GREENHOUSE_DOMAIN_BOARDS.entrySet()) {
            if (d.contains(entry.getKey())) return entry.getValue();
        }
        // Default tech boards
        return new String[]{"stripe", "airbnb", "datadog", "notion", "linear"};
    }

    public List<Map<String, String>> fetchGreenhouseJobs(String keyword) {
        return fetchGreenhouseJobs(null, keyword);
    }

    public List<Map<String, String>> fetchGreenhouseJobs(String domain, String keyword) {
        List<Map<String, String>> results = new ArrayList<>();
        String[] boards = getGreenhouseBoards(domain);
        for (String board : boards) {
            try {
                String url = "https://boards-api.greenhouse.io/v1/boards/" + board + "/jobs";
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = mapper.readTree(response.getBody());
                    JsonNode jobsNode = root.path("jobs");
                    if (jobsNode.isArray()) {
                        for (JsonNode job : jobsNode) {
                            String title = job.path("title").asText("");
                            // Accept if title matches keyword or we need minimum results
                            if (!title.isEmpty() && (title.toLowerCase().contains(keyword.split(" ")[0].toLowerCase()) || results.size() < 2)) {
                                Map<String, String> item = new LinkedHashMap<>();
                                String companyCapitalized = Character.toUpperCase(board.charAt(0)) + board.substring(1);
                                item.put("name", companyCapitalized);
                                item.put("title", title);
                                item.put("location", job.path("location").path("name").asText("Global Remote"));
                                item.put("salary", "Industry Benchmark");
                                item.put("url", job.path("absolute_url").asText("https://boards.greenhouse.io/" + board));
                                item.put("requiredSkills", keyword);
                                item.put("source", "Greenhouse Job Board API");
                                results.add(item);
                                if (results.size() >= 4) break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore silent board timeouts
            }
            if (results.size() >= 5) break;
        }
        return results;
    }

    // ── Lever Public Postings Search ───────────────────────────────────────────
    // Fix 2: Domain-specific Lever boards
    private static final Map<String, String[]> LEVER_DOMAIN_BOARDS = Map.ofEntries(
        Map.entry("ai",               new String[]{"openai", "scale", "cohere", "aisera", "cognitivescale"}),
        Map.entry("machine learning", new String[]{"openai", "scale", "cohere", "aisera", "cognitivescale"}),
        Map.entry("devops",           new String[]{"cloudflare", "grafana", "scout24", "contentsquare", "couchbase"}),
        Map.entry("cloud",            new String[]{"cloudflare", "grafana", "couchbase", "scout24", "cribl"}),
        Map.entry("cybersecurity",    new String[]{"cloudflare", "orca", "snyk", "armis", "lacework"}),
        Map.entry("fintech",          new String[]{"brex", "mercury", "ramp", "divvy", "puzzle"}),
        Map.entry("finance",          new String[]{"brex", "mercury", "ramp", "divvy", "puzzle"}),
        Map.entry("marketing",        new String[]{"iterable", "braze", "sprinklr", "contentful", "seismic"}),
        Map.entry("design",           new String[]{"figma", "miro", "notionhq", "loom", "webflow"}),
        Map.entry("healthtech",       new String[]{"tempus", "cityblock", "hims", "ro", "zocdoc"}),
        Map.entry("edtech",           new String[]{"duolingo", "coursera", "chegg", "noodle", "brainly"}),
        Map.entry("ecommerce",        new String[]{"shopify", "bolt", "checkout", "recharge", "affirm"}),
        Map.entry("full stack",       new String[]{"linear", "vercel", "supabase", "retool", "airtable"}),
        Map.entry("sales",            new String[]{"gong", "salesloft", "outreach", "apollo", "chorus"})
    );

    private String[] getLeverBoards(String domain) {
        if (domain == null) return new String[]{"netflix", "spotify", "palantir", "cloudflare"};
        String d = domain.toLowerCase();
        for (Map.Entry<String, String[]> entry : LEVER_DOMAIN_BOARDS.entrySet()) {
            if (d.contains(entry.getKey())) return entry.getValue();
        }
        return new String[]{"netflix", "spotify", "atlassian", "cloudflare"};
    }

    public List<Map<String, String>> fetchLeverJobs(String keyword) {
        return fetchLeverJobs(null, keyword);
    }

    public List<Map<String, String>> fetchLeverJobs(String domain, String keyword) {
        List<Map<String, String>> results = new ArrayList<>();
        String[] companies = getLeverBoards(domain);
        for (String company : companies) {
            try {
                String url = "https://api.lever.co/v0/postings/" + company + "?mode=json";
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode array = mapper.readTree(response.getBody());
                    if (array.isArray()) {
                        for (JsonNode posting : array) {
                            String text = posting.path("text").asText("");
                            if (!text.isEmpty() && (text.toLowerCase().contains(keyword.split(" ")[0].toLowerCase()) || results.size() < 2)) {
                                Map<String, String> item = new LinkedHashMap<>();
                                String cName = Character.toUpperCase(company.charAt(0)) + company.substring(1);
                                item.put("name", cName);
                                item.put("title", text);
                                item.put("location", posting.path("categories").path("location").asText("Hybrid / Remote"));
                                item.put("salary", "Salary not disclosed");
                                item.put("url", posting.path("hostedUrl").asText("https://jobs.lever.co/" + company));
                                item.put("requiredSkills", keyword);
                                item.put("source", "Lever Job Board API");
                                results.add(item);
                                if (results.size() >= 4) break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore silent Lever timeouts
            }
            if (results.size() >= 4) break;
        }
        return results;
    }

    // ── DuckDuckGo Search Crawler Fallback ────────────────────────────────────
    public List<Map<String, String>> fetchDuckDuckGoJobs(String keyword, String skills) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String query = keyword + " " + skills + " top companies hiring jobs 2026";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            ResponseEntity<String> response = restTemplate.exchange(
                searchUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String html = response.getBody();
                Pattern snippetPattern = Pattern.compile("class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL);
                Matcher matcher = snippetPattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 5) {
                    String snippet = matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&").trim();
                    if (snippet.length() > 25) {
                        String companyName = extractCompanyNameFromSnippet(snippet);
                        if (!companyName.isEmpty()) {
                            Map<String, String> item = new LinkedHashMap<>();
                            item.put("name", companyName);
                            item.put("title", keyword + " Role");
                            item.put("location", "India / Global Remote");
                            item.put("salary", "Market Competitive");
                            item.put("url", "https://duckduckgo.com/?q=" + encodedQuery);
                            item.put("requiredSkills", skills);
                            item.put("source", "Live Web Search");
                            results.add(item);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] DDG search warning: " + e.getMessage());
        }
        return results;
    }

    // ── Wellfound Startup Search Crawler ──────────────────────────────────────
    public List<Map<String, String>> fetchWellfoundJobs(String keyword) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String query = "site:wellfound.com/jobs " + keyword;
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            ResponseEntity<String> response = restTemplate.exchange(
                searchUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String html = response.getBody();
                Pattern snippetPattern = Pattern.compile("class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL);
                Matcher matcher = snippetPattern.matcher(html);
                
                Pattern titlePattern = Pattern.compile("class=\"result__a\"[^>]*>(.*?)</a>", Pattern.DOTALL);
                Matcher titleMatcher = titlePattern.matcher(html);

                int count = 0;
                while (matcher.find() && titleMatcher.find() && count < 5) {
                    String snippet = matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&").trim();
                    String titleText = titleMatcher.group(1).replaceAll("<[^>]*>", "").trim();
                    
                    if (titleText.toLowerCase().contains("jobs") || titleText.toLowerCase().contains("hiring")) {
                        String companyName = titleText.split("[-|:]")[0].replace("Jobs at", "").replace("Jobs", "").replace("Hiring", "").trim();
                        if (companyName.length() >= 2) {
                            Map<String, String> item = new LinkedHashMap<>();
                            item.put("name", companyName);
                            item.put("title", keyword + " Specialist");
                            item.put("location", "Remote / Hybrid");
                            item.put("salary", "Equity & Competitive Market Rate");
                            item.put("url", "https://wellfound.com");
                            item.put("requiredSkills", keyword);
                            item.put("source", "Wellfound Startup Jobs");
                            results.add(item);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] Wellfound crawl warning: " + e.getMessage());
        }
        return results;
    }

    private String extractCompanyNameFromSnippet(String snippet) {
        Pattern p = Pattern.compile("\\b([A-Z][a-zA-Z0-9]+(?:\\s+[A-Z][a-zA-Z0-9]+){0,2})\\b");
        Matcher m = p.matcher(snippet);
        while (m.find()) {
            String word = m.group(1).trim();
            if (word.length() >= 3 && !isStopWord(word)) {
                return word;
            }
        }
        return "";
    }

    private boolean isStopWord(String word) {
        Set<String> stops = Set.of("The", "This", "That", "These", "Those", "Hiring", "Companies", "Jobs",
                "Top", "Best", "Remote", "India", "Global", "Salary", "Career", "Skills", "Engineer", "Developer");
        return stops.contains(word);
    }

    public List<Map<String, String>> fetchJoobleJobs(String keyword, String location) {
        List<Map<String, String>> results = new ArrayList<>();
        if (joobleApiKey == null || joobleApiKey.trim().isEmpty()) return results;
        try {
            String url = "https://jooble.org/api/" + joobleApiKey.trim();
            Map<String, String> requestBody = Map.of("keywords", keyword, "location", location != null ? location : "");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, req, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode jobs = root.path("jobs");
                if (jobs.isArray()) {
                    for (JsonNode job : jobs) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("name", job.path("company").asText("Jooble Partner"));
                        item.put("title", job.path("title").asText(keyword));
                        item.put("location", job.path("location").asText("India / Global"));
                        item.put("salary", job.path("salary").asText("Market Benchmark"));
                        item.put("url", job.path("link").asText("https://jooble.org"));
                        item.put("requiredSkills", keyword);
                        item.put("source", "Jooble Live Job API");
                        results.add(item);
                        if (results.size() >= 5) break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] Jooble API warning: " + e.getMessage());
        }
        return results;
    }

    public List<Map<String, String>> fetchJSearchJobs(String keyword, String location) {
        List<Map<String, String>> results = new ArrayList<>();
        if (jsearchApiKey == null || jsearchApiKey.trim().isEmpty()) return results;
        try {
            String query = URLEncoder.encode(keyword + " " + (location != null ? location : ""), StandardCharsets.UTF_8);
            String url = "https://jsearch.p.rapidapi.com/search?query=" + query + "&page=1&num_pages=1";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", jsearchApiKey.trim());
            headers.set("X-RapidAPI-Host", "jsearch.p.rapidapi.com");
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    for (JsonNode job : data) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("name", job.path("employer_name").asText("Global Tech Employer"));
                        item.put("title", job.path("job_title").asText(keyword));
                        item.put("location", job.path("job_city").asText("Remote") + ", " + job.path("job_country").asText());
                        item.put("salary", job.path("job_min_salary").asText("Competitive Market Salary"));
                        item.put("url", job.path("job_apply_link").asText("https://jsearch.p.rapidapi.com"));
                        item.put("requiredSkills", keyword);
                        item.put("source", "JSearch RapidAPI");
                        results.add(item);
                        if (results.size() >= 5) break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] JSearch API warning: " + e.getMessage());
        }
        return results;
    }

    // ── Google Official Career Discovery (Custom Search API) ────────────────────
    public List<Map<String, String>> fetchGoogleJobs(String keyword, String location) {
        List<Map<String, String>> results = new ArrayList<>();
        if (googleApiKey == null || googleApiKey.trim().isEmpty() || googleCx == null || googleCx.trim().isEmpty()) {
            return results;
        }
        try {
            String query = keyword + " careers hiring " + (location != null && !location.isEmpty() ? location : "India");
            String url = "https://www.googleapis.com/customsearch/v1?key=" + googleApiKey.trim()
                    + "&cx=" + googleCx.trim()
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&num=6";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode items = root.path("items");
                if (items.isArray()) {
                    for (JsonNode itemNode : items) {
                        String title = itemNode.path("title").asText("");
                        String snippet = itemNode.path("snippet").asText("");
                        String link = itemNode.path("link").asText("");
                        String displayLink = itemNode.path("displayLink").asText("");

                        String companyName = displayLink.replace("www.", "").replace(".com", "").replace(".io", "").replace(".careers", "");
                        if (!companyName.isEmpty()) {
                            companyName = Character.toUpperCase(companyName.charAt(0)) + companyName.substring(1);
                        } else {
                            companyName = "Verified Employer";
                        }

                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("name", companyName);
                        item.put("title", title.isEmpty() ? keyword : title);
                        item.put("location", location != null && !location.isEmpty() ? location : "India / Remote");
                        item.put("salary", "Competitive Market Pay");
                        item.put("url", link);
                        item.put("description", snippet);
                        item.put("requiredSkills", keyword);
                        item.put("source", "Google Official Career Discovery");
                        results.add(item);
                        if (results.size() >= 5) break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MARKET INTELLIGENCE] Google Custom Search discovery notice: " + e.getMessage());
        }
        return results;
    }

    private List<Map<String, String>> fetchWithRetry(java.util.function.Supplier<List<Map<String, String>>> supplier, String providerName) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                List<Map<String, String>> res = supplier.get();
                if (res != null && !res.isEmpty()) {
                    return res;
                }
            } catch (Exception e) {
                if (attempt == 2) {
                    System.err.println("[PROVIDER RETRY] " + providerName + " failed after 2 attempts: " + e.getMessage());
                }
            }
        }
        return List.of();
    }

    public int calculateEvidenceBasedMatchScore(Map<String, String> job, List<String> candidateSkills, String careerDomain, String experienceLevel, String candidateLocation) {
        if (job == null) return 60;
        
        String title = job.getOrDefault("title", "").toLowerCase();
        String reqSkillsStr = job.getOrDefault("requiredSkills", "").toLowerCase();
        String descStr = job.getOrDefault("description", "").toLowerCase();
        String locStr = job.getOrDefault("location", "").toLowerCase();
        String combinedJD = title + " " + reqSkillsStr + " " + descStr;

        // 1. Skill Intersection Score (0 to 50 pts)
        int skillScore = 0;
        if (candidateSkills != null && !candidateSkills.isEmpty()) {
            int matchedCount = 0;
            for (String skill : candidateSkills) {
                if (skill != null && skill.trim().length() >= 2) {
                    if (combinedJD.contains(skill.trim().toLowerCase())) {
                        matchedCount++;
                    }
                }
            }
            double ratio = (double) matchedCount / Math.max(1, Math.min(8, candidateSkills.size()));
            skillScore = (int) Math.round(ratio * 50.0);
        } else {
            skillScore = 30;
        }

        // 2. Role & Domain Relevance Score (0 to 30 pts)
        int domainScore = 15;
        if (careerDomain != null && !careerDomain.trim().isEmpty()) {
            String[] domainWords = careerDomain.toLowerCase().split("[\\s&,]+");
            int wordMatches = 0;
            for (String dw : domainWords) {
                if (dw.length() >= 3 && title.contains(dw)) {
                    wordMatches++;
                }
            }
            if (wordMatches >= 2) domainScore = 30;
            else if (wordMatches == 1) domainScore = 22;
            else domainScore = 12;
        }

        // 3. Experience Level Alignment Score (0 to 10 pts)
        int expScore = 8;
        if (experienceLevel != null) {
            String expLower = experienceLevel.toLowerCase();
            if (expLower.contains("fresher") && (title.contains("junior") || title.contains("associate") || title.contains("trainee") || title.contains("entry"))) {
                expScore = 10;
            } else if (expLower.contains("senior") && (title.contains("senior") || title.contains("lead") || title.contains("principal"))) {
                expScore = 10;
            }
        }

        // 4. Location Proximity & Indian Hub Score (0 to 10 pts)
        int locScore = 5;
        boolean isIndianCity = locStr.contains("chennai") || locStr.contains("bengaluru") || locStr.contains("bangalore") ||
                locStr.contains("hyderabad") || locStr.contains("pune") || locStr.contains("mumbai") ||
                locStr.contains("delhi") || locStr.contains("gurugram") || locStr.contains("noida") ||
                locStr.contains("kochi") || locStr.contains("coimbatore") || locStr.contains("india");
        boolean isTopIntl = locStr.contains("usa") || locStr.contains("uk") || locStr.contains("united kingdom") ||
                locStr.contains("canada") || locStr.contains("europe") || locStr.contains("uae") ||
                locStr.contains("dubai") || locStr.contains("singapore") || locStr.contains("australia");
        if (isIndianCity || isTopIntl) {
            locScore = 10;
        }

        int total = skillScore + domainScore + expScore + locScore;
        return Math.max(58, Math.min(97, total));
    }

    private List<Map<String, String>> deduplicateAndRankJobs(List<Map<String, String>> rawList, List<String> candidateSkills, String careerDomain) {
        Map<String, Map<String, String>> deduped = new LinkedHashMap<>();
        String domainLower = (careerDomain != null) ? careerDomain.toLowerCase() : "";
        List<String> cleanSkills = (candidateSkills != null) ? candidateSkills : List.of();

        for (Map<String, String> job : rawList) {
            String title = job.getOrDefault("title", "").toLowerCase();
            String name = job.getOrDefault("name", "").toLowerCase();
            String key = (name + " - " + title).trim();

            if (key.isEmpty() || deduped.containsKey(key)) {
                continue;
            }

            if (isDomainMismatch(title, domainLower)) {
                continue;
            }

            // Location localization check: India vs Global
            String loc = job.getOrDefault("location", "India / Remote").toLowerCase();
            boolean isIndia = loc.contains("chennai") || loc.contains("bengaluru") || loc.contains("bangalore") ||
                    loc.contains("hyderabad") || loc.contains("pune") || loc.contains("mumbai") ||
                    loc.contains("delhi") || loc.contains("gurugram") || loc.contains("noida") ||
                    loc.contains("kochi") || loc.contains("coimbatore") || loc.contains("india");

            job.put("isIndia", String.valueOf(isIndia));
            job.put("scope", isIndia ? "India" : "Global");
            job.put("postedDate", job.getOrDefault("postedDate", "Active 2026 Opening"));

            // Calculate evidence-based match score
            int matchScore = calculateEvidenceBasedMatchScore(job, cleanSkills, careerDomain, "mid-level", "India");
            job.put("matchScore", String.valueOf(matchScore));
            job.put("similarityScore", String.valueOf(matchScore));

            // Calculate matched & missing skills
            String combinedText = (title + " " + job.getOrDefault("requiredSkills", "") + " " + job.getOrDefault("description", "")).toLowerCase();
            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();

            for (String s : cleanSkills) {
                if (s != null && !s.trim().isEmpty()) {
                    if (combinedText.contains(s.trim().toLowerCase())) {
                        matched.add(s.trim());
                    } else if (missing.size() < 3) {
                        missing.add(s.trim());
                    }
                }
            }

            if (matched.isEmpty() && !cleanSkills.isEmpty()) {
                matched.addAll(cleanSkills.subList(0, Math.min(3, cleanSkills.size())));
            }

            job.put("matchedSkills", String.join(", ", matched.subList(0, Math.min(4, matched.size()))));
            job.put("missingSkills", missing.isEmpty() ? "None Critical" : String.join(", ", missing));

            deduped.put(key, job);
        }

        // Rank by calculated match score descending, placing India jobs first on ties
        List<Map<String, String>> resultList = new ArrayList<>(deduped.values());
        resultList.sort((j1, j2) -> {
            int score1 = 70;
            int score2 = 70;
            try { score1 = Integer.parseInt(j1.getOrDefault("matchScore", "70")); } catch (Exception ignored) {}
            try { score2 = Integer.parseInt(j2.getOrDefault("matchScore", "70")); } catch (Exception ignored) {}

            boolean in1 = "true".equalsIgnoreCase(j1.get("isIndia"));
            boolean in2 = "true".equalsIgnoreCase(j2.get("isIndia"));

            if (in1 != in2) {
                return in1 ? -1 : 1;
            }
            return Integer.compare(score2, score1);
        });

        return resultList;
    }

    private boolean isDomainMismatch(String jobTitle, String domainLower) {
        if (domainLower == null || domainLower.trim().isEmpty() || jobTitle == null || jobTitle.trim().isEmpty()) return false;
        String titleLower = jobTitle.toLowerCase();

        boolean isGenericSalesOrSupport = titleLower.contains("inside sales") || titleLower.contains("sales jedi") || titleLower.contains("sales contractor") || titleLower.contains("service desk") || titleLower.contains("helpdesk") || titleLower.contains("freelance writer") || titleLower.contains("customer service") || titleLower.contains("call center") || titleLower.contains("support jedi") || titleLower.contains("bilingual implementation");
        boolean isCandidateSalesOrSupport = domainLower.contains("sales") || domainLower.contains("customer support") || domainLower.contains("writing");

        if (isGenericSalesOrSupport && !isCandidateSalesOrSupport) {
            return true;
        }

        boolean isTechJob = titleLower.contains("developer") || titleLower.contains("engineer") || titleLower.contains("sde") || titleLower.contains("architect") || titleLower.contains("backend") || titleLower.contains("frontend") || titleLower.contains("full stack") || titleLower.contains("devops");
        boolean isMarketingJob = titleLower.contains("seo") || titleLower.contains("sem") || titleLower.contains("marketing") || titleLower.contains("growth") || titleLower.contains("ads") || titleLower.contains("media");
        boolean isDesignJob = titleLower.contains("ui/ux") || titleLower.contains("ux designer") || titleLower.contains("ui designer") || titleLower.contains("figma");
        boolean isMechanicalJob = titleLower.contains("mechanical") || titleLower.contains("cad") || titleLower.contains("solidworks") || titleLower.contains("catia");
        boolean isFinanceJob = titleLower.contains("accountant") || titleLower.contains("auditor") || titleLower.contains("tax") || titleLower.contains("financial analyst");

        boolean isTechCandidate = domainLower.contains("backend") || domainLower.contains("frontend") || domainLower.contains("software") || domainLower.contains("full-stack") || domainLower.contains("cloud") || domainLower.contains("devops") || domainLower.contains("ai") || domainLower.contains("machine learning");
        boolean isMarketingCandidate = domainLower.contains("marketing") || domainLower.contains("seo") || domainLower.contains("sem") || domainLower.contains("growth");
        boolean isDesignCandidate = domainLower.contains("design") || domainLower.contains("ui/ux");
        boolean isMechanicalCandidate = domainLower.contains("mechanical") || domainLower.contains("automotive") || domainLower.contains("cad");
        boolean isFinanceCandidate = domainLower.contains("finance") || domainLower.contains("accounting") || domainLower.contains("audit") || domainLower.contains("commerce");

        // If candidate matches both Tech and Marketing (dual domain), allow both Tech and Marketing jobs!
        if (isTechCandidate && isMarketingCandidate) {
            if (isTechJob || isMarketingJob) return false;
            return true;
        }

        if (isMarketingCandidate && !isTechCandidate && (isTechJob || isMechanicalJob || isFinanceJob)) return true;
        if (isTechCandidate && !isMarketingCandidate && (isMarketingJob || isMechanicalJob || isFinanceJob)) return true;
        if (isDesignCandidate && (isTechJob || isMechanicalJob || isFinanceJob)) return true;
        if (isMechanicalCandidate && (isTechJob || isMarketingJob || isFinanceJob)) return true;
        if (isFinanceCandidate && (isTechJob || isMarketingJob || isMechanicalJob)) return true;

        return false;
    }
}

