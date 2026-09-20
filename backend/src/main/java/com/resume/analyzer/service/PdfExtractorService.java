package com.resume.analyzer.service;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class PdfExtractorService {

    @org.springframework.beans.factory.annotation.Value("${app.pdf.ocr.enabled:true}")
    private boolean ocrEnabled;

    @org.springframework.beans.factory.annotation.Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @org.springframework.beans.factory.annotation.Value("${app.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @org.springframework.beans.factory.annotation.Value("${app.gemini.fallback-models:gemini-2.5-flash-lite,gemini-2.5-pro}")
    private String geminiFallbackModels;

    @org.springframework.beans.factory.annotation.Value("${app.pdf.gemini-fallback.enabled:false}")
    private boolean geminiPdfFallbackEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate geminiRestTemplate = buildGeminiRestTemplate();

    private static RestTemplate buildGeminiRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(75_000);
        return new RestTemplate(factory);
    }

    private static final int MAX_OCR_PAGES = 2;
    private static final int MAX_OCR_IMAGES = 4;
    private static final int MAX_IMAGE_DIMENSION = 1800;

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        return extractTextFromPdfBytes(file.getBytes());
    }

    public String extractTextFromPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return extractTextFromPdfBytes(bytes);
    }

    public String extractTextFromPdfBytes(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            List<String> links = harvestLinkAnnotations(document);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            boolean hasImages = containsImageObjects(document);
            String trimmedText = text != null ? text.trim() : "";

            System.out.println("[PDF EXTRACTOR] Standard text extracted: " + trimmedText.length()
                    + " chars (images detected: " + hasImages + ")");

            boolean imageHeavy = hasImages && trimmedText.length() < 200;
            boolean needsOcr = trimmedText.length() < 80 || imageHeavy;

            if (needsOcr && ocrEnabled) {
                System.out.println("[PDF EXTRACTOR] OCR required for this PDF: starting safe embedded-image/CLI OCR pipeline...");
                String ocrText = extractWithOcr(document, pdfBytes);
                if (ocrText != null && ocrText.trim().length() > trimmedText.length()) {
                    trimmedText = ocrText.trim();
                }
            } else if (!ocrEnabled) {
                System.out.println("[PDF EXTRACTOR] OCR is disabled; using text-layer extraction only.");
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

            String result = finalOutput.toString().trim();
            if (result.length() < 160) {
                System.err.println("[PDF EXTRACTOR] Extraction rejected: only " + result.length()
                        + " chars. Refusing unsafe downstream fallback.");
                return "";
            }
            return result;
        } catch (OutOfMemoryError oom) {
            System.err.println("[PDF EXTRACTOR] JVM memory limit reached while processing PDF. OCR request aborted safely.");
            return "";
        }
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
                        if (xobj instanceof PDImageXObject) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Compatibility entry point for callers that already have a PDDocument.
     * Uses a tightly bounded PDFBox renderer only as a last-resort fallback.
     */
    public String extractWithOcr(PDDocument document) {
        return extractWithPdfBoxRenderOcr(document);
    }

    /**
     * Production OCR path. On Linux/macOS, extract embedded PDF images with
     * Poppler's pdfimages utility and send only bounded images to Tesseract.
     * This avoids Apache Tika/PDFBox full-page rasterization of high-resolution
     * scanned resumes, which previously caused Java heap exhaustion on Render.
     */
    private String extractWithOcr(PDDocument document, byte[] pdfBytes) {
        String embedded = runPdfImagesOcr(pdfBytes);
        if (embedded != null && embedded.trim().length() >= 160) {
            System.out.println("[PDF EXTRACTOR] Embedded-image OCR succeeded: "
                    + embedded.trim().length() + " chars");
            return embedded.trim();
        }

        System.out.println("[PDF EXTRACTOR] Embedded-image OCR insufficient; using external Poppler pdftoppm page OCR...");
        String rendered = runPopplerPageOcr(pdfBytes);
        if (rendered != null && rendered.trim().length() >= 160) {
            System.out.println("[PDF EXTRACTOR] Poppler page OCR succeeded: "
                    + rendered.trim().length() + " chars");
            return rendered.trim();
        }

        // Optional server-side multimodal fallback. Disabled by default on the free-tier deployment so provider quota cannot make scanned-PDF extraction nondeterministic.
        if (!geminiPdfFallbackEnabled) {
            System.out.println("[PDF EXTRACTOR] Server Gemini PDF fallback disabled; returning to client-side OCR fallback.");
            return rendered == null ? (embedded == null ? "" : embedded.trim()) : rendered.trim();
        }

        // Final server-side multimodal fallback: send the original PDF directly to Gemini.
        // Gemini's PDF/document input can understand both native text and rendered page content,
        // which is useful for scanned resumes that Tesseract cannot reliably transcribe.
        String geminiText = extractWithGeminiPdf(pdfBytes);
        if (geminiText != null && geminiText.trim().length() >= 160) {
            System.out.println("[PDF EXTRACTOR] Gemini multimodal PDF transcription succeeded: "
                    + geminiText.trim().length() + " chars");
            return geminiText.trim();
        }

        System.err.println("[PDF EXTRACTOR] All safe OCR/transcription methods returned insufficient text.");
        return geminiText == null ? (rendered == null ? "" : rendered.trim()) : geminiText.trim();
    }

    private String extractWithGeminiPdf(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0 || geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            System.err.println("[PDF EXTRACTOR] Gemini PDF fallback unavailable: API key not configured.");
            return "";
        }

        // Use current stable Gemini models for scanned-PDF transcription.
        // Flash-Lite is tried first because it is optimized for high-throughput document parsing.
        LinkedHashSet<String> models = new LinkedHashSet<>();
        models.add("gemini-3.5-flash-lite");
        models.add("gemini-3.6-flash");
        models.add("gemini-2.5-flash");

        if (geminiFallbackModels != null) {
            for (String value : geminiFallbackModels.split(",")) {
                if (value != null && !value.trim().isEmpty()) {
                    models.add(value.trim());
                }
            }
        }

        String prompt =
                "Transcribe this resume PDF into plain text for a downstream resume parser. " +
                "Read every page and every visible text region, including text inside images, columns, " +
                "headers, footers, tables and icon-adjacent labels. Preserve names, emails, phone numbers, " +
                "URLs, education, experience, projects, skills, certifications, achievements and dates " +
                "exactly as shown. Do not infer or invent missing information. Output only the transcribed " +
                "resume text and nothing else.";

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> pdfPart = Map.of(
                "inline_data", Map.of(
                        "mime_type", "application/pdf",
                        "data", Base64.getEncoder().encodeToString(pdfBytes)
                )
        );
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(textPart, pdfPart)
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        for (String model : models) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    System.out.println("[PDF EXTRACTOR] Gemini PDF transcription attempt "
                            + attempt + "/2 using model " + model);

                    String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                            + java.net.URLEncoder.encode(model, StandardCharsets.UTF_8)
                            + ":generateContent?key="
                            + java.net.URLEncoder.encode(geminiApiKey.trim(), StandardCharsets.UTF_8);

                    ResponseEntity<String> response = geminiRestTemplate.postForEntity(
                            url,
                            new HttpEntity<>(requestBody, headers),
                            String.class
                    );

                    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        System.err.println("[PDF EXTRACTOR] Gemini model " + model
                                + " returned HTTP " + response.getStatusCode());
                        if (isRetryableGeminiStatus(response.getStatusCode().value()) && attempt < 2) {
                            sleepBeforeGeminiRetry(attempt);
                            continue;
                        }
                        break;
                    }

                    JsonNode root = objectMapper.readTree(response.getBody());
                    StringBuilder text = new StringBuilder();
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray()) {
                        for (JsonNode candidate : candidates) {
                            JsonNode parts = candidate.path("content").path("parts");
                            if (parts.isArray()) {
                                for (JsonNode part : parts) {
                                    JsonNode t = part.path("text");
                                    if (t.isTextual()) {
                                        text.append(t.asText()).append("\n");
                                    }
                                }
                            }
                        }
                    }

                    String result = text.toString().replaceAll("\\s{3,}", "\n").trim();
                    if (result.length() >= 160) {
                        System.out.println("[PDF EXTRACTOR] Gemini PDF transcription succeeded with "
                                + model + ": " + result.length() + " chars");
                        return result;
                    }

                    System.err.println("[PDF EXTRACTOR] Gemini model " + model
                            + " returned insufficient transcription (" + result.length() + " chars).");
                    break;

                } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
                    int status = httpEx.getStatusCode().value();
                    System.err.println("[PDF EXTRACTOR] Gemini model " + model
                            + " HTTP " + status + ": " + httpEx.getResponseBodyAsString());

                    if (isRetryableGeminiStatus(status) && attempt < 2) {
                        sleepBeforeGeminiRetry(attempt);
                        continue;
                    }
                    break;

                } catch (Exception e) {
                    System.err.println("[PDF EXTRACTOR] Gemini model " + model
                            + " failed: " + e.getMessage());
                    break;
                }
            }
        }

        System.err.println("[PDF EXTRACTOR] All configured Gemini PDF transcription models failed.");
        return "";
    }

    private boolean isRetryableGeminiStatus(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private void sleepBeforeGeminiRetry(int attempt) {
        try {
            Thread.sleep(attempt == 1 ? 1200L : 2200L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String runPopplerPageOcr(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0 || isWindowsPlatform()) {
            return "";
        }

        Path tempDir = null;
        Path pdfFile = null;
        try {
            tempDir = Files.createTempDirectory("vrezer-pdftoppm-");
            pdfFile = tempDir.resolve("resume.pdf");
            Files.write(pdfFile, pdfBytes);

            String prefix = tempDir.resolve("page").toString();
            Process process = new ProcessBuilder(
                    "pdftoppm",
                    "-png",
                    "-f", "1",
                    "-l", String.valueOf(MAX_OCR_PAGES),
                    "-r", "180",
                    "-scale-to", "2200",
                    pdfFile.toString(),
                    prefix
            ).redirectErrorStream(true).start();

            String commandOutput = readProcessOutput(process, 45);
            if (process.exitValue() != 0) {
                System.err.println("[PDF EXTRACTOR] pdftoppm failed: " + commandOutput);
                return "";
            }

            List<Path> pages;
            try (Stream<Path> stream = Files.list(tempDir)) {
                pages = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().matches("page-\\d+\\.png"))
                        .sorted()
                        .limit(MAX_OCR_PAGES)
                        .toList();
            }

            if (pages.isEmpty()) {
                System.err.println("[PDF EXTRACTOR] pdftoppm produced no page images.");
                return "";
            }

            StringBuilder allText = new StringBuilder();
            for (Path page : pages) {
                File image = page.toFile();
                String best = runTesseractOcr(image, "3");
                String block = runTesseractOcr(image, "6");
                String sparse = runTesseractOcr(image, "11");

                if (block.length() > best.length()) best = block;
                if (sparse.length() > best.length()) best = sparse;

                if (best.length() < 160) {
                    String alt = runTesseractOcr(image, "4");
                    if (alt.length() > best.length()) best = alt;
                }

                if (!best.trim().isEmpty()) {
                    allText.append(best.trim()).append("\n\n");
                }
            }

            return allText.toString().trim();
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] Poppler page OCR failed: " + e.getMessage());
            return "";
        } finally {
            if (pdfFile != null) {
                try { Files.deleteIfExists(pdfFile); } catch (Exception ignored) {}
            }
            if (tempDir != null) {
                try (Stream<Path> stream = Files.walk(tempDir)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
        }
    }

    private String runPdfImagesOcr(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0 || isWindowsPlatform()) {
            return "";
        }

        Path tempDir = null;
        Path pdfFile = null;
        try {
            tempDir = Files.createTempDirectory("vrezer-pdf-ocr-");
            pdfFile = tempDir.resolve("resume.pdf");
            Files.write(pdfFile, pdfBytes);

            String prefix = tempDir.resolve("page").toString();
            Process process = new ProcessBuilder(
                    "pdfimages", "-j", "-f", "1", "-l", String.valueOf(MAX_OCR_PAGES),
                    pdfFile.toString(), prefix
            ).redirectErrorStream(true).start();

            String commandOutput = readProcessOutput(process, 20);
            if (process.exitValue() != 0) {
                System.err.println("[PDF EXTRACTOR] pdfimages failed: " + commandOutput);
                return "";
            }

            List<Path> images;
            try (Stream<Path> stream = Files.list(tempDir)) {
                images = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith("page-"))
                        .sorted()
                        .limit(MAX_OCR_IMAGES)
                        .toList();
            }

            if (images.isEmpty()) {
                System.out.println("[PDF EXTRACTOR] pdfimages found no embedded image files.");
                return "";
            }

            StringBuilder all = new StringBuilder();
            for (Path imagePath : images) {
                File boundedImage = null;
                try {
                    boundedImage = createBoundedOcrImage(imagePath);
                    if (boundedImage == null) continue;

                    String ocr = runTesseractOcr(boundedImage, "6");
                    if (ocr.length() < 120) {
                        String sparse = runTesseractOcr(boundedImage, "11");
                        if (sparse.length() > ocr.length()) ocr = sparse;
                    }

                    if (!ocr.trim().isEmpty()) {
                        all.append(ocr.trim()).append("\n\n");
                    }
                } catch (OutOfMemoryError oom) {
                    System.err.println("[PDF EXTRACTOR] Skipping oversized embedded image after memory pressure.");
                    break;
                } finally {
                    if (boundedImage != null && boundedImage.exists()) boundedImage.delete();
                }
            }

            return all.toString().trim();
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] Embedded-image OCR failed: " + e.getMessage());
            return "";
        } catch (OutOfMemoryError oom) {
            System.err.println("[PDF EXTRACTOR] Embedded-image OCR hit JVM memory pressure; aborting OCR safely.");
            return "";
        } finally {
            if (pdfFile != null) {
                try { Files.deleteIfExists(pdfFile); } catch (Exception ignored) {}
            }
            if (tempDir != null) {
                try { Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                }); } catch (Exception ignored) {}
            }
        }
    }

    private File createBoundedOcrImage(Path source) throws IOException {
        File temp = File.createTempFile("vrezer_ocr_bounded_", ".png");
        BufferedImage scaled = readImageBounded(source.toFile(), MAX_IMAGE_DIMENSION);
        if (scaled == null) {
            temp.delete();
            return null;
        }

        BufferedImage gray = new BufferedImage(
                scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage(scaled, 0, 0, null);
        } finally {
            g.dispose();
            scaled.flush();
        }

        ImageIO.write(gray, "png", temp);
        gray.flush();
        return temp;
    }

    private BufferedImage readImageBounded(File source, int maxDimension) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(source)) {
            if (input == null) return null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int factor = Math.max(1, (int) Math.ceil(
                        Math.max(width, height) / (double) maxDimension));

                javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                if (factor > 1) {
                    param.setSourceSubsampling(factor, factor, 0, 0);
                }
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    private String readProcessOutput(Process process, int timeoutSeconds) throws IOException, InterruptedException {
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("OCR process timed out");
        }
        try (InputStream is = process.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private String runTesseractOcr(File imageFile, String psm) {
        if (imageFile == null || !imageFile.exists()) return "";
        try {
            Process process = new ProcessBuilder(
                    "tesseract", imageFile.getAbsolutePath(), "stdout",
                    "-l", "eng", "--oem", "1", "--psm", psm
            ).redirectErrorStream(true).start();

            boolean finished = process.waitFor(18, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }

            try (InputStream is = process.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] Tesseract OCR unavailable/failed: " + e.getMessage());
            return "";
        }
    }

    /**
     * Last-resort PDFBox rendering used only when pdfimages cannot extract an
     * embedded scan. One page at 72 DPI keeps Java heap bounded.
     */
    private String extractWithPdfBoxRenderOcr(PDDocument document) {
        if (document == null || document.getNumberOfPages() == 0) return "";

        PDFRenderer renderer = new PDFRenderer(document);
        int pages = Math.min(document.getNumberOfPages(), 1);
        StringBuilder full = new StringBuilder();

        for (int p = 0; p < pages; p++) {
            BufferedImage img = null;
            File tempImg = null;
            try {
                renderer.setSubsamplingAllowed(true);
                img = renderer.renderImageWithDPI(p, 72, ImageType.GRAY);
                tempImg = File.createTempFile("vrezer_pdfbox_ocr_", ".png");
                ImageIO.write(img, "png", tempImg);

                String pageText = runTesseractOcr(tempImg, "6");
                if (pageText.length() < 120) {
                    String sparse = runTesseractOcr(tempImg, "11");
                    if (sparse.length() > pageText.length()) pageText = sparse;
                }
                if (!pageText.trim().isEmpty()) {
                    full.append(pageText.trim()).append("\n\n");
                }
            } catch (OutOfMemoryError oom) {
                System.err.println("[PDF EXTRACTOR] Bounded PDFBox render still exceeded heap; refusing unsafe fallback.");
                return "";
            } catch (Exception ex) {
                System.err.println("[PDF EXTRACTOR] Bounded PDFBox OCR failed on page " + p + ": " + ex.getMessage());
            } finally {
                if (img != null) img.flush();
                if (tempImg != null && tempImg.exists()) tempImg.delete();
            }
        }
        return full.toString().trim();
    }

    private boolean isWindowsPlatform() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
