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

    // Updated method with language support
    public ChatbotResponse processChat(String message, byte[] image, String sessionId, String language) {
        try {
            // Double-check agriculture relevance
            if (!isAgricultureRelated(message)) {
                String errorMessage = getLocalizedErrorMessage(language);
                return new ChatbotResponse(
                        errorMessage,
                        false,
                        "NON_AGRICULTURE_TOPIC",
                        sessionId
                );
            }

            String prompt = createAgriculturePrompt(message, language);
            String geminiResponse;

            // Use appropriate Gemini method
            if (image != null && image.length > 0) {
                geminiResponse = geminiService.getAnswerWithImage(prompt, image);
                log.info("Processed chat with image - SessionId: {}, Language: {}, MessageLength: {}",
                        sessionId, language, message.length());
            } else {
                geminiResponse = geminiService.getAnswer(prompt);
                log.info("Processed text-only chat - SessionId: {}, Language: {}, MessageLength: {}",
                        sessionId, language, message.length());
            }

            String responseText = extractResponseText(geminiResponse);

            return new ChatbotResponse(
                    responseText,
                    true,
                    image != null ? "TEXT_WITH_IMAGE" : "TEXT_ONLY",
                    sessionId
            );

        } catch (Exception e) {
            log.error("Error processing chat - SessionId: {}, Language: {}, Error: {}", sessionId, language, e.getMessage());
            String errorMessage = getLocalizedTechnicalError(language);
            return new ChatbotResponse(
                    errorMessage,
                    false,
                    "TECHNICAL_ERROR",
                    sessionId
            );
        }
    }

    // Legacy method for backward compatibility
    public ChatbotResponse processChat(String message, byte[] image, String sessionId) {
        return processChat(message, image, sessionId, "en");
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

    private String createAgriculturePrompt(String userMessage, String language) {
        String languageInstruction = getLanguageInstruction(language);

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
            
            LANGUAGE REQUIREMENT:
            %s
            
            User Question: %s
            
            Provide a helpful, concise, and practical agriculture-focused response suitable for Indian farmers.
            """, languageInstruction, userMessage);
    }

    private String getLanguageInstruction(String language) {
        switch (language.toLowerCase()) {
            case "hi":
                return "Respond in Hindi (Devanagari script). Use simple Hindi words that farmers understand.";
            case "bn":
                return "Respond in Bengali (Bangla script). Use simple Bengali words that farmers understand.";
            case "te":
                return "Respond in Telugu script. Use simple Telugu words that farmers understand.";
            case "ta":
                return "Respond in Tamil script. Use simple Tamil words that farmers understand.";
            case "mr":
                return "Respond in Marathi (Devanagari script). Use simple Marathi words that farmers understand.";
            case "gu":
                return "Respond in Gujarati script. Use simple Gujarati words that farmers understand.";
            case "kn":
                return "Respond in Kannada script. Use simple Kannada words that farmers understand.";
            case "ml":
                return "Respond in Malayalam script. Use simple Malayalam words that farmers understand.";
            case "pa":
                return "Respond in Punjabi (Gurmukhi script). Use simple Punjabi words that farmers understand.";
            case "or":
                return "Respond in Odia script. Use simple Odia words that farmers understand.";
            case "en":
                return "Respond in English. Use simple English words that farmers understand.";
            default:
                return "Respond in English. Use simple English mixed with Hindi terms that Indian farmers understand.";
        }
    }

    private String getLocalizedErrorMessage(String language) {
        switch (language.toLowerCase()) {
            case "hi":
                return "मैं केवल कृषि संबंधी सलाह देता हूं। कृपया फसल, खेती, मिट्टी, खाद, कीड़े-मकोड़े, या अन्य कृषि विषयों के बारे में पूछें।";
            case "bn":
                return "আমি শুধুমাত্র কৃষি বিষয়ে পরামর্শ দিই। অনুগ্রহ করে ফসল, চাষাবাদ, মাটি, সার, পোকামাকড় নিয়ন্ত্রণ বা অন্যান্য কৃষি বিষয়ে জিজ্ঞাসা করুন।";
            case "te":
                return "నేను వ్యవసాయ మార్గదర్శనం మాత్రమే అందిస్తాను. దయచేసి పంటలు, వ్యవసాయం, మట్టి, ఎరువులు, చీడపురుగుల నియంత్రణ లేదా ఇతర వ్యవసాయ అంశాల గురించి అడగండి।";
            case "ta":
                return "நான் விவசாய வழிகாட்டுதல் மட்டுமே வழங்குகிறேன். பயிர்கள், விவசாயம், மண், உரங்கள், பூச்சி கட்டுப்பாடு அல்லது பிற விவசாய தலைப்புகளைப் பற்றி கேளுங்கள்.";
            case "mr":
                return "मी केवळ शेतीविषयक मार्गदर्शन देतो. कृपया पिके, शेती, माती, खत, कीड नियंत्रण किंवा इतर कृषी विषयांबद्दल विचारा.";
            case "gu":
                return "હું ફક્ત કૃષિ માર્ગદર્શન આપું છું. કૃપા કરીને પાકો, ખેતી, માટી, ખાતર, જંતુ નિયંત્રણ અથવા અન્ય કૃષિ વિષયો વિશે પૂછો.";
            case "kn":
                return "ನಾನು ಕೃಷಿ ಮಾರ್ಗದರ್ಶನವನ್ನು ಮಾತ್ರ ಒದಗಿಸುತ್ತೇನೆ. ದಯವಿಟ್ಟು ಬೆಳೆಗಳು, ಕೃಷಿ, ಮಣ್ಣು, ಗೊಬ್ಬರ, ಕೀಟ ನಿಯಂತ್ರಣ ಅಥವಾ ಇತರ ಕೃಷಿ ವಿಷಯಗಳ ಬಗ್ಗೆ ಕೇಳಿ.";
            case "ml":
                return "ഞാൻ കാർഷിക മാർഗ്ഗനിർദ്ദേശം മാത്രമേ നൽകുന്നുള്ളൂ. വിളകൾ, കൃഷി, മണ്ണ്, വളം, കീടനിയന്ത്രണം അല്ലെങ്കിൽ മറ്റ് കാർഷിക വിഷയങ്ങളെക്കുറിച്ച് ചോദിക്കുക.";
            case "pa":
                return "ਮੈਂ ਸਿਰਫ਼ ਖੇਤੀਬਾੜੀ ਦੀ ਮਾਰਗਦਰਸ਼ਨ ਦਿੰਦਾ ਹਾਂ। ਕਿਰਪਾ ਕਰਕੇ ਫਸਲਾਂ, ਖੇਤੀ, ਮਿੱਟੀ, ਖਾਦ, ਕੀੜੇ-ਮਕੌੜਿਆਂ ਦੀ ਰੋਕਥਾਮ ਜਾਂ ਹੋਰ ਖੇਤੀਬਾੜੀ ਦੇ ਵਿਸ਼ਿਆਂ ਬਾਰੇ ਪੁੱਛੋ।";
            case "or":
                return "ମୁଁ କେବଳ କୃଷି ମାର୍ଗଦର୍ଶନ ପ୍ରଦାନ କରେ। ଦୟାକରି ଫସଲ, ଚାଷ, ମାଟି, ସାର, କୀଟ ନିୟନ୍ତ୍ରଣ କିମ୍ବା ଅନ୍ୟ କୃଷି ବିଷୟ ବିଷୟରେ ପଚାରନ୍ତୁ।";
            case "en":
            default:
                return "I specialize in agricultural guidance only. Please ask about crops, farming, soil, fertilizers, pest control, or other agricultural topics.";
        }
    }

    private String getLocalizedTechnicalError(String language) {
        switch (language.toLowerCase()) {
            case "hi":
                return "मुझे तकनीकी समस्या हो रही है। कृपया अपना कृषि प्रश्न दोबारा पूछें या अलग तरीके से पूछें।";
            case "bn":
                return "আমার প্রযুক্তিগত সমস্যা হচ্ছে। দয়া করে আপনার কৃষি প্রশ্নটি আবার জিজ্ঞাসা করুন বা ভিন্নভাবে জিজ্ঞাসা করুন।";
            case "te":
                return "నాకు సాంకేతిక సమস్యలు ఎదురవుతున్నాయి. దయచేసి మీ వ్యవసాయ ప్రశ్న మళ్లీ అడగండి లేదా వేరే విధంగా అడగండి।";
            case "ta":
                return "எனக்கு தொழில்நுட்ப சிக்கல்கள் உள்ளன. தயவுசெய்து உங்கள் விவசாய கேள்வியை மீண்டும் கேளுங்கள் அல்லது வேறுவிதமாக கேளுங்கள்.";
            case "mr":
                return "मला तांत्रिक अडचणी येत आहेत. कृपया तुमचा शेतीविषयक प्रश्न पुन्हा विचारा किंवा वेगळ्या पद्धतीने विचारा.";
            case "gu":
                return "મને તકનીકી મુશ્કેલીઓ આવી રહી છે. કૃપા કરીને તમારો કૃષિ પ્રશ્ન ફરીથી પૂછો અથવા અલગ રીતે પૂછો.";
            case "kn":
                return "ನನಗೆ ತಾಂತ್ರಿಕ ತೊಂದರೆಗಳು ಆಗುತ್ತಿವೆ. ದಯವಿಟ್ಟು ನಿಮ್ಮ ಕೃಷಿ ಪ್ರಶ್ನೆಯನ್ನು ಮತ್ತೆ ಕೇಳಿ ಅಥವಾ ಬೇರೆ ರೀತಿಯಲ್ಲಿ ಕೇಳಿ.";
            case "ml":
                return "എനിക്ക് സാങ്കേതിക പ്രശ്നങ്ങൾ നേരിടുന്നു. ദയവായി നിങ്ങളുടെ കാർഷിക ചോദ്യം വീണ്ടും ചോദിക്കുക അല്ലെങ്കിൽ വ്യത്യസ്തമായി ചോദിക്കുക.";
            case "pa":
                return "ਮੈਨੂੰ ਤਕਨੀਕੀ ਮੁਸ਼ਕਲਾਂ ਆ ਰਹੀਆਂ ਹਨ। ਕਿਰਪਾ ਕਰਕੇ ਆਪਣਾ ਖੇਤੀਬਾੜੀ ਦਾ ਸਵਾਲ ਦੁਬਾਰਾ ਪੁੱਛੋ ਜਾਂ ਵੱਖਰੇ ਤਰੀਕੇ ਨਾਲ ਪੁੱਛੋ।";
            case "or":
                return "ମୋର ବୈଷୟିକ ସମସ୍ୟା ହେଉଛି। ଦୟାକରି ଆପଣଙ୍କର କୃଷି ପ୍ରଶ୍ନ ପୁଣି ପଚାରନ୍ତୁ କିମ୍ବା ଭିନ୍ନ ଉପାୟରେ ପଚାରନ୍ତୁ।";
            case "en":
            default:
                return "I'm experiencing technical difficulties processing your agricultural query. Please try again or rephrase your question.";
        }
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
        private String language;

        public ChatbotResponse() {}

        public ChatbotResponse(String message, boolean success, String responseType, String sessionId) {
            this.message = message;
            this.success = success;
            this.responseType = responseType;
            this.sessionId = sessionId;
            this.language = "en"; // default
        }

        public ChatbotResponse(String message, boolean success, String responseType, String sessionId, String language) {
            this.message = message;
            this.success = success;
            this.responseType = responseType;
            this.sessionId = sessionId;
            this.language = language;
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

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
}