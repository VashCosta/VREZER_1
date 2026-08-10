package com.resume.analyzer.dto;

import java.util.List;

public class SalaryEstimateDto {
    private Double min;
    private Double max;
    private Double median;
    private String currency = "INR";
    private int confidence;
    private int dataPoints;
    private List<String> sources;
    private boolean isEstimate;
    private String status = "OK"; // OK, INSUFFICIENT_DATA

    public SalaryEstimateDto() {}

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public int getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(int dataPoints) {
        this.dataPoints = dataPoints;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public boolean isEstimate() {
        return isEstimate;
    }

    public void setEstimate(boolean estimate) {
        isEstimate = estimate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
