package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ResumeParserService — Deep structural extractor for candidate bio-data.
 *
 * STRICT RULE: Every field must be extracted from the raw resume text.
 * Never return placeholder values.
 * If a field cannot be found, return an empty string or an empty list.
 */
@Service
public class ResumeParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Regex Patterns ─────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN    = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern PHONE_PATTERN    = Pattern.compile("(\\+\\d{1,3}[\\s.-]?)?\\(?\\d{3,5}\\)?[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4}");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(?i)(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9_%-]+");
    private static final Pattern GITHUB_PATTERN   = Pattern.compile("(?i)(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9_-]+");
    private static final Pattern CGPA_PATTERN     = Pattern.compile("(?i)(cgpa|gpa|marks|score|percentage|grade)\\s*[:/]?\\s*(\\d{1,2}\\.?\\d{0,2}\\s*[/]?\\s*\\d{0,2}%?)");
    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("(?i)(20\\d{2}|19\\d{2})\\s*[-–—/to]+\\s*(20\\d{2}|19\\d{2}|present|current|now)");
    private static final Pattern MONTH_YEAR_RANGE_PATTERN = Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)\\s*(\\d{4})?\\s*[-–—/to]+\\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december|present|current|now)\\s*(\\d{4})?");

    // Comprehensive programming languages
    private static final List<String> PROGRAMMING_LANGUAGES = Arrays.asList(
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "C", "Go", "Golang",
            "Rust", "Ruby", "PHP", "Kotlin", "Swift", "Scala", "R", "MATLAB", "Dart",
            "Bash", "Shell", "Perl", "Haskell", "Lua", "Julia", "Elixir", "Erlang", "SQL", "HTML", "CSS", "HTML5", "CSS3"
    );

    // Cross-domain frameworks, platforms, and tools
    private static final List<String> FRAMEWORKS_TOOLS = Arrays.asList(
            // Web & Backend & Architecture
            "React", "React Native", "Angular", "Vue.js", "Next.js", "Nuxt.js", "Svelte",
            "Node.js", "Express.js", "Spring Boot", "Spring MVC", "Spring Security", "Spring Cloud",
            "Django", "Flask", "FastAPI", "Laravel", "Ruby on Rails", "ASP.NET", ".NET Core",
            "Hibernate", "JPA", "MyBatis", "GraphQL", "REST APIs", "REST API", "gRPC", "Microservices",
            "MVC Architecture", "MVC", "PostgreSQL", "MySQL", "MariaDB", "MongoDB", "Redis", "Elasticsearch", "Cassandra",
            "SQLite", "Oracle", "Firebase", "DynamoDB", "Supabase", "Prisma", "Sequelize",
            // Cloud & DevOps & Tools
            "Docker", "Kubernetes", "Helm", "Terraform", "Ansible", "Jenkins", "GitHub Actions",
            "CI/CD", "GitLab CI", "CircleCI", "Travis CI", "ArgoCD", "Linux", "Ubuntu",
            "AWS", "Azure", "GCP", "Google Cloud", "Heroku", "Vercel", "Netlify", "DigitalOcean",
            "Lambda", "EC2", "S3", "RDS", "CloudFront", "SQS", "SNS", "ECS", "EKS",
            "Kafka", "RabbitMQ", "Celery", "Nginx", "Apache", "Tomcat",
            "Git", "GitHub", "GitLab", "Bitbucket", "JIRA", "Confluence", "Postman", "Swagger", "VS Code", "Visual Studio Code", "Canva",
            // AI, ML & Data Science
            "Artificial Intelligence", "Machine Learning", "Data Science", "Generative AI", "Deep Learning", "NLP",
            "Computer Vision", "TensorFlow", "PyTorch", "Keras", "scikit-learn", "OpenCV", "Hugging Face",
            "Pandas", "NumPy", "Matplotlib", "Seaborn", "SciPy", "NLTK", "spaCy", "AI Tools",
            "LangChain", "LlamaIndex", "RAG", "Vector Database", "Pinecone", "ChromaDB", "Milvus", "Qdrant",
            "Hadoop", "Spark", "Airflow", "dbt", "Snowflake", "BigQuery", "Databricks",
            // Data / Business Analysis
            "Tableau", "Power BI", "Excel", "Looker", "DAX", "Power Query", "Data Warehousing", "ETL",
            "Business Intelligence", "VBA", "Dashboarding", "Data Modeling",
            // UI/UX Design
            "Figma", "Adobe XD", "Sketch", "InVision", "Wireframing", "Prototyping", "User Research",
            "Usability Testing", "Design Systems", "Information Architecture", "Interaction Design",
            "Photoshop", "Illustrator", "TailwindCSS", "Bootstrap", "Material UI",
            // Digital Marketing & Growth
            "Digital Marketing", "SEO", "SEM", "Google Ads", "Meta Ads", "Facebook Ads", "GA4", "Google Analytics",
            "HubSpot", "Marketo", "MailChimp", "Content Marketing", "Social Media Marketing", "Keyword Research",
            "Content Strategy", "Copywriting", "A/B Testing", "Conversion Rate Optimization", "CRO", "Growth Hacking",
            "Email Marketing", "Performance Marketing", "CRM", "Salesforce", "Meta Ads Manager",
            "PPC", "Pay Per Click", "Search Engine Optimization", "Google Search Console", "GSC",
            "Ahrefs", "SEMrush", "Screaming Frog", "Google Tag Manager", "GTM", "Looker Studio",
            "Social Media Optimization", "SMO", "Influencer Marketing", "Affiliate Marketing",
            "CTR", "CPC", "ROAS", "CPM", "CPA", "Lead Generation", "WordPress Marketing", "Shopify Marketing", "Klaviyo", "Attentive", "Sprinklr",
            // Finance & Accounting
            "Financial Modeling", "Valuation", "DCF", "LBO", "Taxation", "Audit", "Tally", "GST",
            "Equity Research", "Corporate Finance", "Ledger Reconciliation", "IFRS", "GAAP",
            // HR & Operations
            "Recruiting", "Onboarding", "HRIS", "Labor Laws", "Talent Acquisition", "Employee Relations",
            "Performance Appraisal", "People Analytics", "Workday",
            // Mechanical & Core Engineering
            "AutoCAD", "SolidWorks", "ANSYS", "FEA", "CFD", "GD&T", "Manufacturing", "CAD", "CATIA",
            "Thermodynamics", "Fluid Mechanics", "Mechatronics", "Thermal Management",
            // Civil & Construction
            "Staad.Pro", "Revit", "BIM", "Primavera", "Surveying", "Structural Analysis", "Concrete Design"
    );

    private static final List<String> SOFT_SKILLS = Arrays.asList(
            "Leadership", "Problem Solving", "Analytical Thinking", "Communication", "Team Collaboration",
            "Teamwork", "Quick Learner", "Creative Content", "Agile", "Scrum", "Kanban", "Critical Thinking",
            "Time Management", "Mentoring", "Project Management", "Stakeholder Management", "Presentation",
            "Decision Making", "Strategic Planning", "Negotiation", "Adaptability"
    );

    // Section header markers
    private static final List<String> EXPERIENCE_HEADERS = Arrays.asList(
            "experience", "work experience", "professional experience", "employment history",
            "work history", "internship", "internships", "career history", "industry experience"
    );
    private static final List<String> PROJECT_HEADERS = Arrays.asList(
            "projects", "personal projects", "academic projects", "project work",
            "notable projects", "portfolio", "open source", "key projects"
    );
    private static final List<String> EDUCATION_HEADERS = Arrays.asList(
            "education", "academic background", "qualifications", "academic qualifications",
            "educational background", "academics"
    );
    private static final List<String> SKILLS_HEADERS = Arrays.asList(
            "skills", "technical skills", "skills & abilities", "competencies", "tools & platforms", "technologies"
    );
    private static final List<String> CERTIFICATIONS_HEADERS = Arrays.asList(
            "certifications", "certificates", "professional certifications", "licenses", "courses", "trainings"
    );
    private static final List<String> ACHIEVEMENTS_HEADERS = Arrays.asList(
            "achievements", "awards", "honors", "accomplishments", "recognition", "extracurricular"
    );
    private static final List<String> LANGUAGES_HEADERS = Arrays.asList(
            "languages", "languages known", "spoken languages"
    );
    private static final List<String> SUMMARY_HEADERS = Arrays.asList(
            "summary", "professional summary", "career summary", "about me", "profile",
            "career objective", "objective", "executive summary", "personal summary"
    );

    // ═══════════════════════════════════════════════════════════════════════════
    // Main entry point
    // ═══════════════════════════════════════════════════════════════════════════
    public Map<String, Object> parseResumeText(String rawText) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (rawText == null || rawText.trim().isEmpty()) {
            result.put("error", "No resume text provided.");
            return result;
        }

        String text = rawText;
        String lower = text.toLowerCase();

        // Contact information
        result.put("email",    firstMatch(EMAIL_PATTERN, text));
        result.put("phone",    firstMatch(PHONE_PATTERN, text));
        result.put("linkedin", firstMatch(LINKEDIN_PATTERN, text));
        result.put("github",   firstMatch(GITHUB_PATTERN, text));
        String candidateName = extractName(text);
        result.put("name", candidateName);

        // Academic
        result.put("cgpa",      extractCgpa(text));
        result.put("education", extractEducationFromText(text));

        // Technical skill matching — exclude email and URLs to avoid false positives
        String textForSkills = lower.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}", "")
                                   .replaceAll("https?://\\S+", "");
        List<String> detectedLangs = matchSkills(textForSkills, PROGRAMMING_LANGUAGES);
        List<String> detectedFrameworks = matchSkills(textForSkills, FRAMEWORKS_TOOLS);

        List<String> allDetected = new ArrayList<>();
        allDetected.addAll(detectedLangs);
        allDetected.addAll(detectedFrameworks);
        allDetected = sanitizeSkillsList(allDetected, candidateName);

        result.put("programmingLanguages", detectedLangs);
        result.put("frameworks",           detectedFrameworks);
        List<String> detectedSoftSkills = matchSkills(lower, SOFT_SKILLS);
        result.put("softSkills",           detectedSoftSkills);
        result.put("transferableSkills",   detectedSoftSkills);
        result.put("allDetectedSkills",    allDetected);

        // Professional Summary & Objective
        String summary = extractProfessionalSummary(text);
        result.put("professionalSummary", summary);
        result.put("careerObjective",     summary);

        // Career sections — strictly extracted from text
        List<Map<String, String>> expList = extractExperienceFromText(text);
        result.put("experience",      expList);
        List<Map<String, String>> internshipList = expList.stream()
                .filter(e -> {
                    String r = e.getOrDefault("role", "").toLowerCase();
                    String c = e.getOrDefault("company", "").toLowerCase();
                    String d = e.getOrDefault("description", "").toLowerCase();
                    return r.contains("intern") || c.contains("intern") || d.contains("intern");
                })
                .toList();
        result.put("internships",     internshipList);
        result.put("projects",        extractProjectsFromText(text));
        result.put("certifications",  extractSectionBullets(text, CERTIFICATIONS_HEADERS, 12));
        result.put("achievements",    extractSectionBullets(text, ACHIEVEMENTS_HEADERS, 10));
        result.put("languages",       extractLanguagesSpoken(text));

        return result;
    }

    public List<String> sanitizeSkillsList(List<String> rawSkills, String candidateName) {
        if (rawSkills == null || rawSkills.isEmpty()) return new ArrayList<>();
        Set<String> stops = new HashSet<>(Arrays.asList(
            "SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS", "PROJECTS", "CERTIFICATIONS", "ACHIEVEMENTS",
            "DECLARATION", "INTERNSHIPS", "PROFILE", "CONTACT", "INTERNSHIP", "PRESENT", "MONTH", "YEAR",
            "NAME", "EMAIL", "PHONE", "LINKEDIN", "GITHUB", "PORTFOLIO", "WORK", "DETAILS", "RESPONSIBILITIES",
            "DESCRIPTION", "DUTIES", "CURRICULUM", "VITAE", "RESUME", "REVIEWS", "TITLES", "HEADING",
            "SECTION", "PAGE", "ADDRESS", "CITY", "STATE", "COUNTRY", "INDIA", "GLOBAL", "BACHELOR", "MASTER",
            "UNIVERSITY", "COLLEGE", "INSTITUTE", "SCHOOL", "DEGREE", "GPA", "CGPA", "MARKS", "SUMM", "EXP",
            "EDU", "PROJ", "CERT", "INFO", "OVERVIEW", "OBJECTIVE", "BACKGROUND", "HISTORY", "QUALIFICATIONS"
        ));
        if (candidateName != null && !candidateName.trim().isEmpty()) {
            for (String w : candidateName.toUpperCase().split("\\s+")) {
                if (!w.trim().isEmpty()) stops.add(w.trim());
            }
        }

        List<String> clean = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String s : rawSkills) {
            if (s == null || s.trim().length() < 2) continue;
            String upper = s.trim().toUpperCase();
            if (!stops.contains(upper) && !stops.contains(upper.replaceAll("[^A-Z]", "")) && !seen.contains(upper)) {
                clean.add(s.trim());
                seen.add(upper);
            }
        }
        return clean;
    }

    // ── Name Extraction ────────────────────────────────────────────────────────
    public String extractName(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        String[] lines = text.split("\\r?\\n");
        List<String> stopWords = Arrays.asList(
                "resume", "curriculum", "vitae", "cv", "contact", "email", "phone",
                "profile", "summary", "experience", "education", "skills", "projects",
                "page", "objective", "address", "linkedin", "github", "portfolio",
                "certificate", "achievement", "declaration", "b.tech", "b.e", "bachelor",
                "master", "pursuing", "student", "developer", "engineer", "intern", "madurai",
                "chennai", "india", "pune", "bengaluru", "delhi", "mumbai"
        );

        int limit = Math.min(lines.length, 12);
        for (int i = 0; i < limit; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() > 50 || line.contains("@")
                    || line.contains("http") || line.contains("|") || line.contains(":") || line.contains("+")) continue;

            String lineLower = line.toLowerCase();
            boolean hasStop = stopWords.stream().anyMatch(w -> lineLower.startsWith(w) || lineLower.equals(w));
            if (hasStop) continue;

            // Remove titles like Mr., Dr., Ms.
            String cleanedLine = line.replaceAll("(?i)^(mr\\.|ms\\.|mrs\\.|dr\\.)\\s+", "").trim();

            // Match full names (Title Case or ALL CAPS with 2-4 tokens)
            if (cleanedLine.matches("^[A-Za-z.'-]+(\\s+[A-Za-z.'-]+){1,3}$") && cleanedLine.length() >= 3) {
                // Ensure it's not a common degree line
                if (!cleanedLine.toUpperCase().contains("B.TECH") && !cleanedLine.toUpperCase().contains("ENGINEERING")) {
                    return formatName(cleanedLine);
                }
            }
        }

        // Email handle fallback
        String email = firstMatch(EMAIL_PATTERN, text);
        if (!email.isEmpty() && email.contains("@")) {
            String handle = email.substring(0, email.indexOf('@'));
            String clean = handle.replaceAll("[0-9_.\\-]+", " ").trim();
            if (clean.length() >= 3) return formatName(clean);
        }

        return "";
    }

    private String formatName(String raw) {
        return Arrays.stream(raw.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + (w.length() > 1 ? w.substring(1).toLowerCase() : ""))
                .reduce((a, b) -> a + " " + b).orElse(raw.trim());
    }

    // ── CGPA & Percentage ──────────────────────────────────────────────────────
    private String extractCgpa(String text) {
        Matcher m = CGPA_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(2).trim();
        }
        Matcher bare = Pattern.compile("\\b(\\d\\.\\d{1,2})\\s*/\\s*(10|4\\.0|4)\\b").matcher(text);
        if (bare.find()) return bare.group(1) + "/" + bare.group(2);

        Matcher pct = Pattern.compile("\\b(\\d{2}\\.?\\d?)\\s*%").matcher(text);
        if (pct.find()) return pct.group(1) + "%";

        return "";
    }

    // ── Education Extractor ────────────────────────────────────────────────────
    private List<Map<String, String>> extractEducationFromText(String text) {
        List<Map<String, String>> results = new ArrayList<>();

        Pattern degreePattern = Pattern.compile(
                "(?i)(?<![a-zA-Z0-9])(B\\.?Tech|B\\.?E\\.?|Bachelor[s]? of [A-Za-z\\s&]+|" +
                "M\\.?Tech|M\\.?E\\.?|Master[s]? of [A-Za-z\\s&]+|MBA|BCA|MCA|" +
                "B\\.?Sc\\.?|M\\.?Sc\\.?|Ph\\.?D\\.?|B\\.?Com|B\\.?A\\.?|Diploma|Class 12 \\(HSC\\)|Class 10 \\(SSLC\\)|HSC|SSLC|12th|10th)(?![a-zA-Z0-9])" +
                "[^\\n]{0,140}"
        );

        Matcher dm = degreePattern.matcher(text);
        Set<String> seen = new HashSet<>();
        while (dm.find()) {
            String line = dm.group().trim().replaceAll("\\s+", " ");
            String key = line.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (key.length() > 3 && !seen.contains(key) && line.length() < 180) {
                seen.add(key);
                Map<String, String> edu = new LinkedHashMap<>();
                edu.put("degree", line);
                edu.put("cgpa",   extractCgpa(extractNearbyLines(text, dm.start(), 3)));
                edu.put("years",  extractYearsNear(text, dm.start()));
                results.add(edu);
                if (results.size() >= 5) break;
            }
        }
        return results;
    }

    // ── Experience & Internships Extractor ──────────────────────────────────────
    private List<Map<String, String>> extractExperienceFromText(String text) {
        List<Map<String, String>> results = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        int startIdx = findSectionStart(lines, EXPERIENCE_HEADERS);
        int endIdx   = findSectionEnd(lines, startIdx, PROJECT_HEADERS, EDUCATION_HEADERS,
                SKILLS_HEADERS, CERTIFICATIONS_HEADERS, ACHIEVEMENTS_HEADERS);

        if (startIdx < 0) return results;

        Map<String, String> currentEntry = null;
        StringBuilder bullets = new StringBuilder();
        Pattern dateRange = Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|20\\d{2}|19\\d{2})[\\s,.-]*(20\\d{2}|19\\d{2}|present|current|now)?", Pattern.CASE_INSENSITIVE);

        for (int i = startIdx + 1; i < endIdx && i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() > 250) continue;

            boolean hasDate = dateRange.matcher(line).find() && (line.contains("-") || line.contains("–") || line.contains("—") || line.toLowerCase().contains("to") || line.toLowerCase().contains("present"));
            boolean isBullet = line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("–") || line.startsWith(">") || line.startsWith("–");

            if (!isBullet && line.length() < 130 && (looksLikeJobTitle(line) || hasDate || line.contains(" - ") || line.contains(" – "))) {
                if (currentEntry != null && (!currentEntry.get("company").isEmpty() || !currentEntry.get("role").isEmpty())) {
                    currentEntry.put("description", bullets.toString().trim());
                    results.add(currentEntry);
                    bullets = new StringBuilder();
                }
                currentEntry = new LinkedHashMap<>();
                currentEntry.put("role", "");
                currentEntry.put("company", "");
                currentEntry.put("duration", hasDate ? line : "");

                if (line.contains("—") || line.contains("-") || line.contains("–") || line.contains("|")) {
                    String[] parts = line.split("[—–\\-|]");
                    if (parts.length >= 2) {
                        currentEntry.put("role", parts[0].trim());
                        currentEntry.put("company", parts[1].replaceAll("\\(.*?\\)", "").trim());
                    }
                } else if (!hasDate) {
                    if (looksLikeJobTitle(line)) {
                        currentEntry.put("role", line);
                    } else {
                        currentEntry.put("company", line);
                    }
                }
            } else if (isBullet && currentEntry != null) {
                bullets.append(line.replaceAll("^[•\\-*–>]\\s*", "")).append(" | ");
            } else if (currentEntry != null && !isBullet && line.length() > 5) {
                // If it's descriptive prose under the entry
                if (line.length() > 60 || line.startsWith("Developed") || line.startsWith("Built") || line.startsWith("Engineered") || line.startsWith("Trained") || line.startsWith("Created") || line.startsWith("Performed")) {
                    bullets.append(line).append(" | ");
                } else if (currentEntry.get("company").isEmpty()) {
                    currentEntry.put("company", line);
                } else if (currentEntry.get("role").isEmpty()) {
                    currentEntry.put("role", line);
                }
            }
        }

        if (currentEntry != null && (!currentEntry.get("company").isEmpty() || !currentEntry.get("role").isEmpty())) {
            currentEntry.put("description", bullets.toString().trim());
            results.add(currentEntry);
        }

        return results;
    }

    // ── Projects Extractor ─────────────────────────────────────────────────────
    private List<Map<String, String>> extractProjectsFromText(String text) {
        List<Map<String, String>> results = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        int startIdx = findSectionStart(lines, PROJECT_HEADERS);
        int endIdx   = findSectionEnd(lines, startIdx,
                EXPERIENCE_HEADERS, EDUCATION_HEADERS, SKILLS_HEADERS, CERTIFICATIONS_HEADERS, ACHIEVEMENTS_HEADERS);

        if (startIdx < 0) return results;

        Map<String, String> currentProj = null;
        StringBuilder desc = new StringBuilder();

        for (int i = startIdx + 1; i < endIdx && i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            boolean isBullet = line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("–") || line.startsWith(">");

            if (!isBullet && line.length() <= 100 && !looksLikeDateLine(line) && !line.startsWith("Developed") && !line.startsWith("Implemented") && !line.startsWith("Engineered") && !line.startsWith("Designed") && !line.startsWith("Applied")) {
                if (currentProj != null && !currentProj.get("title").isEmpty()) {
                    currentProj.put("description", desc.toString().trim());
                    results.add(currentProj);
                    desc = new StringBuilder();
                }
                currentProj = new LinkedHashMap<>();
                currentProj.put("title", line);
                currentProj.put("tech",  extractTechFromLine(line));
                currentProj.put("description", "");
            } else if (currentProj != null) {
                String clean = line.replaceAll("^[•\\-*–>]\\s*", "");
                if (clean.toLowerCase().startsWith("tech") || clean.startsWith("|") || clean.contains("built with") || clean.contains("tech stack")) {
                    currentProj.put("tech", extractTechFromLine(clean));
                } else {
                    desc.append(clean).append(" ");
                    // Also extract any mentioned tech from descriptions
                    String techInDesc = extractTechFromLine(clean);
                    if (!techInDesc.isEmpty()) {
                        String existing = currentProj.getOrDefault("tech", "");
                        currentProj.put("tech", existing.isEmpty() ? techInDesc : existing + ", " + techInDesc);
                    }
                }
            }
        }

        if (currentProj != null && !currentProj.get("title").isEmpty()) {
            currentProj.put("description", desc.toString().trim());
            results.add(currentProj);
        }

        return results;
    }

    // ── Section Bullet Extractor ──────────────────────────────────────────────
    private List<String> extractSectionBullets(String text, List<String> headerKeywords, int maxItems) {
        List<String> results = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        int startIdx = findSectionStart(lines, headerKeywords);
        int endIdx   = findSectionEnd(lines, startIdx,
                EXPERIENCE_HEADERS, PROJECT_HEADERS, EDUCATION_HEADERS, SKILLS_HEADERS, LANGUAGES_HEADERS);

        if (startIdx < 0) return results;

        for (int i = startIdx + 1; i < endIdx && i < lines.length; i++) {
            String line = lines[i].trim().replaceAll("^[•\\-*–>]\\s*", "");
            if (!line.isEmpty() && line.length() > 3 && line.length() < 250) {
                results.add(line);
                if (results.size() >= maxItems) break;
            }
        }
        return results;
    }

    // ── Spoken Languages ───────────────────────────────────────────────────────
    private List<String> extractLanguagesSpoken(String text) {
        List<String> langs = new ArrayList<>();
        String lower = text.toLowerCase();
        String[] commonLangs = {
            "English", "Tamil", "Hindi", "Telugu", "Kannada", "Malayalam", "Marathi",
            "Bengali", "Gujarati", "Punjabi", "Spanish", "French", "German", "Arabic",
            "Mandarin", "Japanese", "Korean", "Portuguese", "Italian", "Russian"
        };
        for (String lang : commonLangs) {
            if (lower.contains(lang.toLowerCase())) {
                langs.add(lang);
            }
        }
        return langs;
    }

    // ── Skill Matching ────────────────────────────────────────────────────────
    private List<String> matchSkills(String lowerText, List<String> vocab) {
        List<String> found = new ArrayList<>();
        String cleanText = " " + lowerText.replaceAll("[^a-zA-Z0-9+#.#-]", " ") + " ";
        for (String skill : vocab) {
            String skillClean = skill.trim();
            if (skillClean.isEmpty()) continue;

            Pattern p = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9+#])" + Pattern.quote(skillClean) + "(?=[^a-zA-Z0-9+#]|$)");
            if (p.matcher(cleanText).find()) {
                found.add(skill);
            }
        }
        return found;
    }

    // ── Section Navigation Helpers ────────────────────────────────────────────
    private int findSectionStart(String[] lines, List<String> headers) {
        for (int i = 0; i < lines.length; i++) {
            String low = lines[i].trim().toLowerCase().replaceAll("[^a-z\\s]", "").trim();
            for (String h : headers) {
                if (low.equals(h) || low.startsWith(h + " ") || low.endsWith(" " + h)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SafeVarargs
    private final int findSectionEnd(String[] lines, int start, List<String>... terminators) {
        if (start < 0) return lines.length;
        List<String> allTerms = new ArrayList<>();
        for (List<String> t : terminators) allTerms.addAll(t);

        for (int i = start + 1; i < lines.length; i++) {
            String low = lines[i].trim().toLowerCase().replaceAll("[^a-z\\s]", "").trim();
            for (String h : allTerms) {
                if (low.equals(h) || low.startsWith(h + " ")) return i;
            }
        }
        return lines.length;
    }

    // ── Professional Summary Extractor ────────────────────────────────────────
    public String extractProfessionalSummary(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String[] lines = text.split("\\r?\\n");

        int startIdx = findSectionStart(lines, SUMMARY_HEADERS);
        if (startIdx >= 0) {
            int endIdx = findSectionEnd(lines, startIdx,
                    EXPERIENCE_HEADERS, PROJECT_HEADERS, EDUCATION_HEADERS, SKILLS_HEADERS, CERTIFICATIONS_HEADERS, ACHIEVEMENTS_HEADERS);
            StringBuilder sb = new StringBuilder();
            for (int i = startIdx + 1; i < endIdx && i < lines.length; i++) {
                String line = lines[i].trim().replaceAll("^[•\\-*–>]\\s*", "");
                if (!line.isEmpty() && line.length() > 3) {
                    sb.append(line).append(" ");
                }
            }
            String result = sb.toString().trim();
            if (result.length() >= 25) return result;
        }

        // If no explicit header, inspect top lines for opening summary paragraph
        StringBuilder topSummary = new StringBuilder();
        int limit = Math.min(lines.length, 14);
        for (int i = 0; i < limit; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() > 350) continue;
            String low = line.toLowerCase();
            if (low.contains("@") || low.contains("linkedin.com") || low.contains("github.com") || low.contains("phone") || low.contains("+91") || low.contains("http")) continue;
            if (low.equals("education") || low.equals("experience") || low.equals("skills") || low.equals("projects")) break;

            if (low.contains("aspiring") || low.contains("passionate") || low.contains("experienced")
                    || low.contains("student with") || low.contains("specialist with") || low.contains("skilled in")
                    || low.contains("hands-on experience") || low.contains("proven track record") || low.contains("seeking a")
                    || low.contains("developer with") || low.contains("engineer with") || low.contains("motivated")) {
                topSummary.append(line.replaceAll("^[•\\-*–>]\\s*", "")).append(" ");
                for (int j = i + 1; j < limit && j < lines.length; j++) {
                    String nextLine = lines[j].trim();
                    if (nextLine.isEmpty()) break;
                    String nextLow = nextLine.toLowerCase();
                    if (nextLow.contains("@") || nextLow.contains("http") || nextLow.contains("202") || nextLow.equals("education") || nextLow.equals("experience")) break;
                    topSummary.append(nextLine.replaceAll("^[•\\-*–>]\\s*", "")).append(" ");
                }
                break;
            }
        }

        return topSummary.toString().trim();
    }

    // ── Utility Helpers ────────────────────────────────────────────────────────
    private String firstMatch(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group() : "";
    }

    private String extractNearbyLines(String text, int charPos, int lineCount) {
        int start = Math.max(0, charPos - 50);
        int end   = Math.min(text.length(), charPos + 400);
        return text.substring(start, end);
    }

    private String extractYearsNear(String text, int charPos) {
        String snippet = extractNearbyLines(text, charPos, 3);
        Matcher m = YEAR_RANGE_PATTERN.matcher(snippet);
        if (m.find()) return m.group();
        Matcher single = Pattern.compile("\\b(20\\d{2}|19\\d{2})\\b").matcher(snippet);
        if (single.find()) return single.group();
        return "";
    }

    private boolean looksLikeJobTitle(String line) {
        String l = line.toLowerCase();
        return l.contains("intern") || l.contains("developer") || l.contains("engineer")
                || l.contains("specialist") || l.contains("manager") || l.contains("analyst")
                || l.contains("architect") || l.contains("lead") || l.contains("scientist")
                || l.contains("consultant") || l.contains("designer") || l.contains("associate")
                || l.contains("executive") || l.contains("strategist") || l.contains("researcher")
                || l.contains("accountant") || l.contains("officer") || l.contains("trainee");
    }

    private boolean looksLikeDateLine(String line) {
        return line.matches(".*\\b(20\\d{2}|19\\d{2}|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|" +
                "March|April|June|July|August|September|October|November|December|present|current|now)\\b.*");
    }

    private String extractTechFromLine(String line) {
        List<String> found = matchSkills(line.toLowerCase(), FRAMEWORKS_TOOLS);
        found.addAll(matchSkills(line.toLowerCase(), PROGRAMMING_LANGUAGES));
        return String.join(", ", found.stream().distinct().toList());
    }

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return "{}";
        }
    }
}
