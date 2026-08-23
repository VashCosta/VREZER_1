package com.resume.analyzer;

import com.resume.analyzer.service.AtsAnalysisEngine;
import com.resume.analyzer.service.FileParsingService;
import com.resume.analyzer.service.ResumeIntelligenceEngine;
import com.resume.analyzer.service.ResumeParserService;
import com.resume.analyzer.service.VrezerAiAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ResumeAnalysisIntegrationTest {

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private FileParsingService fileParsingService;

    @Autowired
    private ResumeIntelligenceEngine resumeIntelligenceEngine;

    @Autowired
    private AtsAnalysisEngine atsAnalysisEngine;

    @Autowired
    private VrezerAiAgentService vrezerAiAgentService;

    private String resumeJava;
    private String resumePython;
    private String resumeReact;
    private String resumeMarketing;
    private String resumeDevOps;

    @BeforeEach
    void setUp() {
        resumeJava = """
            Karthick Raja
            Email: karthick.java@gmail.com | Phone: +91 9876543210
            Location: Bengaluru, India
            
            EDUCATION
            B.E. Computer Science & Engineering, Anna University (2018 - 2022) | CGPA: 8.5/10
            
            WORK EXPERIENCE
            Backend Developer — Enterprise Solutions Ltd (Jan 2022 - Present)
            • Built high-throughput REST APIs using Java 17, Spring Boot, and PostgreSQL.
            • Microservices architecture with Kafka messaging and Redis caching.
            • Automated CI/CD pipelines with Docker and GitHub Actions.
            
            TECHNICAL SKILLS
            Java, Spring Boot, Microservices, PostgreSQL, Redis, Kafka, REST API, Docker, Git
            """;

        resumePython = """
            Dr. Elena Rostova
            Email: elena.data@ai-tech.io | Phone: +1 415 555 0199
            Location: San Francisco, CA
            
            EDUCATION
            Ph.D. Computer Science (Machine Learning Focus), Stanford University
            
            WORK EXPERIENCE
            Senior AI Research Scientist — Neural Labs (Mar 2020 - Present)
            • Developed Deep Learning models using PyTorch, TensorFlow, and Hugging Face.
            • Implemented RAG architectures with Pinecone vector database and LangChain.
            • Published 5 research papers on Large Language Model optimization.
            
            TECHNICAL SKILLS
            Python, PyTorch, TensorFlow, scikit-learn, OpenCV, Hugging Face, LangChain, RAG, Vector Database, SQL
            """;

        resumeReact = """
            Ananya Sharma
            Email: ananya.design@frontend.dev | Phone: +91 9123456789
            Location: Pune, India
            
            EDUCATION
            B.Tech Information Technology, MIT Pune (2020 - 2024)
            
            WORK EXPERIENCE
            Frontend Developer Intern — TechCraft Solutions (Jun 2023 - Present)
            • Built responsive user interfaces using React.js, TypeScript, TailwindCSS, and Next.js.
            • Integrated GraphQL and REST endpoints with State Management using Redux Toolkit.
            
            TECHNICAL SKILLS
            JavaScript, TypeScript, React, Next.js, HTML5, CSS3, TailwindCSS, Redux, GraphQL
            """;

        resumeMarketing = """
            Vivash Vel C.s
            Email: vivash.marketing@growth.co | Phone: +91 9988776655
            Location: Chennai, India
            
            EDUCATION
            Bachelor of Commerce (B.Com), University of Madras
            
            WORK EXPERIENCE
            Performance Marketing Specialist — Digital Growth Agency (2021 - Present)
            • Managed $500K+ ad budgets across Google Ads, Meta Ads, and LinkedIn Marketing.
            • Scaled organic traffic by 180% using technical SEO, Google Analytics 4, and A/B testing.
            
            TECHNICAL SKILLS
            Digital Marketing, SEO, SEM, Google Ads, Meta Ads, Google Analytics, Content Strategy, A/B Testing
            """;

        resumeDevOps = """
            Marcus Vance
            Email: marcus.cloud@infrastructure.net | Phone: +44 20 7946 0912
            Location: London, UK
            
            EDUCATION
            B.Sc. Network Systems Engineering, King's College London
            
            WORK EXPERIENCE
            Lead Cloud Architect — Global Cloud Ops (2019 - Present)
            • Architected multi-cloud infrastructure on AWS and Azure using Terraform and Ansible.
            • Orchestrated Kubernetes (EKS) clusters, Helm charts, and Prometheus/Grafana monitoring.
            
            TECHNICAL SKILLS
            AWS, Azure, Docker, Kubernetes, Terraform, Ansible, Jenkins, CI/CD, Prometheus, Grafana
            """;
    }

    @Test
    void testFiveResumesProduceDistinctOutputs() {
        Map<String, Object> resultJava = vrezerAiAgentService.buildDynamicLocalEngineDossier(resumeJava, null);
        Map<String, Object> resultPython = vrezerAiAgentService.buildDynamicLocalEngineDossier(resumePython, null);
        Map<String, Object> resultReact = vrezerAiAgentService.buildDynamicLocalEngineDossier(resumeReact, null);
        Map<String, Object> resultMarketing = vrezerAiAgentService.buildDynamicLocalEngineDossier(resumeMarketing, null);
        Map<String, Object> resultDevOps = vrezerAiAgentService.buildDynamicLocalEngineDossier(resumeDevOps, null);

        // Verify Names
        assertEquals("Karthick Raja", resultJava.get("name"));
        assertTrue(resultPython.get("name").toString().contains("Elena Rostova"));
        assertEquals("Ananya Sharma", resultReact.get("name"));
        assertEquals("Vivash Vel C.s", resultMarketing.get("name"));
        assertEquals("Marcus Vance", resultDevOps.get("name"));

        // Verify Domains are distinct
        assertNotEquals(resultJava.get("careerDomain"), resultMarketing.get("careerDomain"));
        assertNotEquals(resultPython.get("careerDomain"), resultReact.get("careerDomain"));

        // Verify top skills match extracted text accurately
        List<String> javaSkills = (List<String>) resultJava.get("topSkills");
        assertTrue(javaSkills.contains("Java") || javaSkills.contains("Spring Boot"));

        List<String> pythonSkills = (List<String>) resultPython.get("topSkills");
        assertTrue(pythonSkills.contains("Python") || pythonSkills.contains("PyTorch"));

        List<String> reactSkills = (List<String>) resultReact.get("topSkills");
        assertTrue(reactSkills.contains("React") || reactSkills.contains("TypeScript"));

        List<String> marketingSkills = (List<String>) resultMarketing.get("topSkills");
        assertTrue(marketingSkills.contains("SEO") || marketingSkills.contains("Digital Marketing") || marketingSkills.contains("Google Ads"));

        // Verify ATS Scores are dynamic numbers > 0
        int scoreJava = (Integer) resultJava.get("atsScore");
        int scoreMarketing = (Integer) resultMarketing.get("atsScore");
        assertTrue(scoreJava > 0 && scoreJava <= 100);
        assertTrue(scoreMarketing > 0 && scoreMarketing <= 100);
    }

    @Test
    void testActualHighClassAtsPdfExtraction() throws Exception {
        File pdfFile = new File("C:\\Users\\vivas\\Downloads\\Vivash_Vel_CS_Resume_Final_ATS_Professional.pdf");
        if (!pdfFile.exists()) {
            System.out.println("Test PDF not present at path, skipping.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(pdfFile)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "Vivash_Vel_CS_Resume_Final_ATS_Professional.pdf",
                "application/pdf",
                fis
            );

            // Run end-to-end extraction through FileParsingService
            Map<String, Object> extractionResult = fileParsingService.extractAndStructure(multipartFile);
            assertNotNull(extractionResult);

            String rawText = (String) extractionResult.get("rawText");
            assertNotNull(rawText);
            assertTrue(rawText.length() > 1000, "Raw extracted text length should be > 1000 chars, but was " + rawText.length());

            // Check candidate name
            String name = (String) extractionResult.get("name");
            assertNotNull(name);
            assertTrue(name.toLowerCase().contains("vivash"), "Name should contain 'Vivash' but was: " + name);

            // Check contact details
            String phone = (String) extractionResult.get("phone");
            assertTrue(phone.contains("8610552795"), "Phone should contain 8610552795 but was: " + phone);

            String linkedin = (String) extractionResult.get("linkedin");
            assertTrue(linkedin.contains("vivashvel"), "LinkedIn should contain vivashvel but was: " + linkedin);

            // Check Education
            List<?> eduList = (List<?>) extractionResult.get("education");
            assertFalse(eduList.isEmpty(), "Education list should not be empty");

            // Check Skills
            List<String> skills = (List<String>) extractionResult.get("allDetectedSkills");
            assertNotNull(skills);
            assertTrue(skills.size() >= 8, "Expected at least 8 detected skills, but got: " + skills);
            assertTrue(skills.contains("Java") || skills.contains("Spring Boot"), "Skills should contain Java/Spring Boot");

            // Check Projects
            List<?> projList = (List<?>) extractionResult.get("projects");
            assertFalse(projList.isEmpty(), "Projects should not be empty");

            // Check Internships / Experience
            List<?> expList = (List<?>) extractionResult.get("experience");
            assertFalse(expList.isEmpty(), "Experience should not be empty");

            // Check Certifications
            List<?> certList = (List<?>) extractionResult.get("certifications");
            assertFalse(certList.isEmpty(), "Certifications should not be empty");

            // Test Dossier generation & ATS scoring
            Map<String, Object> dossier = vrezerAiAgentService.buildDynamicLocalEngineDossier(rawText, null);
            assertNotNull(dossier);
            int atsScore = (Integer) dossier.get("atsScore");
            assertTrue(atsScore >= 70, "ATS Score should be >= 70 for this rich resume, but was: " + atsScore);

            // Verify Experience is proper (NOT 13 years)
            String expSummary = (String) dossier.get("experience");
            assertNotNull(expSummary);
            assertFalse(expSummary.contains("13"), "Experience should NOT be 13 years! Found: " + expSummary);
            assertTrue(expSummary.contains("Internship") || expSummary.contains("Fresher") || expSummary.contains("Entry Level"), "Expected Internship/Fresher experience, but was: " + expSummary);

            // Verify Experience level is FRESHER (student pursuing B.Tech)
            String expLevel = (String) dossier.get("experienceLevel");
            assertEquals("FRESHER", expLevel, "Experience level for a pursuing student should be FRESHER");

            System.out.println("==== TEST SUCCESS: HIGH CLASS ATS RESUME EXTRACTED PERFECTLY ====");
            System.out.println("Candidate Name: " + name);
            System.out.println("Experience: " + expSummary + " (Level: " + expLevel + ")");
            System.out.println("Role: " + dossier.get("role"));
            System.out.println("Detected Skills (" + skills.size() + "): " + skills);
            System.out.println("ATS Score: " + atsScore + " (" + dossier.get("atsScoreText") + ")");
        }
    }
}
