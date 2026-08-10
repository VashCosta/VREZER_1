package com.resume.analyzer.dto;

import com.resume.analyzer.model.Role;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String profilePhoto;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String token, String type, Long id, String name, String email, Role role, String profilePhoto, String message) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.profilePhoto = profilePhoto;
        this.message = message;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static AuthResponseBuilder builder() { return new AuthResponseBuilder(); }

    public static class AuthResponseBuilder {
        private String token;
        private String type = "Bearer";
        private Long id;
        private String name;
        private String email;
        private Role role;
        private String profilePhoto;
        private String message;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder type(String type) { this.type = type; return this; }
        public AuthResponseBuilder id(Long id) { this.id = id; return this; }
        public AuthResponseBuilder name(String name) { this.name = name; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder role(Role role) { this.role = role; return this; }
        public AuthResponseBuilder profilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; return this; }
        public AuthResponseBuilder message(String message) { this.message = message; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, type, id, name, email, role, profilePhoto, message);
        }
    }
}
