package com.resume.analyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableCaching
@EntityScan("com.resume.analyzer")
@EnableJpaRepositories("com.resume.analyzer")
public class Application {

    @Value("${app.pdf.ocr.enabled:true}")
    private boolean pdfOcrEnabled;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${app.llama.api-key:}")
    private String llamaApiKey;

    @Value("${app.llama.model:openai/gpt-oss-120b}")
    private String llamaModel;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }

    @Bean
    public CommandLineRunner productionDiagnosticsRunner(javax.sql.DataSource dataSource) {
        return args -> {
            System.out.println("================================================================================");
            System.out.println("[VREZER PROD DIAGNOSTIC] Environment: "
                    + System.getProperty("os.name") + " / Java " + System.getProperty("java.version"));
            System.out.println("[VREZER PROD DIAGNOSTIC] PDF OCR configured: " + pdfOcrEnabled);
            System.out.println("[VREZER PROD DIAGNOSTIC] Gemini model configured: "
                    + safe(geminiModel) + " | API key configured: " + hasValue(geminiApiKey));
            System.out.println("[VREZER PROD DIAGNOSTIC] Groq/Llama model configured: "
                    + safe(llamaModel) + " | API key configured: " + hasValue(llamaApiKey));

            TesseractStatus tesseract = detectBinary("tesseract", "--version");
            System.out.println("[VREZER PROD DIAGNOSTIC] Tesseract binary: "
                    + (tesseract.available ? "AVAILABLE" : "NOT_AVAILABLE")
                    + (tesseract.version.isEmpty() ? "" : " | " + tesseract.version));

            TesseractStatus pdfImages = detectBinary("pdfimages", "-v");
            System.out.println("[VREZER PROD DIAGNOSTIC] Poppler pdfimages: "
                    + (pdfImages.available ? "AVAILABLE" : "NOT_AVAILABLE")
                    + (pdfImages.version.isEmpty() ? "" : " | " + pdfImages.version));

            System.out.println("[VREZER PROD DIAGNOSTIC] OCR execution path: "
                    + (isWindows() ? "Windows OCR bridge" : "Poppler embedded-image extraction -> bounded Tesseract"));
            System.out.println("================================================================================");

            try (java.sql.Connection conn = dataSource.getConnection()) {
                System.out.println("[VREZER DB DIAGNOSTIC] Connected Database: "
                        + conn.getMetaData().getDatabaseProductName()
                        + " " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println("[VREZER DB DIAGNOSTIC] Connection URL: " + conn.getMetaData().getURL());
                System.out.println("[VREZER DB DIAGNOSTIC] DB User: " + conn.getMetaData().getUserName());
                System.out.println("================================================================================");
            } catch (Exception e) {
                System.err.println("[VREZER DB DIAGNOSTIC] Database diagnostic warning: " + e.getMessage());
            }
        };
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "NOT_CONFIGURED" : value.trim();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private TesseractStatus detectBinary(String command, String versionArg) {
        if (isWindows() && "pdfimages".equals(command)) {
            return new TesseractStatus(false, "Linux/Unix production tool not required on Windows");
        }

        try {
            Process process = new ProcessBuilder(command, versionArg)
                    .redirectErrorStream(true)
                    .start();

            String firstLine = "";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                firstLine = reader.readLine();
            }

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new TesseractStatus(false, "version check timed out");
            }

            if (process.exitValue() == 0) {
                return new TesseractStatus(true, firstLine == null ? "" : firstLine.trim());
            }

            return new TesseractStatus(false,
                    firstLine == null ? "version check failed" : firstLine.trim());
        } catch (Exception e) {
            return new TesseractStatus(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static final class TesseractStatus {
        private final boolean available;
        private final String version;

        private TesseractStatus(boolean available, String version) {
            this.available = available;
            this.version = version == null ? "" : version;
        }
    }
}
