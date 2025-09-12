package com.ubaid.ai_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubaid.ai_service.model.FertilizerDetail;
import com.ubaid.ai_service.model.FertilizerRecommendation;
import com.ubaid.ai_service.model.SoilData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class SoilAIService {

    private final GeminiService geminiService;

    public SoilAIService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public FertilizerRecommendation generateFertilizerRecommendation(SoilData soilData) {
        try {
            String prompt = createPromptForSoilAnalysis(soilData);
            String aiResponse;

            // Choose the appropriate method based on available data
            if (soilData.getSoilImage() != null) {
                aiResponse = geminiService.getAnswerWithImage(prompt, soilData.getSoilImage());
            } else {
                aiResponse = geminiService.getAnswer(prompt);
            }

            System.out.println("RESPONSE FROM AI: " + aiResponse);
            return processAiResponse(soilData, aiResponse);
        } catch (Exception e) {
            System.err.println("Error generating fertilizer recommendation for crop: " + soilData.getCropType() +
                    ", Error: " + e.getMessage());
            return createDefaultRecommendation(soilData);
        }
    }

    private FertilizerRecommendation processAiResponse(SoilData soilData, String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            // Extract text from Gemini response structure
            JsonNode candidatesNode = rootNode.path("candidates");
            if (candidatesNode.isEmpty() || !candidatesNode.isArray()) {
                System.out.println("No candidates found in AI response");
                return createDefaultRecommendation(soilData);
            }

            JsonNode textNode = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.asText().trim().isEmpty()) {
                System.out.println("No text found in AI response");
                return createDefaultRecommendation(soilData);
            }

            String jsonContent = cleanJsonResponse(textNode.asText());
            System.out.println("CLEANED JSON CONTENT: " + jsonContent);

            // Parse the cleaned JSON content
            JsonNode analysisJson = mapper.readTree(jsonContent);

            String detectedSoilType = analysisJson.path("detectedSoilType").asText("Unknown");
            String generalRecommendation = analysisJson.path("generalRecommendation").asText();
            List<FertilizerDetail> fertilizers = extractFertilizerDetails(analysisJson.path("fertilizers"), soilData.getCropType());
            List<String> applicationTips = extractStringList(analysisJson.path("applicationTips"));
            List<String> seasonalAdvice = extractStringList(analysisJson.path("seasonalAdvice"));
            List<String> pesticideRecommendation = extractStringList(analysisJson.path("pesticideRecommendation"));

            // Ensure only 2 fertilizers maximum
            if (fertilizers.size() > 2) {
                fertilizers = fertilizers.subList(0, 2);
            }

            // Ensure pesticide recommendations are limited to 3-4 lines
            if (pesticideRecommendation.size() > 4) {
                pesticideRecommendation = pesticideRecommendation.subList(0, 4);
            }

            return FertilizerRecommendation.builder()
                    .detectedSoilType(detectedSoilType.isEmpty() ? "Unknown" : detectedSoilType)
                    .cropType(soilData.getCropType())
                    .areaValue(soilData.getAreaValue())
                    .areaUnit(soilData.getAreaUnit().toString())
                    .season(soilData.getSeason().toString())
                    .language(soilData.getLanguage())
                    .generalRecommendation(generalRecommendation.isEmpty() ?
                            "AI recommendation for " + soilData.getCropType() + " cultivation." :
                            generalRecommendation)
                    .fertilizers(fertilizers)
                    .applicationTips(applicationTips)
                    .seasonalAdvice(seasonalAdvice)
                    .pesticideRecommendation(pesticideRecommendation.isEmpty() ?
                            getDefaultPesticideRecommendation(soilData.getCropType(), soilData.getSeason().toString()) :
                            pesticideRecommendation)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            System.err.println("Error processing AI response for crop: " + soilData.getCropType() +
                    ", Error: " + e.getMessage());
            return createDefaultRecommendation(soilData);
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

    private List<FertilizerDetail> extractFertilizerDetails(JsonNode fertilizersNode, String cropType) {
        List<FertilizerDetail> fertilizers = new ArrayList<>();
        if (fertilizersNode.isArray()) {
            int count = 0;
            for (JsonNode fertilizer : fertilizersNode) {
                if (count >= 2) break; // Limit to 2 fertilizers only
                try {
                    FertilizerDetail detail = FertilizerDetail.builder()
                            .name(fertilizer.path("name").asText("Unknown Fertilizer"))
                            .company(fertilizer.path("company").asText("IFFCO"))
                            .quantity(fertilizer.path("quantity").asText("50 kg per acre"))
                            .applicationMethod(fertilizer.path("applicationMethod").asText("As per package instructions"))
                            .npkRatio(fertilizer.path("npkRatio").asText("20:20:20"))
                            .build();
                    fertilizers.add(detail);
                    count++;
                } catch (Exception e) {
                    System.out.println("Error parsing fertilizer detail: " + e.getMessage());
                }
            }
        }
        return fertilizers.isEmpty() ? getDefaultFertilizers(cropType) : fertilizers;
    }

    private List<String> extractStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            arrayNode.forEach(item -> {
                String text = item.asText().trim();
                if (!text.isEmpty() && text.length() > 5) { // Filter out very short text
                    result.add(text);
                }
            });
        }
        return result;
    }

    private FertilizerRecommendation createDefaultRecommendation(SoilData soilData) {
        String detectedSoilType = soilData.getSoilType() != null ? soilData.getSoilType() : "Unknown";

        return FertilizerRecommendation.builder()
                .detectedSoilType(detectedSoilType)
                .cropType(soilData.getCropType())
                .areaValue(soilData.getAreaValue())
                .areaUnit(soilData.getAreaUnit().toString())
                .season(soilData.getSeason().toString())
                .language(soilData.getLanguage())
                .generalRecommendation("Standard fertilizer recommendation for " + soilData.getCropType() +
                        " cultivation during " + soilData.getSeason() + " season.")
                .fertilizers(getDefaultFertilizers(soilData.getCropType()))
                .applicationTips(getDefaultApplicationTips())
                .seasonalAdvice(getDefaultSeasonalAdvice(soilData.getSeason().toString()))
                .pesticideRecommendation(getDefaultPesticideRecommendation(soilData.getCropType(), soilData.getSeason().toString()))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<FertilizerDetail> getDefaultFertilizers(String cropType) {
        // Always return exactly 2 fertilizers
        switch (cropType.toLowerCase()) {
            case "wheat":
                return Arrays.asList(
                        FertilizerDetail.builder()
                                .name("DAP (Di-Ammonium Phosphate)")
                                .company("IFFCO")
                                .quantity("100 kg per acre")
                                .applicationMethod("Apply as basal dose before sowing")
                                .npkRatio("18:46:0")
                                .build(),
                        FertilizerDetail.builder()
                                .name("Urea")
                                .company("IFFCO")
                                .quantity("130 kg per acre")
                                .applicationMethod("Split application - 65 kg at sowing, 65 kg at tillering")
                                .npkRatio("46:0:0")
                                .build()
                );
            case "rice":
                return Arrays.asList(
                        FertilizerDetail.builder()
                                .name("NPK Complex")
                                .company("Coromandel")
                                .quantity("125 kg per acre")
                                .applicationMethod("Apply before transplanting")
                                .npkRatio("20:20:0:13")
                                .build(),
                        FertilizerDetail.builder()
                                .name("Urea")
                                .company("IFFCO")
                                .quantity("110 kg per acre")
                                .applicationMethod("Split in 3 doses - transplanting, tillering, panicle initiation")
                                .npkRatio("46:0:0")
                                .build()
                );
            default:
                return Arrays.asList(
                        FertilizerDetail.builder()
                                .name("NPK Complex")
                                .company("IFFCO")
                                .quantity("50 kg per acre")
                                .applicationMethod("Broadcast before sowing")
                                .npkRatio("20:20:20")
                                .build(),
                        FertilizerDetail.builder()
                                .name("Urea")
                                .company("IFFCO")
                                .quantity("25 kg per acre")
                                .applicationMethod("Top dressing after 30 days")
                                .npkRatio("46:0:0")
                                .build()
                );
        }
    }

    private List<String> getDefaultApplicationTips() {
        return Arrays.asList(
                "Apply fertilizers during cool morning (6-9 AM) or evening hours (4-6 PM)",
                "Ensure adequate soil moisture before fertilizer application",
                "Mix fertilizer properly with soil to avoid nutrient loss",
                "Avoid application during windy conditions"
        );
    }

    private List<String> getDefaultSeasonalAdvice(String season) {
        switch (season.toLowerCase()) {
            case "kharif":
                return Arrays.asList(
                        "Monitor monsoon patterns before fertilizer application",
                        "Apply nitrogen in split doses to prevent leaching during heavy rains",
                        "Consider using slow-release fertilizers during monsoon"
                );
            case "rabi":
                return Arrays.asList(
                        "Apply phosphorus-rich fertilizers during cool weather",
                        "Reduce nitrogen doses in winter to prevent lodging",
                        "Consider micronutrient supplements during winter months"
                );
            case "summer":
                return Arrays.asList(
                        "Increase potassium application for heat stress tolerance",
                        "Apply fertilizers with irrigation to prevent burning",
                        "Use mulching to reduce fertilizer loss due to evaporation"
                );
            default:
                return Arrays.asList(
                        "Monitor weather conditions before application",
                        "Adjust quantity based on seasonal rainfall patterns",
                        "Split application for better nutrient uptake and efficiency"
                );
        }
    }

    private List<String> getDefaultPesticideRecommendation(String cropType, String season) {
        switch (cropType.toLowerCase()) {
            case "wheat":
                return Arrays.asList(
                        "Apply Chlorpyrifos 20% EC (2ml/liter) for aphid control during tillering stage",
                        "Use Propiconazole 25% EC (1ml/liter) to prevent rust diseases in cool weather",
                        "Monitor for stem borer and apply Cartap Hydrochloride if threshold reached",
                        "Avoid chemical sprays during flowering to protect beneficial pollinators"
                );
            case "rice":
                return Arrays.asList(
                        "Use Cartap Hydrochloride 4G (25kg/acre) against stem borer during vegetative stage",
                        "Apply Pretilachlor 50% EC (2-3 liter/acre) for weed control within 3-5 days of transplanting",
                        "Monitor brown plant hopper and use Thiamethoxam 25% WG if population exceeds threshold",
                        "Apply copper-based fungicides during humid conditions to prevent bacterial leaf blight"
                );
            default:
                return Arrays.asList(
                        "Monitor crop regularly for early pest detection and intervention",
                        "Use neem-based organic pesticides as first line of defense against common pests",
                        "Apply chemical pesticides only when pest threshold levels are exceeded",
                        "Follow integrated pest management (IPM) practices for sustainable crop protection"
                );
        }
    }

    private String createPromptForSoilAnalysis(SoilData soilData) {
        String cropType = soilData.getCropType();
        Double areaValue = soilData.getAreaValue();
        String areaUnit = soilData.getAreaUnit().toString();
        String season = soilData.getSeason().toString();
        String location = soilData.getLocation() != null ? soilData.getLocation() : "India";
        String language = soilData.getLanguage();
        String providedSoilType = soilData.getSoilType();
        boolean hasImage = soilData.getSoilImage() != null;

        String languageInstruction = getLanguageInstruction(language);
        String soilAnalysisInstruction = createSoilAnalysisInstruction(providedSoilType, hasImage);

        return String.format("""
        You are an expert agricultural consultant specializing in soil analysis and fertilizer recommendations for Indian farming conditions.

        %s

        %s

        Provide response in EXACT JSON format:

        {
          "detectedSoilType": "%s",
          "generalRecommendation": "Brief overview of fertilizer strategy for this soil-crop combination",
          "fertilizers": [
            {
              "name": "Specific fertilizer name",
              "company": "Indian fertilizer company/brand",
              "quantity": "Exact quantity needed for the given area",
              "applicationMethod": "How and when to apply this fertilizer",
              "npkRatio": "NPK ratio of the fertilizer"
            },
            {
              "name": "Second fertilizer name",
              "company": "Indian fertilizer company/brand", 
              "quantity": "Exact quantity needed for the given area",
              "applicationMethod": "How and when to apply this fertilizer",
              "npkRatio": "NPK ratio of the fertilizer"
            }
          ],
          "applicationTips": [
            "Practical farming tip 1",
            "Practical farming tip 2",
            "Practical farming tip 3",
            "Practical farming tip 4"
          ],
          "seasonalAdvice": [
            "Season-specific advice 1",
            "Season-specific advice 2",
            "Season-specific advice 3"
          ],
          "pesticideRecommendation": [
            "Specific pesticide recommendation with dosage for common pests",
            "Disease prevention advice with fungicide details if needed",
            "Integrated pest management tip or organic alternative",
            "Safety precaution or timing advice for pesticide application"
          ]
        }

        Crop and Area Information:
        - Crop Type: %s
        - Farm Area: %.1f %s
        - Growing Season: %s
        - Location: %s
        %s

        Requirements:
        1. %s
        2. Recommend EXACTLY 2 Indian fertilizer brands (IFFCO, Coromandel, NFL, RCF, etc.)
        3. Calculate exact quantities for %.1f %s area
        4. Consider %s season requirements for %s crop
        5. Focus on cost-effective and locally available fertilizers
        6. Consider soil type characteristics for nutrient availability
        7. Provide EXACTLY 3-4 pesticide recommendations with specific product names and dosages
        8. Include both chemical and organic pest control options where applicable

        Provide ONLY the JSON response above.
        """,
                languageInstruction,
                soilAnalysisInstruction,
                getSoilTypeForResponse(providedSoilType, hasImage),
                cropType, areaValue, areaUnit.toLowerCase(),
                season.toLowerCase(), location,
                getLocationInfo(location),
                getSoilAnalysisRequirement(providedSoilType, hasImage),
                areaValue, areaUnit.toLowerCase(),
                season.toLowerCase(), cropType.toLowerCase()
        );
    }

    private String createSoilAnalysisInstruction(String providedSoilType, boolean hasImage) {
        if (providedSoilType != null && hasImage) {
            return String.format("SOIL ANALYSIS: User provided soil type '%s'. Also analyze the soil image to verify and provide additional insights.", providedSoilType);
        } else if (providedSoilType != null) {
            return String.format("SOIL ANALYSIS: User provided soil type '%s'. Base recommendations on this soil type.", providedSoilType);
        } else if (hasImage) {
            return "SOIL ANALYSIS: Analyze the provided soil image to determine the soil type (clay, sandy, loamy, silt, red soil, black soil, etc.).";
        } else {
            return "SOIL ANALYSIS: No specific soil type provided. Provide general recommendations.";
        }
    }

    private String getSoilTypeForResponse(String providedSoilType, boolean hasImage) {
        if (providedSoilType != null && hasImage) {
            return "Verify provided soil type '" + providedSoilType + "' with image analysis or use provided type";
        } else if (providedSoilType != null) {
            return providedSoilType;
        } else if (hasImage) {
            return "Detected soil type from image analysis";
        } else {
            return "General soil type";
        }
    }

    private String getLocationInfo(String location) {
        return location != null ? "- Provided Location: " + location : "";
    }

    private String getSoilAnalysisRequirement(String providedSoilType, boolean hasImage) {
        if (providedSoilType != null && hasImage) {
            return String.format("Use provided soil type '%s' and verify with image analysis if any discrepancy found", providedSoilType);
        } else if (providedSoilType != null) {
            return String.format("Base analysis on provided soil type '%s'", providedSoilType);
        } else if (hasImage) {
            return "Analyze soil image carefully to detect soil type (clay, sandy, loamy, red, black, etc.)";
        } else {
            return "Provide general soil-crop recommendations";
        }
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
}