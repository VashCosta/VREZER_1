package com.resume.analyzer.repository;

import com.resume.analyzer.model.ResumeScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeScoreRepository extends JpaRepository<ResumeScore, Long> {
    Optional<ResumeScore> findByResumeId(Long resumeId);
}
