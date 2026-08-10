package com.resume.analyzer.repository;

import com.resume.analyzer.model.ResumeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ResumeEmbeddingRepository — Spring Data JPA repository for the resume_embeddings table.
 *
 * Supports lookup by user ID, dedup via SHA-256 hash, and listing all candidates
 * for the recruiter similarity-search dashboard.
 */
@Repository
public interface ResumeEmbeddingRepository extends JpaRepository<ResumeEmbedding, Long> {

    /**
     * Find an existing embedding by SHA-256 hash of resume text.
     * Used to avoid re-computing embeddings for the same document.
     */
    Optional<ResumeEmbedding> findByResumeHash(String resumeHash);

    /**
     * Retrieve all embeddings for a specific user (multi-resume history).
     */
    List<ResumeEmbedding> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Check if an embedding already exists for the given hash (faster than full fetch).
     */
    boolean existsByResumeHash(String resumeHash);

    /**
     * Delete all embeddings for a user (GDPR / account deletion).
     */
    void deleteByUserId(Long userId);

    /**
     * Retrieve the N most recently indexed resumes for the recruiter batch view.
     */
    @Query("SELECT r FROM ResumeEmbedding r ORDER BY r.createdAt DESC")
    List<ResumeEmbedding> findTopNRecent(@Param("limit") int limit);

    /**
     * Count embeddings by target role (for admin analytics).
     */
    @Query("SELECT r.targetRole, COUNT(r) FROM ResumeEmbedding r GROUP BY r.targetRole ORDER BY COUNT(r) DESC")
    List<Object[]> countByTargetRole();
}
