package com.pinterq.backend.controller;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.Material;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.FlashcardRepository;
import com.pinterq.backend.repository.MaterialRepository;
import com.pinterq.backend.repository.QuizRepository;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.service.GeminiAiService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final GeminiAiService geminiAiService;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FlashcardRepository flashcardRepository;
    private final QuizRepository quizRepository;

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

    @GetMapping("/flashcards/{categoryId}")
    public ResponseEntity<?> getFlashcardsByCategory(@PathVariable Long categoryId) {
        var flashcards = flashcardRepository.findAll().stream()
                .filter(f -> f.getMaterial() != null 
                          && f.getMaterial().getCategory() != null 
                          && f.getMaterial().getCategory().getId().equals(categoryId))
                .toList();
        return ResponseEntity.ok(flashcards);
    }

    @GetMapping("/quizzes/{categoryId}")
    public ResponseEntity<?> getQuizzesByCategory(@PathVariable Long categoryId) {
        var quizzes = quizRepository.findAll().stream()
                .filter(q -> q.getMaterial() != null 
                          && q.getMaterial().getCategory() != null 
                          && q.getMaterial().getCategory().getId().equals(categoryId))
                .toList();
        return ResponseEntity.ok(quizzes);
    }

    @PostMapping("/generate-adaptive")
    public ResponseEntity<?> generateAdaptive(@RequestBody AdaptiveRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Material latestMaterial = materialRepository.findAll().stream()
                .filter(m -> m.getCategory() != null && m.getCategory().getId().equals(category.getId()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new RuntimeException("Belum ada materi di matkul ini"));

        geminiAiService.generateAdaptiveQuizzes(latestMaterial.getContent(), latestMaterial, request.getDifficulty());

        return ResponseEntity.ok("Soal adaptif " + request.getDifficulty() + " berhasil ditambahkan!");
    }

    @Data
    static class AdaptiveRequest {
        private Long categoryId;
        private String difficulty;
    }
}
