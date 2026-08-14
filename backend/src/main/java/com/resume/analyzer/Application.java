package com.resume.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EntityScan("com.resume.analyzer")
@EnableJpaRepositories("com.resume.analyzer")
public class Application {
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
    public org.springframework.boot.CommandLineRunner dbDiagnosticRunner(javax.sql.DataSource dataSource) {
        return args -> {
            try (java.sql.Connection conn = dataSource.getConnection()) {
                System.out.println("================================================================================");
                System.out.println("[VREZER DB DIAGNOSTIC] Connected Database: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println("[VREZER DB DIAGNOSTIC] Connection URL: " + conn.getMetaData().getURL());
                System.out.println("[VREZER DB DIAGNOSTIC] DB User: " + conn.getMetaData().getUserName());
                System.out.println("================================================================================");
            } catch (Exception e) {
                System.err.println("[VREZER DB DIAGNOSTIC] Database diagnostic warning: " + e.getMessage());
            }
        };
    }
}
