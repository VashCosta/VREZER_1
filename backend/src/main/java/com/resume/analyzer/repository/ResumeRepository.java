package com.resume.analyzer.repository;

import com.resume.analyzer.model.Resume;
import com.resume.analyzer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserOrderByUploadDateDesc(User user);
    List<Resume> findByUserIdOrderByUploadDateDesc(Long userId);
}
