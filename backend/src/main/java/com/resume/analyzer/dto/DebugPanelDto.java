package com.resume.analyzer.dto;

import java.util.*;

/**
 * Phase 11: Developer Debug Panel DTO.
 * Captures complete pipeline diagnostics for developer inspection.
 */
public class DebugPanelDto {

    private Map<String, Object> parsedResumeJson;
    private Map<String, Object> candidateProfile;
    private String generatedSearchQuery;
    private List<String> apiRequests;
    private List<String> apiResponses;
    private List<Map<String, String>> retrievedJobs;
    private int removedDuplicatesCount;
    private List<Map<String, Object>> similarityScores;
    private List<Map<String, String>> rejectedJobs;
    private List<Map<String, String>> rankedJobs;
    private Map<String, Object> atsBreakdown;
    private String aiPrompt;
    private String aiResponse;
    private Map<String, Object> dashboardJson;

    public DebugPanelDto() {
        this.parsedResumeJson = new LinkedHashMap<>();
        this.candidateProfile = new LinkedHashMap<>();
        this.generatedSearchQuery = "";
        this.apiRequests = new ArrayList<>();
        this.apiResponses = new ArrayList<>();
        this.retrievedJobs = new ArrayList<>();
        this.removedDuplicatesCount = 0;
        this.similarityScores = new ArrayList<>();
        this.rejectedJobs = new ArrayList<>();
        this.rankedJobs = new ArrayList<>();
        this.atsBreakdown = new LinkedHashMap<>();
        this.aiPrompt = "";
        this.aiResponse = "";
        this.dashboardJson = new LinkedHashMap<>();
    }

    // Getters and Setters
    public Map<String, Object> getParsedResumeJson() { return parsedResumeJson; }
    public void setParsedResumeJson(Map<String, Object> parsedResumeJson) { this.parsedResumeJson = parsedResumeJson; }

    public Map<String, Object> getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(Map<String, Object> candidateProfile) { this.candidateProfile = candidateProfile; }

    public String getGeneratedSearchQuery() { return generatedSearchQuery; }
    public void setGeneratedSearchQuery(String generatedSearchQuery) { this.generatedSearchQuery = generatedSearchQuery; }

    public List<String> getApiRequests() { return apiRequests; }
    public void setApiRequests(List<String> apiRequests) { this.apiRequests = apiRequests; }

    public List<String> getApiResponses() { return apiResponses; }
    public void setApiResponses(List<String> apiResponses) { this.apiResponses = apiResponses; }

    public List<Map<String, String>> getRetrievedJobs() { return retrievedJobs; }
    public void setRetrievedJobs(List<Map<String, String>> retrievedJobs) { this.retrievedJobs = retrievedJobs; }

    public int getRemovedDuplicatesCount() { return removedDuplicatesCount; }
    public void setRemovedDuplicatesCount(int removedDuplicatesCount) { this.removedDuplicatesCount = removedDuplicatesCount; }

    public List<Map<String, Object>> getSimilarityScores() { return similarityScores; }
    public void setSimilarityScores(List<Map<String, Object>> similarityScores) { this.similarityScores = similarityScores; }

    public List<Map<String, String>> getRejectedJobs() { return rejectedJobs; }
    public void setRejectedJobs(List<Map<String, String>> rejectedJobs) { this.rejectedJobs = rejectedJobs; }

    public List<Map<String, String>> getRankedJobs() { return rankedJobs; }
    public void setRankedJobs(List<Map<String, String>> rankedJobs) { this.rankedJobs = rankedJobs; }

    public Map<String, Object> getAtsBreakdown() { return atsBreakdown; }
    public void setAtsBreakdown(Map<String, Object> atsBreakdown) { this.atsBreakdown = atsBreakdown; }

    public String getAiPrompt() { return aiPrompt; }
    public void setAiPrompt(String aiPrompt) { this.aiPrompt = aiPrompt; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public Map<String, Object> getDashboardJson() { return dashboardJson; }
    public void setDashboardJson(Map<String, Object> dashboardJson) { this.dashboardJson = dashboardJson; }
}
