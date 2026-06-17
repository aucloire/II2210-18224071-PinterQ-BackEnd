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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://aucloire.stei.my.id", allowedHeaders = "*", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.pinterq.backend.service.NotificationService notificationService;

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "PinterQ API is reachable"));
    }

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
                .claim("fullName", user.getFullName() != null ? user.getFullName() : "")
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username sudah dipakai"));
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email sudah dipakai"));
        }

        User.Role resolvedRole;
        try {
            resolvedRole = (request.getRole() != null && !request.getRole().isBlank())
                    ? User.Role.valueOf(request.getRole().toUpperCase())
                    : User.Role.MURID;
        } catch (IllegalArgumentException e) {
            resolvedRole = User.Role.MURID;
        }
        if (resolvedRole == User.Role.SUPERADMIN) {
            resolvedRole = User.Role.MURID;
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(resolvedRole)
                .fullName(request.getFullName() != null ? request.getFullName() : null)
                .approvalStatus(User.ApprovalStatus.PENDING)
                .build();
        userRepository.save(newUser);

        try {
            notificationService.notifyAdminsOnRegistration(newUser.getUsername());
        } catch (Exception e) {}

        return ResponseEntity.ok(Map.of(
            "message", "Registrasi berhasil. Silakan tunggu persetujuan superadmin.",
            "requiresApproval", true
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Username atau password salah"));
        }
        if (user.getApprovalStatus() == User.ApprovalStatus.PENDING) {
            return ResponseEntity.status(403).body(Map.of("error", "Akun Anda belum disetujui Admin"));
        }
        if (user.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
            return ResponseEntity.status(403).body(Map.of("error", "Akun Anda ditolak Admin"));
        }

        String token = generateToken(user);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", user.getId(),
            "username", user.getUsername(),
            "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
            "role", user.getRole().name(),
            "profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "",
            "approvalStatus", user.getApprovalStatus().name()
        ));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "role", user.getRole().name(),
            "profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "",
            "approvalStatus", user.getApprovalStatus().name()
        ));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        
        if (updates.containsKey("fullName")) user.setFullName(updates.get("fullName"));
        if (updates.containsKey("email")) user.setEmail(updates.get("email"));
        if (updates.containsKey("profileImageUrl")) user.setProfileImageUrl(updates.get("profileImageUrl"));
        if (updates.containsKey("password") && !updates.get("password").isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(updates.get("password")));
        }
        
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profil berhasil diperbarui"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "email", u.getEmail(),
                    "fullName", u.getFullName() != null ? u.getFullName() : "",
                    "role", u.getRole().name(),
                    "profileImageUrl", u.getProfileImageUrl() != null ? u.getProfileImageUrl() : "",
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
        private String role;
        private String fullName;
    }
}
