package com.ubaid.ai_service.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AgricultureChatbotService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    // Comprehensive agriculture keywords
    private final List<String> agricultureKeywords = Arrays.asList(
            // Crops and plants
            "crop", "crops", "farming", "agriculture", "agricultural", "farm", "farmer", "cultivation",
            "wheat", "rice", "corn", "maize", "cotton", "sugarcane", "potato", "tomato", "onion",
            "vegetable", "vegetables", "fruit", "fruits", "grain", "cereal", "pulse", "legume",
            "barley", "bajra", "jowar", "mustard", "groundnut", "soybean", "chickpea", "lentil",

            // Soil and fertilizers
            "soil", "fertilizer", "fertiliser", "manure", "compost", "organic", "nitrogen",
            "phosphorus", "potassium", "npk", "urea", "dap", "nutrient", "nutrients",

            // Pest and disease management
            "pest", "pests", "disease", "diseases", "pesticide", "insecticide", "fungicide",
            "herbicide", "weed", "weeds", "insect", "insects", "bug", "bugs",

            // Farming practices
            "seed", "seeds", "sowing", "planting", "harvest", "harvesting", "irrigation",
            "watering", "plowing", "tilling", "weeding", "pruning", "transplanting",

            // Seasons and weather
            "season", "seasonal", "weather", "rain", "rainfall", "drought", "monsoon",
            "kharif", "rabi", "summer", "winter", "spring",

            // Farm equipment and structures
            "tractor", "plow", "cultivator", "harrow", "sprayer", "thresher", "combine",
            "greenhouse", "nursery", "field", "fields", "plantation", "garden",

            // General farming terms
            "yield", "production", "growth", "plant", "plants", "plantation", "cropping",
            "agronomy", "horticulture", "livestock", "cattle", "dairy", "poultry"
    );

    public AgricultureChatbotService(GeminiService geminiService) {
        this.geminiService = geminiService;
        this.objectMapper = new ObjectMapper();
    }

    public ChatbotResponse processChat(String message, byte[] image, String sessionId) {
        try {
            // Double-check agriculture relevance
            if (!isAgricultureRelated(message)) {
                return new ChatbotResponse(
                        "I specialize in agricultural guidance only. Please ask about crops, farming, soil, fertilizers, pest control, or other agricultural topics.",
                        false,
                        "NON_AGRICULTURE_TOPIC",
                        sessionId
                );
            }

            String prompt = createAgriculturePrompt(message);
            String geminiResponse;

            // Use appropriate Gemini method
            if (image != null && image.length > 0) {
                geminiResponse = geminiService.getAnswerWithImage(prompt, image);
                log.info("Processed chat with image - SessionId: {}, MessageLength: {}", sessionId, message.length());
            } else {
                geminiResponse = geminiService.getAnswer(prompt);
                log.info("Processed text-only chat - SessionId: {}, MessageLength: {}", sessionId, message.length());
            }

            String responseText = extractResponseText(geminiResponse);

            return new ChatbotResponse(
                    responseText,
                    true,
                    image != null ? "TEXT_WITH_IMAGE" : "TEXT_ONLY",
                    sessionId
            );

        } catch (Exception e) {
            log.error("Error processing chat - SessionId: {}, Error: {}", sessionId, e.getMessage());
            return new ChatbotResponse(
                    "I'm experiencing technical difficulties processing your agricultural query. Please try again or rephrase your question.",
                    false,
                    "TECHNICAL_ERROR",
                    sessionId
            );
        }
    }

    private boolean isAgricultureRelated(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase().trim();

        boolean hasAgricultureKeyword = agricultureKeywords.stream()
                .anyMatch(keyword -> lowerMessage.contains(keyword.toLowerCase()));

        if (!hasAgricultureKeyword) {
            // Check for common agricultural question patterns
            String[] agriPatterns = {
                    "how to grow", "when to plant", "fertilizer for", "pest control",
                    "crop disease", "soil preparation", "irrigation", "harvest time",
                    "farming", "cultivation", "agriculture"
            };

            hasAgricultureKeyword = Arrays.stream(agriPatterns)
                    .anyMatch(pattern -> lowerMessage.contains(pattern));
        }

        return hasAgricultureKeyword;
    }

    private String createAgriculturePrompt(String userMessage) {
        return String.format("""
            You are an expert agricultural consultant specializing in Indian farming conditions and practices.
            
            STRICT GUIDELINES:
            1. ONLY provide responses related to agriculture, farming, crops, soil, fertilizers, pest control, or plant-related topics
            2. Keep responses between 80-100 words maximum - be concise and practical
            3. Focus on actionable advice for Indian farming context
            4. Use simple language that farmers can easily understand
            5. Do not reveal your AI model details or technical information
            6. Provide specific, practical solutions rather than generic advice
            7. Include relevant local farming practices when possible
            8. Focus on immediate actionable advice
            
            User Question: %s
            
            Provide a helpful, concise, and practical agriculture-focused response suitable for Indian farmers.
            """, userMessage);
    }

    private String extractResponseText(String geminiResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(geminiResponse);

            JsonNode candidatesNode = rootNode.path("candidates");
            if (candidatesNode.isEmpty() || !candidatesNode.isArray()) {
                return "I couldn't process your agricultural question properly. Please try rephrasing it with more specific farming details.";
            }

            JsonNode textNode = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String responseText = textNode.asText().trim();

            if (responseText.isEmpty()) {
                return "Please provide more specific details about your agricultural query for better assistance.";
            }

            return limitResponseLength(responseText);

        } catch (Exception e) {
            log.error("Error extracting response text from Gemini: {}", e.getMessage());
            return "I encountered an issue processing your agricultural question. Please try asking again with more specific farming details.";
        }
    }

    private String limitResponseLength(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "Please provide more specific agricultural details for better guidance.";
        }

        String[] words = response.split("\\s+");

        if (words.length > 95) {
            StringBuilder truncated = new StringBuilder();
            for (int i = 0; i < 95; i++) {
                truncated.append(words[i]).append(" ");
            }
            String result = truncated.toString().trim();
            if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) {
                result += "...";
            }
            return result;
        }

        return response;
    }

    // Response DTO
    public static class ChatbotResponse {
        private String message;
        private boolean success;
        private String responseType;
        private String sessionId;

        public ChatbotResponse() {}

        public ChatbotResponse(String message, boolean success, String responseType, String sessionId) {
            this.message = message;
            this.success = success;
            this.responseType = responseType;
            this.sessionId = sessionId;
        }

        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getResponseType() { return responseType; }
        public void setResponseType(String responseType) { this.responseType = responseType; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}