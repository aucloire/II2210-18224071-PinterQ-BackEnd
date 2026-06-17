package com.pinterq.backend.controller;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.ClassGroup;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.ClassGroupRepository;
import com.pinterq.backend.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;

    @GetMapping("/public")
    public ResponseEntity<?> getPublicCategories() {
        List<Category> categories = categoryRepository.findAll();
        return ResponseEntity.ok(categories.stream().map(c -> Map.of(
            "id", c.getId(),
            "name", c.getName(),
            "createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""
        )).toList());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserCategories(@PathVariable Long userId) {
        List<Category> categories = categoryRepository.findByUser_Id(userId);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category newCategory = Category.builder()
                .name(request.getName())
                .user(user)
                .classGroup(resolveClass(request.getClassId()))
                .build();
        categoryRepository.save(newCategory);

        return ResponseEntity.ok(Map.of(
                "id", newCategory.getId(),
                "name", newCategory.getName(),
                "classId", request.getClassId()
        ));
    }

    private ClassGroup resolveClass(Long classId) {
        if (classId == null) return null;
        return classGroupRepository.findById(classId).orElse(null);
    }

    @Data
    static class CategoryRequest {
        private Long userId;
        private String name;
        private Long classId;
    }
}
