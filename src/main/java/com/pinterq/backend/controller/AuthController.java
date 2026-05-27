package com.pinterq.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.security.JwtTokenProvider;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private JdbcTemplate jdbc;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findAll().stream().anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            return ResponseEntity.badRequest().body("Username sudah dipakai");
        }

        if (userRepository.findAll().stream().anyMatch(u -> u.getEmail().equals(request.getEmail()))) {
            return ResponseEntity.badRequest().body("Email sudah terdaftar");
        }
        
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .isApproved(true)
                .build();
        jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'USER'");
        jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_approved BOOLEAN DEFAULT TRUE");
        userRepository.save(newUser);
        return ResponseEntity.ok(new AuthResponse(newUser.getId(), newUser.getUsername(), newUser.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username atau password salah");
        }

        boolean passwordValid = passwordEncoder.matches(request.getPassword(), user.getPasswordHash())
            || request.getPassword().equals(user.getPasswordHash()); // fallback plain text for old users

        if (!passwordValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username atau password salah");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getUsername(), user.getRole(), token));
    }

    @Data
    static class AuthRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    static class AuthResponse {
        private Long userId;
        private String username;
        private String role;
        private String token;

        public AuthResponse(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public AuthResponse(Long userId, String username, String role, String token) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.token = token;
        }
    }
}
