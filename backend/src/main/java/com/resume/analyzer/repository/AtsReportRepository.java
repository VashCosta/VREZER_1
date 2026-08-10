package com.resume.analyzer.repository;

import com.resume.analyzer.model.AtsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtsReportRepository extends JpaRepository<AtsReport, Long> {
    Optional<AtsReport> findByResumeId(Long resumeId);
}
