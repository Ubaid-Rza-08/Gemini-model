package com.ubaid.ai_service.controller;

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
import reactor.core.publisher.Mono;


import java.util.Map;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TextToSpeechController {

    private final TextToSpeechService textToSpeechService;

    @PostMapping(value = "/convert", produces = "audio/mpeg")
    public ResponseEntity<byte[]> convertTextToSpeech(
            @RequestParam("text") @NotBlank @Size(max = 5000) String text,
            @RequestParam(value = "voiceId", required = false) String voiceId,
            @RequestParam(value = "outputFormat", defaultValue = "mp3_44100_128") String outputFormat,
            @RequestParam(value = "modelId", defaultValue = "eleven_multilingual_v2") String modelId) {

        try {
            log.info("TTS conversion request - Text length: {}, VoiceId: {}, Format: {}",
                    text.length(), voiceId, outputFormat);

            // Validate text length for TTS
            if (text.trim().length() < 1) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Text cannot be empty\"}".getBytes());
            }

            // Convert text to speech
            byte[] audioBytes = textToSpeechService.convertTextToSpeech(text, voiceId, outputFormat, modelId);

            // Set appropriate headers for audio response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.add("Content-Disposition", "attachment; filename=\"speech.mp3\"");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

            log.info("TTS conversion successful - Audio size: {} bytes", audioBytes.length);

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error in TTS conversion: {}", e.getMessage());

            String errorMessage = String.format("{\"error\":\"TTS conversion failed: %s\"}",
                    e.getMessage().replace("\"", "'"));

            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorMessage.getBytes());
        }
    }

    @PostMapping(value = "/convert-async", produces = "audio/mpeg")
    public Mono<ResponseEntity<byte[]>> convertTextToSpeechAsync(
            @RequestParam("text") @NotBlank @Size(max = 5000) String text,
            @RequestParam(value = "voiceId", required = false) String voiceId,
            @RequestParam(value = "outputFormat", defaultValue = "mp3_44100_128") String outputFormat,
            @RequestParam(value = "modelId", defaultValue = "eleven_multilingual_v2") String modelId) {

        log.info("Async TTS conversion request - Text length: {}", text.length());

        if (text.trim().length() < 1) {
            return Mono.just(ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Text cannot be empty\"}".getBytes()));
        }

        return textToSpeechService.convertTextToSpeechAsync(text, voiceId, outputFormat, modelId)
                .map(audioBytes -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
                    headers.setContentLength(audioBytes.length);
                    headers.add("Content-Disposition", "attachment; filename=\"speech.mp3\"");
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

                    log.info("Async TTS conversion successful - Audio size: {} bytes", audioBytes.length);
                    return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
                })
                .onErrorResume(error -> {
                    log.error("Error in async TTS conversion: {}", error.getMessage());
                    String errorMessage = String.format("{\"error\":\"Async TTS conversion failed: %s\"}",
                            error.getMessage().replace("\"", "'"));

                    return Mono.just(ResponseEntity.internalServerError()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(errorMessage.getBytes()));
                });
    }

    @GetMapping("/voices")
    public ResponseEntity<String> getAvailableVoices() {
        try {
            String voices = textToSpeechService.getAvailableVoices();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(voices);
        } catch (Exception e) {
            log.error("Error fetching voices: {}", e.getMessage());
            String errorMessage = String.format("{\"error\":\"Failed to fetch voices: %s\"}",
                    e.getMessage().replace("\"", "'"));
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorMessage);
        }
    }

    @PostMapping("/convert-json")
    public ResponseEntity<?> convertTextToSpeechJson(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String voiceId = request.get("voiceId");
            String outputFormat = request.getOrDefault("outputFormat", "mp3_44100_128");
            String modelId = request.getOrDefault("modelId", "eleven_multilingual_v2");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Text parameter is required"));
            }

            if (text.length() > 5000) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Text too long. Maximum 5000 characters allowed."));
            }

            byte[] audioBytes = textToSpeechService.convertTextToSpeech(text, voiceId, outputFormat, modelId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.add("Content-Disposition", "attachment; filename=\"speech.mp3\"");

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error in JSON TTS conversion: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "TTS conversion failed: " + e.getMessage()));
        }
    }
}
