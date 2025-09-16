package com.ubaid.ai_service.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubaid.ai_service.model.SoilData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SoilHealthCardService {

    private final GeminiService geminiService;
    private final SoilAIService soilAIService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SoilData extractSoilDataFromHealthCard(byte[] healthCardImage, String language) {
        try {
            String prompt = createSoilHealthCardExtractionPrompt(language);
            String aiResponse = geminiService.getAnswerWithImage(prompt, healthCardImage);

            log.info("Received AI response for soil health card extraction");
            return parseSoilDataFromAiResponse(aiResponse, healthCardImage, language);

        } catch (Exception e) {
            log.error("Error extracting data from soil health card: {}", e.getMessage());
            // Return default soil data if extraction fails
            return createDefaultSoilData(language, healthCardImage);
        }
    }

    private String createSoilHealthCardExtractionPrompt(String language) {
        String languageInstruction = getLanguageInstruction(language);

        return String.format("""
            You are an expert in analyzing Indian Soil Health Cards. Extract all relevant information from this soil health card image.
            
            %s
            
            Analyze the soil health card image and extract the following information in EXACT JSON format:
            
            {
              "soilType": "Detected soil type from the card (clay/sandy/loamy/silt/red_soil/black_soil/alluvial/laterite/mountain_soil/desert_soil)",
              "cropType": "Recommended or mentioned crop type from the card",
              "areaValue": 1.0,
              "areaUnit": "ACRE",
              "season": "KHARIF",
              "location": "Location/district mentioned in the card",
              "soilParameters": {
                "ph": "pH value if mentioned",
                "nitrogen": "Nitrogen level (Low/Medium/High)",
                "phosphorus": "Phosphorus level (Low/Medium/High)", 
                "potassium": "Potassium level (Low/Medium/High)",
                "organicCarbon": "Organic carbon level if mentioned",
                "sulfur": "Sulfur level if mentioned",
                "zinc": "Zinc level if mentioned",
                "iron": "Iron level if mentioned",
                "manganese": "Manganese level if mentioned",
                "copper": "Copper level if mentioned",
                "boron": "Boron level if mentioned"
              },
              "recommendations": {
                "limeRecommendation": "Lime recommendation from card",
                "organicMatterRecommendation": "Organic matter recommendation",
                "fertilizerRecommendation": "Fertilizer recommendations mentioned in card"
              }
            }
            
            Instructions:
            1. Extract soil type carefully - look for soil classification in the card
            2. If crop type is not mentioned, suggest most suitable crop based on soil type and location
            3. Default area to 1.0 ACRE if not specified
            4. Determine season based on location and current recommendations
            5. Extract all nutrient levels (N, P, K and micronutrients)
            6. Include any specific recommendations mentioned in the card
            7. If any field cannot be determined from the image, use appropriate default values
            8. Focus on extracting quantitative data wherever possible
            
            Provide ONLY the JSON response above, no additional text.
            """, languageInstruction);
    }

    private SoilData parseSoilDataFromAiResponse(String aiResponse, byte[] healthCardImage, String language) {
        try {
            JsonNode rootNode = objectMapper.readTree(aiResponse);

            // Extract text from Gemini response structure
            JsonNode candidatesNode = rootNode.path("candidates");
            if (candidatesNode.isEmpty() || !candidatesNode.isArray()) {
                log.warn("No candidates found in AI response for soil health card");
                return createDefaultSoilData(language, healthCardImage);
            }

            JsonNode textNode = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.asText().trim().isEmpty()) {
                log.warn("No text found in AI response for soil health card");
                return createDefaultSoilData(language, healthCardImage);
            }

            String jsonContent = cleanJsonResponse(textNode.asText());
            log.info("Cleaned JSON content from soil health card: {}", jsonContent);

            // Parse the extracted JSON
            JsonNode extractedData = objectMapper.readTree(jsonContent);

            // Create SoilData object from extracted information
            SoilData soilData = new SoilData();

            // Set basic information
            soilData.setSoilType(extractedData.path("soilType").asText(null));
            soilData.setCropType(extractedData.path("cropType").asText("wheat")); // default crop
            soilData.setAreaValue(extractedData.path("areaValue").asDouble(1.0));

            // Parse area unit
            String areaUnitStr = extractedData.path("areaUnit").asText("ACRE");
            try {
                soilData.setAreaUnit(SoilData.AreaUnit.valueOf(areaUnitStr.toUpperCase()));
            } catch (Exception e) {
                soilData.setAreaUnit(SoilData.AreaUnit.ACRE);
            }

            // Parse season
            String seasonStr = extractedData.path("season").asText("KHARIF");
            try {
                soilData.setSeason(SoilData.Season.valueOf(seasonStr.toUpperCase()));
            } catch (Exception e) {
                soilData.setSeason(SoilData.Season.KHARIF);
            }

            soilData.setLocation(extractedData.path("location").asText(null));
            soilData.setLanguage(language);
            soilData.setSoilImage(healthCardImage); // Store the health card image

            log.info("Successfully extracted soil data from health card: CropType={}, SoilType={}, Location={}",
                    soilData.getCropType(), soilData.getSoilType(), soilData.getLocation());

            return soilData;

        } catch (Exception e) {
            log.error("Error parsing soil data from AI response: {}", e.getMessage());
            return createDefaultSoilData(language, healthCardImage);
        }
    }

    private String cleanJsonResponse(String rawResponse) {
        // Remove markdown code blocks and clean up the response
        String cleaned = rawResponse
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .replaceAll("\\\\n", "\n")
                .trim();

        // Find JSON content between curly braces
        int startIndex = cleaned.indexOf("{");
        int endIndex = cleaned.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }

        return cleaned;
    }

    private SoilData createDefaultSoilData(String language, byte[] healthCardImage) {
        SoilData soilData = new SoilData();
        soilData.setSoilType("loamy"); // Default soil type
        soilData.setCropType("wheat"); // Default crop
        soilData.setAreaValue(1.0);
        soilData.setAreaUnit(SoilData.AreaUnit.ACRE);
        soilData.setSeason(SoilData.Season.KHARIF);
        soilData.setLocation("India");
        soilData.setLanguage(language);
        soilData.setSoilImage(healthCardImage);

        log.info("Created default soil data for failed health card extraction");
        return soilData;
    }

    private String getLanguageInstruction(String language) {
        switch (language.toLowerCase()) {
            case "hi":
                return "Extract information and understand text in Hindi (Devanagari script) from the soil health card.";
            case "bn":
                return "Extract information and understand text in Bengali (Bangla script) from the soil health card.";
            case "te":
                return "Extract information and understand text in Telugu script from the soil health card.";
            case "ta":
                return "Extract information and understand text in Tamil script from the soil health card.";
            case "mr":
                return "Extract information and understand text in Marathi (Devanagari script) from the soil health card.";
            case "gu":
                return "Extract information and understand text in Gujarati script from the soil health card.";
            case "kn":
                return "Extract information and understand text in Kannada script from the soil health card.";
            case "ml":
                return "Extract information and understand text in Malayalam script from the soil health card.";
            case "pa":
                return "Extract information and understand text in Punjabi (Gurmukhi script) from the soil health card.";
            case "or":
                return "Extract information and understand text in Odia script from the soil health card.";
            case "en":
                return "Extract information from the soil health card (text may be in English or mixed with regional language).";
            default:
                return "Extract information from the soil health card (text may be in English or mixed with Hindi/regional languages).";
        }
    }
}
