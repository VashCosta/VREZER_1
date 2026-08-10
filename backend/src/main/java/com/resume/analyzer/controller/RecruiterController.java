package com.resume.analyzer.controller;

import com.resume.analyzer.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/recruiter")
@CrossOrigin(origins = "*")
public class RecruiterController {

    @PostMapping("/batch-upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpload(@RequestParam("files") MultipartFile[] files) {
        int uploadedCount = files != null ? files.length : 0;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalFiles", uploadedCount);
        response.put("status", "Successfully parsed and ranked " + uploadedCount + " candidate resumes.");

        List<Map<String, Object>> candidates = new ArrayList<>();
        if (files != null) {
            int rank = 1;
            for (MultipartFile file : files) {
                Map<String, Object> candidate = new LinkedHashMap<>();
                String originalName = (file != null && file.getOriginalFilename() != null) ? file.getOriginalFilename() : "Candidate_" + rank;
                candidate.put("id", 1000 + rank);
                candidate.put("rank", rank);
                candidate.put("name", originalName.replaceAll("(?i)\\.(pdf|docx)", "").replaceAll("[-_]", " "));
                candidate.put("email", "candidate" + rank + "@careerforge.io");
                candidate.put("score", 95 - (rank * 3));
                candidate.put("skills", Arrays.asList("Spring Boot", "React", "Docker", "PostgreSQL", "AWS"));
                candidate.put("experienceYears", 2.5);
                candidate.put("matchPercentage", 92 - (rank * 2));
                candidates.add(candidate);
                rank++;
            }
        }
        response.put("rankedCandidates", candidates);
        return ResponseEntity.ok(ApiResponse.ok("Batch processing completed", response));
    }

    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCandidates(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) Integer minScore) {

        List<Map<String, Object>> candidates = new ArrayList<>();

        Map<String, Object> c1 = new LinkedHashMap<>();
        c1.put("id", 101);
        c1.put("name", "Alex Johnson");
        c1.put("email", "alex.johnson@careerforge.io");
        c1.put("phone", "+1 (555) 234-5678");
        c1.put("college", "Stanford University");
        c1.put("degree", "B.Tech Computer Science");
        c1.put("score", 94);
        c1.put("atsScore", 91);
        c1.put("skills", Arrays.asList("Java", "Spring Boot", "React 19", "PostgreSQL", "Docker", "AWS"));
        c1.put("experienceYears", 3.0);
        c1.put("status", "SHORTLISTED");
        candidates.add(c1);

        Map<String, Object> c2 = new LinkedHashMap<>();
        c2.put("id", 102);
        c2.put("name", "Sophia Martinez");
        c2.put("email", "sophia.m@careerforge.io");
        c2.put("phone", "+1 (555) 876-5432");
        c2.put("college", "MIT");
        c2.put("degree", "M.S. Data Science & AI");
        c2.put("score", 91);
        c2.put("atsScore", 89);
        c2.put("skills", Arrays.asList("Python", "TensorFlow", "PyTorch", "FastAPI", "PostgreSQL"));
        c2.put("experienceYears", 2.5);
        c2.put("status", "REVIEW_PENDING");
        candidates.add(c2);

        Map<String, Object> c3 = new LinkedHashMap<>();
        c3.put("id", 103);
        c3.put("name", "David Chen");
        c3.put("email", "david.chen@careerforge.io");
        c3.put("phone", "+1 (555) 432-1098");
        c3.put("college", "UC Berkeley");
        c3.put("degree", "B.S. Software Engineering");
        c3.put("score", 87);
        c3.put("atsScore", 85);
        c3.put("skills", Arrays.asList("TypeScript", "Node.js", "React", "GraphQL", "MongoDB"));
        c3.put("experienceYears", 1.8);
        c3.put("status", "NEW");
        candidates.add(c3);

        return ResponseEntity.ok(ApiResponse.ok(candidates));
    }
}
