package com.pinterq.backend.controller;

import com.pinterq.backend.model.Category;
import com.pinterq.backend.model.Flashcard;
import com.pinterq.backend.model.Material;
import com.pinterq.backend.model.Quiz;
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
import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
public class StudyController {

    private final GeminiAiService geminiAiService;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FlashcardRepository flashcardRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final com.pinterq.backend.service.NotificationService notificationService;

    @GetMapping("/test-ai")
    public ResponseEntity<?> testAi() {
        try {
            Material testMaterial = Material.builder().title("Test").content("Test content").build();
            // This is a test, keeping it simple
            return ResponseEntity.ok("AI test endpoint simplified. Use /generate for real logic.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<?> generate(@RequestBody GenerateRequest request) {
        System.out.println(">>> RECEIVED GENERATE REQUEST: " + request);
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            Category category = categoryRepository.findById(request.getCategoryId())
                    .or(() -> categoryRepository.findByClassGroup_Id(request.getCategoryId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category/Class not found"));

            // 1. Panggil AI terlebih dahulu (Synchronous)
            GeminiAiService.GeneratedStudyData aiData = geminiAiService.generateStudyMaterials(request.getContent());

            // 2. Jika sukses, baru simpan Material
            Material material = Material.builder()
                    .user(user)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(category)
                    .build();

            Material savedMaterial = materialRepository.save(material);

            // 3. Simpan Kuis & Flashcard dengan relasi ke Material
            if (aiData.getQuizzes() != null && !aiData.getQuizzes().isEmpty()) {
                aiData.getQuizzes().forEach(q -> q.setMaterial(savedMaterial));
                quizRepository.saveAll(aiData.getQuizzes());
            }

            if (aiData.getFlashcards() != null && !aiData.getFlashcards().isEmpty()) {
                aiData.getFlashcards().forEach(f -> f.setMaterial(savedMaterial));
                flashcardRepository.saveAll(aiData.getFlashcards());
            }

            // 4. Notifikasi
            try {
                if (category.getClassGroup() != null) {
                    notificationService.notifyStudentsOnNewMaterial(category.getClassGroup(), savedMaterial.getTitle());
                }
                notificationService.notifyUserOnGenerationComplete(user.getId(), savedMaterial.getTitle());
            } catch (Exception e) {
                System.err.println("Notification Error: " + e.getMessage());
            }

            return ResponseEntity.ok(savedMaterial);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("GENERATE ERROR: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gagal memproses data melalui AI, silakan coba lagi");
        }
    }

    @PostMapping("/materials")
    public ResponseEntity<?> createMaterial(@RequestBody GenerateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .or(() -> categoryRepository.findByClassGroup_Id(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category or Class not found"));

        Material material = Material.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .category(category)
                .build();

        return ResponseEntity.ok(materialRepository.save(material));
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<?> updateMaterial(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        if (updates.containsKey("title")) material.setTitle(updates.get("title"));
        if (updates.containsKey("content")) material.setContent(updates.get("content"));
        return ResponseEntity.ok(materialRepository.save(material));
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) {
        materialRepository.deleteById(id);
        return ResponseEntity.ok("Materi dihapus");
    }

    @PostMapping("/quizzes")
    public ResponseEntity<?> createQuiz(@RequestBody Map<String, Object> data) {
        Material material = materialRepository.findById(Long.valueOf(data.get("materialId").toString()))
                .orElseThrow(() -> new RuntimeException("Material not found"));
        Quiz quiz = Quiz.builder()
                .material(material)
                .question(data.get("question").toString())
                .optionA(data.get("optionA").toString())
                .optionB(data.get("optionB").toString())
                .optionC(data.get("optionC").toString())
                .optionD(data.get("optionD").toString())
                .correctAnswer(data.get("correctAnswer").toString())
                .explanation(data.get("explanation") != null ? data.get("explanation").toString() : null)
                .build();
        return ResponseEntity.ok(quizRepository.save(quiz));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        quizRepository.deleteById(id);
        return ResponseEntity.ok("Kuis dihapus");
    }

    @PostMapping("/flashcards")
    public ResponseEntity<?> createFlashcard(@RequestBody Map<String, Object> data) {
        Material material = materialRepository.findById(Long.valueOf(data.get("materialId").toString()))
                .orElseThrow(() -> new RuntimeException("Material not found"));
        Flashcard flashcard = Flashcard.builder()
                .material(material)
                .question(data.get("question").toString())
                .answer(data.get("answer").toString())
                .isMemorized(false)
                .build();
        return ResponseEntity.ok(flashcardRepository.save(flashcard));
    }

    @DeleteMapping("/flashcards/{id}")
    public ResponseEntity<?> deleteFlashcard(@PathVariable Long id) {
        flashcardRepository.deleteById(id);
        return ResponseEntity.ok("Flashcard dihapus");
    }

    @GetMapping("/flashcards/{categoryId}")
    public ResponseEntity<?> getFlashcardsByCategory(@PathVariable Long categoryId) {
        var flashcards = flashcardRepository.findAll().stream()
                .filter(f -> f.getMaterial() != null)
                .filter(f -> {
                    Material m = f.getMaterial();
                    if (m.getCategory() != null) {
                        if (m.getCategory().getId().equals(categoryId)) return true;
                        if (m.getCategory().getClassGroup() != null && m.getCategory().getClassGroup().getId().equals(categoryId)) return true;
                    }
                    return false;
                })
                .toList();
        return ResponseEntity.ok(flashcards);
    }

    @GetMapping("/quizzes/{categoryId}")
    public ResponseEntity<?> getQuizzesByCategory(@PathVariable Long categoryId) {
        var quizzes = quizRepository.findAll().stream()
                .filter(q -> q.getMaterial() != null)
                .filter(q -> {
                    Material m = q.getMaterial();
                    if (m.getCategory() != null) {
                        if (m.getCategory().getId().equals(categoryId)) return true;
                        if (m.getCategory().getClassGroup() != null && m.getCategory().getClassGroup().getId().equals(categoryId)) return true;
                    }
                    return false;
                })
                .toList();
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/materials/{categoryId}")
    public ResponseEntity<?> getMaterialsByCategory(@PathVariable Long categoryId) {
        var materials = materialRepository.findAll().stream()
                .filter(m -> m.getCategory() != null)
                .filter(m -> {
                    if (m.getCategory().getId().equals(categoryId)) return true;
                    if (m.getCategory().getClassGroup() != null && m.getCategory().getClassGroup().getId().equals(categoryId)) return true;
                    return false;
                })
                .toList();
        return ResponseEntity.ok(materials);
    }

    @PostMapping("/generate-adaptive")
    public ResponseEntity<?> generateAdaptive(@RequestBody AdaptiveRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .or(() -> categoryRepository.findByClassGroup_Id(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category or Class not found"));

        Material latestMaterial = materialRepository.findAll().stream()
                .filter(m -> m.getCategory() != null && m.getCategory().getId().equals(category.getId()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new RuntimeException("Belum ada materi di matkul ini"));

        geminiAiService.generateAdaptiveQuizzes(latestMaterial.getContent(), latestMaterial, request.getDifficulty());

        return ResponseEntity.ok("Soal adaptif " + request.getDifficulty() + " berhasil ditambahkan!");
    }

    @PostMapping("/submit-attempt")
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

    @GetMapping("/history/{userId}")
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