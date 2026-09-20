package com.resume.analyzer.service;

import com.resume.analyzer.dto.StructuredResumeDto;
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
 * FileParsingService — Autonomous multi-layer resume extraction pipeline.
 *
 * Extraction order:
 *  1. PdfExtractorService (Position-sorted PDF extraction + Link Harvester + Windows Media OCR)
 *  2. Apache Tika AutoDetectParser (DOCX, RTF, TXT, ODT)
 *  3. Apache POI (XWPFWordExtractor) for Word documents
 *
 * After raw text extraction, builds a StructuredResumeDto via ResumeParserService
 * for AI prompt grounding and ATS analysis.
 */
@Service
public class FileParsingService {

    private final Tika tikaMimeDetector = new Tika();
    private final AutoDetectParser tikaParser = new AutoDetectParser();

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private PdfExtractorService pdfExtractorService;

    /**
     * Primary entry point — extracts raw text from any uploaded resume file.
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

        // ── Strategy 1: Dedicated PDF Extractor (position-sorted text + safe OCR) ──
        // NEVER pass a PDF to Apache Tika as a generic fallback. Tika's PDF OCR renderer
        // can rasterize high-resolution scanned pages and exhaust the Render free-tier JVM.
        if (lowerName.endsWith(".pdf")) {
            try {
                String pdfText = pdfExtractorService.extractTextFromPdf(file);
                if (pdfText != null && pdfText.trim().length() >= 160) {
                    System.out.println("[FILE PARSING] PDF extractor succeeded. Length: " + pdfText.length());
                    return pdfText.trim();
                }

                System.err.println("[FILE PARSING] Safe PDF extraction returned insufficient content; rejecting PDF instead of invoking unsafe Tika OCR fallback.");
                throw new IllegalArgumentException(
                        "This PDF could not be read reliably. Please upload a text-based PDF or a clearer scanned PDF.");
            } catch (OutOfMemoryError oom) {
                System.err.println("[FILE PARSING] PDF processing reached JVM memory limit; Tika fallback disabled.");
                throw new IllegalArgumentException(
                        "The uploaded PDF is too complex for the production OCR memory limit. Please export it at normal quality and try again.");
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception pdfEx) {
                System.err.println("[FILE PARSING] Dedicated PDF extractor failed: " + pdfEx.getMessage());
                throw new IllegalArgumentException(
                        "The uploaded PDF could not be processed safely. Please upload a normal-quality PDF.");
            }
        }

        // ── Strategy 2: Apache POI for Word documents (.docx / .doc) ──
        if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
            try {
                String docxText = extractTextFromDocx(file.getInputStream());
                if (docxText != null && docxText.trim().length() > 50) {
                    System.out.println("[FILE PARSING] POI DOCX extraction succeeded. Length: " + docxText.length());
                    return docxText.trim();
                }
            } catch (Exception docxEx) {
                System.err.println("[FILE PARSING] POI DOCX extraction failed: " + docxEx.getMessage());
            }
        }

        // ── Strategy 3: Apache Tika AutoDetectParser (generic fallback) ──
        try {
            String tikaText = extractWithTika(file.getInputStream());
            if (tikaText != null && tikaText.trim().length() > 50) {
                System.out.println("[FILE PARSING] Tika extraction succeeded. Length: " + tikaText.length());
                return tikaText.trim();
            }
        } catch (Exception tikaEx) {
            System.err.println("[FILE PARSING] Tika AutoDetectParser failed: " + tikaEx.getMessage());
        }

        // ── Strategy 4: Tika simple string parsing (last resort) ──
        try {
            String simpleText = tikaMimeDetector.parseToString(file.getInputStream());
            if (simpleText != null && !simpleText.trim().isEmpty()) {
                System.out.println("[FILE PARSING] Tika simple parse succeeded. Length: " + simpleText.length());
                return simpleText.trim();
            }
        } catch (Exception ex) {
            System.err.println("[FILE PARSING] All extraction strategies failed: " + ex.getMessage());
        }

        throw new RuntimeException("Unable to extract text from the uploaded file. Please ensure the file is a valid, non-encrypted document.");
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
                + ", experience=" + dto.getExperience().size()
                + ", projects=" + dto.getProjects().size());

        return parsed;
    }

    /**
     * Apache Tika AutoDetectParser — handles PDF, DOCX, DOC, RTF, ODT, TXT, etc.
     */
    private String extractWithTika(InputStream inputStream) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(1024 * 1024); // 1MB cap
        Metadata metadata = new Metadata();
        tikaParser.parse(inputStream, handler, metadata, new org.apache.tika.parser.ParseContext());
        String text = handler.toString();
        return text.replaceAll("\\s{3,}", "\n").trim();
    }

    /**
     * Apache POI — DOCX extractor for complex Word documents.
     */
    public String extractTextFromDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /**
     * Builds a validated StructuredResumeDto from raw text and pre-parsed map.
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
        dto.setInternships((List<Map<String, String>>) parsed.getOrDefault("internships", List.of()));
        dto.setProjects((List<Map<String, String>>) parsed.getOrDefault("projects", List.of()));

        // ── Skills ──────────────────────────────────────────────────────────
        dto.setProgrammingLanguages(safeList(parsed.get("programmingLanguages")));
        dto.setFrameworks(safeList(parsed.get("frameworks")));
        dto.setSoftSkills(safeList(parsed.get("softSkills")));
        dto.setAllDetectedSkills(safeList(parsed.get("allDetectedSkills")));

        // Categorize additional skill sub-lists from allDetectedSkills pool
        List<String> allSkills = dto.getAllDetectedSkills();
        dto.setDatabases(filterByKeywords(allSkills, DB_KEYWORDS));
        dto.setCloudPlatforms(filterByKeywords(allSkills, CLOUD_KEYWORDS));
        dto.setLibraries(filterByKeywords(allSkills, LIB_KEYWORDS));
        dto.setTools(filterByKeywords(allSkills, TOOL_KEYWORDS));
        dto.setTechnicalSkills(allSkills);

        // ── Credentials ─────────────────────────────────────────────────────
        dto.setCertifications(safeList(parsed.get("certifications")));
        dto.setAchievements(safeList(parsed.get("achievements")));
        dto.setLanguagesSpoken(safeList(parsed.get("languages")));
        dto.setCareerObjective(safe(parsed.get("careerObjective")));

        // ── Metadata ────────────────────────────────────────────────────────
        int detected = countDetectedFields(dto);
        dto.setTotalFieldsDetected(detected);
        dto.setValidated(detected > 3);

        return dto;
    }

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
        "JIRA", "Confluence", "Figma", "Tableau", "Power BI", "Swagger", "IntelliJ", "VS Code", "Canva"
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
