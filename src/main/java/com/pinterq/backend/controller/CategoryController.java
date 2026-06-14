package com.pinterq.backend.controller;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

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
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(c -> c.getUser().getId().equals(userId))
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category newCategory = Category.builder()
                .name(request.getName())
                .user(user)
                .build();
        categoryRepository.save(newCategory);
        return ResponseEntity.ok(newCategory);
    }

    @Data
    static class CategoryRequest {
        private Long userId;
        private String name;
    }
}
