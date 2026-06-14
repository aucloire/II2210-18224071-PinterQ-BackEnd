package com.pinterq.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        return userRepository.findById(userId).map(user -> ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "role", user.getRole().name(),
            "profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "",
            "approvalStatus", user.getApprovalStatus().name()
        ))).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId, @RequestBody ProfileUpdateRequest request) {
        return userRepository.findById(userId).map(user -> {
            if (request.getFullName() != null) user.setFullName(request.getFullName());
            if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
            // Check username uniqueness
            if (request.getUsername() != null) {
                userRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new RuntimeException("Username sudah dipakai");
                    }
                });
                user.setUsername(request.getUsername());
            }
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "message", "Profil berhasil diperbarui",
                "fullName", user.getFullName(),
                "username", user.getUsername(),
                "profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : ""
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestBody UsernameCheckRequest request) {
        boolean taken = userRepository.findByUsername(request.getUsername()).isPresent();
        return ResponseEntity.ok(Map.of("available", !taken));
    }

    @Data
    static class ProfileUpdateRequest {
        private String fullName;
        private String username;
        private String profileImageUrl;
    }

    @Data
    static class UsernameCheckRequest {
        private String username;
    }
}
