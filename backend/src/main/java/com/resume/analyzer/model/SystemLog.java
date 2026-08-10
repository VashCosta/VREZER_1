package com.resume.analyzer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;
    private String module;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime timestamp;

    public SystemLog() {}

    public SystemLog(Long id, String level, String module, String message, LocalDateTime timestamp) {
        this.id = id;
        this.level = level;
        this.module = module;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public static SystemLogBuilder builder() { return new SystemLogBuilder(); }

    public static class SystemLogBuilder {
        private Long id;
        private String level;
        private String module;
        private String message;
        private LocalDateTime timestamp;

        public SystemLogBuilder id(Long id) { this.id = id; return this; }
        public SystemLogBuilder level(String level) { this.level = level; return this; }
        public SystemLogBuilder module(String module) { this.module = module; return this; }
        public SystemLogBuilder message(String message) { this.message = message; return this; }
        public SystemLogBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public SystemLog build() {
            return new SystemLog(id, level, module, message, timestamp);
        }
    }
}
