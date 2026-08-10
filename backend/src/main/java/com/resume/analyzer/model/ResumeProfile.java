package com.resume.analyzer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canonical Resume Profile Object.
 * Every downstream service (ATS Engine, RAG Service, Job Aggregator, Gemini AI Orchestrator)
 * must consume this EXACT profile object to guarantee single-source-of-truth consistency.
 */
public class ResumeProfile {
    private String candidateId;
    private String analysisId;
    private String name;
    private String email;
    private String phone;
    private String location;
    private String education;
    private String degree;
    private String specialization;
    private String cgpa;
    private String experience;
    private double experienceYears;
    private String seniority; // FRESHER, JUNIOR, MID_LEVEL, SENIOR, LEAD
    private List<String> jobTitles = new ArrayList<>();
    private List<String> skills = new ArrayList<>();
    private List<String> verifiedSkills = new ArrayList<>();
    private List<String> inferredSkills = new ArrayList<>();
    private List<String> technicalSkills = new ArrayList<>();
    private List<String> softSkills = new ArrayList<>();
    private List<Map<String, String>> projects = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();
    private List<String> achievements = new ArrayList<>();
    private List<String> industries = new ArrayList<>();
    private String careerDomain;
    private List<String> targetRoles = new ArrayList<>();
    private String primaryTargetRole;
    private List<String> languages = new ArrayList<>();
    private String resumeText;
    private String resumeHash;

    public ResumeProfile() {}

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getCgpa() { return cgpa; }
    public void setCgpa(String cgpa) { this.cgpa = cgpa; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public double getExperienceYears() { return experienceYears; }
    public void setExperienceYears(double experienceYears) { this.experienceYears = experienceYears; }

    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }

    public List<String> getJobTitles() { return jobTitles; }
    public void setJobTitles(List<String> jobTitles) { this.jobTitles = jobTitles; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getVerifiedSkills() { return verifiedSkills; }
    public void setVerifiedSkills(List<String> verifiedSkills) { this.verifiedSkills = verifiedSkills; }

    public List<String> getInferredSkills() { return inferredSkills; }
    public void setInferredSkills(List<String> inferredSkills) { this.inferredSkills = inferredSkills; }

    public List<String> getTechnicalSkills() { return technicalSkills; }
    public void setTechnicalSkills(List<String> technicalSkills) { this.technicalSkills = technicalSkills; }

    public List<String> getSoftSkills() { return softSkills; }
    public void setSoftSkills(List<String> softSkills) { this.softSkills = softSkills; }

    public List<Map<String, String>> getProjects() { return projects; }
    public void setProjects(List<Map<String, String>> projects) { this.projects = projects; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }

    public List<String> getIndustries() { return industries; }
    public void setIndustries(List<String> industries) { this.industries = industries; }

    public String getCareerDomain() { return careerDomain; }
    public void setCareerDomain(String careerDomain) { this.careerDomain = careerDomain; }

    public List<String> getTargetRoles() { return targetRoles; }
    public void setTargetRoles(List<String> targetRoles) { this.targetRoles = targetRoles; }

    public String getPrimaryTargetRole() { return primaryTargetRole; }
    public void setPrimaryTargetRole(String primaryTargetRole) { this.primaryTargetRole = primaryTargetRole; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }

    public String getResumeHash() { return resumeHash; }
    public void setResumeHash(String resumeHash) { this.resumeHash = resumeHash; }
}
