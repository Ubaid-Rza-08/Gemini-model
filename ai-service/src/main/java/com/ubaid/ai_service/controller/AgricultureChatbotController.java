package com.ubaid.ai_service.controller;

import com.ubaid.ai_service.service.AgricultureChatbotService;
import com.ubaid.ai_service.service.TextToSpeechService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.Arrays;
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
    private final TextToSpeechService textToSpeechService;

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

    @PostMapping(value = "/chat-with-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mpeg")
    public ResponseEntity<byte[]> chatWithBotAndAudio(
            @RequestParam("message") @NotBlank @Size(max = 1000) String message,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "voiceId", required = false) String voiceId,
            @RequestParam(value = "outputFormat", defaultValue = "mp3_44100_128") String outputFormat) {

        try {
            // Generate session ID if not provided
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            // Validate language parameter
            if (!isValidLanguage(language)) {
                String errorMessage = "{\"error\":\"Invalid language code\"}";
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorMessage.getBytes());
            }

            // Extract image bytes if present
            byte[] imageBytes = null;
            if (image != null && !image.isEmpty()) {
                // Validate image size and type (same as above)
                if (image.getSize() > 5 * 1024 * 1024) {
                    String errorMessage = "{\"error\":\"Image size too large\"}";
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(errorMessage.getBytes());
                }

                String contentType = image.getContentType();
                if (contentType == null || !isValidImageType(contentType)) {
                    String errorMessage = "{\"error\":\"Invalid image format\"}";
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(errorMessage.getBytes());
                }

                imageBytes = image.getBytes();
            }

            // Process the chat request
            AgricultureChatbotService.ChatbotResponse chatResponse = chatbotService.processChat(message, imageBytes, sessionId, language);

            if (!chatResponse.isSuccess()) {
                // Return error as JSON
                String errorMessage = String.format("{\"error\":\"%s\"}",
                        chatResponse.getMessage().replace("\"", "'"));
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorMessage.getBytes());
            }

            // Convert AI response to speech
            String modelId = getModelIdForLanguage(language);
            byte[] audioBytes = textToSpeechService.convertTextToSpeech(
                    chatResponse.getMessage(), voiceId, outputFormat, modelId);

            // Set appropriate headers for audio response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.add("Content-Disposition", "attachment; filename=\"agriculture_response.mp3\"");
            headers.add("X-Session-Id", sessionId);
            headers.add("X-Response-Type", chatResponse.getResponseType());
            headers.add("X-Language", language);

            log.info("Chat with audio processed - SessionId: {}, Language: {}, Audio size: {} bytes",
                    sessionId, language, audioBytes.length);

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error in chat with audio endpoint - SessionId: {}, Language: {}, Error: {}",
                    sessionId, language, e.getMessage());

            String errorMessage = String.format("{\"error\":\"Technical error: %s\"}",
                    e.getMessage().replace("\"", "'"));
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorMessage.getBytes());
        }
    }

    @PostMapping(value = "/text-to-speech")
    public ResponseEntity<byte[]> convertResponseToAudio(
            @RequestParam("text") @NotBlank @Size(max = 5000) String text,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "voiceId", required = false) String voiceId,
            @RequestParam(value = "outputFormat", defaultValue = "mp3_44100_128") String outputFormat) {

        try {
            // Validate language
            if (!isValidLanguage(language)) {
                String errorMessage = "{\"error\":\"Invalid language code\"}";
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorMessage.getBytes());
            }

            String modelId = getModelIdForLanguage(language);
            byte[] audioBytes = textToSpeechService.convertTextToSpeech(text, voiceId, outputFormat, modelId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.add("Content-Disposition", "attachment; filename=\"agriculture_tts.mp3\"");
            headers.add("X-Language", language);

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error in text-to-speech conversion: {}", e.getMessage());
            String errorMessage = String.format("{\"error\":\"TTS conversion failed: %s\"}",
                    e.getMessage().replace("\"", "'"));
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorMessage.getBytes());
        }
    }

    private boolean isValidLanguage(String language) {
        return Arrays.asList("en", "hi", "bn", "te", "ta", "mr", "gu", "kn", "ml", "pa", "or")
                .contains(language.toLowerCase());
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp");
    }

    private String getModelIdForLanguage(String language) {
        // Use multilingual model for all languages
        // You can customize this based on specific language requirements
        switch (language.toLowerCase()) {
            case "hi":
            case "bn":
            case "te":
            case "ta":
            case "mr":
            case "gu":
            case "kn":
            case "ml":
            case "pa":
            case "or":
                return "eleven_multilingual_v2";
            case "en":
            default:
                return "eleven_multilingual_v2";
        }
    }
}