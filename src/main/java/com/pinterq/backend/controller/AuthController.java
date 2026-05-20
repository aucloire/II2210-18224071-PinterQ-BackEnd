package com.pinterq.backend.controller;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findAll().stream().anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            return ResponseEntity.badRequest().body("Username sudah dipakai");
        }
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .build();
        userRepository.save(newUser);
        return ResponseEntity.ok(new AuthResponse(newUser.getId(), newUser.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername()) && u.getPasswordHash().equals(request.getPassword()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body("Username atau password salah");
        }
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getUsername()));
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
        public AuthResponse(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}