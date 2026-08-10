package com.resume.analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explainable ATS Intelligence Engine.
 * Recalculates ATS compatibility across 11 dimensions strictly from resume evidence.
 * If a target Job Description is provided, calculates semantic similarity separately.
 */
@Service
public class AtsAnalysisEngine {

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private SemanticVectorSearchService semanticVectorSearchService;

    public Map<String, Object> calculateAtsAnalysis(String resumeText, String jobDescription, Map<String, Object> baseParsed) {
        return calculateAtsAnalysis(resumeText, jobDescription);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> calculateAtsAnalysis(String resumeText, String jobDescription) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (resumeText == null || resumeText.trim().isEmpty()) {
            result.put("atsScore", 0);
            result.put("score", 0);
            result.put("atsScoreText", "NO CONTENT");
            result.put("explanation", "No resume text was provided for ATS evaluation.");
            return result;
        }

        Map<String, Object> parsed = resumeParserService.parseResumeText(resumeText);
        String lower = resumeText.toLowerCase();

        List<String> skills = (List<String>) parsed.getOrDefault("allDetectedSkills", List.of());
        List<Map<String, String>> exp = (List<Map<String, String>>) parsed.getOrDefault("experience", List.of());
        List<Map<String, String>> edu = (List<Map<String, String>>) parsed.getOrDefault("education", List.of());
        List<Map<String, String>> proj = (List<Map<String, String>>) parsed.getOrDefault("projects", List.of());
        List<String> certs = (List<String>) parsed.getOrDefault("certifications", List.of());
        List<String> achievements = (List<String>) parsed.getOrDefault("achievements", List.of());

        // 11 Evidence-Based Dimensional Scores (0 - 100)
        int skillsScore = calculateSkillsScore(skills);                        // 20%
        int keywordDensityScore = calculateKeywordDensityScore(resumeText, skills); // 15%
        int experienceScore = calculateExperienceScore(exp, resumeText);       // 15%
        int projectsScore = calculateProjectsScore(proj, resumeText);           // 10%
        int educationScore = calculateEducationScore(edu, String.valueOf(parsed.getOrDefault("cgpa", ""))); // 10%
        int sectionCompletenessScore = calculateSectionCompleteness(parsed, exp, edu, proj, skills); // 10%
        int certificationsScore = certs.isEmpty() ? 20 : Math.min(certs.size() * 30 + 35, 100); // 5%
        int achievementsScore = achievements.isEmpty() ? 20 : Math.min(achievements.size() * 30 + 35, 100); // 5%
        int formattingScore = calculateFormattingScore(resumeText, lower);     // 5%
        int readabilityScore = calculateReadabilityScore(resumeText);          // 5%
        int grammarScore = calculateGrammarScore(resumeText);

        // Weighted ATS Score (Strictly Normalized 0 - 100) — 11 dimensions, weights sum exactly to 1.00
        double weightedOverall = (skillsScore * 0.20) +
                                 (keywordDensityScore * 0.15) +
                                 (experienceScore * 0.15) +
                                 (projectsScore * 0.10) +
                                 (educationScore * 0.10) +
                                 (sectionCompletenessScore * 0.05) +   // reduced from 10% to fit grammar
                                 (certificationsScore * 0.05) +
                                 (achievementsScore * 0.05) +
                                 (formattingScore * 0.05) +
                                 (readabilityScore * 0.05) +
                                 (grammarScore * 0.05);                 // grammar was computed but not weighted — now included

        int overallScore = (int) Math.round(weightedOverall);
        overallScore = Math.max(10, Math.min(99, overallScore));

        // Separate Job Description Semantic Matcher (Independent Metric)
        List<String> missingKeywords = new ArrayList<>();
        double jdMatchScore = -1.0;
        if (jobDescription != null && !jobDescription.trim().isEmpty()) {
            jdMatchScore = semanticVectorSearchService.calculateVectorSimilarity(resumeText, jobDescription);
            missingKeywords = extractMissingKeywords(resumeText, jobDescription);
        }

        Map<String, Integer> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("skillsScore", skillsScore);
        scoreBreakdown.put("keywordOptimizationScore", keywordDensityScore);
        scoreBreakdown.put("experienceScore", experienceScore);
        scoreBreakdown.put("projectsScore", projectsScore);
        scoreBreakdown.put("educationScore", educationScore);
        scoreBreakdown.put("sectionCompletenessScore", sectionCompletenessScore);
        scoreBreakdown.put("certificationsScore", certificationsScore);
        scoreBreakdown.put("achievementsScore", achievementsScore);
        scoreBreakdown.put("formattingScore", formattingScore);
        scoreBreakdown.put("readabilityScore", readabilityScore);
        scoreBreakdown.put("grammarScore", grammarScore);

        List<String> strengths = new ArrayList<>();
        if (skillsScore >= 70) strengths.add("Strong technical keyword presence (" + skills.size() + " detected competencies)");
        if (projectsScore >= 70) strengths.add("Documented project impact with " + proj.size() + " capstones/initiatives");
        if (experienceScore >= 70) strengths.add("Clear professional experience with industry impact indicators");
        if (educationScore >= 75) strengths.add("Verified academic pedigree & recognized degree credentials");
        if (formattingScore >= 80) strengths.add("Clean ATS-compliant text layout and readable structure");

        List<String> weaknesses = new ArrayList<>();
        if (certs.isEmpty()) weaknesses.add("No professional certifications detected on resume");
        if (achievements.isEmpty()) weaknesses.add("No explicit awards or honors section identified");
        if (skills.size() < 6) weaknesses.add("Low density of domain-specific technical keyword phrases");
        if (missingKeywords.size() > 0) weaknesses.add("Missing target keywords: " + String.join(", ", missingKeywords.subList(0, Math.min(missingKeywords.size(), 4))));

        List<String> citations = new ArrayList<>();
        if (parsed.get("email") != null && !String.valueOf(parsed.get("email")).isEmpty()) citations.add("Contact info verified: " + parsed.get("email"));
        if (!skills.isEmpty()) citations.add("Core skill evidence: " + String.join(", ", skills.subList(0, Math.min(skills.size(), 6))));

        result.put("score", overallScore);
        result.put("atsScore", overallScore);
        result.put("atsScoreText", overallScore >= 85 ? "EXCELLENT" : overallScore >= 70 ? "GOOD" : overallScore >= 55 ? "AVERAGE" : "NEEDS IMPROVEMENT");
        if (jdMatchScore >= 0.0) {
            result.put("jdMatchScore", Math.round(jdMatchScore));
        }
        result.put("explanation", "ATS score computed from 11 evidence-based dimensional metrics for candidate resume.");
        result.put("scoreBreakdown", scoreBreakdown);
        result.put("strengths", strengths);
        result.put("weaknesses", weaknesses);
        result.put("missingKeywords", missingKeywords);
        result.put("citations", citations);

        return result;
    }

    private int calculateGrammarScore(String text) {
        if (text == null || text.trim().isEmpty()) return 70;
        long typos = Pattern.compile("(?i)\\b(teh|receive|adress|expereince|develepor|manger)\\b").matcher(text).results().count();
        return (int) Math.max(50, 95 - (typos * 8));
    }

    private int calculateReadabilityScore(String text) {
        if (text == null || text.trim().isEmpty()) return 70;
        String[] sentences = text.split("[.!?]+");
        double avgWordsPerSentence = (double) text.split("\\s+").length / Math.max(1, sentences.length);
        if (avgWordsPerSentence >= 10 && avgWordsPerSentence <= 25) return 92;
        return 78;
    }

    private int calculateFormattingScore(String text, String lower) {
        if (text == null || text.trim().isEmpty()) return 25;
        int score = 55;
        if (lower.contains("@")) score += 10;
        if (Pattern.compile("\\+?\\d{10,12}").matcher(text).find()) score += 10;
        if (text.contains("•") || text.contains("- ") || text.contains("–") || text.contains("*")) score += 15;
        if (lower.contains("table") || lower.contains("column") || lower.contains("graphic")) score -= 15;
        if (text.length() > 300 && text.length() < 6000) score += 10;
        return Math.max(25, Math.min(score, 98));
    }

    private int calculateSectionCompleteness(Map<String, Object> parsed, List<?> exp, List<?> edu, List<?> proj, List<?> skills) {
        int score = 15;
        if (!exp.isEmpty()) score += 25;
        if (!edu.isEmpty()) score += 20;
        if (!proj.isEmpty()) score += 20;
        if (!skills.isEmpty()) score += 15;
        if (parsed.get("email") != null && !String.valueOf(parsed.get("email")).isEmpty()) score += 5;
        return Math.min(score, 100);
    }

    private int calculateKeywordDensityScore(String text, List<String> skills) {
        if (skills == null || skills.isEmpty()) return 20;
        int count = skills.size();
        double wordCount = Math.max(1.0, text.split("\\s+").length);
        double densityBonus = Math.min(25.0, (count / wordCount) * 450.0);
        int score = 30 + (int) Math.round(count * 2.8 + densityBonus);
        return Math.max(20, Math.min(98, score));
    }

    private int calculateSkillsScore(List<String> skills) {
        if (skills == null || skills.isEmpty()) return 20;
        int count = skills.size();
        int score = 30 + (int) Math.round(count * 3.5);
        return Math.max(20, Math.min(98, score));
    }

    private int calculateProjectsScore(List<Map<String, String>> proj, String text) {
        if (proj == null || proj.isEmpty()) return 25;
        long metricsCount = Pattern.compile("(\\d+%|\\$\\d+|\\b\\d+x\\b|₹\\d+)").matcher(text).results().count();
        int score = 35 + proj.size() * 16 + (int) Math.min(25, metricsCount * 8);
        return Math.max(25, Math.min(98, score));
    }

    private int calculateExperienceScore(List<Map<String, String>> exp, String text) {
        if (exp == null || exp.isEmpty()) return 30;
        int score = 35 + exp.size() * 15;
        String lower = text.toLowerCase();
        if (lower.contains("senior") || lower.contains("lead") || lower.contains("manager") || lower.contains("architect")) score += 10;
        if (lower.contains("managed") || lower.contains("headed") || lower.contains("architected") || lower.contains("scaled")) score += 10;
        return Math.max(25, Math.min(98, score));
    }

    private int calculateEducationScore(List<Map<String, String>> edu, String cgpa) {
        if (edu == null || edu.isEmpty()) return 25;
        int score = 55;
        if (cgpa != null && !cgpa.trim().isEmpty()) score += 15;
        String deg = edu.get(0).getOrDefault("degree", "").toLowerCase();
        if (deg.contains("m.") || deg.contains("master") || deg.contains("ph.d") || deg.contains("b.tech") || deg.contains("b.e") || deg.contains("ca")) score += 15;
        return Math.max(25, Math.min(98, score));
    }

    private List<String> extractMissingKeywords(String resumeText, String jobDescription) {
        List<String> missing = new ArrayList<>();
        String resumeLower = resumeText.toLowerCase();
        String[] jdWords = jobDescription.split("[^a-zA-Z0-9+#.#]");
        Set<String> checked = new HashSet<>();
        for (String word : jdWords) {
            String w = word.trim();
            if (w.length() >= 4 && !checked.contains(w.toLowerCase())) {
                checked.add(w.toLowerCase());
                if (!resumeLower.contains(w.toLowerCase())) {
                    missing.add(w);
                    if (missing.size() >= 8) break;
                }
            }
        }
        return missing;
    }
}
