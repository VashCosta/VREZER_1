package com.resume.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String originalFileName;
    private String fileType;
    private Long fileSize;

    private String parsedName;
    private String parsedEmail;
    private String parsedPhone;
    private String parsedGithub;
    private String parsedLinkedin;

    @Column(columnDefinition = "TEXT")
    private String parsedEducationJson;

    @Column(columnDefinition = "TEXT")
    private String parsedSkillsJson;

    @Column(columnDefinition = "TEXT")
    private String parsedProjectsJson;

    @Column(columnDefinition = "TEXT")
    private String parsedExperienceJson;

    @Column(columnDefinition = "TEXT")
    private String parsedCertificationsJson;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    private Integer score;

    private LocalDateTime uploadDate;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
