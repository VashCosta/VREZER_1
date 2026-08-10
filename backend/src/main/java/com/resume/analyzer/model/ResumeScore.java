package com.resume.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(columnDefinition = "TEXT")
    private String missingSectionsJson;

    @Column(columnDefinition = "TEXT")
    private String grammarReviewJson;

    @Column(columnDefinition = "TEXT")
    private String formattingReviewJson;

    @Column(columnDefinition = "TEXT")
    private String suggestionsJson;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
