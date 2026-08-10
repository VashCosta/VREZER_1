package com.resume.analyzer.controller;

import com.resume.analyzer.dto.ApiResponse;
import com.resume.analyzer.dto.AuthRequest;
import com.resume.analyzer.dto.AuthResponse;
import com.resume.analyzer.model.Role;
import com.resume.analyzer.model.User;
import com.resume.analyzer.model.UserProfile;
import com.resume.analyzer.repository.UserProfileRepository;
import com.resume.analyzer.repository.UserRepository;
import com.resume.analyzer.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest.Login loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();

        String jwt = tokenProvider.generateToken(user.getEmail(), user.getRole().name());

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profilePhoto(user.getProfilePhoto())
                .message("Login successful")
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody AuthRequest.Register registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email address is already in use!"));
        }

        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole() != null ? registerRequest.getRole() : Role.STUDENT)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        // Create default profile
        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .targetCompany("Google / FAANG")
                .dreamRole("Software Engineer")
                .experienceYears(1.0)
                .college("Stanford / NIT")
                .degree("B.Tech Computer Science")
                .cgpa(8.8)
                .bio("Aspiring Software Engineer passionate about AI and scalable systems.")
                .build();
        userProfileRepository.save(profile);

        String jwt = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .message("Account registered successfully")
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Account created successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated"));
        }
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found"));
        }
        User user = userOpt.get();
        Optional<UserProfile> profileOpt = userProfileRepository.findByUser(user);

        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("user", user, "profile", profileOpt.orElse(null))));
    }
}
