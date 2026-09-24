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
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.*;

@Service
public class PdfExtractorService {

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

            // 3. If text is sparse / empty and document has pages/images, execute Autonomous OCR
            if (trimmedText.length() < 80 || (trimmedText.length() < 150 && hasImages)) {
                System.out.println("[PDF EXTRACTOR] Triggering high-precision OCR extraction pipeline...");
                String ocrText = extractWithOcr(pdfBytes);
                if (ocrText != null && ocrText.trim().length() > trimmedText.length()) {
                    trimmedText = ocrText.trim();
                }
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

            return finalOutput.toString().trim();
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
     * Bounded Linux OCR fallback for scanned PDFs.
     *
     * Poppler renders one page at a time at 110 DPI and Tesseract processes that
     * single image. No PDFRenderer/large BufferedImage is kept in the JVM.
     */
    public String extractWithOcr(byte[] pdfBytes) {
        StringBuilder fullOcrText = new StringBuilder();
        File tempPdf = null;

        try {
            tempPdf = File.createTempFile("vrezer_upload_", ".pdf");
            Files.write(tempPdf.toPath(), pdfBytes);

            // Process at most six pages; each page is rendered independently.
            try (PDDocument bounded = PDDocument.load(pdfBytes)) {
                int maxPages = Math.min(bounded.getNumberOfPages(), 6);
                for (int p = 0; p < maxPages; p++) {
                    File tempBase = null;
                    File tempPng = null;
                    Process render = null;
                    Process ocr = null;
                    try {
                        tempBase = File.createTempFile("vrezer_ocr_", "_page");
                        if (tempBase.exists()) tempBase.delete();
                        String prefix = tempBase.getAbsolutePath();

                        render = new ProcessBuilder(
                            "pdftoppm",
                            "-f", String.valueOf(p + 1),
                            "-l", String.valueOf(p + 1),
                            "-r", "110",
                            "-png",
                            "-singlefile",
                            tempPdf.getAbsolutePath(),
                            prefix
                        ).redirectErrorStream(true).start();

                        String renderOutput = readProcessOutput(render, 45);
                        if (!render.waitFor(50, TimeUnit.SECONDS) || render.exitValue() != 0) {
                            System.err.println("[PDF EXTRACTOR] Poppler failed on page " + p + ": " + renderOutput);
                            continue;
                        }

                        tempPng = new File(prefix + ".png");
                        if (!tempPng.exists() || tempPng.length() == 0) continue;

                        ocr = new ProcessBuilder(
                            "tesseract",
                            tempPng.getAbsolutePath(),
                            "stdout",
                            "--psm", "6",
                            "-l", "eng"
                        ).redirectErrorStream(true).start();

                        String pageText = readProcessOutput(ocr, 60);
                        if (!ocr.waitFor(65, TimeUnit.SECONDS)) {
                            ocr.destroyForcibly();
                            System.err.println("[PDF EXTRACTOR] Tesseract timed out on page " + p);
                            continue;
                        }

                        if (ocr.exitValue() == 0 && pageText != null && !pageText.trim().isEmpty()) {
                            fullOcrText.append(pageText.trim()).append("\n\n");
                        }
                    } catch (Exception ex) {
                        System.err.println("[PDF EXTRACTOR] OCR on page " + p + " failed: " + ex.getMessage());
                    } finally {
                        if (ocr != null && ocr.isAlive()) ocr.destroyForcibly();
                        if (render != null && render.isAlive()) render.destroyForcibly();
                        deleteQuietly(tempPng);
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

    private String readProcessOutput(Process process, int maxLines) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null && lines++ < maxLines) {
                sb.append(line).append('\\n');
                if (sb.length() > 120_000) break;
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void deleteQuietly(File file) {
        if (file != null) {
            try { Files.deleteIfExists(file.toPath()); } catch (Exception ignored) {}
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
