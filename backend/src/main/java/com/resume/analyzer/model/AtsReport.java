package com.resume.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ats_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    private Integer atsScore;

    private boolean hasTables;
    private boolean hasIcons;
    private boolean hasColumns;
    private boolean fontIssues;
    private boolean marginRisk;

    @Column(columnDefinition = "TEXT")
    private String missingKeywordsJson;

    @Column(columnDefinition = "TEXT")
    private String weakKeywordsJson;

    @Column(columnDefinition = "TEXT")
    private String actionVerbsJson;

    @Column(columnDefinition = "TEXT")
    private String suggestionsJson;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
