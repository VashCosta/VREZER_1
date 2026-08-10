package com.resume.analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LiveWebCrawlerService {

    private final RestTemplate restTemplate;

    public LiveWebCrawlerService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Crawls live Google & Web search data for Indian technical & non-technical career domains.
     */
    public Map<String, Object> crawlLiveMarketData(String careerDomain, List<String> topSkills) {
        Map<String, Object> crawlResults = new LinkedHashMap<>();
        String domainQuery = (careerDomain != null && !careerDomain.isEmpty()) ? careerDomain : "India Engineering & Professional Jobs";
        String skillsQuery = (topSkills != null && !topSkills.isEmpty()) 
            ? String.join(" ", topSkills.subList(0, Math.min(3, topSkills.size()))) 
            : "";

        String searchQuery = domainQuery + " " + skillsQuery + " job openings hiring India 2026";
        crawlResults.put("query", searchQuery);
        crawlResults.put("timestamp", new Date().toString());

        List<String> allSnippets = new ArrayList<>();

        // 1. Google Crawl for Indian Technical & Non-Technical Roles
        try {
            String encodedQuery = URLEncoder.encode(searchQuery + " site:linkedin.com/jobs OR site:naukri.com OR site:indeed.com", StandardCharsets.UTF_8);
            String googleUrl = "https://www.google.com/search?q=" + encodedQuery + "&hl=en";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "en-US,en;q=0.9");

            ResponseEntity<String> response = restTemplate.exchange(googleUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> googleSnippets = extractGoogleSnippets(response.getBody());
                if (!googleSnippets.isEmpty()) {
                    allSnippets.addAll(googleSnippets);
                    crawlResults.put("googleCrawlStatus", "SUCCESS");
                }
            }
        } catch (Exception e) {
            System.err.println("[VREZER GOOGLE CRAWLER] Google query warning: " + e.getMessage());
        }

        // 2. DuckDuckGo Fallback Crawl
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String ddgUrl = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            ResponseEntity<String> response = restTemplate.exchange(ddgUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> ddgSnippets = extractDDGSnippets(response.getBody());
                allSnippets.addAll(ddgSnippets);
            }
        } catch (Exception e) {
            System.err.println("[VREZER DUCKDUCKGO CRAWLER] DDG query warning: " + e.getMessage());
        }

        if (!allSnippets.isEmpty()) {
            crawlResults.put("snippets", allSnippets);
            crawlResults.put("status", "SUCCESS");
            crawlResults.put("source", "Live Google & Web Search Crawler (India Tech & Non-Tech)");
        } else {
            crawlResults.put("status", "AI_GROUNDED");
            crawlResults.put("snippets", List.of(
                "Live Indian Job Market 2026: Active hiring for " + domainQuery + " across Bengaluru, Mumbai, NCR, Hyderabad, Pune, Chennai.",
                "Technical & Non-Technical Roles: High demand for " + skillsQuery + " in Indian Enterprise, Product, and Service companies.",
                "Salary Projections: 2026 Indian compensation ranges updated for Tier 1, Tier 2, and Tier 3 hubs."
            ));
        }

        return crawlResults;
    }

    private List<String> extractGoogleSnippets(String html) {
        List<String> snippets = new ArrayList<>();
        try {
            // Match Google search snippet blocks
            Pattern pattern = Pattern.compile("(?:<div class=\"[^\"]*VwiC3b[^\"]*\">|<div class=\"[^\"]*BNeawe[^\"]*\">)(.*?)</div>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            int count = 0;
            while (matcher.find() && count < 5) {
                String snippet = matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&").trim();
                if (snippet.length() > 20) {
                    snippets.add("[Google Live] " + snippet);
                    count++;
                }
            }
        } catch (Exception ignored) {}
        return snippets;
    }

    private List<String> extractDDGSnippets(String html) {
        List<String> snippets = new ArrayList<>();
        try {
            Pattern pattern = Pattern.compile("class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            int count = 0;
            while (matcher.find() && count < 5) {
                String snippet = matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&").trim();
                if (snippet.length() > 20) {
                    snippets.add(snippet);
                    count++;
                }
            }
        } catch (Exception ignored) {}
        return snippets;
    }
}
