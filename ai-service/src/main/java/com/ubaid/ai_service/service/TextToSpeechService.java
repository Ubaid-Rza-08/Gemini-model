package com.ubaid.ai_service.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class TextToSpeechService {

    private final WebClient webClient;

    @Value("${elevenlabs.api.key}")
    private String elevenLabsApiKey;

    @Value("${elevenlabs.api.url}")
    private String elevenLabsApiUrl;

    @Value("${elevenlabs.voice.id}")
    private String defaultVoiceId;

    public TextToSpeechService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    public byte[] convertTextToSpeech(String text) {
        return convertTextToSpeech(text, defaultVoiceId, "mp3_44100_128", "eleven_multilingual_v2");
    }

    public byte[] convertTextToSpeech(String text, String voiceId, String outputFormat, String modelId) {
        try {
            // Clean text for better TTS output
            String cleanedText = cleanTextForTTS(text);

            if (cleanedText.trim().isEmpty()) {
                throw new RuntimeException("Text cannot be empty after cleaning");
            }

            Map<String, Object> requestBody = Map.of(
                    "text", cleanedText,
                    "model_id", modelId != null ? modelId : "eleven_multilingual_v2",
                    "voice_settings", Map.of(
                            "stability", 0.5,
                            "similarity_boost", 0.75
                    )
            );

            String voiceIdToUse = voiceId != null ? voiceId : defaultVoiceId;
            String outputFormatToUse = outputFormat != null ? outputFormat : "mp3_44100_128";

            String fullUrl = String.format("%s/text-to-speech/%s?output_format=%s",
                    elevenLabsApiUrl, voiceIdToUse, outputFormatToUse);

            log.info("Converting text to speech - Length: {} characters, VoiceId: {}, Format: {}",
                    cleanedText.length(), voiceIdToUse, outputFormatToUse);

            byte[] audioBytes = webClient.post()
                    .uri(fullUrl)
                    .header("xi-api-key", elevenLabsApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (audioBytes == null || audioBytes.length == 0) {
                throw new RuntimeException("Received empty audio response from ElevenLabs");
            }

            log.info("Successfully converted text to speech - Audio size: {} bytes", audioBytes.length);
            return audioBytes;

        } catch (WebClientResponseException e) {
            log.error("HTTP Error calling ElevenLabs API - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("ElevenLabs API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling ElevenLabs API: {}", e.getMessage());
            throw new RuntimeException("Failed to convert text to speech: " + e.getMessage(), e);
        }
    }

    public Mono<byte[]> convertTextToSpeechAsync(String text, String voiceId, String outputFormat, String modelId) {
        try {
            String cleanedText = cleanTextForTTS(text);

            if (cleanedText.trim().isEmpty()) {
                return Mono.error(new RuntimeException("Text cannot be empty after cleaning"));
            }

            Map<String, Object> requestBody = Map.of(
                    "text", cleanedText,
                    "model_id", modelId != null ? modelId : "eleven_multilingual_v2",
                    "voice_settings", Map.of(
                            "stability", 0.5,
                            "similarity_boost", 0.75
                    )
            );

            String voiceIdToUse = voiceId != null ? voiceId : defaultVoiceId;
            String outputFormatToUse = outputFormat != null ? outputFormat : "mp3_44100_128";

            String fullUrl = String.format("%s/text-to-speech/%s?output_format=%s",
                    elevenLabsApiUrl, voiceIdToUse, outputFormatToUse);

            log.info("Converting text to speech asynchronously - Length: {} characters", cleanedText.length());

            return webClient.post()
                    .uri(fullUrl)
                    .header("xi-api-key", elevenLabsApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(60))
                    .doOnSuccess(audioBytes ->
                            log.info("Successfully converted text to speech asynchronously - Audio size: {} bytes",
                                    audioBytes != null ? audioBytes.length : 0))
                    .doOnError(error -> log.error("Error in async TTS conversion: {}", error.getMessage()));

        } catch (Exception e) {
            log.error("Error setting up async TTS conversion: {}", e.getMessage());
            return Mono.error(new RuntimeException("Failed to setup text to speech conversion: " + e.getMessage()));
        }
    }

    private String cleanTextForTTS(String text) {
        if (text == null) {
            return "";
        }

        // Remove or replace problematic characters for TTS
        return text
                .replaceAll("\\*", "") // Remove asterisks
                .replaceAll("\\#", "") // Remove hash symbols
                .replaceAll("_{2,}", " ") // Replace multiple underscores with space
                .replaceAll("\\s+", " ") // Replace multiple spaces with single space
                .replaceAll("[\\r\\n]+", ". ") // Replace line breaks with periods
                .replaceAll("\\.\\.+", ".") // Replace multiple dots with single dot
                .trim();
    }

    // Method to get available voice models
    public String getAvailableVoices() {
        try {
            String fullUrl = String.format("%s/voices", elevenLabsApiUrl);

            return webClient.get()
                    .uri(fullUrl)
                    .header("xi-api-key", elevenLabsApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

        } catch (Exception e) {
            log.error("Error fetching available voices: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch available voices: " + e.getMessage());
        }
    }
}
