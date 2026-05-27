package com.pinterq.backend.controller;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.Material;
import com.pinterq.backend.model.QuizAttempt;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.CategoryRepository;
import com.pinterq.backend.repository.FlashcardRepository;
import com.pinterq.backend.repository.MaterialRepository;
import com.pinterq.backend.repository.QuizAttemptRepository;
import com.pinterq.backend.repository.QuizRepository;
import com.pinterq.backend.repository.UserRepository;
import com.pinterq.backend.service.GeminiAiService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

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
    private final QuizAttemptRepository quizAttemptRepository;

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

    @PostMapping("/quiz-attempt")
    public ResponseEntity<?> submitAttempt(@RequestBody SubmitAttemptRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Materi not found"));

        QuizAttempt attempt = QuizAttempt.builder()
                .user(user)
                .material(material)
                .score(BigDecimal.valueOf(request.getScore()))
                .build();

        QuizAttempt saved = quizAttemptRepository.save(attempt);

        return ResponseEntity.ok(Map.of(
            "attemptId", saved.getId(),
            "score", saved.getScore(),
            "attemptDate", saved.getAttemptDate() != null ? saved.getAttemptDate().toString() : ""
        ));
    }

    @GetMapping("/quiz-history/{userId}")
    public ResponseEntity<?> getHistory(@PathVariable Long userId) {
        var attempts = quizAttemptRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .map(a -> Map.of(
                    "attemptId", a.getId(),
                    "materialId", a.getMaterial().getId(),
                    "materialTitle", a.getMaterial().getTitle(),
                    "score", a.getScore(),
                    "attemptDate", a.getAttemptDate() != null ? a.getAttemptDate().toString() : ""
                ))
                .toList();
        return ResponseEntity.ok(attempts);
    }

    @Data
    static class GenerateRequest {
        private Long userId;
        private Long categoryId;
        private String title;
        private String content;
    }

    @Data
    static class AdaptiveRequest {
        private Long categoryId;
        private String difficulty;
    }

    @Data
    static class SubmitAttemptRequest {
        private Long userId;
        private Long materialId;
        private double score;
    }
}