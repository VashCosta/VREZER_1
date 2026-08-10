package com.resume.analyzer.repository;

import com.resume.analyzer.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findTop50ByOrderByTimestampDesc();
}
