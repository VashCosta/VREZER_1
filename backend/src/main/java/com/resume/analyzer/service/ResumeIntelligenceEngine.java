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
        String experienceSummary = yearsOfExp <= 0.0 ? "Fresher / Entry Level" : String.format(Locale.US, "%.1f Years Industrial Experience", yearsOfExp);

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
        String careerDomainCombined = domainsMap.get("careerDomain");
        
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
     * Accurately parses date ranges from resume experience entries without adding education degree dates.
     */
    public double calculateYearsOfExperienceFromDates(List<Map<String, String>> expList, List<Map<String, String>> internships, String fullText) {
        int currentYear = LocalDate.now().getYear();
        double totalYears = 0.0;
        Set<Integer> activeYears = new HashSet<>();

        String workText = fullText;
        // Exclude Education section if present to avoid treating B.Tech graduation years (e.g. 2011-2015) as work experience
        String lowerFull = fullText.toLowerCase();
        int eduIdx = lowerFull.indexOf("education");
        if (eduIdx != -1) {
            int nextSec = lowerFull.indexOf("experience", eduIdx + 10);
            if (nextSec == -1) nextSec = lowerFull.indexOf("projects", eduIdx + 10);
            if (nextSec == -1) nextSec = lowerFull.indexOf("skills", eduIdx + 10);
            if (nextSec > eduIdx) {
                workText = fullText.substring(0, eduIdx) + " " + fullText.substring(nextSec);
            } else {
                workText = fullText.substring(0, eduIdx);
            }
        }

        Pattern yearRangePattern = Pattern.compile("(?i)\\b(20\\d{2}|19\\d{2})\\s*[-–—/to]+\\s*(20\\d{2}|19\\d{2}|present|current|now)\\b");
        
        Matcher m = yearRangePattern.matcher(workText);
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

        if (!activeYears.isEmpty()) {
            totalYears = activeYears.size();
        } else if (expList != null && !expList.isEmpty()) {
            totalYears = Math.min(expList.size() * 1.5, 10.0);
        } else {
            Matcher expPhrase = Pattern.compile("(?i)(\\d+)\\+?\\s*(?:years|yrs)\\s*(?:of)?\\s*(?:experience|exp)").matcher(fullText);
            if (expPhrase.find()) {
                try {
                    totalYears = Double.parseDouble(expPhrase.group(1));
                } catch (Exception ignored) {}
            }
        }

        // Student / Fresher check
        boolean isStudent = lowerFull.contains("student") || lowerFull.contains("pursuing") || lowerFull.contains("expected graduation") || lowerFull.contains("fresher");
        if (isStudent && totalYears <= 2.0) {
            totalYears = 0.0;
        }

        return Math.max(0.0, Math.min(30.0, totalYears));
    }

    public String determineExperienceLevel(double years, List<Map<String, String>> expList, List<Map<String, String>> internships, String fullText) {
        String lower = fullText.toLowerCase();
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
    public Map<String, String> detectPrimaryAndSecondaryDomains(String fullText, List<String> skills, List<String> progLangs, List<String> frameworks, String degree, String spec) {
        String lower = fullText.toLowerCase();
        String skillsStr = (skills != null ? String.join(" ", skills) : "").toLowerCase();
        String combined = lower + " " + skillsStr + " " + (degree != null ? degree.toLowerCase() : "") + " " + (spec != null ? spec.toLowerCase() : "");

        Map<String, Integer> domainScores = new LinkedHashMap<>();

        domainScores.put("Mechanical Engineering", scoreDomain(combined, new String[]{
            "mechanical", "solidworks", "ansys", "fea", "cad", "catia", "thermodynamics", "fluid mechanics",
            "gd&t", "manufacturing", "mechatronics", "hydraulics", "hvac", "cnc", "cnc machining"
        }));

        domainScores.put("Civil Engineering", scoreDomain(combined, new String[]{
            "civil engineering", "staad", "staad.pro", "revit", "bim", "primavera", "surveying", "concrete design",
            "structural analysis", "construction management"
        }));

        domainScores.put("Electrical & Electronics", scoreDomain(combined, new String[]{
            "electrical engineering", "electronics engineering", "embedded c", "plc", "scada", "microcontrollers",
            "circuit design", "vlsi", "verilog", "vhdl", "pcb design", "matlab", "simulink", "iot"
        }));

        domainScores.put("Finance & Accounting", scoreDomain(combined, new String[]{
            "finance", "accounting", "chartered accountant", "ca", "valuation", "dcf", "lbo", "audit", "taxation",
            "gst", "tally", "equity research", "financial modeling", "corporate finance", "ledger"
        }));

        domainScores.put("Human Resources", scoreDomain(combined, new String[]{
            "human resources", "hrbp", "talent acquisition", "recruiting", "onboarding", "hris", "payroll",
            "employee relations", "people analytics", "labor laws"
        }));

        domainScores.put("Sales & Business Development", scoreDomain(combined, new String[]{
            "sales", "business development", "b2b sales", "account management", "lead generation", "crm",
            "salesforce crm", "hubspot crm", "pipeline management", "inside sales"
        }));

        domainScores.put("Digital Marketing", scoreDomain(combined, new String[]{
            "seo", "sem", "google ads", "meta ads", "facebook ads", "content marketing", "social media marketing",
            "ga4", "google analytics", "hubspot", "marketo", "growth marketing", "performance marketing",
            "copywriting", "a/b testing", "conversion rate", "cro", "campaign", "email marketing", "adwords",
            "digital marketing", "search engine optimization", "content strategy", "growth hacking", "ppc"
        }));

        domainScores.put("UI/UX Design", scoreDomain(combined, new String[]{
            "figma", "adobe xd", "sketch", "invision", "wireframing", "prototyping", "user research",
            "usability testing", "design system", "ui/ux", "user experience", "user interface"
        }));

        domainScores.put("AI & Machine Learning", scoreDomain(combined, new String[]{
            "machine learning", "deep learning", "pytorch", "tensorflow", "nlp", "natural language processing",
            "computer vision", "llm", "large language model", "transformers", "hugging face", "langchain",
            "rag", "vector database", "qdrant", "scikit-learn", "data science", "keras", "generative ai"
        }));

        domainScores.put("Data & Business Analytics", scoreDomain(combined, new String[]{
            "power bi", "tableau", "excel", "dax", "power query", "business intelligence", "sql queries",
            "data analysis", "data analyst", "data visualization", "business analyst", "etl"
        }));

        domainScores.put("Cybersecurity", scoreDomain(combined, new String[]{
            "cybersecurity", "penetration testing", "ethical hacking", "soc analyst", "siem", "incident response",
            "vulnerability assessment", "wireshark", "metasploit", "cryptography"
        }));

        domainScores.put("Backend Development", scoreDomain(combined, new String[]{
            "spring boot", "spring mvc", "hibernate", "jpa", "rest api", "microservices", "java backend",
            "django", "fastapi", "express.js", "node.js backend", "golang", "grpc", "postgresql", "mysql", "redis", "kafka", "java"
        }));

        domainScores.put("Cloud & DevOps", scoreDomain(combined, new String[]{
            "devops", "kubernetes", "docker", "terraform", "ansible", "jenkins", "ci/cd", "aws", "azure", "gcp"
        }));

        domainScores.put("Frontend Development", scoreDomain(combined, new String[]{
            "react", "react.js", "vue", "vue.js", "angular", "next.js", "tailwind", "tailwindcss", "html5", "css3",
            "typescript", "javascript", "redux"
        }));

        List<Map.Entry<String, Integer>> sorted = domainScores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        String primary = !sorted.isEmpty() ? sorted.get(0).getKey() : "Backend Development";
        String secondary = (sorted.size() > 1 && sorted.get(1).getValue() >= 2) ? sorted.get(1).getKey() : "";

        Map<String, String> result = new LinkedHashMap<>();
        result.put("primaryDomain", primary);
        result.put("secondaryDomain", secondary);
        result.put("careerDomain", secondary.isEmpty() ? primary : (primary + " & " + secondary));
        return result;
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
        Pattern p = Pattern.compile("(?i)(B\\.?Tech|B\\.?E\\.?|M\\.?Tech|M\\.?E\\.?|MBA|BCA|MCA|B\\.?Sc|M\\.?Sc|B\\.?A\\.?|B\\.?Com|Ph\\.?D|Diploma|Chartered Accountant|CA)");
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
