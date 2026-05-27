package com.pinterq.backend.controller;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.security.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findAll().stream().anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            return ResponseEntity.badRequest().body("Username sudah dipakai");
        }
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .isApproved(true)
                .build();
        userRepository.save(newUser);
        return ResponseEntity.ok(new AuthResponse(newUser.getId(), newUser.getUsername(), newUser.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername()))
                .findFirst()
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
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
