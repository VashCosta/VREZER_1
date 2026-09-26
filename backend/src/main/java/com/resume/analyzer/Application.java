package com.resume.analyzer;

import com.resume.analyzer.controller.AdminController;
import com.resume.analyzer.controller.AuthController;
import com.resume.analyzer.security.CustomUserDetailsService;
import com.resume.analyzer.security.JwtAuthFilter;
import com.resume.analyzer.security.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableCaching
@ComponentScan(
    basePackages = "com.resume.analyzer",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            AdminController.class,
            AuthController.class,
            SecurityConfig.class,
            JwtAuthFilter.class,
            CustomUserDetailsService.class
        }
    )
)
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
}
