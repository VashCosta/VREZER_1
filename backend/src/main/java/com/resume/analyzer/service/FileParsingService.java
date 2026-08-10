package com.resume.analyzer.service;

import com.resume.analyzer.dto.StructuredResumeDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * FileParsingService — Tika-first resume extraction pipeline.
 *
 * Extraction order (per spec):
 *  1. Apache Tika AutoDetectParser  — handles PDF, DOCX, DOC, RTF, TXT, ODP, etc.
 *  2. Apache PDFBox (PDFTextStripper) — fallback for PDFs Tika cannot fully extract.
 *  3. Apache POI (XWPFWordExtractor) — fallback for Word documents.
 *
 * After raw text extraction, builds a StructuredResumeDto via ResumeParserService
 * for AI prompt grounding.
 */
@Service
public class FileParsingService {

    // Tika instances are thread-safe and should be reused
    private final Tika tikaMimeDetector = new Tika();
    private final AutoDetectParser tikaParser = new AutoDetectParser();

    @Autowired
    private ResumeParserService resumeParserService;

    /**
     * Primary entry point — extracts raw text from any uploaded resume file.
     * Strategy: Tika first (handles all formats), PDFBox/POI as fallback.
     *
     * @param file The uploaded MultipartFile (PDF, DOCX, DOC, RTF, TXT)
     * @return Extracted raw text
     */
    public String extractText(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or null.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null.");
        }

        String lowerName = filename.toLowerCase().trim();
        System.out.println("[FILE PARSING] Extracting text from: " + filename + " (" + file.getSize() + " bytes)");

        // ── Strategy 1: Apache Tika AutoDetectParser (primary, format-agnostic) ──
        try {
            String tikaText = extractWithTika(file.getInputStream());
            if (tikaText != null && tikaText.trim().length() > 50) {
                System.out.println("[FILE PARSING] Tika extraction succeeded. Length: " + tikaText.length());
                return tikaText.trim();
            }
        } catch (Exception tikaEx) {
            System.err.println("[FILE PARSING] Tika AutoDetectParser failed, trying format-specific fallback: " + tikaEx.getMessage());
        }

        // ── Strategy 2a: Apache PDFBox for PDFs (fallback) ──
        if (lowerName.endsWith(".pdf")) {
            try {
                String pdfText = extractTextFromPdf(file.getInputStream());
                if (pdfText != null && pdfText.trim().length() > 50) {
                    System.out.println("[FILE PARSING] PDFBox fallback succeeded. Length: " + pdfText.length());
                    return pdfText.trim();
                }
            } catch (Exception pdfEx) {
                System.err.println("[FILE PARSING] PDFBox fallback failed: " + pdfEx.getMessage());
            }
        }

        // ── Strategy 2b: Apache POI for Word documents (fallback) ──
        if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
            try {
                String docxText = extractTextFromDocx(file.getInputStream());
                if (docxText != null && docxText.trim().length() > 50) {
                    System.out.println("[FILE PARSING] POI DOCX fallback succeeded. Length: " + docxText.length());
                    return docxText.trim();
                }
            } catch (Exception docxEx) {
                System.err.println("[FILE PARSING] POI DOCX fallback failed: " + docxEx.getMessage());
            }
        }

        // ── Strategy 3: Tika simple string parsing (last resort) ──
        try {
            String simpleText = tikaMimeDetector.parseToString(file.getInputStream());
            if (simpleText != null && !simpleText.trim().isEmpty()) {
                System.out.println("[FILE PARSING] Tika simple parse succeeded. Length: " + simpleText.length());
                return simpleText.trim();
            }
        } catch (Exception ex) {
            System.err.println("[FILE PARSING] All extraction strategies failed: " + ex.getMessage());
        }

        throw new RuntimeException("Unable to extract text from the uploaded file. Please ensure the file is a valid, non-encrypted PDF or DOCX document.");
    }

    /**
     * Extracts raw text AND returns a fully structured, validated StructuredResumeDto.
     * This is the preferred pipeline entry point for AI analysis.
     */
    public Map<String, Object> extractAndStructure(MultipartFile file) throws Exception {
        String rawText = extractText(file);

        // Parse into structured fields
        Map<String, Object> parsed = resumeParserService.parseResumeText(rawText);
        parsed.put("rawText", rawText);
        parsed.put("filename", file.getOriginalFilename());
        parsed.put("fileSizeBytes", file.getSize());

        // Build and validate the DTO
        StructuredResumeDto dto = buildStructuredDto(rawText, parsed);
        parsed.put("structuredDto", dto.toMap());
        parsed.put("isStructured", true);

        System.out.println("[FILE PARSING] Structured extraction complete: name=" + dto.getName()
                + ", skills=" + dto.getAllDetectedSkills().size()
                + ", education=" + dto.getEducation().size()
                + ", experience=" + dto.getExperience().size());

        return parsed;
    }

    /**
     * Apache Tika AutoDetectParser — handles PDF, DOCX, DOC, RTF, ODT, TXT, etc.
     * Content handler is capped at 1MB of text to prevent memory issues.
     */
    private String extractWithTika(InputStream inputStream) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(1024 * 1024); // 1MB cap
        Metadata metadata = new Metadata();
        tikaParser.parse(inputStream, handler, metadata, new org.apache.tika.parser.ParseContext());
        String text = handler.toString();
        // Tika sometimes returns lots of whitespace — normalize
        return text.replaceAll("\\s{3,}", "\n").trim();
    }

    /**
     * Apache PDFBox — Fallback PDF extractor preserving column order better for some scanned PDFs.
     */
    public String extractTextFromPdf(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // better column ordering
            return stripper.getText(document);
        }
    }

    /**
     * Apache POI — Fallback DOCX extractor for complex Word documents.
     */
    public String extractTextFromDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /**
     * Builds a validated StructuredResumeDto from raw text and pre-parsed map.
     * Validates each field: strips whitespace, removes empty entries, normalizes lists.
     */
    @SuppressWarnings("unchecked")
    private StructuredResumeDto buildStructuredDto(String rawText, Map<String, Object> parsed) {
        StructuredResumeDto dto = new StructuredResumeDto();
        dto.setRawText(rawText);

        // ── Identity ────────────────────────────────────────────────────────
        dto.setName(safe(parsed.get("name")));
        dto.setEmail(safe(parsed.get("email")));
        dto.setPhone(safe(parsed.get("phone")));
        dto.setLinkedin(safe(parsed.get("linkedin")));
        dto.setGithub(safe(parsed.get("github")));
        dto.setPortfolio(safe(parsed.get("portfolio")));

        // ── Education ───────────────────────────────────────────────────────
        List<Map<String, String>> edu = (List<Map<String, String>>) parsed.getOrDefault("education", List.of());
        dto.setEducation(edu);
        if (!edu.isEmpty()) {
            Map<String, String> primary = edu.get(0);
            dto.setDegree(primary.getOrDefault("degree", ""));
            dto.setSpecialization(primary.getOrDefault("specialization", primary.getOrDefault("field", "")));
            dto.setInstitution(primary.getOrDefault("institution", ""));
        }
        dto.setCgpa(safe(parsed.get("cgpa")));

        // ── Experience & Projects ───────────────────────────────────────────
        dto.setExperience((List<Map<String, String>>) parsed.getOrDefault("experience", List.of()));
        dto.setInternships(extractInternships(parsed));
        dto.setProjects((List<Map<String, String>>) parsed.getOrDefault("projects", List.of()));

        // ── Skills ──────────────────────────────────────────────────────────
        dto.setProgrammingLanguages(safeList(parsed.get("programmingLanguages")));
        dto.setFrameworks(safeList(parsed.get("frameworks")));
        dto.setSoftSkills(safeList(parsed.get("softSkills")));
        dto.setAllDetectedSkills(safeList(parsed.get("allDetectedSkills")));

        // Categorize additional skill sub-lists from the allDetectedSkills pool
        List<String> allSkills = dto.getAllDetectedSkills();
        dto.setDatabases(filterByKeywords(allSkills, DB_KEYWORDS));
        dto.setCloudPlatforms(filterByKeywords(allSkills, CLOUD_KEYWORDS));
        dto.setLibraries(filterByKeywords(allSkills, LIB_KEYWORDS));
        dto.setTools(filterByKeywords(allSkills, TOOL_KEYWORDS));
        dto.setTechnicalSkills(allSkills); // full set

        // ── Credentials ─────────────────────────────────────────────────────
        dto.setCertifications(extractCertifications(rawText));
        dto.setAchievements(extractAchievements(rawText));
        dto.setResearchPapers(extractResearchPapers(rawText));
        dto.setVolunteerWork(extractVolunteerWork(rawText));
        dto.setLanguagesSpoken(extractLanguagesSpoken(rawText));
        dto.setCareerObjective(extractCareerObjective(rawText));

        // ── Metadata ────────────────────────────────────────────────────────
        int detected = countDetectedFields(dto);
        dto.setTotalFieldsDetected(detected);
        dto.setValidated(detected > 3);

        return dto;
    }

    // ── Extraction Helper Methods ──────────────────────────────────────────

    private static final List<String> DB_KEYWORDS = Arrays.asList(
        "MySQL", "PostgreSQL", "MongoDB", "Redis", "Oracle", "SQLite", "Cassandra",
        "Elasticsearch", "DynamoDB", "Firebase", "Supabase", "MariaDB", "MS SQL", "H2"
    );
    private static final List<String> CLOUD_KEYWORDS = Arrays.asList(
        "AWS", "Azure", "GCP", "Google Cloud", "Heroku", "Vercel", "Netlify",
        "DigitalOcean", "CloudFlare", "Lambda", "EC2", "S3", "EKS", "AKS"
    );
    private static final List<String> LIB_KEYWORDS = Arrays.asList(
        "Pandas", "NumPy", "Matplotlib", "Seaborn", "scikit-learn", "TensorFlow",
        "PyTorch", "Keras", "Hugging Face", "OpenCV", "LangChain", "NLTK", "spaCy"
    );
    private static final List<String> TOOL_KEYWORDS = Arrays.asList(
        "Git", "GitHub", "GitLab", "Docker", "Kubernetes", "Jenkins", "Postman",
        "JIRA", "Confluence", "Figma", "Tableau", "Power BI", "Swagger", "IntelliJ"
    );

    private List<String> filterByKeywords(List<String> skills, List<String> keywords) {
        List<String> result = new ArrayList<>();
        for (String skill : skills) {
            for (String kw : keywords) {
                if (skill.equalsIgnoreCase(kw) || skill.toLowerCase().contains(kw.toLowerCase())) {
                    result.add(skill);
                    break;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractInternships(Map<String, Object> parsed) {
        // Check for explicit internships list from parser
        Object internshipsRaw = parsed.get("internships");
        if (internshipsRaw instanceof List) {
            List<?> list = (List<?>) internshipsRaw;
            if (!list.isEmpty()) {
                return (List<Map<String, String>>) internshipsRaw;
            }
        }
        // Try to identify internships from experience list (entries with "intern" in title/description)
        List<Map<String, String>> expList = (List<Map<String, String>>) parsed.getOrDefault("experience", List.of());
        List<Map<String, String>> internships = new ArrayList<>();
        for (Map<String, String> exp : expList) {
            String title = exp.getOrDefault("title", "").toLowerCase();
            String desc = exp.getOrDefault("description", "").toLowerCase();
            if (title.contains("intern") || desc.contains("internship")) {
                internships.add(exp);
            }
        }
        return internships;
    }

    private List<String> extractCertifications(String rawText) {
        List<String> certs = new ArrayList<>();
        String[] lines = rawText.split("\\n");
        boolean inCertSection = false;
        for (String line : lines) {
            String lower = line.toLowerCase().trim();
            if (lower.matches(".*(certification|certifications|certified|credential|license|accreditation).*")) {
                inCertSection = true;
                continue;
            }
            if (inCertSection) {
                if (isSectionHeader(lower) && !lower.contains("certif")) { inCertSection = false; continue; }
                String clean = line.trim().replaceAll("^[•\\-*–]\\s*", "");
                if (clean.length() > 5 && clean.length() < 120 && !clean.isEmpty()) {
                    certs.add(clean);
                    if (certs.size() >= 10) break;
                }
            }
        }
        return certs;
    }

    private List<String> extractAchievements(String rawText) {
        List<String> achievements = new ArrayList<>();
        String[] lines = rawText.split("\\n");
        boolean inAchievSection = false;
        for (String line : lines) {
            String lower = line.toLowerCase().trim();
            if (lower.matches(".*(achievement|accomplishment|award|honor|recognition|distinction|winner|rank).*") && lower.length() < 50) {
                inAchievSection = true;
                continue;
            }
            if (inAchievSection) {
                if (isSectionHeader(lower) && !lower.contains("achiev")) { inAchievSection = false; continue; }
                String clean = line.trim().replaceAll("^[•\\-*–]\\s*", "");
                if (clean.length() > 5 && clean.length() < 150 && !clean.isEmpty()) {
                    achievements.add(clean);
                    if (achievements.size() >= 8) break;
                }
            }
        }
        return achievements;
    }

    private List<String> extractResearchPapers(String rawText) {
        List<String> papers = new ArrayList<>();
        String[] lines = rawText.split("\\n");
        boolean inResearchSection = false;
        for (String line : lines) {
            String lower = line.toLowerCase().trim();
            if (lower.matches(".*(research|publication|paper|journal|conference|ieee|acm|arxiv).*") && lower.length() < 50) {
                inResearchSection = true;
                continue;
            }
            if (inResearchSection) {
                if (isSectionHeader(lower) && !lower.contains("research") && !lower.contains("paper")) {
                    inResearchSection = false; continue;
                }
                String clean = line.trim().replaceAll("^[•\\-*–]\\s*", "");
                if (clean.length() > 10 && clean.length() < 250 && !clean.isEmpty()) {
                    papers.add(clean);
                    if (papers.size() >= 5) break;
                }
            }
        }
        return papers;
    }

    private List<String> extractVolunteerWork(String rawText) {
        List<String> volunteer = new ArrayList<>();
        String[] lines = rawText.split("\\n");
        boolean inVolunteerSection = false;
        for (String line : lines) {
            String lower = line.toLowerCase().trim();
            if (lower.matches(".*(volunteer|community|social work|ngo|non-profit|charity|service).*") && lower.length() < 50) {
                inVolunteerSection = true;
                continue;
            }
            if (inVolunteerSection) {
                if (isSectionHeader(lower) && !lower.contains("volunteer")) { inVolunteerSection = false; continue; }
                String clean = line.trim().replaceAll("^[•\\-*–]\\s*", "");
                if (clean.length() > 5 && clean.length() < 150 && !clean.isEmpty()) {
                    volunteer.add(clean);
                    if (volunteer.size() >= 5) break;
                }
            }
        }
        return volunteer;
    }

    private List<String> extractLanguagesSpoken(String rawText) {
        List<String> langs = new ArrayList<>();
        String lower = rawText.toLowerCase();
        // Common spoken language keywords
        String[] commonLangs = {
            "English", "Hindi", "Tamil", "Telugu", "Kannada", "Malayalam", "Marathi",
            "Bengali", "Gujarati", "Punjabi", "Spanish", "French", "German", "Arabic",
            "Mandarin", "Japanese", "Korean", "Portuguese", "Italian", "Russian", "Urdu"
        };
        // Only extract if there's a "Languages" section header
        if (lower.contains("language")) {
            for (String lang : commonLangs) {
                if (rawText.contains(lang) || rawText.contains(lang.toLowerCase())) {
                    langs.add(lang);
                }
            }
        }
        return langs;
    }

    private String extractCareerObjective(String rawText) {
        String[] lines = rawText.split("\\n");
        boolean inObjectiveSection = false;
        StringBuilder objective = new StringBuilder();
        for (String line : lines) {
            String lower = line.toLowerCase().trim();
            if (lower.matches(".*(objective|career objective|summary|professional summary|profile|about me).*") && lower.length() < 50) {
                inObjectiveSection = true;
                continue;
            }
            if (inObjectiveSection) {
                if (isSectionHeader(lower) && objective.length() > 20) break;
                if (!line.trim().isEmpty()) {
                    objective.append(line.trim()).append(" ");
                    if (objective.length() > 400) break;
                }
            }
        }
        return objective.toString().trim();
    }

    private boolean isSectionHeader(String lower) {
        return lower.matches(".*(experience|education|skills|projects|internship|certifi|achievement|award|publication|research|volunteer|language|contact|reference|summary|objective|profile|career).*")
               && lower.length() < 60;
    }

    private int countDetectedFields(StructuredResumeDto dto) {
        int count = 0;
        if (!dto.getName().isEmpty()) count++;
        if (!dto.getEmail().isEmpty()) count++;
        if (!dto.getPhone().isEmpty()) count++;
        if (!dto.getEducation().isEmpty()) count++;
        if (!dto.getExperience().isEmpty()) count++;
        if (!dto.getProjects().isEmpty()) count++;
        if (!dto.getAllDetectedSkills().isEmpty()) count++;
        if (!dto.getProgrammingLanguages().isEmpty()) count++;
        if (!dto.getCertifications().isEmpty()) count++;
        if (!dto.getAchievements().isEmpty()) count++;
        if (!dto.getLinkedin().isEmpty()) count++;
        if (!dto.getGithub().isEmpty()) count++;
        if (!dto.getCareerObjective().isEmpty()) count++;
        return count;
    }

    private String safe(Object val) {
        return (val != null && !val.toString().trim().equals("null")) ? val.toString().trim() : "";
    }

    @SuppressWarnings("unchecked")
    private List<String> safeList(Object val) {
        if (val instanceof List) return (List<String>) val;
        return new ArrayList<>();
    }
}
