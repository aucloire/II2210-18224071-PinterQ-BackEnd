package com.pinterq.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            if (request.getUsername() != null) user.setUsername(request.getUsername());
            if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "message", "Profil berhasil diperbarui",
                "fullName", user.getFullName(),
                "profileImageUrl", user.getProfileImageUrl()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Data
    static class ProfileUpdateRequest {
        private String fullName;
        private String username;
        private String profileImageUrl;
    }
}
