package com.resume.analyzer.controller;

import com.resume.analyzer.service.FileParsingService;
import com.resume.analyzer.service.SkillIntelligenceService;
import com.resume.analyzer.service.VrezerAiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/analyzer")
@CrossOrigin(
        origins = {
                "https://vashcosta.github.io",
                "https://vrezer-3-0.vercel.app",
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:9000"
        },
        allowedHeaders = "*",
        methods = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.HEAD,
                RequestMethod.PATCH
        },
        allowCredentials = "true",
        maxAge = 3600
)
public class VrezerAnalyzerController {

    @Autowired
    private FileParsingService fileParsingService;

    @Autowired
    private VrezerAiAgentService vrezerAiAgentService;

    @Autowired
    private SkillIntelligenceService skillIntelligenceService;

    @Autowired
    private com.resume.analyzer.service.MarketIntelligenceService marketIntelligenceService;

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("commit", "bf48aeac");
        version.put("status", "ONLINE");
        version.put("environment", System.getenv("SPRING_PROFILES_ACTIVE") != null ? System.getenv("SPRING_PROFILES_ACTIVE") : "production");
        version.put("backendVersion", "VREZER 3.0 Production Build");
        version.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(version);
    }

    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractText(@RequestParam("file") MultipartFile file) {
        String filename = (file != null && file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Resume";
        System.out.println("[ANALYZER] Extracting resume text from file: " + filename);
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "RESUME_EXTRACTION_FAILED",
                    "text", "",
                    "message", "Please upload a valid, non-empty resume document."
                ));
            }
            String rawText = fileParsingService.extractText(file);
            if (rawText == null || rawText.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "RESUME_EXTRACTION_FAILED",
                    "text", "",
                    "message", "Unable to extract readable text from document. Please verify PDF/DOCX formatting."
                ));
            }
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "text", rawText
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "RESUME_EXTRACTION_FAILED",
                "text", "",
                "message", "Extraction error: " + e.getMessage()
            ));
        }
    }

    // Production async upload pipeline: queue -> process -> poll status.
    private static final int ASYNC_MAX_FILE_BYTES = 20 * 1024 * 1024;
    private static final long ASYNC_JOB_TTL_MS = 20L * 60L * 1000L;
    private static final int ASYNC_MAX_TRACKED_JOBS = 50;
    private final ConcurrentMap<String, AsyncAnalysisJob> asyncJobs = new ConcurrentHashMap<>();
    private final ExecutorService asyncAnalysisExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "vrezer-analysis-worker");
        t.setDaemon(true);
        return t;
    });

    @PostMapping("/analyze-file-async")
    public ResponseEntity<Map<String, Object>> queueFileAnalysis(@RequestParam("file") MultipartFile file) {
        cleanupAsyncJobs();
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "No resume file was provided."));
        }
        if (file.getSize() > ASYNC_MAX_FILE_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "Resume file is too large. Maximum supported size is 20 MB."));
        }
        if (asyncJobs.size() >= ASYNC_MAX_TRACKED_JOBS) {
            return ResponseEntity.status(429).body(Map.of("status", "ERROR", "error", "VREZER analysis queue is temporarily full. Please retry shortly."));
        }
        final byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "Could not read the uploaded resume: " + e.getMessage()));
        }
        String jobId = "job_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        AsyncAnalysisJob job = new AsyncAnalysisJob(jobId, file.getOriginalFilename(), System.currentTimeMillis());
        asyncJobs.put(jobId, job);
        try {
            asyncAnalysisExecutor.submit(() -> processAsyncAnalysis(job, fileBytes, file.getContentType()));
        } catch (RuntimeException ex) {
            asyncJobs.remove(jobId);
            return ResponseEntity.status(503).body(Map.of("status", "ERROR", "error", "VREZER analysis worker is unavailable. Please retry."));
        }
        return ResponseEntity.accepted().body(Map.of("status", "QUEUED", "jobId", jobId, "message", "Resume accepted. VREZER analysis is running server-side."));
    }

    @GetMapping("/analyze-file-status/{jobId}")
    public ResponseEntity<Map<String, Object>> getAsyncAnalysisStatus(@PathVariable String jobId) {
        cleanupAsyncJobs();
        AsyncAnalysisJob job = asyncJobs.get(jobId);
        if (job == null) {
            return ResponseEntity.status(404).body(Map.of("status", "ERROR", "error", "Analysis job was not found or has expired."));
        }
        return ResponseEntity.ok(job.toResponse());
    }

    private void processAsyncAnalysis(AsyncAnalysisJob job, byte[] fileBytes, String contentType) {
        job.status = "PROCESSING";
        job.message = "Extracting resume content…";
        try {
            String filename = job.filename != null && !job.filename.isBlank() ? job.filename : "resume.pdf";
            MultipartFile inMemoryFile = new InMemoryMultipartFile("file", filename,
                    contentType != null ? contentType : "application/octet-stream", fileBytes);
            String extractedText = fileParsingService.extractText(inMemoryFile);
            if (extractedText == null || extractedText.trim().length() < 20) {
                throw new IllegalArgumentException("The uploaded resume could not be read reliably. Please upload a clearer PDF/DOCX file.");
            }
            job.message = "Running AI + RAG career intelligence…";
            Map<String, Object> dossier = vrezerAiAgentService.analyzeResumeWithAiAgent(extractedText, "", null);
            if (dossier == null || dossier.isEmpty()) throw new IllegalStateException("VREZER returned an empty analysis dossier.");
            job.result = dossier;
            job.status = "SUCCESS";
            job.message = "Analysis complete.";
            job.completedAt = System.currentTimeMillis();
        } catch (OutOfMemoryError oom) {
            job.status = "ERROR";
            job.message = "Resume processing exceeded the available server memory. Please upload a smaller or simpler document.";
            job.completedAt = System.currentTimeMillis();
            System.err.println("[ASYNC ANALYSIS] OutOfMemoryError while processing " + job.filename);
        } catch (Exception e) {
            job.status = "ERROR";
            job.message = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName() + " occurred during analysis.";
            job.completedAt = System.currentTimeMillis();
            System.err.println("[ASYNC ANALYSIS] Job " + job.id + " failed: " + job.message);
            e.printStackTrace();
        }
    }

    private void cleanupAsyncJobs() {
        long cutoff = System.currentTimeMillis() - ASYNC_JOB_TTL_MS;
        for (Map.Entry<String, AsyncAnalysisJob> entry : asyncJobs.entrySet()) {
            AsyncAnalysisJob job = entry.getValue();
            long lastTouched = job.completedAt > 0 ? job.completedAt : job.createdAt;
            if (lastTouched < cutoff) asyncJobs.remove(entry.getKey(), job);
        }
    }

    private static final class AsyncAnalysisJob {
        final String id;
        final String filename;
        final long createdAt;
        volatile long completedAt;
        volatile String status = "QUEUED";
        volatile String message = "Waiting for the VREZER analysis worker…";
        volatile Map<String, Object> result;
        AsyncAnalysisJob(String id, String filename, long createdAt) { this.id = id; this.filename = filename; this.createdAt = createdAt; }
        Map<String, Object> toResponse() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", status); out.put("jobId", id); out.put("filename", filename); out.put("message", message);
            if ("SUCCESS".equals(status) && result != null) out.putAll(result);
            if ("ERROR".equals(status)) out.put("error", message);
            return out;
        }
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String name, originalFilename, contentType;
        private final byte[] bytes;
        InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name; this.originalFilename = originalFilename; this.contentType = contentType; this.bytes = bytes != null ? bytes : new byte[0];
        }
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes.clone(); }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
        @Override public void transferTo(Path dest) throws IOException { Files.write(dest, bytes); }
    }

    @PostMapping("/jobs")
    public ResponseEntity<List<Map<String, String>>> searchLiveJobs(@RequestBody(required = false) Map<String, Object> payload) {
        String query = payload != null ? String.valueOf(payload.getOrDefault("query", payload.getOrDefault("careerDomain", ""))) : "";
        String location = payload != null ? String.valueOf(payload.getOrDefault("location", "India")) : "India";
        String experienceLevel = payload != null ? String.valueOf(payload.getOrDefault("experienceLevel", "FRESHER")) : "FRESHER";
        
        List<String> skills = new ArrayList<>();
        if (payload != null && payload.get("skills") instanceof List) {
            skills = (List<String>) payload.get("skills");
        }
        
        List<Map<String, String>> jobs = marketIntelligenceService.fetchLiveMarketJobs(query, skills, location, experienceLevel);
        return ResponseEntity.ok(jobs != null ? jobs : List.of());
    }

    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeResume(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = "X-GEMINI-API-KEY", required = false) String headerApiKey) {
        String rawText = "";
        String jobDescription = "";
        String customApiKey = headerApiKey;

        try {

            if (rawBody != null && !rawBody.trim().isEmpty()) {
                String trimmed = rawBody.trim();
                if (trimmed.startsWith("{")) {
                    try {
                        Map<?, ?> payload = mapper.readValue(trimmed, Map.class);
                        rawText = payload.containsKey("resumeText") ? String.valueOf(payload.get("resumeText")) : "";
                        jobDescription = payload.containsKey("jobDescription") ? String.valueOf(payload.get("jobDescription")) : "";
                        if (payload.get("apiKey") != null && !String.valueOf(payload.get("apiKey")).trim().isEmpty()) {
                            customApiKey = String.valueOf(payload.get("apiKey")).trim();
                        }
                    } catch (Exception ex) {
                        rawText = trimmed;
                    }
                } else {
                    rawText = trimmed;
                }
            }

            if (rawText == null || rawText.trim().length() < 20) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Resume text is empty or too short to analyze. Please upload a valid PDF or document.");
                err.put("name", "");
                err.put("atsScore", 0);
                return ResponseEntity.ok(err);
            }
            Map<String, Object> aiResult = vrezerAiAgentService.analyzeResumeWithAiAgent(rawText, jobDescription, customApiKey);
            return ResponseEntity.ok(aiResult);

        } catch (Exception e) {
            System.err.println("[CONTROLLER] Pipeline execution error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
            Map<String, Object> errResponse = new LinkedHashMap<>();
            errResponse.put("status", "ERROR");
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = e.getClass().getSimpleName() + " occurred during AI analysis execution.";
            }
            errResponse.put("error", errorMsg);
            return ResponseEntity.badRequest().body(errResponse);
        }
    }




    @PostMapping("/validate-key")
    public ResponseEntity<Map<String, Object>> validateApiKey(@RequestBody(required = false) Map<String, String> payload) {
        String apiKey = payload != null ? payload.get("apiKey") : null;
        Map<String, Object> validation = vrezerAiAgentService.testAndValidateGeminiKey(apiKey);
        return ResponseEntity.ok(validation);
    }

    @PostMapping("/jd-match")
    public ResponseEntity<Map<String, Object>> matchJobDescription(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-GEMINI-API-KEY", required = false) String headerApiKey) {
        String rawText = payload.getOrDefault("resumeText", "");
        String jobDescription = payload.getOrDefault("jobDescription", "");
        String apiKey = payload.getOrDefault("apiKey", headerApiKey);

        if (rawText.trim().length() < 20 || jobDescription.trim().length() < 10) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Please provide both valid resume text and job description content for comparison.",
                "matchPercentage", 0
            ));
        }

        Map<String, Object> aiResult = vrezerAiAgentService.analyzeResumeWithAiAgent(rawText, jobDescription, apiKey);
        return ResponseEntity.ok(aiResult);
    }

    /**
     * Extracts raw text and returns a fully structured, validated resume JSON.
     * This is the two-step flow: extract → preview structured data → then analyze.
     */
    @PostMapping("/structured-extract")
    public ResponseEntity<Map<String, Object>> extractStructured(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided."));
            }
            Map<String, Object> structured = fileParsingService.extractAndStructure(file);
            return ResponseEntity.ok(structured);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to extract and structure resume: " + e.getMessage()));
        }
    }

    /**
     * Calls ESCO Skills API, O*NET Occupation Data, and LanguageTool
     * to enrich skill intelligence beyond what the AI can infer alone.
     */
    @PostMapping("/skill-intelligence")
    public ResponseEntity<Map<String, Object>> getSkillIntelligence(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<String> skills = (List<String>) payload.getOrDefault("skills", List.of());
            String careerDomain = String.valueOf(payload.getOrDefault("careerDomain", "Software Engineering"));
            String resumeText = String.valueOf(payload.getOrDefault("resumeText", ""));
            String experienceLevel = String.valueOf(payload.getOrDefault("experienceLevel", "mid-level"));

            Map<String, Object> enrichment = skillIntelligenceService.enrichSkillIntelligence(
                skills, careerDomain, resumeText, experienceLevel);
            return ResponseEntity.ok(enrichment);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "ERROR",
                "error", "Skill intelligence enrichment failed: " + e.getMessage()));
        }
    }

    /**
     * Standalone grammar and writing quality check via LanguageTool API.
     */
    @PostMapping("/grammar-check")
    public ResponseEntity<Map<String, Object>> checkGrammar(@RequestBody Map<String, String> payload) {
        try {
            String text = payload.getOrDefault("text", "");
            if (text.trim().length() < 20) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Text too short for grammar analysis. Minimum 20 characters required."));
            }
            Map<String, Object> grammarResult = skillIntelligenceService.checkGrammar(text);
            return ResponseEntity.ok(grammarResult);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "ERROR",
                "error", "Grammar check failed: " + e.getMessage()));
        }
    }

    /**
     * Generates a professional PDF report from the AI analysis result.
     * Uses Apache PDFBox to write text-based content to a PDF byte stream.
     */
    @PostMapping(value = "/generate-pdf-report", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generatePdfReport(@RequestBody Map<String, Object> aiResult) {
        try {
            byte[] pdfBytes = buildPdfReport(aiResult);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                    .filename("VREZER_Career_Intelligence_Report.pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] buildPdfReport(Map<String, Object> d) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(
                org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            doc.addPage(page);

            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                     new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                org.apache.pdfbox.pdmodel.font.PDType1Font font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
                org.apache.pdfbox.pdmodel.font.PDType1Font fontNormal = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;

                float margin = 50;
                float yPos = page.getMediaBox().getHeight() - margin;
                float lineHeight = 16;

                // Title
                cs.beginText();
                cs.setFont(font, 16);
                cs.setNonStrokingColor(new java.awt.Color(30, 30, 50));
                cs.newLineAtOffset(margin, yPos);
                cs.showText("VREZER AI Career Intelligence Report");
                cs.endText();
                yPos -= lineHeight * 2;

                // Write key fields
                String[][] fields = {
                    {"Candidate", getStr(d, "name", "N/A")},
                    {"Role", getStr(d, "role", "N/A")},
                    {"Career Domain", getStr(d, "careerDomain", "N/A")},
                    {"Career Level", getStr(d, "careerLevel", "N/A")},
                    {"ATS Score", getStr(d, "atsScore", "0") + "/100  [" + getStr(d, "atsScoreText", "") + "]"},
                    {"AI Model", getStr(d, "aiModelUsed", "N/A")},
                    {"Confidence", getStr(d, "confidenceScore", "0") + "%"},
                    {"Email", getStr(d, "email", "N/A")},
                    {"LinkedIn", getStr(d, "linkedin", "N/A")},
                    {"GitHub", getStr(d, "github", "N/A")}
                };

                for (String[] field : fields) {
                    cs.beginText();
                    cs.setFont(font, 10);
                    cs.newLineAtOffset(margin, yPos);
                    cs.showText(field[0] + ":");
                    cs.setFont(fontNormal, 10);
                    cs.newLineAtOffset(100, 0);
                    String val = field[1].length() > 60 ? field[1].substring(0, 60) + "..." : field[1];
                    cs.showText(val);
                    cs.endText();
                    yPos -= lineHeight;
                    if (yPos < margin) break;
                }

                // Summary section
                yPos -= lineHeight;
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Professional Summary");
                cs.endText();
                yPos -= lineHeight;

                String summary = getStr(d, "professionalSummary", "");
                if (!summary.isEmpty()) {
                    // Word wrap at ~80 chars
                    List<String> lines = wrapText(summary, 80);
                    for (String line : lines) {
                        if (yPos < margin) break;
                        cs.beginText();
                        cs.setFont(fontNormal, 9);
                        cs.newLineAtOffset(margin, yPos);
                        cs.showText(line);
                        cs.endText();
                        yPos -= lineHeight - 2;
                    }
                }
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxChars) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (!current.isEmpty()) current.append(" ");
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    @PostMapping(value = "/download-zip", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> downloadZipReport(@RequestBody Map<String, Object> aiResult) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);

            // 1. Complete AI Report
            addZipEntry(zos, "Complete_AI_Report.md", buildCompleteAiReport(aiResult));
            
            // 2. ATS Compatibility Report
            addZipEntry(zos, "ATS_Compatibility_Report.md", buildAtsReport(aiResult));

            // 3. Resume Improvement Report
            addZipEntry(zos, "Resume_Improvement_Report.md", buildImprovementReport(aiResult));

            // 4. Skill Gap Report
            addZipEntry(zos, "Skill_Gap_Report.md", buildSkillGapReport(aiResult));

            // 5. Career Roadmap
            addZipEntry(zos, "Career_Roadmap.md", buildRoadmapReport(aiResult));

            // 6. Interview Preparation Guide
            addZipEntry(zos, "Interview_Preparation_Guide.md", buildInterviewGuide(aiResult));

            // 7. Dashboard Summary
            addZipEntry(zos, "Dashboard_Summary.md", buildDashboardSummary(aiResult));

            // 8. Resume vs JD Report
            addZipEntry(zos, "Resume_vs_JD_Report.md", buildResumeVsJdReport(aiResult));

            // 9. AI Cover Letter
            addZipEntry(zos, "AI_Cover_Letter.md", buildCoverLetter(aiResult));

            zos.close();

            byte[] zipBytes = baos.toByteArray();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                    .filename("vrezer_career_intelligence.zip").build());

            return ResponseEntity.ok().headers(headers).body(zipBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addZipEntry(java.util.zip.ZipOutputStream zos, String filename, String content) throws Exception {
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String getStr(Map<?, ?> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object val = map.get(key);
        return val == null ? defaultValue : String.valueOf(val);
    }

    private String buildCompleteAiReport(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER Complete Career Intelligence Report\n\n");
        sb.append("## Candidate Information\n");
        sb.append("- **Name**: ").append(getStr(d, "name", "N/A")).append("\n");
        sb.append("- **Email**: ").append(getStr(d, "email", "N/A")).append("\n");
        sb.append("- **Phone**: ").append(getStr(d, "phone", "N/A")).append("\n");
        sb.append("- **LinkedIn**: ").append(getStr(d, "linkedin", "N/A")).append("\n");
        sb.append("- **GitHub**: ").append(getStr(d, "github", "N/A")).append("\n\n");
        sb.append("## Professional Summary\n");
        sb.append(getStr(d, "professionalSummary", "")).append("\n\n");
        sb.append("## Strategic Career Forecast\n");
        sb.append(getStr(d, "strategicForecast", "")).append("\n\n");
        sb.append("## Key Scores\n");
        sb.append("- **ATS Score**: ").append(getStr(d, "atsScore", "0")).append("/100\n");
        sb.append("- **Profile Strength**: ").append(getStr(d, "profileStrength", "0")).append("/100\n");
        sb.append("- **Confidence Score**: ").append(getStr(d, "confidenceScore", "0")).append("/100\n\n");
        return sb.toString();
    }

    private String buildAtsReport(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER ATS Compatibility Report\n\n");
        sb.append("## ATS Compatibility Score: ").append(getStr(d, "atsScore", "0")).append("/100\n");
        sb.append("### Score Category: ").append(getStr(d, "atsScoreText", "N/A")).append("\n\n");
        
        Object detailsObj = d.get("atsScoreDetails");
        if (detailsObj instanceof Map) {
            Map<?, ?> details = (Map<?, ?>) detailsObj;
            sb.append("### Score Breakdown\n");
            sb.append("- **Section Completeness**: ").append(getStr(details, "sectionCompletenessScore", "N/A")).append("/100\n");
            sb.append("- **Keyword Density**: ").append(getStr(details, "keywordOptimizationScore", "N/A")).append("/100\n");
            sb.append("- **Formatting Quality**: ").append(getStr(details, "formattingScore", "N/A")).append("/100\n");
            sb.append("- **Achievement Metrics**: ").append(getStr(details, "achievementScore", "N/A")).append("/100\n\n");
            sb.append("### Explanation\n").append(getStr(details, "explanation", "N/A")).append("\n\n");
        }
        return sb.toString();
    }

    private String buildImprovementReport(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER Resume Improvement Report\n\n");
        sb.append("## Strategic Improvements\n");
        Object qualityObj = d.get("resumeQualityScoreDetails");
        if (qualityObj instanceof Map) {
            Map<?, ?> q = (Map<?, ?>) qualityObj;
            sb.append("- **Observations**: ").append(getStr(q, "explanation", "No structural remarks available.")).append("\n\n");
        }
        
        Object rewritesObj = d.get("bulletPointRewrites");
        if (rewritesObj instanceof List) {
            sb.append("## Bullet Point Rewrites (High impact conversions)\n");
            List<?> list = (List<?>) rewritesObj;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    sb.append("### Original:\n> ").append(getStr(m, "original", "")).append("\n");
                    sb.append("### AI Suggested High Impact Revision:\n> **").append(getStr(m, "aiRewritten", "")).append("**\n");
                    sb.append("- **Metrics Boost**: ").append(getStr(m, "impactMetricMetric", "Measurable Conversion Improvement")).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    private String buildSkillGapReport(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER Skill Gap & Domain Competency Report\n\n");
        sb.append("## Career Specialization: ").append(getStr(d, "role", "N/A")).append("\n");
        sb.append("## Core Domain: ").append(getStr(d, "careerDomain", "N/A")).append("\n\n");
        
        Object gapObj = d.get("technicalSkillsScoreDetails");
        if (gapObj instanceof Map) {
            Map<?, ?> g = (Map<?, ?>) gapObj;
            sb.append("### Technical Alignment Analysis\n");
            sb.append(getStr(g, "explanation", "Aligns with industry domain standards.")).append("\n\n");
        }
        
        sb.append("## Missing Competencies & Skill Gaps\n");
        Object missing = d.get("missingSkills");
        if (missing instanceof List) {
            List<?> list = (List<?>) missing;
            for (Object skill : list) {
                sb.append("- ").append(skill).append("\n");
            }
        } else {
            sb.append("- None found or fully aligned.\n");
        }
        return sb.toString();
    }

    private String buildRoadmapReport(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER Professional Upskilling Roadmap\n\n");
        sb.append("## Phased Learning Timeline\n\n");
        Object roadObj = d.get("careerGrowthTimeline");
        if (roadObj instanceof List) {
            List<?> list = (List<?>) roadObj;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    sb.append("### Stage: ").append(getStr(m, "stage", "Phase")).append("\n");
                    sb.append("- **Target Title**: ").append(getStr(m, "title", "Upgraded Competency")).append("\n");
                    sb.append("- **Expected Compensation Progression**: ").append(getStr(m, "expectedSalaryProgression", "Competitive Market Increase")).append("\n");
                    sb.append("- **Recommended Certifications**: ").append(getStr(m, "recommendedCertifications", "N/A")).append("\n");
                    sb.append("- **Strategic Notes**: ").append(getStr(m, "roadmapNotes", "")).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    private String buildInterviewGuide(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER AI Interview Preparation Guide\n\n");
        
        Object prepObj = d.get("interviewPreparation");
        if (prepObj instanceof Map) {
            Map<?, ?> prep = (Map<?, ?>) prepObj;
            
            sb.append("## 1. Technical Domain Deep Dives\n\n");
            Object techObj = prep.get("technicalQuestions");
            if (techObj instanceof List) {
                for (Object q : (List<?>) techObj) {
                    if (q instanceof Map) {
                        Map<?, ?> mq = (Map<?, ?>) q;
                        sb.append("### Question:\n").append(getStr(mq, "question", "")).append("\n");
                        sb.append("### Model Recommendation:\n").append(getStr(mq, "modelAnswer", "")).append("\n\n");
                    }
                }
            }
            
            sb.append("## 2. STAR Method Behavioral Questions\n\n");
            Object behObj = prep.get("behavioralQuestions");
            if (behObj instanceof List) {
                for (Object q : (List<?>) behObj) {
                    if (q instanceof Map) {
                        Map<?, ?> mq = (Map<?, ?>) q;
                        sb.append("### Question:\n").append(getStr(mq, "question", "")).append("\n");
                        sb.append("### Recommended Response (STAR Method):\n").append(getStr(mq, "starAnswer", "")).append("\n\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private String buildDashboardSummary(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER Dashboard Summary\n\n");
        sb.append("Candidate name: ").append(getStr(d, "name", "N/A")).append("\n");
        sb.append("Target Specialization: ").append(getStr(d, "role", "N/A")).append("\n");
        sb.append("Domain Match: ").append(getStr(d, "careerDomain", "N/A")).append("\n");
        sb.append("ATS Compatibility Score: ").append(getStr(d, "atsScore", "0")).append("/100\n");
        sb.append("Confidence Indicator: ").append(getStr(d, "confidenceScore", "0")).append("%\n");
        return sb.toString();
    }

    private String buildResumeVsJdReport(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# VREZER Resume vs Job Description Gap Analysis\n\n");
        sb.append("## Analysis Parameters\n");
        sb.append("- Candidate domain matching: ").append(getStr(d, "careerDomain", "N/A")).append("\n");
        sb.append("- Match Score vs Target Job: ").append(getStr(d, "atsScore", "0")).append("/100\n\n");
        sb.append("## Keyword Gap Findings\n");
        Object missing = d.get("missingSkills");
        if (missing instanceof List) {
            for (Object skill : (List<?>) missing) {
                sb.append("- [MISSING] ").append(skill).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildCoverLetter(Map<String, Object> d) {
        String name = getStr(d, "name", "Candidate");
        String role = getStr(d, "role", "Specialist");
        String domain = getStr(d, "careerDomain", "Professional Domain");
        
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Custom-Tailored Cover Letter\n\n");
        sb.append("Dear Hiring Manager,\n\n");
        sb.append("I am writing to express my strong interest in the ").append(role).append(" position. ");
        sb.append("With a proven track record in ").append(domain).append(", ");
        sb.append("I am confident in my ability to make a significant contribution to your organization.\n\n");
        sb.append("My professional background is built on a strong foundation of key skills, including the core competencies identified in my dossier. ");
        sb.append("I thrive in dynamic environments that require rigorous execution and analytical problem solving.\n\n");
        sb.append("Thank you for your time and consideration. I welcome the opportunity to discuss how my qualifications align with your strategic needs.\n\n");
        sb.append("Sincerely,\n");
        sb.append(name).append("\n");
        return sb.toString();
    }
}
