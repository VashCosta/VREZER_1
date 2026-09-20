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
        // First try extracting the original embedded scan images. This is fast and avoids
        // rendering the PDF in the JVM.
        String embedded = runPdfImagesOcr(pdfBytes);
        if (embedded != null && embedded.trim().length() >= 160) {
            System.out.println("[PDF EXTRACTOR] Embedded-image OCR succeeded: "
                    + embedded.trim().length() + " chars");
            return embedded.trim();
        }

        // For many resume PDFs the page is composed from multiple image/vector layers, so
        // pdfimages can return only a logo/background. Render the actual PDF pages with the
        // external Poppler process instead of PDFBox. The subprocess memory is independent of
        // the Java heap, and the output dimensions are hard-bounded.
        System.out.println("[PDF EXTRACTOR] Embedded-image OCR insufficient; using external Poppler pdftoppm page OCR...");
        String rendered = runPopplerPageOcr(pdfBytes);
        if (rendered != null && rendered.trim().length() >= 160) {
            System.out.println("[PDF EXTRACTOR] Poppler page OCR succeeded: "
                    + rendered.trim().length() + " chars");
            return rendered.trim();
        }

        // Do not fall back to PDFBox page rasterization on Render. That path previously caused
        // repeated Java heap exhaustion on high-resolution scanned resumes.
        System.err.println("[PDF EXTRACTOR] All safe OCR methods returned insufficient text.");
        return rendered == null ? "" : rendered.trim();
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
                    "-r", "120",
                    "-scale-to", String.valueOf(MAX_IMAGE_DIMENSION),
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
                File bounded = page.toFile();
                try {
                    // Use multiple page-layout modes because resumes commonly contain columns,
                    // dense skill lists, and icon/text blocks.
                    String best = runTesseractOcr(bounded, "3");
                    String block = runTesseractOcr(bounded, "6");
                    String sparse = runTesseractOcr(bounded, "11");

                    if (block.length() > best.length()) best = block;
                    if (sparse.length() > best.length()) best = sparse;

                    if (!best.trim().isEmpty()) {
                        allText.append(best.trim()).append("\n\n");
                    }
                } finally {
                    // Tesseract reads the file directly; Java does not load the entire raster into
                    // a BufferedImage, avoiding another large heap allocation.
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

    private boolean isWindowsPlatform() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
