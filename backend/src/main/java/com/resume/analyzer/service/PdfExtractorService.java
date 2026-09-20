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
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PdfExtractorService {

    @org.springframework.beans.factory.annotation.Value("${app.pdf.ocr.enabled:false}")
    private boolean ocrEnabled;

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        return extractTextFromPdfBytes(file.getBytes());
    }

    public String extractTextFromPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return extractTextFromPdfBytes(bytes);
    }

    public String extractTextFromPdfBytes(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            // 1. Harvest interactive link annotations (LinkedIn, GitHub, mailto, portfolio)
            List<String> links = harvestLinkAnnotations(document);

            // 2. Standard position-sorted text extraction
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            boolean hasImages = containsImageObjects(document);
            String trimmedText = text != null ? text.trim() : "";

            System.out.println("[PDF EXTRACTOR] Standard text extracted: " + trimmedText.length() + " chars (images detected: " + hasImages + ")");

            // 3. OCR activates for short/image-heavy PDFs. Render has bounded Tesseract OCR.
            if (ocrEnabled && (trimmedText.length() < 80 || (trimmedText.length() < 150 && hasImages))) {
                System.out.println("[PDF EXTRACTOR] OCR enabled: starting bounded OCR fallback...");
                String ocrText = extractWithOcr(document);
                if (ocrText != null && ocrText.trim().length() > trimmedText.length()) {
                    trimmedText = ocrText.trim();
                }
            } else if (!ocrEnabled && (trimmedText.length() < 80 || (trimmedText.length() < 200 && hasImages))) {
                System.out.println("[PDF EXTRACTOR] OCR unavailable; text-layer extraction may be incomplete.");
            }

            // 4. Merge harvested clickable links if they are not already in the text
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
            // Never treat a link-only/image-only PDF as a successful full extraction.
            if (result.length() < 160) return "";
            return result;
        }
    }

    /**
     * Harvests all URI link annotations from the PDF document pages.
     */
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
     * Multi-page rendering and OCR extraction using Native Windows Media OCR bridge.
     */
    public String extractWithOcr(PDDocument document) {
        StringBuilder fullOcrText = new StringBuilder();
        PDFRenderer renderer = new PDFRenderer(document);
        int maxPages = Math.min(document.getNumberOfPages(), 2);

        File scriptFile = resolveOcrScriptFile();

        for (int p = 0; p < maxPages; p++) {
            File tempImg = null;
            try {
                // Keep cloud OCR deliberately bounded. 120 DPI grayscale is sufficient
                // for most resume scans while using substantially less heap than 200 DPI color.
                renderer.setSubsamplingAllowed(true);
                 BufferedImage img = renderer.renderImageWithDPI(p, 105, ImageType.GRAY);
                tempImg = File.createTempFile("vrezer_pdf_ocr_p" + p + "_", ".png");
                ImageIO.write(img, "png", tempImg);
                img.flush();

                String pageOcr = isWindowsPlatform() && scriptFile != null && scriptFile.exists()
                        ? runWindowsOcr(scriptFile, tempImg)
                        : runTesseractOcr(tempImg);
                if (pageOcr != null && !pageOcr.trim().isEmpty()) {
                    fullOcrText.append(pageOcr.trim()).append("\n\n");
                }
            } catch (Exception ex) {
                System.err.println("[PDF EXTRACTOR] OCR on page " + p + " failed: " + ex.getMessage());
            } finally {
                if (tempImg != null && tempImg.exists()) {
                    tempImg.delete();
                }
            }
        }

        return fullOcrText.toString().trim();
    }

    private boolean isWindowsPlatform() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Linux/macOS OCR fallback used by Render containers. */
    private String runTesseractOcr(File imageFile) {
        if (imageFile == null || !imageFile.exists()) return "";
        try {
            Process process = new ProcessBuilder(
                    "tesseract", imageFile.getAbsolutePath(), "stdout", "-l", "eng", "--psm", "6"
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

    private File resolveOcrScriptFile() {
        // Try local resource / filesystem paths
        File[] candidates = {
            new File("src/main/resources/WinOcr.ps1"),
            new File("backend/src/main/resources/WinOcr.ps1"),
            new File("RES_2026/backend/src/main/resources/WinOcr.ps1")
        };
        for (File c : candidates) {
            if (c.exists()) return c;
        }

        // Extract from classpath resource if running inside a jar
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("WinOcr.ps1")) {
            if (is != null) {
                File tempScript = File.createTempFile("WinOcr_", ".ps1");
                tempScript.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tempScript)) {
                    is.transferTo(fos);
                }
                return tempScript;
            }
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] Unable to extract WinOcr.ps1 from classpath: " + e.getMessage());
        }

        return new File("src/main/resources/WinOcr.ps1");
    }

    private String runWindowsOcr(File scriptFile, File imageFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", scriptFile.getAbsolutePath(),
                "-ImagePath", imageFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            process.waitFor();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[PDF EXTRACTOR] runWindowsOcr error: " + e.getMessage());
            return "";
        }
    }
}
