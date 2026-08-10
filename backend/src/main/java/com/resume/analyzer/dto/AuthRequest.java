package com.resume.analyzer.dto;

import com.resume.analyzer.model.Role;

public class AuthRequest {

    public static class Login {
        private String email;
        private String password;

        public Login() {}
        public Login(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Register {
        private String name;
        private String email;
        private String password;
        private Role role = Role.STUDENT;

        public Register() {}
        public Register(String name, String email, String password, Role role) {
            this.name = name;
            this.email = email;
            this.password = password;
            if (role != null) this.role = role;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
    }

    public static class ForgotPassword {
        private String email;

        public ForgotPassword() {}
        public ForgotPassword(String email) { this.email = email; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPassword {
        private String email;
        private String token;
        private String newPassword;

        public ResetPassword() {}
        public ResetPassword(String email, String token, String newPassword) {
            this.email = email;
            this.token = token;
            this.newPassword = newPassword;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
