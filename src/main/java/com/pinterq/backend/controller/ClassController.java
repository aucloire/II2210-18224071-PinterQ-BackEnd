package com.pinterq.backend.controller;

import com.pinterq.backend.model.ClassGroup;
import com.pinterq.backend.model.ClassMember;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.ClassGroupRepository;
import com.pinterq.backend.repository.ClassMemberRepository;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.repository.CategoryRepository;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
public class ClassController {

    private final ClassGroupRepository classGroupRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody CreateClassRequest req) {
        User teacher = userRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        ClassGroup cls = ClassGroup.builder()
                .name(req.getName())
                .teacher(teacher)
                .build();
        classGroupRepository.save(cls);

        // Auto-create category for class materials
        Category classCat = Category.builder()
                .name("Materi Kelas: " + cls.getName())
                .user(teacher)
                .classGroup(cls)
                .build();
        categoryRepository.save(classCat);

        return ResponseEntity.ok(Map.of(
                "id", cls.getId(),
                "name", cls.getName(),
                "classCode", cls.getClassCode(),
                "teacherName", cls.getTeacher().getUsername()
        ));
    }

    /**
     * Student joins a class using the class code.
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinClass(@RequestBody JoinClassRequest req) {
        ClassGroup cls = classGroupRepository.findByClassCode(req.getClassCode())
                .orElseThrow(() -> new RuntimeException("Kelas tidak ditemukan"));

        User student = userRepository.findById(req.getStudentId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check membership
        var existing = classMemberRepository.findByClassGroupAndUser(cls, student);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Anda sudah bergabung di kelas ini"));
        }

        ClassMember member = ClassMember.builder()
                .classGroup(cls)
                .user(student)
                .build();
        classMemberRepository.save(member);

        // Reload class with members for trigger
        ClassGroup reload = classGroupRepository.findById(cls.getId()).orElseThrow();
        reload.getMembers().add(member);

        // Notify teacher
        notificationService.notifyTeacherOnJoin(
                cls.getTeacher().getId(),
                student.getFullName() != null ? student.getFullName() : student.getUsername(),
                cls.getName()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Berhasil bergabung ke kelas",
                "className", cls.getName()
        ));
    }

    @GetMapping("/my/{teacherId}")
    public ResponseEntity<?> getTeacherClasses(@PathVariable Long teacherId) {
        List<Map<String, Object>> classes = classGroupRepository.findAll().stream()
                .filter(c -> c.getTeacher().getId().equals(teacherId))
                .map(cls -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", cls.getId());
                    m.put("name", cls.getName());
                    m.put("classCode", cls.getClassCode());
                    m.put("memberCount", cls.getMembers() != null ? cls.getMembers().size() : 0);
                    m.put("createdAt", cls.getCreatedAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/students/{teacherId}")
    public ResponseEntity<?> getTeacherStudents(@PathVariable Long teacherId) {
        List<Map<String, Object>> students = classGroupRepository.findAll().stream()
                .filter(c -> c.getTeacher().getId().equals(teacherId))
                .flatMap(cls -> cls.getMembers().stream())
                .distinct()
                .map(cm -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", cm.getUser().getId());
                    m.put("username", cm.getUser().getUsername());
                    m.put("fullName", cm.getUser().getFullName());
                    m.put("email", cm.getUser().getEmail());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllClasses() {
        List<Map<String, Object>> classes = classGroupRepository.findAll().stream()
                .map(cls -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", cls.getId());
                    m.put("name", cls.getName());
                    m.put("classCode", cls.getClassCode());
                    m.put("memberCount", cls.getMembers() != null ? cls.getMembers().size() : 0);
                    m.put("teacherName", cls.getTeacher() != null ? cls.getTeacher().getFullName() : "Unknown");
                    m.put("createdAt", cls.getCreatedAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> getStudentJoinedClasses(@PathVariable Long userId) {
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ClassMember> members = classMemberRepository.findByUser(student);
        List<Map<String, Object>> classes = members.stream().map(m -> {
            ClassGroup cls = m.getClassGroup();
            Map<String, Object> c = new HashMap<>();
            c.put("id", cls.getId());
            c.put("name", cls.getName());
            c.put("classCode", cls.getClassCode());
            c.put("memberCount", cls.getMembers() != null ? cls.getMembers().size() : 0);
            c.put("teacherName", cls.getTeacher() != null ? cls.getTeacher().getFullName() : "Unknown");
            return c;
        }).toList();
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/{classId}/members")
    public ResponseEntity<?> getClassMembers(@PathVariable Long classId) {
        ClassGroup cls = classGroupRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        List<Map<String, Object>> members = cls.getMembers().stream().map(cm -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", cm.getUser().getId());
            m.put("username", cm.getUser().getUsername());
            m.put("fullName", cm.getUser().getFullName());
            return m;
        }).toList();
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable Long classId) {
        classGroupRepository.deleteById(classId);
        return ResponseEntity.ok(Map.of("message", "Kelas berhasil dihapus"));
    }

    @lombok.Data
    static class CreateClassRequest {
        private String name;
        private Long teacherId;
    }
    @lombok.Data
    static class JoinClassRequest {
        private Long studentId;
        private String classCode;
    }
}
