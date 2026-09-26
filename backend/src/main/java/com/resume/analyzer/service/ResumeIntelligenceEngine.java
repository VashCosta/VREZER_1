package com.resume.analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Candidate Profile Engine & Dynamic Query Generator.
 * Extracts candidate attributes from raw/parsed resume text and generates
 * candidate-specific weighted search queries.
 */
@Service
public class ResumeIntelligenceEngine {

    @Autowired
    private ResumeParserService resumeParserService;

    // Vocabularies for feature extraction
    private static final List<String> DATABASES = Arrays.asList(
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch", "Cassandra",
            "SQLite", "Oracle", "DynamoDB", "MariaDB", "Supabase", "Pinecone", "ChromaDB",
            "Neo4j", "CouchDB", "CockroachDB", "Snowflake", "BigQuery", "Redshift"
    );

    private static final List<String> CLOUD_PLATFORMS = Arrays.asList(
            "AWS", "Azure", "GCP", "Google Cloud", "Heroku", "Vercel", "Netlify",
            "DigitalOcean", "Cloudflare", "OpenStack", "IBM Cloud"
    );

    private static final List<String> DEVOPS_TOOLS = Arrays.asList(
            "Docker", "Kubernetes", "Helm", "Terraform", "Ansible", "Jenkins",
            "GitHub Actions", "CI/CD", "GitLab CI", "CircleCI", "ArgoCD", "Prometheus",
            "Grafana", "Nagios", "Datadog"
    );

    private static final List<String> AI_TOOLS = Arrays.asList(
            "PyTorch", "TensorFlow", "Keras", "scikit-learn", "OpenCV", "Hugging Face",
            "LangChain", "LlamaIndex", "RAG", "Vector Database", "Generative AI", "LLM",
            "OpenAI", "Gemini", "Anthropic Claude", "Midjourney", "Stable Diffusion"
    );

    /**
     * Extracts structured candidate profile attributes strictly from evidence.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractCandidateProfile(String resumeText, Map<String, Object> baseParsed) {
        Map<String, Object> profile = new LinkedHashMap<>();
        if (baseParsed == null || baseParsed.isEmpty()) {
            baseParsed = resumeParserService.parseResumeText(resumeText);
        }

        String text = resumeText == null ? "" : resumeText;
        String lower = text.toLowerCase();
        String candidateName = String.valueOf(baseParsed.getOrDefault("name", "Candidate"));

        // 1. Education
        List<Map<String, String>> eduList = (List<Map<String, String>>) baseParsed.getOrDefault("education", List.of());
        String degree = "";
        String specialization = "";
        String university = "";
        String eduSummary = "Degree Holder";

        if (!eduList.isEmpty()) {
            String rawEdu = eduList.get(0).getOrDefault("degree", "");
            university = eduList.get(0).getOrDefault("institution", extractUniversity(text));
            degree = extractDegreeTitle(rawEdu);
            specialization = extractSpecialization(rawEdu, text);
            eduSummary = rawEdu;
        } else {
            degree = extractDegreeTitle(text);
            specialization = extractSpecialization(text, text);
            university = extractUniversity(text);
        }
        String cgpa = String.valueOf(baseParsed.getOrDefault("cgpa", ""));

        // 2. Skills
        List<String> progLangs = (List<String>) baseParsed.getOrDefault("programmingLanguages", List.of());
        List<String> frameworks = (List<String>) baseParsed.getOrDefault("frameworks", List.of());
        List<String> softSkills = (List<String>) baseParsed.getOrDefault("softSkills", List.of());
        List<String> techSkills = (List<String>) baseParsed.getOrDefault("allDetectedSkills", List.of());

        List<String> databases = matchVocab(lower, DATABASES);
        List<String> cloudPlatforms = matchVocab(lower, CLOUD_PLATFORMS);
        List<String> devopsTools = matchVocab(lower, DEVOPS_TOOLS);
        List<String> aiTools = matchVocab(lower, AI_TOOLS);

        // 3. Experience & Date-Based Calculation
        List<Map<String, String>> expList = (List<Map<String, String>>) baseParsed.getOrDefault("experience", List.of());
        List<Map<String, String>> internships = (List<Map<String, String>>) baseParsed.getOrDefault("internships", List.of());
        
        double yearsOfExp = calculateYearsOfExperienceFromDates(expList, internships, text);
        String experienceLevel = determineExperienceLevel(yearsOfExp, expList, internships, text);
        
        String experienceSummary;
        if (yearsOfExp <= 0.0) {
            if (internships != null && !internships.isEmpty()) {
                experienceSummary = "Internship Experience";
            } else {
                experienceSummary = "Fresher / Entry Level";
            }
        } else {
            experienceSummary = String.format(Locale.US, "%.1f Years Experience", yearsOfExp);
        }

        List<String> internshipTitles = internships.stream()
                .map(i -> i.getOrDefault("role", i.getOrDefault("company", "Intern")))
                .filter(s -> !s.isEmpty()).toList();

        // 4. Projects
        List<Map<String, String>> projects = (List<Map<String, String>>) baseParsed.getOrDefault("projects", List.of());
        List<String> projectTitles = projects.stream()
                .map(p -> p.getOrDefault("title", ""))
                .filter(s -> !s.isEmpty()).toList();
        List<String> projectTech = projects.stream()
                .map(p -> p.getOrDefault("tech", ""))
                .filter(s -> !s.isEmpty()).toList();

        // 5. Certifications & Achievements
        List<String> certifications = (List<String>) baseParsed.getOrDefault("certifications", List.of());
        List<String> achievements = (List<String>) baseParsed.getOrDefault("achievements", List.of());

        // 6. Online links
        String github = String.valueOf(baseParsed.getOrDefault("github", ""));
        String linkedin = String.valueOf(baseParsed.getOrDefault("linkedin", ""));
        String portfolio = extractPortfolioUrl(text);

        // 7. Domain Detection (Dual Primary + Secondary Domains)
        Map<String, String> domainsMap = detectPrimaryAndSecondaryDomains(text, techSkills, progLangs, frameworks, degree, specialization);
        String primaryDomain = domainsMap.get("primaryDomain");
        String secondaryDomain = domainsMap.get("secondaryDomain");
        String careerDomainCombined = domainsMap.get("primaryDomain");
        
        // 8. Target Roles & Objective
        String careerObjective = extractCareerObjective(text);
        List<String> targetRoles = inferTargetRoles(primaryDomain, techSkills, progLangs, experienceLevel, text);
        String primaryTargetRole = targetRoles.isEmpty() ? primaryDomain + " Specialist" : targetRoles.get(0);

        // 9. Location & Work Preference
        String preferredLocation = extractPreferredLocation(text);
        String preferredWorkMode = lower.contains("remote") || lower.contains("work from home") ? "Remote" :
                                   lower.contains("hybrid") ? "Hybrid" : "Onsite";

        // 10. LPA Target Salary Calculation in INR & USD (Domain & Experience Calibrated)
        String lpaRange;
        String usdRange;
        String pDomLower = ((primaryDomain != null ? primaryDomain : "") + " " + (secondaryDomain != null ? secondaryDomain : "") + " " + (careerDomainCombined != null ? careerDomainCombined : "") + " " + techSkills.toString()).toLowerCase();
        boolean isHighTech = pDomLower.contains("backend") || pDomLower.contains("frontend") || pDomLower.contains("full-stack") || pDomLower.contains("cloud") || pDomLower.contains("devops") || pDomLower.contains("ai") || pDomLower.contains("machine learning") || pDomLower.contains("software") || pDomLower.contains("java") || pDomLower.contains("python");
        boolean isMarketing = pDomLower.contains("marketing") || pDomLower.contains("seo") || pDomLower.contains("sem") || pDomLower.contains("growth") || pDomLower.contains("google ads");
        boolean isCoreEng = pDomLower.contains("mechanical") || pDomLower.contains("civil") || pDomLower.contains("electrical") || pDomLower.contains("cad");

        if (yearsOfExp < 1.0) {
            if (isHighTech) {
                lpaRange = "6 - 12 LPA";
                usdRange = "$ 8K - 15K USD";
            } else if (isMarketing) {
                lpaRange = "4 - 8 LPA";
                usdRange = "$ 5K - 10K USD";
            } else if (isCoreEng) {
                lpaRange = "4 - 7 LPA";
                usdRange = "$ 5K - 9K USD";
            } else {
                lpaRange = "5 - 9 LPA";
                usdRange = "$ 6K - 11K USD";
            }
        } else if (yearsOfExp < 3.0) {
            if (isHighTech) {
                lpaRange = "10 - 18 LPA";
                usdRange = "$ 13K - 23K USD";
            } else if (isMarketing) {
                lpaRange = "6 - 12 LPA";
                usdRange = "$ 8K - 15K USD";
            } else if (isCoreEng) {
                lpaRange = "6 - 11 LPA";
                usdRange = "$ 7K - 14K USD";
            } else {
                lpaRange = "8 - 14 LPA";
                usdRange = "$ 10K - 18K USD";
            }
        } else if (yearsOfExp < 6.0) {
            if (isHighTech) {
                lpaRange = "18 - 30 LPA";
                usdRange = "$ 23K - 38K USD";
            } else if (isMarketing) {
                lpaRange = "10 - 18 LPA";
                usdRange = "$ 13K - 23K USD";
            } else if (isCoreEng) {
                lpaRange = "9 - 16 LPA";
                usdRange = "$ 11K - 20K USD";
            } else {
                lpaRange = "14 - 24 LPA";
                usdRange = "$ 18K - 30K USD";
            }
        } else if (yearsOfExp < 10.0) {
            if (isHighTech) {
                lpaRange = "30 - 55 LPA";
                usdRange = "$ 38K - 70K USD";
            } else if (isMarketing) {
                lpaRange = "18 - 32 LPA";
                usdRange = "$ 23K - 40K USD";
            } else if (isCoreEng) {
                lpaRange = "15 - 28 LPA";
                usdRange = "$ 19K - 35K USD";
            } else {
                lpaRange = "24 - 42 LPA";
                usdRange = "$ 30K - 52K USD";
            }
        } else {
            if (isHighTech) {
                lpaRange = "55 - 90 LPA";
                usdRange = "$ 70K - 115K USD";
            } else if (isMarketing) {
                lpaRange = "32 - 60 LPA";
                usdRange = "$ 40K - 75K USD";
            } else {
                lpaRange = "28 - 50 LPA";
                usdRange = "$ 35K - 63K USD";
            }
        }

        profile.put("name", String.valueOf(baseParsed.getOrDefault("name", "")));
        profile.put("email", String.valueOf(baseParsed.getOrDefault("email", "")));
        profile.put("phone", String.valueOf(baseParsed.getOrDefault("phone", "")));
        profile.put("location", preferredLocation);
        profile.put("preferredLocation", preferredLocation);
        profile.put("degree", degree);
        profile.put("specialization", specialization);
        profile.put("education", eduSummary);
        profile.put("cgpa", cgpa);
        profile.put("primaryDomain", primaryDomain);
        profile.put("secondaryDomain", secondaryDomain);
        profile.put("careerDomain", careerDomainCombined);
        profile.put("expectedLpaRange", lpaRange);
        profile.put("salaryUsd", usdRange);
        profile.put("experience", experienceSummary);
        profile.put("yearsOfExperience", yearsOfExp);
        profile.put("experienceLevel", experienceLevel);
        profile.put("internships", internships);
        profile.put("internshipTitles", internshipTitles);
        profile.put("projects", projects);
        profile.put("projectTitles", projectTitles);
        profile.put("projectTechnologies", projectTech);
        profile.put("programmingLanguages", progLangs);
        profile.put("frameworks", frameworks);
        profile.put("databases", databases);
        profile.put("cloudPlatforms", cloudPlatforms);
        profile.put("devopsTools", devopsTools);
        profile.put("aiTools", aiTools);
        profile.put("technicalSkills", techSkills);
        profile.put("softSkills", softSkills);
        profile.put("achievements", achievements);
        profile.put("certifications", certifications);
        profile.put("portfolio", portfolio);
        profile.put("github", github);
        profile.put("linkedin", linkedin);
        profile.put("careerObjective", careerObjective);
        
        String professionalSummary = String.valueOf(baseParsed.getOrDefault("professionalSummary", ""));
        if (professionalSummary.isEmpty() || professionalSummary.equals("null") || professionalSummary.length() < 25) {
            professionalSummary = candidateName + " is an aspiring " + primaryTargetRole
                + " with core competencies in " + String.join(", ", techSkills.subList(0, Math.min(5, techSkills.size())))
                + ". Demonstrated practical execution across " + careerDomainCombined + " projects and applied system development.";
        }
        profile.put("professionalSummary", professionalSummary);
        profile.put("transferableSkills", (softSkills != null && !softSkills.isEmpty()) ? softSkills : List.of("Problem Solving", "Analytical Thinking", "Teamwork", "Quick Learner", "Creative Content"));
        
        profile.put("preferredWorkMode", preferredWorkMode);
        profile.put("expectedSalary", lpaRange);
        profile.put("targetRoles", targetRoles);
        profile.put("targetJobRole", primaryTargetRole);

        return profile;
    }

    /**
     * Generates a candidate-specific search query string based on candidate's actual domain, skills, role, and level.
     */
    @SuppressWarnings("unchecked")
    public String generateBooleanSearchQuery(Map<String, Object> profile) {
        String domain = String.valueOf(profile.getOrDefault("careerDomain", "Software Development"));
        String targetRole = String.valueOf(profile.getOrDefault("targetJobRole", ""));
        List<String> techSkills = (List<String>) profile.getOrDefault("technicalSkills", List.of());
        String expLevel = String.valueOf(profile.getOrDefault("experienceLevel", "FRESHER"));
        String location = String.valueOf(profile.getOrDefault("preferredLocation", ""));

        StringBuilder query = new StringBuilder();
        if (targetRole != null && !targetRole.trim().isEmpty() && !targetRole.equalsIgnoreCase("Software Development Engineer")) {
            query.append(targetRole.trim());
        } else if (domain != null && !domain.trim().isEmpty()) {
            query.append(domain.trim());
        }

        if (techSkills != null && !techSkills.isEmpty()) {
            int added = 0;
            for (String s : techSkills) {
                if (!query.toString().toLowerCase().contains(s.toLowerCase())) {
                    query.append(" ").append(s);
                    added++;
                    if (added >= 3) break;
                }
            }
        }

        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Remote")) {
            query.append(" ").append(location.trim());
        }

        return query.toString().trim();
    }

    /**
     * Accurately parses date ranges from genuine work experience entries without adding education degree or schooling dates.
     */
    public double calculateYearsOfExperienceFromDates(List<Map<String, String>> expList, List<Map<String, String>> internships, String fullText) {
        int currentYear = LocalDate.now().getYear();
        String lowerFull = fullText.toLowerCase();

        boolean isStudentOrFresher = lowerFull.contains("pursuing") || lowerFull.contains("student") 
                || lowerFull.contains("currently in ") || lowerFull.contains("fresher")
                || lowerFull.contains("expected graduation");

        if (expList == null || expList.isEmpty()) {
            return 0.0;
        }

        // Check if all experience items are internships / trainee roles
        boolean allInternships = true;
        List<Map<String, String>> fullTimeRoles = new ArrayList<>();
        for (Map<String, String> exp : expList) {
            String role = exp.getOrDefault("role", "").toLowerCase();
            String comp = exp.getOrDefault("company", "").toLowerCase();
            String desc = exp.getOrDefault("description", "").toLowerCase();
            if (role.contains("intern") || comp.contains("intern") || desc.contains("intern") || role.contains("trainee")) {
                // internship
            } else {
                allInternships = false;
                fullTimeRoles.add(exp);
            }
        }

        if (isStudentOrFresher || allInternships || fullTimeRoles.isEmpty()) {
            return 0.0;
        }

        // Calculate years only from genuine full-time roles
        Set<Integer> activeYears = new HashSet<>();
        Pattern yearRangePattern = Pattern.compile("(?i)\\b(20\\d{2}|19\\d{2})\\s*[-–—/to]+\\s*(20\\d{2}|19\\d{2}|present|current|now)\\b");

        for (Map<String, String> role : fullTimeRoles) {
            String roleText = role.getOrDefault("duration", "") + " " + role.getOrDefault("description", "");
            Matcher m = yearRangePattern.matcher(roleText);
            while (m.find()) {
                try {
                    int start = Integer.parseInt(m.group(1));
                    String endStr = m.group(2).toLowerCase();
                    int end = (endStr.contains("present") || endStr.contains("current") || endStr.contains("now"))
                            ? currentYear
                            : Integer.parseInt(endStr);
                    if (start <= end && start >= 1995 && end <= currentYear + 1) {
                        for (int y = start; y < end; y++) {
                            activeYears.add(y);
                        }
                        if (start == end) {
                            activeYears.add(start);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!activeYears.isEmpty()) {
            return Math.min(30.0, (double) activeYears.size());
        }

        return Math.min(fullTimeRoles.size() * 1.5, 10.0);
    }

    public String determineExperienceLevel(double years, List<Map<String, String>> expList, List<Map<String, String>> internships, String fullText) {
        String lower = fullText.toLowerCase();
        boolean isStudent = lower.contains("student") || lower.contains("pursuing") || lower.contains("currently in ") || lower.contains("fresher");
        if (isStudent || years <= 0.0) {
            return "FRESHER";
        }
        if (years >= 8.0 || lower.contains("lead engineer") || lower.contains("principal engineer") || lower.contains("director") || lower.contains("vp ")) {
            return "LEAD";
        }
        if (years >= 5.0 || lower.contains("senior software") || lower.contains("senior developer") || lower.contains("senior analyst") || lower.contains("sr. ")) {
            return "SENIOR";
        }
        if (years >= 2.0) {
            return "MID_LEVEL";
        }
        if (years >= 1.0) {
            return "JUNIOR";
        }
        return "FRESHER";
    }

    /**
     * Dual-Domain Detection — returns Primary and Secondary domains.
     */
    /**
     * Deterministic career-domain classifier.
     *
     * The primary domain is derived only from resume evidence. LLM output must
     * never be allowed to replace this value because small model/provider
     * differences can otherwise make the same resume jump between domains.
     *
     * Evidence hierarchy:
     *   1) exact domain phrases in resume text
     *   2) extracted technical/domain skills
     *   3) explicit specialization/degree context
     *
     * Generic degree text is not duplicated into the score, so an academic
     * stream such as "AI & Data Science" cannot overpower a clearly evidenced
     * career direction such as Digital Marketing.
     */
    public Map<String, String> detectPrimaryAndSecondaryDomains(
            String fullText,
            List<String> skills,
            List<String> progLangs,
            List<String> frameworks,
            String degree,
            String spec) {

        String resume = fullText == null ? "" : fullText.toLowerCase(Locale.ROOT);
        String skillsText = skills == null ? "" : String.join(" ", skills).toLowerCase(Locale.ROOT);

        // Do not double-count degree/specification that is already present in the
        // resume text. They are retained only as a small tie-break signal.
        String combined = (resume + " " + skillsText).trim();

        Map<String, Integer> scores = new LinkedHashMap<>();

        scores.put("Mechanical Engineering", scoreDomain(combined, new String[]{
                "mechanical engineering", "mechanical design", "solidworks", "ansys", "fea",
                "cad", "catia", "thermodynamics", "fluid mechanics", "gd&t",
                "manufacturing", "mechatronics", "hydraulics", "hvac", "cnc"
        }));

        scores.put("Civil Engineering", scoreDomain(combined, new String[]{
                "civil engineering", "structural engineering", "staad", "staad.pro", "revit",
                "bim", "primavera", "surveying", "concrete design", "structural analysis",
                "construction management", "geotechnical"
        }));

        scores.put("Electrical & Electronics", scoreDomain(combined, new String[]{
                "electrical engineering", "electronics engineering", "embedded c", "plc",
                "scada", "microcontrollers", "circuit design", "vlsi", "verilog", "vhdl",
                "pcb design", "matlab", "simulink", "iot", "fpga"
        }));

        scores.put("Finance & Accounting", scoreDomain(combined, new String[]{
                "finance", "accounting", "chartered accountant", "valuation", "dcf", "lbo",
                "audit", "taxation", "gst", "tally", "equity research", "financial modeling",
                "corporate finance", "ledger", "banking"
        }));

        scores.put("Human Resources", scoreDomain(combined, new String[]{
                "human resources", "hrbp", "talent acquisition", "recruiting", "onboarding",
                "hris", "payroll", "employee relations", "people analytics", "labor laws"
        }));

        scores.put("Sales & Business Development", scoreDomain(combined, new String[]{
                "sales", "business development", "b2b sales", "account management",
                "lead generation", "crm", "salesforce", "pipeline management", "inside sales"
        }));

        scores.put("Digital Marketing", scoreDomain(combined, new String[]{
                "digital marketing", "search engine optimization", "seo", "sem", "google ads",
                "meta ads", "facebook ads", "social media marketing", "content marketing",
                "google analytics", "ga4", "hubspot", "marketo", "growth marketing",
                "performance marketing", "copywriting", "a/b testing", "conversion rate",
                "cro", "campaign", "email marketing", "adwords", "content strategy",
                "growth hacking", "ppc"
        }));

        scores.put("UI/UX Design", scoreDomain(combined, new String[]{
                "ui/ux", "figma", "adobe xd", "sketch", "invision", "wireframing",
                "prototyping", "user research", "usability testing", "design system",
                "user experience", "user interface", "product design", "interaction design"
        }));

        scores.put("AI & Machine Learning", scoreDomain(combined, new String[]{
                "machine learning", "deep learning", "pytorch", "tensorflow", "nlp",
                "natural language processing", "computer vision", "large language model",
                "llm", "transformers", "hugging face", "langchain", "rag", "vector database",
                "qdrant", "scikit-learn", "data science", "keras", "generative ai", "mlops"
        }));

        scores.put("Data & Business Analytics", scoreDomain(combined, new String[]{
                "power bi", "tableau", "excel", "dax", "power query", "business intelligence",
                "sql queries", "data analysis", "data analyst", "data visualization",
                "business analyst", "etl", "analytics"
        }));

        scores.put("Cybersecurity", scoreDomain(combined, new String[]{
                "cybersecurity", "penetration testing", "ethical hacking", "soc analyst",
                "siem", "incident response", "vulnerability assessment", "wireshark",
                "metasploit", "cryptography", "zero trust", "security engineer"
        }));

        scores.put("Backend Development", scoreDomain(combined, new String[]{
                "backend development", "spring boot", "spring mvc", "hibernate", "jpa",
                "rest api", "microservices", "java backend", "django", "fastapi",
                "express.js", "node.js backend", "golang", "grpc", "postgresql", "mysql",
                "redis", "kafka", "java"
        }));

        scores.put("Cloud & DevOps", scoreDomain(combined, new String[]{
                "devops", "kubernetes", "docker", "terraform", "ansible", "jenkins",
                "ci/cd", "aws", "azure", "gcp", "helm", "argocd", "site reliability",
                "sre", "github actions"
        }));

        scores.put("Frontend Development", scoreDomain(combined, new String[]{
                "frontend development", "react", "react.js", "vue", "vue.js", "angular",
                "next.js", "tailwind", "tailwindcss", "html5", "css3", "typescript",
                "javascript", "redux"
        }));

        scores.put("Data Engineering", scoreDomain(combined, new String[]{
                "data engineer", "spark", "hadoop", "kafka", "airflow", "dbt", "snowflake",
                "bigquery", "etl", "pipeline", "databricks", "redshift"
        }));

        scores.put("Mobile Development", scoreDomain(combined, new String[]{
                "android", "ios", "flutter", "react native", "kotlin", "swift",
                "mobile app", "xcode", "play store", "app store"
        }));

        scores.put("Blockchain & Web3", scoreDomain(combined, new String[]{
                "blockchain", "solidity", "web3", "ethereum", "smart contract", "defi",
                "nft", "metamask", "truffle", "hardhat"
        }));

        scores.put("Healthcare", scoreDomain(combined, new String[]{
                "patient care", "nursing", "clinical", "pharmacology", "ehr", "emr",
                "diagnosis", "triage", "physician", "hospital", "mbbs", "bpharm"
        }));

        scores.put("Education & Teaching", scoreDomain(combined, new String[]{
                "teacher", "curriculum", "lesson plan", "classroom", "pedagogy", "lms",
                "e-learning", "assessment", "professor", "instructor"
        }));

        scores.put("Legal", scoreDomain(combined, new String[]{
                "legal", "attorney", "lawyer", "contracts", "compliance", "litigation",
                "corporate law", "intellectual property", "paralegal", "llb"
        }));

        scores.put("Supply Chain & Logistics", scoreDomain(combined, new String[]{
                "supply chain", "logistics", "procurement", "inventory", "warehouse",
                "erp", "sap", "vendor management", "six sigma", "lean"
        }));

        // Skill-list signals are stronger than incidental mentions in prose.
        if (skills != null && !skills.isEmpty()) {
            boostDomainBySkills(scores, skillsText);
        }

        // Explicit career-intent wording gets a deterministic extra boost.
        applyCareerIntentBoosts(scores, resume);

        // Specialization is a tie-breaker only; it cannot dominate resume evidence.
        if (spec != null && !spec.isBlank()) {
            applyLowWeightContextBoost(scores, spec.toLowerCase(Locale.ROOT));
        }
        if (degree != null && !degree.isBlank()) {
            applyLowWeightContextBoost(scores, degree.toLowerCase(Locale.ROOT));
        }

        List<Map.Entry<String, Integer>> sorted = scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> {
                    int byScore = Integer.compare(b.getValue(), a.getValue());
                    return byScore != 0 ? byScore : a.getKey().compareToIgnoreCase(b.getKey());
                })
                .toList();

        String primary = sorted.isEmpty() ? "Professional & Domain Specialist" : sorted.get(0).getKey();

        String secondary = "";
        if (sorted.size() > 1) {
            Map.Entry<String, Integer> runnerUp = sorted.get(1);
            // Only expose a secondary domain when the runner-up has real evidence
            // and is not merely tied by one incidental keyword.
            int primaryScore = sorted.get(0).getValue();
            if (runnerUp.getValue() >= 4 && runnerUp.getValue() >= Math.max(4, primaryScore - 6)) {
                secondary = runnerUp.getKey();
            }
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("primaryDomain", primary);
        result.put("secondaryDomain", secondary);
        // IMPORTANT: careerDomain is the canonical PRIMARY domain consumed by
        // job retrieval and displayed on the dashboard. Secondary stays separate.
        result.put("careerDomain", primary);
        return result;
    }

    private void boostDomainBySkills(Map<String, Integer> scores, String skillsText) {
        if (skillsText == null || skillsText.isBlank()) return;
        Map<String, String[]> skillAnchors = new LinkedHashMap<>();
        skillAnchors.put("Digital Marketing", new String[]{"seo","sem","google ads","meta ads","social media marketing","content marketing","ga4","google analytics","performance marketing","ppc"});
        skillAnchors.put("AI & Machine Learning", new String[]{"machine learning","deep learning","pytorch","tensorflow","nlp","computer vision","llm","langchain","rag","generative ai","scikit-learn"});
        skillAnchors.put("Backend Development", new String[]{"spring boot","rest api","microservices","java backend","hibernate","jpa","postgresql","redis","kafka"});
        skillAnchors.put("Frontend Development", new String[]{"react","react.js","vue","angular","next.js","tailwind","typescript","javascript"});
        skillAnchors.put("Cloud & DevOps", new String[]{"aws","azure","gcp","docker","kubernetes","terraform","jenkins","ci/cd"});
        skillAnchors.put("Data & Business Analytics", new String[]{"power bi","tableau","excel","dax","data analysis","business analyst"});
        skillAnchors.put("Cybersecurity", new String[]{"cybersecurity","penetration testing","ethical hacking","siem","soc","wireshark","metasploit"});
        skillAnchors.put("UI/UX Design", new String[]{"figma","adobe xd","sketch","wireframing","prototyping","user research"});
        for (Map.Entry<String, String[]> entry : skillAnchors.entrySet()) {
            int hits = 0;
            for (String anchor : entry.getValue()) {
                if (containsWholePhrase(skillsText, anchor)) hits++;
            }
            if (hits > 0) scores.merge(entry.getKey(), hits * 4, Integer::sum);
        }
    }

    private void applyCareerIntentBoosts(Map<String, Integer> scores, String resume) {
        String[] digitalMarketing = {"digital marketing", "performance marketing", "seo specialist", "seo executive", "google ads", "meta ads", "social media marketing", "growth marketing", "marketing specialist", "marketing executive"};
        String[] backend = {"backend developer", "backend engineer", "java developer", "spring boot developer", "api engineer", "microservices engineer"};
        String[] ai = {"machine learning engineer", "ai engineer", "ai/ml engineer", "ml engineer", "data scientist", "ai research scientist"};
        String[] frontend = {"frontend developer", "frontend engineer", "react developer", "ui developer"};
        String[] devops = {"devops engineer", "cloud engineer", "site reliability engineer", "sre", "cloud architect"};
        String[] analytics = {"data analyst", "business analyst", "bi analyst", "business intelligence analyst"};
        String[] design = {"ui/ux designer", "ux designer", "product designer", "ui designer"};
        String[] security = {"cybersecurity analyst", "security analyst", "soc analyst", "security engineer", "penetration tester"};
        String[] finance = {"financial analyst", "accountant", "auditor", "investment analyst", "finance analyst"};
        String[] sales = {"sales executive", "sales manager", "business development executive", "account manager"};

        boostForIntent(scores, "Digital Marketing", resume, digitalMarketing);
        boostForIntent(scores, "Backend Development", resume, backend);
        boostForIntent(scores, "AI & Machine Learning", resume, ai);
        boostForIntent(scores, "Frontend Development", resume, frontend);
        boostForIntent(scores, "Cloud & DevOps", resume, devops);
        boostForIntent(scores, "Data & Business Analytics", resume, analytics);
        boostForIntent(scores, "UI/UX Design", resume, design);
        boostForIntent(scores, "Cybersecurity", resume, security);
        boostForIntent(scores, "Finance & Accounting", resume, finance);
        boostForIntent(scores, "Sales & Business Development", resume, sales);
    }

    private void boostForIntent(Map<String, Integer> scores, String domain, String resume, String[] phrases) {
        int hits = 0;
        for (String phrase : phrases) {
            if (containsWholePhrase(resume, phrase)) hits++;
        }
        if (hits > 0) scores.merge(domain, hits * 7, Integer::sum);
    }

    private void applyLowWeightContextBoost(Map<String, Integer> scores, String context) {
        Map<String, String[]> contextAnchors = new LinkedHashMap<>();
        contextAnchors.put("Digital Marketing", new String[]{"marketing","seo"});
        contextAnchors.put("AI & Machine Learning", new String[]{"artificial intelligence","machine learning","data science"});
        contextAnchors.put("Backend Development", new String[]{"backend","software engineering"});
        contextAnchors.put("Frontend Development", new String[]{"frontend","web development"});
        contextAnchors.put("Cloud & DevOps", new String[]{"cloud","devops"});
        contextAnchors.put("Finance & Accounting", new String[]{"finance","accounting"});
        contextAnchors.put("Mechanical Engineering", new String[]{"mechanical engineering"});
        contextAnchors.put("Civil Engineering", new String[]{"civil engineering"});
        contextAnchors.put("Electrical & Electronics", new String[]{"electrical engineering","electronics engineering"});
        for (Map.Entry<String, String[]> entry : contextAnchors.entrySet()) {
            for (String anchor : entry.getValue()) {
                if (containsWholePhrase(context, anchor)) {
                    scores.merge(entry.getKey(), 2, Integer::sum);
                }
            }
        }
    }

    private boolean containsWholePhrase(String text, String phrase) {
        if (text == null || text.isBlank() || phrase == null || phrase.isBlank()) return false;
        String normalized = " " + text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#./&-]+", " ").trim() + " ";
        String target = " " + phrase.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#./&-]+", " ").trim() + " ";
        return normalized.contains(target);
    }

    public String detectPrimaryCareerDomain(String fullText, List<String> skills, List<String> progLangs, List<String> frameworks, String degree, String spec) {
        return detectPrimaryAndSecondaryDomains(fullText, skills, progLangs, frameworks, degree, spec).get("primaryDomain");
    }

    private int scoreDomain(String text, String[] keywords) {
        int score = 0;
        for (String kw : keywords) {
            Pattern p = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9+#])" + Pattern.quote(kw) + "(?=[^a-zA-Z0-9+#]|$)");
            Matcher m = p.matcher(text);
            int count = 0;
            while (m.find()) {
                count++;
            }
            if (count > 0) {
                score += (kw.length() > 5 ? 3 : 2) + Math.min(count, 3);
            }
        }
        return score;
    }

    public List<String> inferTargetRoles(String domain, List<String> skills, List<String> progLangs, String expLevel, String text) {
        List<String> roles = new ArrayList<>();
        String d = domain.toLowerCase();
        boolean isSenior = "SENIOR".equals(expLevel) || "LEAD".equals(expLevel);
        boolean isFresher = "FRESHER".equals(expLevel) || "JUNIOR".equals(expLevel);

        if (d.contains("digital marketing")) {
            roles.add(isSenior ? "Lead Performance Marketing Manager" : isFresher ? "Digital Marketing Executive" : "Performance Marketing Specialist");
            roles.add("SEO & Growth Specialist");
            roles.add("Google & Meta Ads Manager");
        } else if (d.contains("backend")) {
            String lang = (progLangs != null && !progLangs.isEmpty()) ? progLangs.get(0) : "Java";
            roles.add(isSenior ? "Senior " + lang + " Backend Engineer" : isFresher ? "Junior " + lang + " Developer" : lang + " Backend Engineer");
            roles.add("Microservices & API Engineer");
            roles.add("Backend Systems Developer");
        } else if (d.contains("ai & machine learning") || d.contains("ai")) {
            roles.add(isSenior ? "Senior AI / Machine Learning Engineer" : isFresher ? "Junior ML Engineer" : "AI / Machine Learning Engineer");
            roles.add("Applied NLP / Computer Vision Engineer");
            roles.add("MLOps & RAG Pipeline Specialist");
        } else if (d.contains("analytics") || d.contains("data")) {
            roles.add(isSenior ? "Senior Data Analyst & BI Specialist" : isFresher ? "Associate Data Analyst" : "Data & BI Analyst");
            roles.add("Power BI / Tableau Developer");
            roles.add("Business Analytics Specialist");
        } else if (d.contains("ui/ux") || d.contains("design")) {
            roles.add(isSenior ? "Senior UI/UX & Product Designer" : isFresher ? "Associate UI/UX Designer" : "UI/UX Product Designer");
            roles.add("User Experience Researcher");
            roles.add("Design System & Interaction Specialist");
        } else if (d.contains("devops") || d.contains("cloud")) {
            roles.add(isSenior ? "Senior Cloud DevOps / SRE Lead" : isFresher ? "Junior Cloud Engineer" : "Cloud & DevOps Engineer");
            roles.add("Site Reliability Engineer (SRE)");
            roles.add("Kubernetes Platform Engineer");
        } else if (d.contains("frontend")) {
            roles.add(isSenior ? "Senior Frontend Engineer" : isFresher ? "Junior Frontend Developer" : "Frontend Web Developer");
            roles.add("React / TypeScript UI Engineer");
        } else if (d.contains("finance")) {
            roles.add(isSenior ? "Senior Financial Analyst" : isFresher ? "Junior Financial Analyst" : "Financial Analyst");
            roles.add("Valuation & Audit Specialist");
        } else if (d.contains("human resources")) {
            roles.add(isSenior ? "Senior HR Business Partner" : isFresher ? "HR & Talent Associate" : "Talent Acquisition Specialist");
        } else if (d.contains("mechanical")) {
            roles.add(isSenior ? "Senior Mechanical Design Engineer" : isFresher ? "Junior CAD / Mechanical Engineer" : "Mechanical Design Engineer");
        } else {
            roles.add(isSenior ? "Senior Software Engineer" : isFresher ? "Associate Software Engineer" : "Software Engineer");
        }

        return roles;
    }

    private List<String> matchVocab(String lowerText, List<String> vocab) {
        List<String> matches = new ArrayList<>();
        String cleanText = " " + lowerText.replaceAll("[^a-zA-Z0-9+#.#-]", " ") + " ";
        for (String item : vocab) {
            Pattern p = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9+#])" + Pattern.quote(item.trim()) + "(?=[^a-zA-Z0-9+#]|$)");
            if (p.matcher(cleanText).find()) {
                matches.add(item);
            }
        }
        return matches;
    }

    private String extractUniversity(String text) {
        Matcher m = Pattern.compile("(?i)(university|institute|college|academy|iit|nit|bits|iiit|mit|stanford|harvard|nyu|delhi university)\\s*(?:of\\s+[a-zA-Z\\s]{2,30})?", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group().trim();
        return "Recognized University";
    }

    private String extractDegreeTitle(String text) {
        Pattern p = Pattern.compile("(?i)(?<![a-zA-Z0-9])(B\\.?Tech|B\\.?E\\.?|M\\.?Tech|M\\.?E\\.?|MBA|BCA|MCA|B\\.?Sc|M\\.?Sc|B\\.?A\\.?|B\\.?Com|Ph\\.?D|Diploma|Chartered Accountant|C\\.A\\.)(?![a-zA-Z0-9])");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1).toUpperCase().replaceAll("\\.", "");
        return "Degree Holder";
    }

    private String extractSpecialization(String degreeText, String fullText) {
        String lower = (degreeText + " " + fullText).toLowerCase();
        if (lower.contains("computer science") || lower.contains("cse")) return "Computer Science";
        if (Pattern.compile("\\binformation technology\\b").matcher(lower).find() || Pattern.compile("\\bit\\b").matcher(lower).find()) return "Information Technology";
        if (lower.contains("artificial intelligence") || lower.contains("machine learning")) return "AI & ML";
        if (lower.contains("electronics") || lower.contains("ece")) return "Electronics & Communication";
        if (lower.contains("mechanical")) return "Mechanical Engineering";
        if (lower.contains("civil")) return "Civil Engineering";
        if (lower.contains("finance") || lower.contains("accounting")) return "Finance";
        if (lower.contains("marketing")) return "Marketing";
        if (lower.contains("human resources") || lower.contains("hr")) return "Human Resources";
        return "General Engineering";
    }

    private String extractPortfolioUrl(String text) {
        Matcher m = Pattern.compile("(https?://)?(www\\.)?[a-zA-Z0-9-]+\\.(dev|io|me|portfolio)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group().trim();
        return "";
    }

    private String extractCareerObjective(String text) {
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String l = lines[i].trim();
            if (l.toLowerCase().contains("objective") || l.toLowerCase().contains("summary")) {
                if (i + 1 < lines.length && !lines[i + 1].trim().isEmpty()) {
                    return lines[i + 1].trim();
                }
            }
        }
        return "";
    }

    private String extractPreferredLocation(String text) {
        String[] cities = {"Bengaluru", "Bangalore", "Hyderabad", "Chennai", "Pune", "Mumbai", "Noida", "Gurgaon", "Gurugram", "Delhi", "Kolkata", "San Francisco", "Seattle", "Austin", "New York", "London"};
        for (String c : cities) {
            if (text.toLowerCase().contains(c.toLowerCase())) return c;
        }
        return "Remote / India";
    }

    private String estimateExpectedSalary(double expYears, String domain) {
        // Removed heuristic salary baselines to avoid fabricating compensation ranges.
        // Salary estimates are produced by SalaryIntelligenceService using market evidence.
        return null;
    }

    private String estimateExpectedSalary(double expYears, String domain, String text, List<String> skills) {
        // Removed hardcoded role/experience/college baselines.
        // SalaryIntelligenceService will provide evidence-anchored salary outputs.
        return null;
    }


    @SuppressWarnings("unchecked")
    public com.resume.analyzer.model.ResumeProfile buildCanonicalResumeProfile(String resumeText, Map<String, Object> baseParsed) {
        Map<String, Object> map = extractCandidateProfile(resumeText, baseParsed);
        com.resume.analyzer.model.ResumeProfile p = new com.resume.analyzer.model.ResumeProfile();
        
        p.setCandidateId("CAND-" + UUID.randomUUID().toString().substring(0, 8));
        p.setAnalysisId("ANALYSIS-" + UUID.randomUUID().toString().substring(0, 8));
        p.setName(String.valueOf(map.getOrDefault("name", "")));
        p.setEmail(String.valueOf(map.getOrDefault("email", "")));
        p.setPhone(String.valueOf(map.getOrDefault("phone", "")));
        p.setLocation(String.valueOf(map.getOrDefault("location", "Not detected")));
        p.setEducation(String.valueOf(map.getOrDefault("education", "Not detected")));
        p.setDegree(String.valueOf(map.getOrDefault("degree", "")));
        p.setSpecialization(String.valueOf(map.getOrDefault("specialization", "")));
        p.setCgpa(String.valueOf(map.getOrDefault("cgpa", "")));
        p.setExperience(String.valueOf(map.getOrDefault("experience", "Fresher / Entry Level")));
        
        Object yrs = map.get("yearsOfExperience");
        double expYears = yrs instanceof Number ? ((Number) yrs).doubleValue() : 0.0;
        p.setExperienceYears(expYears);
        p.setSeniority(String.valueOf(map.getOrDefault("experienceLevel", "FRESHER")));
        
        List<String> techSkills = (List<String>) map.getOrDefault("technicalSkills", new ArrayList<>());
        List<String> verifiedSkills = new ArrayList<>();
        List<String> inferredSkills = new ArrayList<>();
        
        String lowerResume = (resumeText == null ? "" : resumeText).toLowerCase();
        for (String s : techSkills) {
            if (s == null || s.trim().isEmpty()) continue;
            String normalized = normalizeSkill(s.trim());
            if (lowerResume.contains(normalized.toLowerCase())) {
                if (!verifiedSkills.contains(normalized)) verifiedSkills.add(normalized);
            } else {
                if (!inferredSkills.contains(normalized)) inferredSkills.add(normalized);
            }
        }
        
        p.setVerifiedSkills(verifiedSkills);
        p.setInferredSkills(inferredSkills);
        p.setTechnicalSkills(verifiedSkills);
        p.setSkills(verifiedSkills);
        p.setSoftSkills((List<String>) map.getOrDefault("softSkills", new ArrayList<>()));
        p.setProjects((List<Map<String, String>>) map.getOrDefault("projects", new ArrayList<>()));
        p.setCertifications((List<String>) map.getOrDefault("certifications", new ArrayList<>()));
        p.setAchievements((List<String>) map.getOrDefault("achievements", new ArrayList<>()));
        p.setCareerDomain(String.valueOf(map.getOrDefault("careerDomain", "General Professional")));
        p.setTargetRoles((List<String>) map.getOrDefault("targetRoles", new ArrayList<>()));
        p.setPrimaryTargetRole(String.valueOf(map.getOrDefault("targetJobRole", "Specialist")));
        p.setResumeText(resumeText);
        
        return p;
    }

    private String normalizeSkill(String skill) {
        if (skill == null) return "";
        String s = skill.trim();
        if (s.equalsIgnoreCase("java")) return "Java";
        if (s.equalsIgnoreCase("python")) return "Python";
        if (s.equalsIgnoreCase("react") || s.equalsIgnoreCase("reactjs") || s.equalsIgnoreCase("react.js")) return "React";
        if (s.equalsIgnoreCase("spring boot") || s.equalsIgnoreCase("springboot")) return "Spring Boot";
        if (s.equalsIgnoreCase("node") || s.equalsIgnoreCase("nodejs") || s.equalsIgnoreCase("node.js")) return "Node.js";
        if (s.equalsIgnoreCase("aws")) return "AWS";
        if (s.equalsIgnoreCase("docker")) return "Docker";
        if (s.equalsIgnoreCase("kubernetes") || s.equalsIgnoreCase("k8s")) return "Kubernetes";
        if (s.equalsIgnoreCase("sql") || s.equalsIgnoreCase("postgresql") || s.equalsIgnoreCase("postgres")) return "PostgreSQL";
        if (s.equalsIgnoreCase("seo")) return "SEO";
        if (s.equalsIgnoreCase("ga4") || s.equalsIgnoreCase("google analytics")) return "GA4";
        if (s.equalsIgnoreCase("ppc") || s.equalsIgnoreCase("google ads")) return "Google Ads";
        return s.length() <= 1 ? s.toUpperCase() : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
