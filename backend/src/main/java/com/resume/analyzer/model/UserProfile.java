package com.resume.analyzer.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String phone;
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private String targetCompany;
    private String dreamRole;
    private Double experienceYears;
    private String college;
    private String degree;
    private Double cgpa;

    @Column(columnDefinition = "TEXT")
    private String bio;

    public UserProfile() {}

    public UserProfile(Long id, User user, String phone, String githubUrl, String linkedinUrl, String portfolioUrl, String targetCompany, String dreamRole, Double experienceYears, String college, String degree, Double cgpa, String bio) {
        this.id = id;
        this.user = user;
        this.phone = phone;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.portfolioUrl = portfolioUrl;
        this.targetCompany = targetCompany;
        this.dreamRole = dreamRole;
        this.experienceYears = experienceYears;
        this.college = college;
        this.degree = degree;
        this.cgpa = cgpa;
        this.bio = bio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getTargetCompany() { return targetCompany; }
    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }

    public String getDreamRole() { return dreamRole; }
    public void setDreamRole(String dreamRole) { this.dreamRole = dreamRole; }

    public Double getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Double experienceYears) { this.experienceYears = experienceYears; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public static UserProfileBuilder builder() { return new UserProfileBuilder(); }

    public static class UserProfileBuilder {
        private Long id;
        private User user;
        private String phone;
        private String githubUrl;
        private String linkedinUrl;
        private String portfolioUrl;
        private String targetCompany;
        private String dreamRole;
        private Double experienceYears;
        private String college;
        private String degree;
        private Double cgpa;
        private String bio;

        public UserProfileBuilder id(Long id) { this.id = id; return this; }
        public UserProfileBuilder user(User user) { this.user = user; return this; }
        public UserProfileBuilder phone(String phone) { this.phone = phone; return this; }
        public UserProfileBuilder githubUrl(String githubUrl) { this.githubUrl = githubUrl; return this; }
        public UserProfileBuilder linkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; return this; }
        public UserProfileBuilder portfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; return this; }
        public UserProfileBuilder targetCompany(String targetCompany) { this.targetCompany = targetCompany; return this; }
        public UserProfileBuilder dreamRole(String dreamRole) { this.dreamRole = dreamRole; return this; }
        public UserProfileBuilder experienceYears(Double experienceYears) { this.experienceYears = experienceYears; return this; }
        public UserProfileBuilder college(String college) { this.college = college; return this; }
        public UserProfileBuilder degree(String degree) { this.degree = degree; return this; }
        public UserProfileBuilder cgpa(Double cgpa) { this.cgpa = cgpa; return this; }
        public UserProfileBuilder bio(String bio) { this.bio = bio; return this; }

        public UserProfile build() {
            return new UserProfile(id, user, phone, githubUrl, linkedinUrl, portfolioUrl, targetCompany, dreamRole, experienceYears, college, degree, cgpa, bio);
        }
    }
}
