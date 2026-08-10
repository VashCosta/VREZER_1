package com.resume.analyzer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String userName;
    private Integer rating;
    private String category;

    @Column(columnDefinition = "TEXT")
    private String comments;

    private LocalDateTime createdAt;

    public Feedback() {}

    public Feedback(Long id, String userEmail, String userName, Integer rating, String category, String comments, LocalDateTime createdAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.userName = userName;
        this.rating = rating;
        this.category = category;
        this.comments = comments;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static FeedbackBuilder builder() { return new FeedbackBuilder(); }

    public static class FeedbackBuilder {
        private Long id;
        private String userEmail;
        private String userName;
        private Integer rating;
        private String category;
        private String comments;
        private LocalDateTime createdAt;

        public FeedbackBuilder id(Long id) { this.id = id; return this; }
        public FeedbackBuilder userEmail(String userEmail) { this.userEmail = userEmail; return this; }
        public FeedbackBuilder userName(String userName) { this.userName = userName; return this; }
        public FeedbackBuilder rating(Integer rating) { this.rating = rating; return this; }
        public FeedbackBuilder category(String category) { this.category = category; return this; }
        public FeedbackBuilder comments(String comments) { this.comments = comments; return this; }
        public FeedbackBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Feedback build() {
            return new Feedback(id, userEmail, userName, rating, category, comments, createdAt);
        }
    }
}
