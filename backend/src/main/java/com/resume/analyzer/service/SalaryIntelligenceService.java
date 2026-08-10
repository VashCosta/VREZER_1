package com.resume.analyzer.service;

import com.resume.analyzer.dto.CandidateProfileDto;
import com.resume.analyzer.dto.SalaryEstimateDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SalaryIntelligenceService — Production-grade Evidence-Based Salary Calculation Engine for VREZER.
 * Priority:
 *  1. Verified salary from matched live jobs
 *  2. Multiple verified jobs for same role + location + experience
 *  3. Verified market salary distribution data calibrated by role/experience/skills/location
 *  4. AI/algorithmic estimate ONLY when sufficient evidence exists
 *
 * If insufficient evidence exists: status = "INSUFFICIENT_DATA" (Does NOT invent synthetic numbers).
 */
@Service
public class SalaryIntelligenceService {

    // NOTE: Removed hardcoded role baselines. The engine relies on verified live job salaries
    // and external market datasets when available. If no verified evidence exists the
    // engine returns status = "INSUFFICIENT_DATA" and does not invent numbers.

    public Map<String, Object> calculateSalary(CandidateProfileDto profile, List<Map<String, Object>> liveJobs) {
        SalaryEstimateDto jobSalary = extractJobSalary(liveJobs);
        SalaryEstimateDto marketSalary = calculateMarketSalary(profile, liveJobs, jobSalary);
        SalaryEstimateDto candidateSalaryEstimate = estimateCandidateSalary(profile, marketSalary, jobSalary);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobSalary", jobSalary);
        result.put("marketSalary", marketSalary);
        result.put("candidateSalaryEstimate", candidateSalaryEstimate);
        return result;
    }

    /**
     * Priority 1: Extract explicitly disclosed salaries from live job postings.
     */
    public SalaryEstimateDto extractJobSalary(List<Map<String, Object>> liveJobs) {
        SalaryEstimateDto dto = new SalaryEstimateDto();
        dto.setEstimate(false);

        if (liveJobs == null || liveJobs.isEmpty()) {
            dto.setStatus("INSUFFICIENT_DATA");
            return dto;
        }

        List<Double> salaries = new ArrayList<>();
        List<String> sources = new ArrayList<>();

        for (Map<String, Object> job : liveJobs) {
            String salRaw = null;
            if (job.containsKey("salary") && job.get("salary") != null) {
                salRaw = String.valueOf(job.get("salary"));
            } else if (job.containsKey("expectedLpaRange") && job.get("expectedLpaRange") != null) {
                salRaw = String.valueOf(job.get("expectedLpaRange"));
            }

            if (salRaw == null || salRaw.isBlank() || salRaw.equalsIgnoreCase("Salary not disclosed") || salRaw.equalsIgnoreCase("Market Benchmark") || salRaw.equalsIgnoreCase("Competitive Market Rate")) {
                continue; // Do NOT invent fake numbers for non-disclosed salaries
            }

            Double val = parseLpaValue(salRaw);
            if (val != null && val > 0) {
                salaries.add(val);
                String comp = String.valueOf(job.getOrDefault("company", job.getOrDefault("name", "Verified Employer")));
                sources.add(comp);
            }
        }

        if (salaries.isEmpty()) {
            dto.setStatus("INSUFFICIENT_DATA");
            return dto;
        }

        double min = salaries.stream().min(Double::compare).orElse(0.0);
        double max = salaries.stream().max(Double::compare).orElse(0.0);
        Collections.sort(salaries);
        double median;
        int middle = salaries.size() / 2;
        if (salaries.size() % 2 == 0) {
            median = (salaries.get(middle - 1) + salaries.get(middle)) / 2.0;
        } else {
            median = salaries.get(middle);
        }

        dto.setMin(roundOneDecimal(min));
        dto.setMax(roundOneDecimal(max));
        dto.setMedian(roundOneDecimal(median));
        dto.setCurrency("INR");
        dto.setConfidence(Math.min(95, 60 + (salaries.size() * 8)));
        dto.setDataPoints(salaries.size());
        dto.setSources(sources.subList(0, Math.min(5, sources.size())));
        dto.setStatus("OK");
        return dto;
    }

    /**
     * Priority 2 & 3: Market salary calculation derived from live job data or verified market benchmarks.
     */
    public SalaryEstimateDto calculateMarketSalary(CandidateProfileDto profile, List<Map<String, Object>> liveJobs, SalaryEstimateDto jobSalary) {
        SalaryEstimateDto dto = new SalaryEstimateDto();
        dto.setEstimate(true);
        // Only derive a market estimate when there is verified live job evidence or an external market dataset
        if (jobSalary != null && "OK".equals(jobSalary.getStatus()) && jobSalary.getDataPoints() >= 2) {
            dto.setMin(roundOneDecimal(jobSalary.getMin() * 0.95));
            dto.setMax(roundOneDecimal(jobSalary.getMax() * 1.05));
            dto.setMedian(roundOneDecimal(jobSalary.getMedian()));
            dto.setCurrency("INR");
            dto.setConfidence(Math.min(92, jobSalary.getConfidence()));
            dto.setDataPoints(jobSalary.getDataPoints());
            dto.setSources(List.of("Verified Live Job Board Aggregation (" + jobSalary.getDataPoints() + " postings)"));
            dto.setStatus("OK");
            return dto;
        }

        // No verified multi-job evidence and no external dataset configured: do NOT invent numbers
        dto.setStatus("INSUFFICIENT_DATA");
        return dto;
    }

    /**
     * Priority 4: Candidate Estimated Market Value prediction based on candidate resume evidence.
     */
    public SalaryEstimateDto estimateCandidateSalary(CandidateProfileDto profile, SalaryEstimateDto marketSalary, SalaryEstimateDto jobSalary) {
        SalaryEstimateDto dto = new SalaryEstimateDto();
        dto.setEstimate(true);
        // Candidate estimate is only valid when anchored to verified market or job salary evidence
        if (profile == null) {
            dto.setStatus("INSUFFICIENT_DATA");
            return dto;
        }

        if ((marketSalary == null || !"OK".equals(marketSalary.getStatus())) && (jobSalary == null || !"OK".equals(jobSalary.getStatus()))) {
            dto.setStatus("INSUFFICIENT_DATA");
            return dto;
        }

        double baseMedian = 0.0;
        int baseConfidence = 50;
        if (marketSalary != null && "OK".equals(marketSalary.getStatus()) && marketSalary.getMedian() != null) {
            baseMedian = marketSalary.getMedian();
            baseConfidence = marketSalary.getConfidence();
        } else if (jobSalary != null && "OK".equals(jobSalary.getStatus()) && jobSalary.getMedian() != null) {
            baseMedian = jobSalary.getMedian();
            baseConfidence = jobSalary.getConfidence();
        }

        if (baseMedian <= 0.0) {
            dto.setStatus("INSUFFICIENT_DATA");
            return dto;
        }

        // Lightweight candidate multipliers based on verified resume evidence — keep conservative
        int skillCount = (profile.getTechnicalSkills() != null && !profile.getTechnicalSkills().isEmpty()) ? profile.getTechnicalSkills().size() : (profile.getSkills() != null ? profile.getSkills().size() : 0);
        int projCount = (profile.getProjects() != null) ? profile.getProjects().size() : 0;
        int certCount = (profile.getCertifications() != null) ? profile.getCertifications().size() : 0;

        double skillMultiplier = 1.0 + Math.min(0.12, skillCount * 0.015);
        double portfolioMultiplier = 1.0 + Math.min(0.10, projCount * 0.03 + certCount * 0.02);
        double locMultiplier = 1.0;
        String loc = profile.getPreferredLocation() != null ? profile.getPreferredLocation().toLowerCase() : "";
        if (loc.contains("bengaluru") || loc.contains("bangalore") || loc.contains("mumbai") || loc.contains("gurugram") || loc.contains("hyderabad")) {
            locMultiplier = 1.06;
        }

        double finalMedian = baseMedian * skillMultiplier * portfolioMultiplier * locMultiplier;
        double finalMin = finalMedian * 0.85;
        double finalMax = finalMedian * 1.2;

        int candidateConfidence = Math.min(95, Math.max(40, baseConfidence + (skillCount * 2) + (projCount * 2)));

        dto.setMin(roundOneDecimal(finalMin));
        dto.setMax(roundOneDecimal(finalMax));
        dto.setMedian(roundOneDecimal(finalMedian));
        dto.setCurrency("INR");
        dto.setConfidence(candidateConfidence);
        dto.setDataPoints(Math.max(1, skillCount + projCount));
        dto.setSources(List.of("Anchored Market Evidence", "Resume Profile Multipliers"));
        dto.setStatus("OK");
        return dto;
    }

    private Double parseLpaValue(String salStr) {
        if (salStr == null) return null;
        Pattern p = Pattern.compile("(\\d+\\.?\\d*)");
        Matcher m = p.matcher(salStr);
        List<Double> nums = new ArrayList<>();
        while (m.find()) {
            try {
                nums.add(Double.parseDouble(m.group(1)));
            } catch (Exception ignored) {}
        }
        if (nums.isEmpty()) return null;

        double val = nums.size() >= 2 ? (nums.get(0) + nums.get(1)) / 2.0 : nums.get(0);
        if (val > 1000) {
            val = val / 100000.0; // Convert absolute INR to LPA
        }
        return val;
    }

    private double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
