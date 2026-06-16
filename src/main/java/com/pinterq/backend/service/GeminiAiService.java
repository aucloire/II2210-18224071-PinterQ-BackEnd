package com.pinterq.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterq.backend.model.Flashcard;
import com.pinterq.backend.model.Material;
import com.pinterq.backend.model.Quiz;
import com.pinterq.backend.repository.FlashcardRepository;
import com.pinterq.backend.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    private final FlashcardRepository flashcardRepository;
    private final QuizRepository quizRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final com.pinterq.backend.service.NotificationService notificationService;

    @lombok.Data
    @lombok.Builder
    public static class GeneratedStudyData {
        private java.util.List<Quiz> quizzes;
        private java.util.List<Flashcard> flashcards;
    }

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${GEMINI_API_KEY:${gemini.api.key}}")
    private String apiKey;

    public GeminiAiService(FlashcardRepository flashcardRepository, QuizRepository quizRepository, com.pinterq.backend.service.NotificationService notificationService) {
        this.flashcardRepository = flashcardRepository;
        this.quizRepository = quizRepository;
        this.notificationService = notificationService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public GeneratedStudyData generateStudyMaterials(String textContent) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String prompt = "Berdasarkan teks berikut, ekstrak informasi penting dan buatkan flashcards (pertanyaan dan jawaban singkat) " +
                        "serta soal kuis pilihan ganda yang komprehensif. Jumlah flashcard dan kuis menyesuaikan dengan kepadatan materi, buatlah seakurat dan sebanyak yang diperlukan. " +
                        "ATURAN PENJELASAN KUIS: Penjelasan (explanation) HARUS bersifat mandiri, komprehensif, dan bergaya seperti guru yang sedang mengajar langsung. DILARANG KERAS menggunakan kata-kata seperti 'Berdasarkan teks...', 'Pada teks dijelaskan...', atau 'Teks menyebutkan...'. " +
                        "Kembalikan HANYA dalam format JSON murni tanpa markdown (tanpa ```json). " +
                        "Struktur: {\"flashcards\": [{\"question\": \"...\", \"answer\": \"...\"}], " +
                        "\"quizzes\": [{\"question\": \"...\", \"optionA\": \"...\", \"optionB\": \"...\", \"optionC\": \"...\", \"optionD\": \"...\", \"correctAnswer\": \"A\", \"explanation\": \"...\"}]} " +
                        "Teks: " + textContent;

                // Body request Gemini API
                Map<String, Object> part = new HashMap<>();
                part.put("text", prompt);

                Map<String, Object> content = new HashMap<>();
                content.put("parts", java.util.List.of(part));

                // Modern Gemini API config for JSON
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("response_mime_type", "application/json");

                Map<String, Object> body = new HashMap<>();
                body.put("contents", java.util.List.of(content));
                body.put("generationConfig", generationConfig);

                // HTTP POST
                String url = apiUrl + "?key=" + apiKey;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                System.out.println("Calling Gemini API (Attempt " + (retryCount + 1) + "): " + apiUrl);

                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String aiResponseText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

                    // Pre-processing: Remove markdown code blocks if present
                    if (aiResponseText.contains("```json")) {
                        aiResponseText = aiResponseText.substring(aiResponseText.indexOf("```json") + 7);
                        if (aiResponseText.contains("```")) {
                            aiResponseText = aiResponseText.substring(0, aiResponseText.lastIndexOf("```"));
                        }
                    } else if (aiResponseText.contains("```")) {
                        aiResponseText = aiResponseText.substring(aiResponseText.indexOf("```") + 3);
                        if (aiResponseText.contains("```")) {
                            aiResponseText = aiResponseText.substring(0, aiResponseText.lastIndexOf("```"));
                        }
                    }
                    aiResponseText = aiResponseText.trim();

                    // Parsing JSON dari AI
                    JsonNode resultJson = objectMapper.readTree(aiResponseText);
                    java.util.List<Flashcard> flashcards = new java.util.ArrayList<>();
                    java.util.List<Quiz> quizzes = new java.util.ArrayList<>();

                    // Parse Flashcards
                    JsonNode flashcardsNode = resultJson.path("flashcards");
                    if (flashcardsNode.isArray()) {
                        for (JsonNode node : flashcardsNode) {
                            flashcards.add(Flashcard.builder()
                                    .question(node.path("question").asText())
                                    .answer(node.path("answer").asText())
                                    .isMemorized(false)
                                    .build());
                        }
                    }

                    // Parse Quizzes
                    JsonNode quizzesNode = resultJson.path("quizzes");
                    if (quizzesNode.isArray()) {
                        for (JsonNode node : quizzesNode) {
                            quizzes.add(Quiz.builder()
                                    .question(node.path("question").asText())
                                    .optionA(node.path("optionA").asText())
                                    .optionB(node.path("optionB").asText())
                                    .optionC(node.path("optionC").asText())
                                    .optionD(node.path("optionD").asText())
                                    .correctAnswer(node.path("correctAnswer").asText())
                                    .explanation(node.path("explanation").asText())
                                    .build());
                        }
                    }

                    if (flashcards.isEmpty() && quizzes.isEmpty()) {
                        throw new RuntimeException("AI returned empty results");
                    }

                    return GeneratedStudyData.builder()
                            .flashcards(flashcards)
                            .quizzes(quizzes)
                            .build();
                }
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                System.err.println("Gemini API Error (Attempt " + (retryCount + 1) + "): " + e.getStatusCode());
                if (e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(5000);
                            continue;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                throw new RuntimeException("API AI sedang sibuk, silakan coba lagi");
            } catch (Exception e) {
                System.err.println("Error generating study materials: " + e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new RuntimeException("Gagal memproses data melalui AI setelah beberapa percobaan");
    }

    @Async
    public void generateAdaptiveQuizzes(String textContent, Material savedMaterial, String difficulty) {
        try {
            String difficultyInstruction = "";
            if ("HOTS".equals(difficulty)) {
                difficultyInstruction = "Tingkat kesulitan: SANGAT SULIT (HOTS - Higher Order Thinking Skills). Fokus pada studi kasus, analisis kritis, evaluasi, dan pemecahan masalah. Jangan berikan soal hafalan murni.";
            } else {
                difficultyInstruction = "Tingkat kesulitan: DASAR (Beginner). Fokus pada definisi simpel, fakta utama, dan pemahaman konsep paling fundamental. Buat bahasanya sangat mudah dipahami.";
            }

            String prompt = "Berdasarkan teks berikut, buatkan 3-5 soal kuis pilihan ganda tambahan. " +
                    difficultyInstruction + " " +
                    "ATURAN PENJELASAN KUIS: Penjelasan (explanation) HARUS bersifat mandiri, komprehensif, dan bergaya seperti guru yang sedang mengajar langsung. DILARANG KERAS menggunakan kata-kata seperti 'Berdasarkan teks...', 'Pada teks dijelaskan...', atau 'Teks menyebutkan...'. " +
                    "Kembalikan HANYA dalam format JSON murni tanpa markdown. " +
                    "Struktur: {\"quizzes\": [{\"question\": \"...\", \"optionA\": \"...\", \"optionB\": \"...\", \"optionC\": \"...\", \"optionD\": \"...\", \"correctAnswer\": \"A\", \"explanation\": \"...\"}]} " +
                    "Teks: " + textContent;

            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            // Modern Gemini API config for JSON
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(content));
            body.put("generationConfig", generationConfig);

            String url = apiUrl + "?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            System.out.println("Calling Gemini API Adaptive: " + apiUrl);
            // Log masked API key for debugging
            if (apiKey != null && apiKey.length() > 8) {
                System.out.println("Using API Key: " + apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4));
            }

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String aiResponseText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                
                System.out.println("AI ADAPTIVE RAW RESPONSE: " + aiResponseText);

                // Pre-processing: Remove markdown code blocks if present
                if (aiResponseText.contains("```json")) {
                    aiResponseText = aiResponseText.substring(aiResponseText.indexOf("```json") + 7);
                    if (aiResponseText.contains("```")) {
                        aiResponseText = aiResponseText.substring(0, aiResponseText.lastIndexOf("```"));
                    }
                } else if (aiResponseText.contains("```")) {
                    aiResponseText = aiResponseText.substring(aiResponseText.indexOf("```") + 3);
                    if (aiResponseText.contains("```")) {
                        aiResponseText = aiResponseText.substring(0, aiResponseText.lastIndexOf("```"));
                    }
                }
                aiResponseText = aiResponseText.trim();

                JsonNode resultJson = objectMapper.readTree(aiResponseText);
                JsonNode quizzesNode = resultJson.path("quizzes");
                
                if (quizzesNode.isArray()) {
                    for (JsonNode node : quizzesNode) {
                        Quiz quiz = Quiz.builder()
                                .question(node.path("question").asText())
                                .optionA(node.path("optionA").asText())
                                .optionB(node.path("optionB").asText())
                                .optionC(node.path("optionC").asText())
                                .optionD(node.path("optionD").asText())
                                .correctAnswer(node.path("correctAnswer").asText())
                                .explanation(node.path("explanation").asText())
                                .material(savedMaterial)
                                .build();
                        quizRepository.save(quiz);
                    }
                }
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Gemini API Adaptive HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Error generating adaptive quizzes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
