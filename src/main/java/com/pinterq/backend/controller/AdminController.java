package com.pinterq.backend.controller;

import com.pinterq.backend.model.User;
import com.pinterq.backend.model.ClassGroup;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.repository.ClassMemberRepository;
import com.pinterq.backend.repository.ClassGroupRepository;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
public class AdminController {

    private final UserRepository userRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassGroupRepository classGroupRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/pending-users")
    public ResponseEntity<?> getPendingUsers() {
        List<User> pending = userRepository.findAll().stream()
                .filter(u -> u.getApprovalStatus() == User.ApprovalStatus.PENDING)
                .toList();
        return ResponseEntity.ok(pending.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "role", u.getRole() != null ? u.getRole().name() : "MURID",
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
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "role", u.getRole() != null ? u.getRole().name() : "MURID",
                "profileImageUrl", u.getProfileImageUrl() != null ? u.getProfileImageUrl() : "",
                "approvalStatus", u.getApprovalStatus().name(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        )));
    }

    @PutMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        return userRepository.findById(userId).map(user -> {
            user.setApprovalStatus(User.ApprovalStatus.APPROVED);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User approved", "userId", userId, "role", user.getRole() != null ? user.getRole().name() : "MURID"));
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
        if (role == null || (!role.equals("MURID") && !role.equals("GURU") && !role.equals("SUPERADMIN"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role tidak valid"));
        }
        return userRepository.findById(userId).map(user -> {
            user.setRole(User.Role.valueOf(role));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Role updated", "userId", userId, "role", role));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengguna tidak ditemukan"));
        
        try {
            // 1. Bersihkan notifikasi
            notificationRepository.deleteAll(user.getNotifications());
            
            // 2. Bersihkan keanggotaan kelas (ClassMember)
            classMemberRepository.deleteByUserId(userId);
            
            // 3. Bersihkan kategori & materi milik user
            categoryRepository.deleteAll(user.getCategories());
            
            // 4. Jika guru, hapus kelas yang diajar (ini akan mentrigger cascade ke materi kelas tersebut)
            classGroupRepository.deleteByTeacherId(userId);
            
            // 5. Akhirnya hapus user
            userRepository.delete(user);
            
            return ResponseEntity.ok(Map.of("message", "Pengguna berhasil dihapus", "userId", userId));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menghapus pengguna: " + e.getMessage());
        }
    }
}
