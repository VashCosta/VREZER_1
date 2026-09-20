package com.resume.analyzer.repository;

import com.resume.analyzer.model.AnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisCacheRepository extends JpaRepository<AnalysisCache, Long> {
    Optional<AnalysisCache> findByResumeHash(String resumeHash);
}
