package com.pinterq.backend.controller;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.Material;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.MaterialRepository;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study")
@CrossOrigin("*")
@RequiredArgsConstructor
public class StudyController {

    private final GeminiAiService geminiAiService;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Material material = Material.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .category(category)
                .build();

        Material savedMaterial = materialRepository.save(material);

        geminiAiService.generateStudyMaterials(request.getContent(), savedMaterial);

        return ResponseEntity.ok("Materi sukses digenerate");
    }
}
