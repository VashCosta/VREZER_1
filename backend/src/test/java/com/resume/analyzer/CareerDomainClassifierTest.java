package com.resume.analyzer;

import com.resume.analyzer.service.ResumeIntelligenceEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class CareerDomainClassifierTest {

    @Autowired
    private ResumeIntelligenceEngine engine;

    @Test
    void marketingCareerWinsOverAiDegreeContext() {
        String resume = """
            Vivash Vel
            B.Tech Artificial Intelligence and Data Science
            Career Objective: Digital Marketing Specialist
            Experience: SEO, Google Ads, Meta Ads, social media marketing and campaign optimization.
            Projects: Performance marketing campaign analysis and content strategy.
            """;

        Map<String, String> result = engine.detectPrimaryAndSecondaryDomains(
                resume,
                List.of("SEO", "Google Ads", "Meta Ads", "Social Media Marketing", "Keyword Research"),
                List.of(),
                List.of(),
                "B.Tech",
                "AI & ML"
        );

        assertEquals("Digital Marketing", result.get("primaryDomain"));
        assertEquals("Digital Marketing", result.get("careerDomain"));
    }

    @Test
    void clearlyTechnicalResumeDoesNotBecomeMarketing() {
        String resume = """
            Backend Developer
            Built REST APIs with Java and Spring Boot.
            Developed microservices using PostgreSQL, Redis and Kafka.
            Technical Skills: Java, Spring Boot, REST API, Microservices, PostgreSQL, Kafka.
            """;

        Map<String, String> result = engine.detectPrimaryAndSecondaryDomains(
                resume,
                List.of("Java", "Spring Boot", "REST API", "Microservices", "PostgreSQL", "Kafka"),
                List.of("Java"),
                List.of("Spring Boot"),
                "B.E.",
                "Computer Science"
        );

        assertEquals("Backend Development", result.get("primaryDomain"));
        assertNotEquals("Digital Marketing", result.get("primaryDomain"));
    }
}
