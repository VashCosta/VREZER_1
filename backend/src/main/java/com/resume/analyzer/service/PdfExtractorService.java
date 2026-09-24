package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PdfExtractorService {

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${GEMINI_PDF_MODEL:gemini-2.5-flash}")
    private String geminiPdfModel;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(90_000);
        return new RestTemplate(factory);
    }

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        return extractTextFromPdfBytes(file.getBytes());
    }

    public String extractTextFromPdf(InputStream inputStream) throws IOException {
        return extractTextFromPdfBytes(inputStream.readAllBytes());
    }

    public String extractTextFromPdfBytes(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes, MemoryUsageSetting.setupTempFileOnly())) {
            List<String> links = harvestLinkAnnotations(document);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            boolean hasImages = containsImageObjects(document);
            String trimmedText = text != null ? text.trim() : "";

            System.out.println("[PDF EXTRACTOR] Standard text extracted: " + trimmedText.length()
                    + " chars (images detected: " + hasImages + ")");

            if (trimmedText.length() < 80 || (trimmedText.length() < 150 && hasImages)) {
                System.out.println("[PDF EXTRACTOR] Native PDF text is sparse; trying Gemini native PDF vision...");
                String aiText = extractWithGeminiPdfVision(pdfBytes);
                if (aiText != null && aiText.trim().length() > trimmedText.length()) {
                    trimmedText = aiText.trim();
                    System.out.println("[PDF EXTRACTOR] Gemini PDF vision extracted " + trimmedText.length() + " chars.");
                } else {
                    System.out.println("[PDF EXTRACTOR] Gemini PDF vision unavailable/empty; using bounded OCR fallback...");
                    String ocrText = extractWithOcr(pdfBytes);
                    if (ocrText != null && ocrText.trim().length() > trimmedText.length()) {
                        trimmedText = ocrText.trim();
                    }
                }
            }

            StringBuilder finalOutput = new StringBuilder(trimmedText);
            if (!links.isEmpty()) {
                StringBuilder linkSection = new StringBuilder();
                for (String link : links) {
                    if (!trimmedText.contains(link)) {
                        linkSection.append("\n").append(link);
                    }
                }
                if (linkSection.length() > 0) {
                    finalOutput.append("\n\nLinks & URLs:").append(linkSection);
                }
            }
            return finalOutput.toString().trim();
        }
    }

    /**
     * Gemini can inspect PDFs natively, including image-only/scanned resume pages.
     * This avoids JVM-heavy PDF-to-image rendering on constrained Render instances.
     */
    private String extractWithGeminiPdfVision(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0 || !isUsableGeminiKey(geminiApiKey)) {
            return "";
        }

        try {
            String model = (geminiPdfModel == null || geminiPdfModel.isBlank())
                    ? "gemini-2.5-flash"
                    : geminiPdfModel.trim();

            String encoded = Base64.getEncoder().encodeToString(pdfBytes);

            Map<String, Object> inlineData = new LinkedHashMap<>();
            inlineData.put("mimeType", "application/pdf");
            inlineData.put("data", encoded);

            Map<String, Object> documentPart = new LinkedHashMap<>();
            documentPart.put("inlineData", inlineData);

            Map<String, Object> textPart = Map.of("text",
                "Extract ALL readable resume content from this PDF. "
              + "This may be a scanned/image-based resume. Preserve the candidate's exact "
              + "name, email, phone, LinkedIn, GitHub, education, experience, internships, "
              + "projects, skills, certifications, achievements, dates and URLs. "
              + "Keep headings and bullet points in a clean plain-text layout. "
              + "Do not summarize, classify, invent, or omit readable text. "
              + "Return only the extracted resume text, with no markdown fences or commentary.");

            Map<String, Object> content = Map.of(
                "parts", List.of(documentPart, textPart)
            );

            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0.0);
            generationConfig.put("maxOutputTokens", 7000);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contents", List.of(content));
            body.put("generationConfig", generationConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + geminiApiKey.trim();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            System.out.println("[PDF EXTRACTOR] Calling Gemini native PDF model: " + model);
            ResponseEntity<String> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.err.println("[PDF EXTRACTOR] Gemini PDF HTTP " + response.getStatusCode().value());
                return "";
            }

            Map<?, ?> root = mapper.readValue(response.getBody(), Map.class);
            Object candidatesObj = root.get("candidates");
            if (!(candidatesObj instanceof List) || ((List<?>) candidatesObj).isEmpty()) return "";

            Object first = ((List<?>) candidatesObj).get(0);
            if (!(first instanceof Map)) return "";
            Map<?, ?> candidate = (Map<?, ?>) first;

            Object contentObj = candidate.get("content");
            if (!(contentObj instanceof Map)) return "";
            Map<?, ?> responseContent = (Map<?, ?>) contentObj;

            Object partsObj = responseContent.get("parts");
            if (!(partsObj instanceof List) || ((List<?>) partsObj).isEmpty()) return "";

            Object part0 = ((List<?>) partsObj).get(0);
            if (!(part0 instanceof Map)) return "";

            Object textObj = ((Map<?, ?>) part0).get("text");
            return textObj == null ? "" : String.valueOf(textObj).trim();
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] Gemini PDF vision failed: " + e.getMessage());
            return "";
        }
    }

    private boolean isUsableGeminiKey(String key) {
        if (key == null) return false;
        String k = key.trim();
        return k.length() >= 15
                && !k.startsWith("YOUR_")
                && !k.startsWith("PASTE_")
                && !k.equalsIgnoreCase("none");
    }

    public List<String> harvestLinkAnnotations(PDDocument document) {
        List<String> links = new ArrayList<>();
        try {
            for (int p = 0; p < document.getNumberOfPages(); p++) {
                PDPage page = document.getPage(p);
                for (PDAnnotation annot : page.getAnnotations()) {
                    if (annot instanceof PDAnnotationLink) {
                        PDAnnotationLink link = (PDAnnotationLink) annot;
                        if (link.getAction() instanceof PDActionURI) {
                            PDActionURI uriAction = (PDActionURI) link.getAction();
                            String uri = uriAction.getURI();
                            if (uri != null && !uri.trim().isEmpty()) {
                                links.add(uri.trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] Could not harvest link annotations: " + e.getMessage());
        }
        return links;
    }

    private boolean containsImageObjects(PDDocument document) {
        try {
            for (int p = 0; p < document.getNumberOfPages(); p++) {
                PDPage page = document.getPage(p);
                PDResources res = page.getResources();
                if (res != null) {
                    for (COSName xName : res.getXObjectNames()) {
                        PDXObject xobj = res.getXObject(xName);
                        if (xobj instanceof PDImageXObject) return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Bounded OCR fallback for environments where Gemini PDF vision is unavailable.
     * Only two pages are rendered, one at a time, at 90 DPI.
     */
    public String extractWithOcr(byte[] pdfBytes) {
        StringBuilder fullOcrText = new StringBuilder();
        File tempPdf = null;

        try {
            tempPdf = File.createTempFile("vrezer_upload_", ".pdf");
            Files.write(tempPdf.toPath(), pdfBytes);

            try (PDDocument bounded = PDDocument.load(pdfBytes, MemoryUsageSetting.setupTempFileOnly())) {
                int maxPages = Math.min(bounded.getNumberOfPages(), 2);

                for (int p = 0; p < maxPages; p++) {
                    File tempBase = null;
                    File tempJpeg = null;
                    try {
                        tempBase = File.createTempFile("vrezer_ocr_", "_page");
                        if (tempBase.exists()) tempBase.delete();
                        String prefix = tempBase.getAbsolutePath();

                        Process render = new ProcessBuilder(
                            "pdftocairo",
                            "-f", String.valueOf(p + 1),
                            "-l", String.valueOf(p + 1),
                            "-r", "90",
                            "-jpeg",
                            "-jpegopt", "quality=65",
                            "-singlefile",
                            tempPdf.getAbsolutePath(),
                            prefix
                        ).redirectErrorStream(true).start();

                        ProcessResult renderResult = waitAndRead(render, 25);
                        if (!renderResult.completed || renderResult.exitCode != 0) {
                            System.err.println("[PDF EXTRACTOR] Poppler failed/timed out on page " + (p + 1)
                                    + ": " + renderResult.output);
                            continue;
                        }

                        tempJpeg = new File(prefix + ".jpg");
                        if (!tempJpeg.exists() || tempJpeg.length() == 0) {
                            System.err.println("[PDF EXTRACTOR] Poppler produced no image for page " + (p + 1));
                            continue;
                        }

                        Process ocr = new ProcessBuilder(
                            "tesseract",
                            tempJpeg.getAbsolutePath(),
                            "stdout",
                            "--psm", "6",
                            "-l", "eng"
                        ).redirectErrorStream(true).start();

                        ProcessResult ocrResult = waitAndRead(ocr, 25);
                        if (!ocrResult.completed || ocrResult.exitCode != 0) {
                            System.err.println("[PDF EXTRACTOR] Tesseract failed/timed out on page " + (p + 1)
                                    + ": " + ocrResult.output);
                            continue;
                        }

                        if (ocrResult.output != null && !ocrResult.output.trim().isEmpty()) {
                            fullOcrText.append(ocrResult.output.trim()).append("\n\n");
                        }
                    } catch (Exception ex) {
                        System.err.println("[PDF EXTRACTOR] OCR on page " + (p + 1) + " failed: " + ex.getMessage());
                    } finally {
                        deleteQuietly(tempJpeg);
                        deleteQuietly(tempBase);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] OCR pipeline failed: " + e.getMessage());
        } finally {
            deleteQuietly(tempPdf);
        }

        return fullOcrText.toString().trim();
    }

    private ProcessResult waitAndRead(Process process, int timeoutSeconds) {
        if (process == null) return new ProcessResult(false, -1, "");
        try {
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new ProcessResult(false, -1, "");
            }

            String output;
            try (InputStream in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (output.length() > 20_000) output = output.substring(0, 20_000);
            return new ProcessResult(true, process.exitValue(), output);
        } catch (Exception e) {
            process.destroyForcibly();
            return new ProcessResult(false, -1, "");
        }
    }

    private void deleteQuietly(File file) {
        if (file != null) {
            try { Files.deleteIfExists(file.toPath()); } catch (Exception ignored) {}
        }
    }

    private static final class ProcessResult {
        private final boolean completed;
        private final int exitCode;
        private final String output;

        private ProcessResult(boolean completed, int exitCode, String output) {
            this.completed = completed;
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }
}
