package com.ubaid.ai_service.controller;

import com.ubaid.ai_service.service.AgricultureChatbotService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.Map;
import java.util.Set;
import java.util.UUID;
@RestController
@RequestMapping("/api/agriculture")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AgricultureChatbotController {

    private final AgricultureChatbotService chatbotService;

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AgricultureChatbotService.ChatbotResponse> chatWithBot(
            @RequestParam("message") @NotBlank @Size(max = 1000) String message,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "language", defaultValue = "en") String language) {

        try {
            // Generate session ID if not provided
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            // Validate language parameter
            if (!isValidLanguage(language)) {
                return ResponseEntity.badRequest()
                        .body(new AgricultureChatbotService.ChatbotResponse(
                                "Invalid language code. Supported languages: en, hi, bn, te, ta, mr, gu, kn, ml, pa, or",
                                false,
                                "INVALID_LANGUAGE",
                                sessionId
                        ));
            }

            // Extract image bytes if present
            byte[] imageBytes = null;
            if (image != null && !image.isEmpty()) {
                // Validate image size (max 5MB)
                if (image.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body(new AgricultureChatbotService.ChatbotResponse(
                                    "Image size too large. Please upload an image smaller than 5MB.",
                                    false,
                                    "IMAGE_SIZE_ERROR",
                                    sessionId
                            ));
                }

                // Validate image type
                String contentType = image.getContentType();
                if (contentType == null || !isValidImageType(contentType)) {
                    return ResponseEntity.badRequest()
                            .body(new AgricultureChatbotService.ChatbotResponse(
                                    "Invalid image format. Please upload JPEG, PNG, or WebP images only.",
                                    false,
                                    "INVALID_IMAGE_TYPE",
                                    sessionId
                            ));
                }

                imageBytes = image.getBytes();
                log.info("Received image upload - Size: {} bytes, Type: {}", imageBytes.length, contentType);
            }

            // Process the chat request with language support
            AgricultureChatbotService.ChatbotResponse response = chatbotService.processChat(message, imageBytes, sessionId, language);

            log.info("Chat processed - SessionId: {}, Language: {}, Success: {}, HasImage: {}",
                    sessionId, language, response.isSuccess(), imageBytes != null);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in chat endpoint - SessionId: {}, Language: {}, Error: {}", sessionId, language, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new AgricultureChatbotService.ChatbotResponse(
                            "Technical error occurred. Please try again with a clear agricultural question.",
                            false,
                            "SERVER_ERROR",
                            sessionId != null ? sessionId : UUID.randomUUID().toString()
                    ));
        }
    }

    @PostMapping("/chat/text")
    public ResponseEntity<AgricultureChatbotService.ChatbotResponse> chatTextOnly(
            @RequestBody ChatTextRequest request) {

        try {
            // Validate request
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AgricultureChatbotService.ChatbotResponse(
                                "Message cannot be empty. Please ask about crops, farming, or agricultural topics.",
                                false,
                                "EMPTY_MESSAGE",
                                request.getSessionId()
                        ));
            }

            // Generate session ID if not provided
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            // Default language if not provided
            String language = request.getLanguage() != null ? request.getLanguage() : "en";

            // Validate language parameter
            if (!isValidLanguage(language)) {
                return ResponseEntity.badRequest()
                        .body(new AgricultureChatbotService.ChatbotResponse(
                                "Invalid language code. Supported languages: en, hi, bn, te, ta, mr, gu, kn, ml, pa, or",
                                false,
                                "INVALID_LANGUAGE",
                                sessionId
                        ));
            }

            // Process text-only chat with language support
            AgricultureChatbotService.ChatbotResponse response = chatbotService.processChat(
                    request.getMessage(), null, sessionId, language);

            log.info("Text-only chat processed - SessionId: {}, Language: {}, Success: {}",
                    sessionId, language, response.isSuccess());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in text chat endpoint: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new AgricultureChatbotService.ChatbotResponse(
                            "Technical error occurred. Please try again with your agricultural question.",
                            false,
                            "SERVER_ERROR",
                            UUID.randomUUID().toString()
                    ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        return ResponseEntity.ok(new HealthResponse("Agriculture Chatbot Service is running", true));
    }

//

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp");
    }

    private boolean isValidLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return false;
        }

        Set<String> validLanguages = Set.of("en", "hi", "bn", "te", "ta", "mr", "gu", "kn", "ml", "pa", "or");
        return validLanguages.contains(language.toLowerCase().trim());
    }

    // Request DTOs
    public static class ChatTextRequest {
        private String message;
        private String sessionId;
        private String language;

        public ChatTextRequest() {}

        public ChatTextRequest(String message, String sessionId, String language) {
            this.message = message;
            this.sessionId = sessionId;
            this.language = language;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    public static class HealthResponse {
        private String message;
        private boolean status;

        public HealthResponse(String message, boolean status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public boolean isStatus() { return status; }
        public void setStatus(boolean status) { this.status = status; }
    }

    public static class LanguageResponse {
        private Map<String, String> supportedLanguages;
        private boolean status;

        public LanguageResponse(Map<String, String> supportedLanguages, boolean status) {
            this.supportedLanguages = supportedLanguages;
            this.status = status;
        }

        public Map<String, String> getSupportedLanguages() { return supportedLanguages; }
        public void setSupportedLanguages(Map<String, String> supportedLanguages) { this.supportedLanguages = supportedLanguages; }

        public boolean isStatus() { return status; }
        public void setStatus(boolean status) { this.status = status; }
    }
}