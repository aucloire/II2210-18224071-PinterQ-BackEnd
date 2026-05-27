package com.pinterq.backend.controller;

import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/pending-users")
    public ResponseEntity<?> getPendingUsers() {
        List<User> pending = userRepository.findAll().stream()
                .filter(u -> u.getApprovalStatus() == User.ApprovalStatus.PENDING)
                .toList();
        return ResponseEntity.ok(pending.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "role", u.getRole() != null ? u.getRole().name() : "USER",
                "approvalStatus", u.getApprovalStatus().name(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        )));
    }

    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers() {
        List<User> all = userRepository.findAll();
        return ResponseEntity.ok(all.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "role", u.getRole() != null ? u.getRole().name() : "USER",
                "approvalStatus", u.getApprovalStatus().name(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        )));
    }

    @PutMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        return userRepository.findById(userId).map(user -> {
            user.setApprovalStatus(User.ApprovalStatus.APPROVED);
            user.setRole(User.Role.USER);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User approved", "userId", userId));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/reject/{userId}")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        return userRepository.findById(userId).map(user -> {
            user.setApprovalStatus(User.ApprovalStatus.REJECTED);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User rejected", "userId", userId));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/set-role/{userId}")
    public ResponseEntity<?> setRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || (!role.equals("USER") && !role.equals("GURU") && !role.equals("SUPERADMIN"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role tidak valid"));
        }
        return userRepository.findById(userId).map(user -> {
            user.setRole(User.Role.valueOf(role));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Role updated", "userId", userId, "role", role));
        }).orElse(ResponseEntity.notFound().build());
    }
}
