package com.pinterq.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateToken(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findAll().stream()
                .anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username sudah dipakai"));
        }
        if (userRepository.findAll().stream()
                .anyMatch(u -> u.getEmail().equals(request.getEmail()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email sudah dipakai"));
        }

        // Default: USER role, PENDING approval (auto-approved for demo)
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .approvalStatus(User.ApprovalStatus.APPROVED)
                .build();
        userRepository.save(newUser);

        String token = generateToken(newUser);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", newUser.getId(),
            "username", newUser.getUsername(),
            "role", newUser.getRole().name(),
            "approvalStatus", newUser.getApprovalStatus().name()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername())
                          && passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Username atau password salah"));
        }
        if (user.getApprovalStatus() == User.ApprovalStatus.PENDING) {
            return ResponseEntity.status(403).body(Map.of("error", "Akun belum disetujui superadmin"));
        }
        if (user.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
            return ResponseEntity.status(403).body(Map.of("error", "Registrasi ditolak"));
        }

        String token = generateToken(user);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", user.getId(),
            "username", user.getUsername(),
            "role", user.getRole().name(),
            "approvalStatus", user.getApprovalStatus().name()
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "email", u.getEmail(),
                    "role", u.getRole().name(),
                    "approvalStatus", u.getApprovalStatus().name(),
                    "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{userId}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User " + user.getUsername() + " telah disetujui"));
    }

    @PutMapping("/users/{userId}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        user.setApprovalStatus(User.ApprovalStatus.REJECTED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User " + user.getUsername() + " telah ditolak"));
    }

    @Data
    static class AuthRequest {
        private String username;
        private String email;
        private String password;
    }
}