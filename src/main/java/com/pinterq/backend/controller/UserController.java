package com.pinterq.backend.controller;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
