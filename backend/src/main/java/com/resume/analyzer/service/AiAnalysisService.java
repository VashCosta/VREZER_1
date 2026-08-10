package com.resume.analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class AiAnalysisService {

    @Autowired
    private VrezerAiAgentService vrezerAiAgentService;

    @Autowired
    private ResumeIntelligenceEngine resumeIntelligenceEngine;

    @Autowired
    private JobAggregatorService jobAggregatorService;

    @Autowired
    private SalaryIntelligenceService salaryIntelligenceService;

    private final ObjectMapper mapper = new ObjectMapper();

    public String analyzeResume(String resumeText) {
        try {
            Map<String, Object> result = vrezerAiAgentService.analyzeResumeWithAiAgent(resumeText, "");
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            System.err.println("[AI ANALYSIS SERVICE] Error during AI agent analysis: " + e.getMessage());
            throw new RuntimeException("AI Resume Analysis Failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> generateAtsAudit(String rawText, List<String> skills) {
        return vrezerAiAgentService.analyzeResumeWithAiAgent(rawText, "");
    }

    public Map<String, Object> matchJobDescription(String resumeText, String jobDescription) {
        return vrezerAiAgentService.analyzeResumeWithAiAgent(resumeText, jobDescription);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeSkillGap(String targetCompany, String targetRole, List<String> currentSkills) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(String.join(", ", currentSkills), "Target Company: " + targetCompany + "\nTarget Role: " + targetRole);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetCompany", targetCompany);
        result.put("targetRole", targetRole);
        result.put("swot", full.getOrDefault("swot", Map.of()));
        result.put("skillIntelligence", full.getOrDefault("skillIntelligence", Map.of()));
        result.put("missingSkills", full.getOrDefault("missingSkills", List.of()));
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateCareerRoadmap(String resumeText, String dreamJob) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(resumeText, dreamJob);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dreamJob", dreamJob);
        result.put("careerGrowthTimeline", full.getOrDefault("careerGrowthTimeline", List.of()));
        result.put("skillIntelligence", full.getOrDefault("skillIntelligence", Map.of()));
        result.put("strategicForecast", full.getOrDefault("strategicForecast", ""));
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateInterviewQuestions(String resumeText, String role, String difficulty) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(resumeText, "Target Role: " + role + "\nDifficulty: " + difficulty);
        return (Map<String, Object>) full.getOrDefault("interviewPreparation", Map.of());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> predictSalary(String resumeText, String role, String companyTier, String location) {
        // Build canonical resume profile and convert to CandidateProfileDto
        com.resume.analyzer.model.ResumeProfile rp = resumeIntelligenceEngine.buildCanonicalResumeProfile(resumeText, null);

        com.resume.analyzer.dto.CandidateProfileDto cp = new com.resume.analyzer.dto.CandidateProfileDto();
        cp.setName(rp.getName());
        cp.setPrimaryRole(rp.getPrimaryTargetRole());
        cp.setYearsOfExperience(rp.getExperienceYears());
        cp.setExperienceLevel(rp.getSeniority());
        cp.setPreferredLocation(rp.getLocation());
        cp.setCareerDomain(rp.getCareerDomain());
        cp.setTechnicalSkills(rp.getVerifiedSkills());
        cp.setSkills(rp.getVerifiedSkills());
        cp.setProjects(rp.getProjects());
        cp.setCertifications(rp.getCertifications());

        // Fetch live jobs asynchronously but don't block other analysis (best-effort here)
        List<Map<String, String>> liveJobs = new ArrayList<>();
        try {
            liveJobs = jobAggregatorService.aggregateAndRankJobs(rp.getCareerDomain(), rp.getVerifiedSkills(), rp.getLocation(), resumeText);
        } catch (Exception e) {
            System.err.println("[AI SALARY] Live job aggregation failed: " + e.getMessage());
        }

        List<Map<String, Object>> liveJobsObj = new ArrayList<>();
        for (Map<String, String> j : liveJobs) {
            liveJobsObj.add(new LinkedHashMap<>(j));
        }

        Map<String, Object> salaryResult = salaryIntelligenceService.calculateSalary(cp, liveJobsObj);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", cp.getPrimaryRole());
        result.put("location", cp.getPreferredLocation());
        result.put("jobSalary", salaryResult.get("jobSalary"));
        result.put("marketSalary", salaryResult.get("marketSalary"));
        result.put("candidateSalaryEstimate", salaryResult.get("candidateSalaryEstimate"));
        int confidenceScore = 0;
        Object estObj = salaryResult.get("candidateSalaryEstimate");
        if (estObj instanceof Map) {
            Object conf = ((Map<?, ?>) estObj).get("confidence");
            if (conf instanceof Number) {
                confidenceScore = ((Number) conf).intValue();
            }
        }
        result.put("confidenceScore", confidenceScore);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzePortfolio(String resumeText, String github, String linkedin, String portfolio) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(resumeText, "");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("githubUrl", github);
        result.put("linkedinUrl", linkedin);
        result.put("portfolioUrl", portfolio);
        result.put("portfolioReadinessScoreDetails", full.getOrDefault("portfolioReadinessScoreDetails", Map.of()));
        result.put("recruiterInsights", full.getOrDefault("recruiterInsights", Map.of()));
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> recommendProjects(String resumeText, List<String> skills, String company) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(
            resumeText != null && !resumeText.trim().isEmpty() ? resumeText : String.join(", ", skills),
            "Target Company: " + company
        );

        List<Map<String, Object>> projects = new ArrayList<>();
        List<String> topSkills = (List<String>) full.getOrDefault("topSkills", skills);
        String domain = String.valueOf(full.getOrDefault("careerDomain", "Software Engineering"));

        projects.add(Map.of(
            "title", domain + " Enterprise Intelligence Platform",
            "techStack", topSkills.isEmpty() ? List.of("Spring Boot", "React", "PostgreSQL") : topSkills,
            "description", "Production-grade enterprise platform utilizing " + String.join(", ", topSkills) + " for end-to-end operational automation.",
            "difficulty", "Advanced",
            "resumeImpact", "Demonstrates senior-level domain proficiency for target roles at " + (company.isEmpty() ? "top tech companies" : company)
        ));
        projects.add(Map.of(
            "title", "Scalable " + domain + " Microservices & Cloud Infrastructure",
            "techStack", topSkills.size() > 2 ? topSkills.subList(0, 3) : List.of("Docker", "Kubernetes", "AWS"),
            "description", "Cloud-native distributed microservices with automated CI/CD pipelines, observability, and high throughput API gateways.",
            "difficulty", "Intermediate",
            "resumeImpact", "High impact demonstration of scalable system architecture and production engineering"
        ));
        return projects;
    }

    public String generateCoverLetter(String resumeText, String name, String company, String role, String skillsSummary) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(
            resumeText != null && !resumeText.trim().isEmpty() ? resumeText : "Candidate: " + name + "\nSkills: " + skillsSummary,
            "Target Company: " + company + "\nTarget Role: " + role
        );

        String candidateName = name.isEmpty() ? String.valueOf(full.getOrDefault("name", "Candidate")) : name;
        String targetRole = role.isEmpty() ? String.valueOf(full.getOrDefault("role", "Specialist")) : role;
        String domain = String.valueOf(full.getOrDefault("careerDomain", "Engineering"));

        StringBuilder sb = new StringBuilder();
        sb.append("Dear Hiring Manager at ").append(company.isEmpty() ? "your organization" : company).append(",\n\n");
        sb.append("I am writing to express my enthusiastic interest in the ").append(targetRole).append(" position. ");
        sb.append("With proven expertise in ").append(domain).append(" and core competencies in ").append(skillsSummary.isEmpty() ? "my professional domain" : skillsSummary).append(", ");
        sb.append("I bring strong technical execution, problem-solving skills, and a commitment to delivery excellence.\n\n");
        sb.append("My professional experience directly matches the strategic requirements of your team. ");
        sb.append("I welcome the opportunity to discuss how my qualifications can add immediate value at ").append(company.isEmpty() ? "your team" : company).append(".\n\n");
        sb.append("Sincerely,\n");
        sb.append(candidateName);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> optimizeLinkedin(String resumeText, String headline, String about) {
        Map<String, Object> full = vrezerAiAgentService.analyzeResumeWithAiAgent(resumeText, "");
        String role = String.valueOf(full.getOrDefault("role", "Specialist"));
        String domain = String.valueOf(full.getOrDefault("careerDomain", "Software Engineering"));
        List<String> topSkills = (List<String>) full.getOrDefault("topSkills", List.of());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentHeadline", headline);
        result.put("optimizedHeadline", role + " | " + domain + " | " + String.join(" • ", topSkills.subList(0, Math.min(topSkills.size(), 4))));
        result.put("currentAbout", about);
        result.put("optimizedAbout", String.valueOf(full.getOrDefault("professionalSummary", "Results-driven specialist with expertise in building scalable, production-grade applications.")));
        result.put("topSearchKeywords", topSkills);
        return result;
    }
}
