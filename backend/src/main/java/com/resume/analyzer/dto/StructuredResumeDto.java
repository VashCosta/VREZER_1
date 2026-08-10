package com.resume.analyzer.dto;

import java.util.*;

/**
 * StructuredResumeDto — Clean, validated 25-field DTO produced by ResumeParserService.
 * This DTO is validated/normalized before being sent to the AI pipeline, ensuring
 * the AI receives structured, clean data rather than raw text alone.
 *
 * STRICT RULE: Every field must come from the actual resume text.
 * Empty strings and empty lists are valid — never return placeholder values.
 */
public class StructuredResumeDto {

    // ── Identity & Contact ──────────────────────────────────────────────────
    private String name = "";
    private String email = "";
    private String phone = "";
    private String linkedin = "";
    private String github = "";
    private String portfolio = "";

    // ── Education ──────────────────────────────────────────────────────────
    private List<Map<String, String>> education = new ArrayList<>();
    private String degree = "";
    private String specialization = "";
    private String institution = "";
    private String cgpa = "";

    // ── Professional Background ────────────────────────────────────────────
    private List<Map<String, String>> experience = new ArrayList<>();
    private List<Map<String, String>> internships = new ArrayList<>();
    private List<Map<String, String>> projects = new ArrayList<>();

    // ── Skills (categorized) ───────────────────────────────────────────────
    private List<String> technicalSkills = new ArrayList<>();
    private List<String> softSkills = new ArrayList<>();
    private List<String> programmingLanguages = new ArrayList<>();
    private List<String> frameworks = new ArrayList<>();
    private List<String> libraries = new ArrayList<>();
    private List<String> databases = new ArrayList<>();
    private List<String> cloudPlatforms = new ArrayList<>();
    private List<String> tools = new ArrayList<>();
    private List<String> allDetectedSkills = new ArrayList<>();

    // ── Credentials & Achievements ─────────────────────────────────────────
    private List<String> certifications = new ArrayList<>();
    private List<String> achievements = new ArrayList<>();

    // ── Online Presence & Research ─────────────────────────────────────────
    private List<String> researchPapers = new ArrayList<>();
    private List<String> volunteerWork = new ArrayList<>();

    // ── Languages & Objective ──────────────────────────────────────────────
    private List<String> languagesSpoken = new ArrayList<>();
    private String careerObjective = "";

    // ── Raw Text (passed through for AI completeness) ──────────────────────
    private String rawText = "";

    // ── Metadata ───────────────────────────────────────────────────────────
    private boolean isValidated = false;
    private int totalFieldsDetected = 0;
    private String parserVersion = "v2.0";

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : ""; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim() : ""; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone != null ? phone.trim() : ""; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin != null ? linkedin.trim() : ""; }

    public String getGithub() { return github; }
    public void setGithub(String github) { this.github = github != null ? github.trim() : ""; }

    public String getPortfolio() { return portfolio; }
    public void setPortfolio(String portfolio) { this.portfolio = portfolio != null ? portfolio.trim() : ""; }

    public List<Map<String, String>> getEducation() { return education; }
    public void setEducation(List<Map<String, String>> education) { this.education = education != null ? education : new ArrayList<>(); }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree != null ? degree.trim() : ""; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization != null ? specialization.trim() : ""; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution != null ? institution.trim() : ""; }

    public String getCgpa() { return cgpa; }
    public void setCgpa(String cgpa) { this.cgpa = cgpa != null ? cgpa.trim() : ""; }

    public List<Map<String, String>> getExperience() { return experience; }
    public void setExperience(List<Map<String, String>> experience) { this.experience = experience != null ? experience : new ArrayList<>(); }

    public List<Map<String, String>> getInternships() { return internships; }
    public void setInternships(List<Map<String, String>> internships) { this.internships = internships != null ? internships : new ArrayList<>(); }

    public List<Map<String, String>> getProjects() { return projects; }
    public void setProjects(List<Map<String, String>> projects) { this.projects = projects != null ? projects : new ArrayList<>(); }

    public List<String> getTechnicalSkills() { return technicalSkills; }
    public void setTechnicalSkills(List<String> technicalSkills) { this.technicalSkills = technicalSkills != null ? technicalSkills : new ArrayList<>(); }

    public List<String> getSoftSkills() { return softSkills; }
    public void setSoftSkills(List<String> softSkills) { this.softSkills = softSkills != null ? softSkills : new ArrayList<>(); }

    public List<String> getProgrammingLanguages() { return programmingLanguages; }
    public void setProgrammingLanguages(List<String> programmingLanguages) { this.programmingLanguages = programmingLanguages != null ? programmingLanguages : new ArrayList<>(); }

    public List<String> getFrameworks() { return frameworks; }
    public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks != null ? frameworks : new ArrayList<>(); }

    public List<String> getLibraries() { return libraries; }
    public void setLibraries(List<String> libraries) { this.libraries = libraries != null ? libraries : new ArrayList<>(); }

    public List<String> getDatabases() { return databases; }
    public void setDatabases(List<String> databases) { this.databases = databases != null ? databases : new ArrayList<>(); }

    public List<String> getCloudPlatforms() { return cloudPlatforms; }
    public void setCloudPlatforms(List<String> cloudPlatforms) { this.cloudPlatforms = cloudPlatforms != null ? cloudPlatforms : new ArrayList<>(); }

    public List<String> getTools() { return tools; }
    public void setTools(List<String> tools) { this.tools = tools != null ? tools : new ArrayList<>(); }

    public List<String> getAllDetectedSkills() { return allDetectedSkills; }
    public void setAllDetectedSkills(List<String> allDetectedSkills) { this.allDetectedSkills = allDetectedSkills != null ? allDetectedSkills : new ArrayList<>(); }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications != null ? certifications : new ArrayList<>(); }

    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements != null ? achievements : new ArrayList<>(); }

    public List<String> getResearchPapers() { return researchPapers; }
    public void setResearchPapers(List<String> researchPapers) { this.researchPapers = researchPapers != null ? researchPapers : new ArrayList<>(); }

    public List<String> getVolunteerWork() { return volunteerWork; }
    public void setVolunteerWork(List<String> volunteerWork) { this.volunteerWork = volunteerWork != null ? volunteerWork : new ArrayList<>(); }

    public List<String> getLanguagesSpoken() { return languagesSpoken; }
    public void setLanguagesSpoken(List<String> languagesSpoken) { this.languagesSpoken = languagesSpoken != null ? languagesSpoken : new ArrayList<>(); }

    public String getCareerObjective() { return careerObjective; }
    public void setCareerObjective(String careerObjective) { this.careerObjective = careerObjective != null ? careerObjective.trim() : ""; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText != null ? rawText : ""; }

    public boolean isValidated() { return isValidated; }
    public void setValidated(boolean validated) { isValidated = validated; }

    public int getTotalFieldsDetected() { return totalFieldsDetected; }
    public void setTotalFieldsDetected(int totalFieldsDetected) { this.totalFieldsDetected = totalFieldsDetected; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    /**
     * Converts this DTO to a Map<String, Object> suitable for passing into the AI prompt.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("email", email);
        m.put("phone", phone);
        m.put("linkedin", linkedin);
        m.put("github", github);
        m.put("portfolio", portfolio);
        m.put("education", education);
        m.put("degree", degree);
        m.put("specialization", specialization);
        m.put("institution", institution);
        m.put("cgpa", cgpa);
        m.put("experience", experience);
        m.put("internships", internships);
        m.put("projects", projects);
        m.put("technicalSkills", technicalSkills);
        m.put("softSkills", softSkills);
        m.put("programmingLanguages", programmingLanguages);
        m.put("frameworks", frameworks);
        m.put("libraries", libraries);
        m.put("databases", databases);
        m.put("cloudPlatforms", cloudPlatforms);
        m.put("tools", tools);
        m.put("allDetectedSkills", allDetectedSkills);
        m.put("certifications", certifications);
        m.put("achievements", achievements);
        m.put("researchPapers", researchPapers);
        m.put("volunteerWork", volunteerWork);
        m.put("languagesSpoken", languagesSpoken);
        m.put("careerObjective", careerObjective);
        m.put("isValidated", isValidated);
        m.put("totalFieldsDetected", totalFieldsDetected);
        m.put("parserVersion", parserVersion);
        return m;
    }
}
