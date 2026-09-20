package com.resume.analyzer.controller;

import com.resume.analyzer.dto.ApiResponse;
import com.resume.analyzer.service.AiAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AiAnalysisController — REST controller for all AI-powered analysis endpoints.
 *
 * STRICT RULE: All endpoints pass resume text to the AI service.
 * No hardcoded defaults like "Google", "Alex Johnson", or "Software Engineer"
 * are inserted into AI calls. If data is missing, the AI handles the absence.
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiAnalysisController {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Autowired
    private com.resume.analyzer.service.VrezerAiAgentService vrezerAiAgentService;

    @PostMapping("/ats-audit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> atsAudit(@RequestBody Map<String, Object> request) {
        String rawText = (String) request.getOrDefault("rawText", "");
        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) request.getOrDefault("skills", Collections.emptyList());
        Map<String, Object> result = aiAnalysisService.generateAtsAudit(rawText, skills);
        return ResponseEntity.ok(ApiResponse.ok("ATS Audit generated", result));
    }

    @PostMapping("/match-jd")
    public ResponseEntity<ApiResponse<Map<String, Object>>> matchJd(@RequestBody Map<String, String> request) {
        String resumeText = request.getOrDefault("resumeText", "");
        String jobDescription = request.getOrDefault("jobDescription", "");
        Map<String, Object> result = aiAnalysisService.matchJobDescription(resumeText, jobDescription);
        return ResponseEntity.ok(ApiResponse.ok("Job Description matched", result));
    }

    @PostMapping("/skill-gap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> skillGap(@RequestBody Map<String, Object> request) {
        String targetCompany = (String) request.getOrDefault("targetCompany", "");
        String targetRole = (String) request.getOrDefault("targetRole", "");
        @SuppressWarnings("unchecked")
        List<String> currentSkills = (List<String>) request.getOrDefault("currentSkills", Collections.emptyList());
        Map<String, Object> result = aiAnalysisService.analyzeSkillGap(targetCompany, targetRole, currentSkills);
        return ResponseEntity.ok(ApiResponse.ok("Skill Gap report generated", result));
    }

    @PostMapping("/career-roadmap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> careerRoadmap(@RequestBody Map<String, String> request) {
        String resumeText = request.getOrDefault("resumeText", "");
        String dreamJob = request.getOrDefault("dreamJob", "");
        Map<String, Object> result = aiAnalysisService.generateCareerRoadmap(resumeText, dreamJob);
        return ResponseEntity.ok(ApiResponse.ok("Career Roadmap generated", result));
    }

    @PostMapping("/interview-questions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> interviewQuestions(@RequestBody Map<String, String> request) {
        String resumeText = request.getOrDefault("resumeText", "");
        String role = request.getOrDefault("role", "");
        String difficulty = request.getOrDefault("difficulty", "Medium");
        Map<String, Object> result = aiAnalysisService.generateInterviewQuestions(resumeText, role, difficulty);
        return ResponseEntity.ok(ApiResponse.ok("Interview Questions generated", result));
    }

    @PostMapping("/predict-salary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> predictSalary(@RequestBody Map<String, Object> request) {
        String resumeText = (String) request.getOrDefault("resumeText", "");
        String role = (String) request.getOrDefault("role", "");
        String companyTier = (String) request.getOrDefault("companyTier", "");
        String location = (String) request.getOrDefault("location", "");
        Map<String, Object> result = aiAnalysisService.predictSalary(resumeText, role, companyTier, location);
        return ResponseEntity.ok(ApiResponse.ok("Salary prediction calculated", result));
    }

    @PostMapping("/analyze-portfolio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzePortfolio(@RequestBody Map<String, String> request) {
        String resumeText = request.getOrDefault("resumeText", "");
        String github = request.getOrDefault("githubUrl", "");
        String linkedin = request.getOrDefault("linkedinUrl", "");
        String portfolio = request.getOrDefault("portfolioUrl", "");
        Map<String, Object> result = aiAnalysisService.analyzePortfolio(resumeText, github, linkedin, portfolio);
        return ResponseEntity.ok(ApiResponse.ok("Portfolio audit completed", result));
    }

    @PostMapping("/recommend-projects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> recommendProjects(@RequestBody Map<String, Object> request) {
        String resumeText = (String) request.getOrDefault("resumeText", "");
        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) request.getOrDefault("skills", Collections.emptyList());
        String company = (String) request.getOrDefault("dreamCompany", "");
        List<Map<String, Object>> result = aiAnalysisService.recommendProjects(resumeText, skills, company);
        return ResponseEntity.ok(ApiResponse.ok("Recommended project blueprints generated", result));
    }

    @PostMapping("/generate-cover-letter")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateCoverLetter(@RequestBody Map<String, String> request) {
        String resumeText = request.getOrDefault("resumeText", "");
        String name = request.getOrDefault("candidateName", "");
        String company = request.getOrDefault("companyName", "");
        String role = request.getOrDefault("jobRole", "");
        String skillsSummary = request.getOrDefault("skillsSummary", "");
        String letter = aiAnalysisService.generateCoverLetter(resumeText, name, company, role, skillsSummary);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("coverLetter", letter)));
    }

    @PostMapping("/optimize-linkedin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> optimizeLinkedin(@RequestBody Map<String, String> request) {
        String resumeText = request.getOrDefault("resumeText", "");
        String headline = request.getOrDefault("currentHeadline", "");
        String about = request.getOrDefault("currentAbout", "");
        Map<String, Object> result = aiAnalysisService.optimizeLinkedin(resumeText, headline, about);
        return ResponseEntity.ok(ApiResponse.ok("LinkedIn Optimization completed", result));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, String>>> chat(@RequestBody Map<String, Object> request) {
        String prompt = String.valueOf(request.getOrDefault("prompt", "")).trim();
        @SuppressWarnings("unchecked")
        Map<String, Object> candidateContext = (Map<String, Object>) request.get("candidateContext");
        String answer = vrezerAiAgentService.askGeneralQuestion(prompt, candidateContext, null);
        return ResponseEntity.ok(ApiResponse.ok("Chatbot reply generated", Map.of("reply", answer)));
    }
}
