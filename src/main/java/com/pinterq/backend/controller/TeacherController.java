package com.pinterq.backend.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.Material;
import com.pinterq.backend.model.QuizAttempt;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.MaterialRepository;
import com.pinterq.backend.repository.QuizAttemptRepository;
import com.pinterq.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://aucloire.stei.my.id", "http://localhost:5173"}, allowedHeaders = "*", allowCredentials = "true")
public class TeacherController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @GetMapping("/stats/{teacherId}")
    public ResponseEntity<?> getTeacherStats(@PathVariable Long teacherId) {
        return userRepository.findById(teacherId).map(teacher -> {
            // Count categories created by teacher
            long categoryCount = categoryRepository.count();
            long materialCount = materialRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getId().equals(teacherId))
                .count();

            // Find all MURIDs who attempted quizzes on this teacher's materials
            List<Material> myMaterials = materialRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getId().equals(teacherId))
                .toList();
            List<Long> materialIds = myMaterials.stream().map(Material::getId).toList();

            long studentCount = 0;
            if (!materialIds.isEmpty()) {
                List<Long> studentIds = quizAttemptRepository.findAll().stream()
                    .filter(a -> materialIds.contains(a.getMaterial().getId()))
                    .map(a -> a.getUser().getId())
                    .distinct()
                    .toList();
                studentCount = studentIds.size();
            }

            return ResponseEntity.ok(Map.of(
                "studentCount", studentCount,
                "categoryCount", categoryCount,
                "materialCount", materialCount
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/students/{teacherId}")
    public ResponseEntity<?> getTeacherStudents(@PathVariable Long teacherId) {
        return userRepository.findById(teacherId).map(teacher -> {
            List<Material> myMaterials = materialRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getId().equals(teacherId))
                .toList();
            List<Long> materialIds = myMaterials.stream().map(Material::getId).toList();

            if (materialIds.isEmpty()) {
                return ResponseEntity.ok(List.<Map<String, Object>>of());
            }

            // Get all distinct MURID attempts on teacher's materials
            Map<Long, Map<String, Object>> studentMap = new LinkedHashMap<>();
            for (QuizAttempt attempt : quizAttemptRepository.findAll()) {
                Material material = attempt.getMaterial();
                if (materialIds.contains(material.getId())) {
                    User student = attempt.getUser();
                    if (student == null) continue;
                    if (student.getRole() != User.Role.MURID) continue;

                    studentMap.computeIfAbsent(student.getId(), sid -> {
                        Map<String, Object> info = new LinkedHashMap<>();
                        info.put("id", sid);
                        info.put("username", student.getUsername());
                        info.put("fullName", student.getFullName() != null ? student.getFullName() : student.getUsername());
                        info.put("email", student.getEmail());
                        info.put("attemptCount", 0);
                        info.put("totalScore", new BigDecimal(0));
                        return info;
                    });

                    Map<String, Object> info = studentMap.get(student.getId());
                    info.put("attemptCount", (int) info.getOrDefault("attemptCount", 0) + 1);
                    BigDecimal current = (BigDecimal) info.getOrDefault("totalScore", new BigDecimal(0));
                    info.put("totalScore", current.add(attempt.getScore()));
                }
            }

            // Calculate avg score and add to each student
            List<Map<String, Object>> result = studentMap.values().stream().map(info -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", info.get("id"));
                r.put("username", info.get("username"));
                r.put("fullName", info.get("fullName"));
                r.put("email", info.get("email"));
                int attempts = (int) info.get("attemptCount");
                r.put("attemptCount", attempts);
                BigDecimal total = (BigDecimal) info.get("totalScore");
                BigDecimal avg = attempts > 0
                    ? total.divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP)
                    : new BigDecimal(0);
                r.put("avgScore", avg.doubleValue());
                return r;
            }).toList();

            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}/progress/{teacherId}")
    public ResponseEntity<?> getStudentProgress(@PathVariable Long studentId, @PathVariable Long teacherId) {
        return userRepository.findById(studentId).map(student -> {
            List<Material> myMaterials = materialRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getId().equals(teacherId))
                .toList();
            List<Long> materialIds = myMaterials.stream().map(Material::getId).toList();

            if (materialIds.isEmpty()) {
                return ResponseEntity.ok(Map.of("student", student.getUsername(), "attempts", List.<Map<String, Object>>of()));
            }

            List<Map<String, Object>> attempts = quizAttemptRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(studentId))
                .filter(a -> materialIds.contains(a.getMaterial().getId()))
                .map(a -> {
                    Material mat = a.getMaterial();
                    return Map.<String, Object>of(
                        "materialTitle", mat.getTitle(),
                        "score", a.getScore().doubleValue(),
                        "attemptDate", a.getAttemptDate() != null ? a.getAttemptDate().toString() : ""
                    );
                })
                .toList();

            return ResponseEntity.ok(Map.of(
                "student", student.getUsername(),
                "fullName", student.getFullName(),
                "attempts", attempts
            ));
        }).orElse(ResponseEntity.notFound().build());
    }
}
