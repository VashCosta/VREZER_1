package com.resume.analyzer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ResumeEmbedding — JPA entity for storing Gemini/OpenAI embedding vectors of resumes.
 *
 * The embedding column stores the vector as a PostgreSQL TEXT representation
 * (e.g. "[0.123, -0.456, ...]") which pgvector casts via the "::vector" operator.
 * This approach avoids needing the pgvector-jdbc extension while maintaining
 * full compatibility with pgvector ANN search queries.
 *
 * Schema: resume_embeddings table (created by db/init.sql on first docker-compose up)
 */
@Entity
@Table(name = "resume_embeddings", indexes = {
    @Index(name = "idx_resume_embeddings_user_id", columnList = "user_id"),
    @Index(name = "idx_resume_embeddings_resume_hash", columnList = "resume_hash")
})
public class ResumeEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to users table — null for anonymous/unauthenticated uploads */
    @Column(name = "user_id")
    private Long userId;

    /** SHA-256 of the raw resume text — prevents duplicate embedding computation */
    @Column(name = "resume_hash", nullable = false, unique = true, length = 64)
    private String resumeHash;

    /**
     * Embedding vector stored as text "[v1, v2, ...]".
     * Persisted as TEXT; pgvector interprets via "::vector" cast in SQL queries.
     * Typical dimension: 768 (Gemini text-embedding-004).
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embeddingJson;

    /** First 500 characters of resume for display in recruiter search results */
    @Column(name = "resume_snippet", columnDefinition = "TEXT")
    private String resumeSnippet;

    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    @Column(name = "target_role", length = 255)
    private String targetRole;

    /** JSON array string of top extracted skills e.g. '["Java","Spring","React"]' */
    @Column(name = "skills_json", columnDefinition = "TEXT")
    private String skillsJson;

    @Column(name = "ats_score")
    private Double atsScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getResumeHash() { return resumeHash; }
    public void setResumeHash(String resumeHash) { this.resumeHash = resumeHash; }

    public String getEmbeddingJson() { return embeddingJson; }
    public void setEmbeddingJson(String embeddingJson) { this.embeddingJson = embeddingJson; }

    public String getResumeSnippet() { return resumeSnippet; }
    public void setResumeSnippet(String resumeSnippet) { this.resumeSnippet = resumeSnippet; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getSkillsJson() { return skillsJson; }
    public void setSkillsJson(String skillsJson) { this.skillsJson = skillsJson; }

    public Double getAtsScore() { return atsScore; }
    public void setAtsScore(Double atsScore) { this.atsScore = atsScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Deserialize embeddingJson back to double[] for cosine similarity computation. */
    public double[] toDoubleArray() {
        if (embeddingJson == null || embeddingJson.isBlank()) return new double[0];
        try {
            String cleaned = embeddingJson.trim().replaceAll("[\\[\\]\\s]", "");
            String[] parts = cleaned.split(",");
            double[] arr = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                arr[i] = Double.parseDouble(parts[i].trim());
            }
            return arr;
        } catch (Exception e) {
            return new double[0];
        }
    }

    /** Serialize double[] embedding to the "[v1, v2, ...]" text format for pgvector. */
    public static String serializeEmbedding(double[] embedding) {
        if (embedding == null || embedding.length == 0) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
