package com.resume.analyzer.dto;

import java.util.*;

/**
 * CandidateProfileDto — Canonical Candidate Representation for VREZER.
 * Every downstream engine (ATS, Job Matching, RAG, Companies, Salary, Career Prediction)
 * consumes THIS SAME structure to guarantee consistent, evidence-based results.
 */
public class CandidateProfileDto {
    private String candidateId;
    private String resumeHash;
    private String name;
    private String email;
    private String phone;
    private String linkedin;
    private String github;
    private String portfolio;

    private String careerDomain;
    private double domainConfidence;
    private String primaryRole;
    private List<String> alternativeRoles = new ArrayList<>();
    
    private String seniority;
    private double yearsOfExperience;
    private String experienceLevel; // FRESHER, JUNIOR, MID, SENIOR, LEAD

    private String degree;
    private String specialization;
    private String university;
    private String cgpa;
    private String educationSummary;

    private List<String> skills = new ArrayList<>();
    private List<String> technicalSkills = new ArrayList<>();
    private List<String> softSkills = new ArrayList<>();
    private List<String> programmingLanguages = new ArrayList<>();
    private List<String> frameworks = new ArrayList<>();
    private List<String> databases = new ArrayList<>();
    private List<String> cloudSkills = new ArrayList<>();
    private List<String> devOpsSkills = new ArrayList<>();
    private List<String> aiSkills = new ArrayList<>();

    private List<Map<String, String>> experience = new ArrayList<>();
    private List<Map<String, String>> internships = new ArrayList<>();
    private List<Map<String, String>> projects = new ArrayList<>();
    private List<String> projectTechnologies = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();
    private List<String> achievements = new ArrayList<>();

    private String industry;
    private String workMode;
    private String preferredLocation;
    private String preferredWorkMode;
    private List<String> searchQueries = new ArrayList<>();
    private String expectedSalary;
    private SalaryEstimateDto salaryEstimate;

    public CandidateProfileDto() {}

    public String getIndustry() { return industry != null ? industry : careerDomain; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getWorkMode() { return workMode != null ? workMode : (preferredWorkMode != null ? preferredWorkMode : "Hybrid"); }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    // Getters and Setters
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getResumeHash() { return resumeHash; }
    public void setResumeHash(String resumeHash) { this.resumeHash = resumeHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }

    public String getGithub() { return github; }
    public void setGithub(String github) { this.github = github; }

    public String getPortfolio() { return portfolio; }
    public void setPortfolio(String portfolio) { this.portfolio = portfolio; }

    public String getCareerDomain() { return careerDomain; }
    public void setCareerDomain(String careerDomain) { this.careerDomain = careerDomain; }

    public double getDomainConfidence() { return domainConfidence; }
    public void setDomainConfidence(double domainConfidence) { this.domainConfidence = domainConfidence; }

    public String getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(String primaryRole) { this.primaryRole = primaryRole; }

    public List<String> getAlternativeRoles() { return alternativeRoles; }
    public void setAlternativeRoles(List<String> alternativeRoles) { this.alternativeRoles = alternativeRoles; }

    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }

    public double getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(double yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getCgpa() { return cgpa; }
    public void setCgpa(String cgpa) { this.cgpa = cgpa; }

    public String getEducationSummary() { return educationSummary; }
    public void setEducationSummary(String educationSummary) { this.educationSummary = educationSummary; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getTechnicalSkills() { return technicalSkills; }
    public void setTechnicalSkills(List<String> technicalSkills) { this.technicalSkills = technicalSkills; }

    public List<String> getSoftSkills() { return softSkills; }
    public void setSoftSkills(List<String> softSkills) { this.softSkills = softSkills; }

    public List<String> getProgrammingLanguages() { return programmingLanguages; }
    public void setProgrammingLanguages(List<String> programmingLanguages) { this.programmingLanguages = programmingLanguages; }

    public List<String> getFrameworks() { return frameworks; }
    public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks; }

    public List<String> getDatabases() { return databases; }
    public void setDatabases(List<String> databases) { this.databases = databases; }

    public List<String> getCloudSkills() { return cloudSkills; }
    public void setCloudSkills(List<String> cloudSkills) { this.cloudSkills = cloudSkills; }

    public List<String> getDevOpsSkills() { return devOpsSkills; }
    public void setDevOpsSkills(List<String> devOpsSkills) { this.devOpsSkills = devOpsSkills; }

    public List<String> getAiSkills() { return aiSkills; }
    public void setAiSkills(List<String> aiSkills) { this.aiSkills = aiSkills; }

    public List<Map<String, String>> getExperience() { return experience; }
    public void setExperience(List<Map<String, String>> experience) { this.experience = experience; }

    public List<Map<String, String>> getInternships() { return internships; }
    public void setInternships(List<Map<String, String>> internships) { this.internships = internships; }

    public List<Map<String, String>> getProjects() { return projects; }
    public void setProjects(List<Map<String, String>> projects) { this.projects = projects; }

    public List<String> getProjectTechnologies() { return projectTechnologies; }
    public void setProjectTechnologies(List<String> projectTechnologies) { this.projectTechnologies = projectTechnologies; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }

    public String getPreferredLocation() { return preferredLocation; }
    public void setPreferredLocation(String preferredLocation) { this.preferredLocation = preferredLocation; }

    public String getPreferredWorkMode() { return preferredWorkMode; }
    public void setPreferredWorkMode(String preferredWorkMode) { this.preferredWorkMode = preferredWorkMode; }

    public List<String> getSearchQueries() { return searchQueries; }
    public void setSearchQueries(List<String> searchQueries) { this.searchQueries = searchQueries; }

    public String getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(String expectedSalary) { this.expectedSalary = expectedSalary; }

    public SalaryEstimateDto getSalaryEstimate() { return salaryEstimate; }
    public void setSalaryEstimate(SalaryEstimateDto salaryEstimate) { this.salaryEstimate = salaryEstimate; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("candidateId", candidateId);
        map.put("resumeHash", resumeHash);
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        map.put("linkedin", linkedin);
        map.put("github", github);
        map.put("portfolio", portfolio);
        map.put("careerDomain", careerDomain);
        map.put("domainConfidence", domainConfidence);
        map.put("primaryRole", primaryRole);
        map.put("alternativeRoles", alternativeRoles);
        map.put("seniority", seniority);
        map.put("yearsOfExperience", yearsOfExperience);
        map.put("experienceLevel", experienceLevel);
        map.put("degree", degree);
        map.put("specialization", specialization);
        map.put("university", university);
        map.put("cgpa", cgpa);
        map.put("educationSummary", educationSummary);
        map.put("skills", skills);
        map.put("technicalSkills", technicalSkills);
        map.put("softSkills", softSkills);
        map.put("programmingLanguages", programmingLanguages);
        map.put("frameworks", frameworks);
        map.put("databases", databases);
        map.put("cloudSkills", cloudSkills);
        map.put("devOpsSkills", devOpsSkills);
        map.put("aiSkills", aiSkills);
        map.put("experience", experience);
        map.put("internships", internships);
        map.put("projects", projects);
        map.put("projectTechnologies", projectTechnologies);
        map.put("certifications", certifications);
        map.put("achievements", achievements);
        map.put("preferredLocation", preferredLocation);
        map.put("preferredWorkMode", preferredWorkMode);
        map.put("searchQueries", searchQueries);
        map.put("expectedSalary", expectedSalary);
        map.put("salaryEstimate", salaryEstimate);
        return map;
    }
}
