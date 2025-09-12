package com.ubaid.ai_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubaid.ai_service.model.FertilizerDetail;
import com.ubaid.ai_service.model.FertilizerRecommendation;
import com.ubaid.ai_service.model.SoilData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
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
            String aiResponse = geminiService.getAnswer(prompt);
            System.out.println("RESPONSE FROM AI: " + aiResponse);
            return processAiResponse(soilData, aiResponse);
        } catch (Exception e) {
            System.err.println("Error generating fertilizer recommendation for soil data of user: " + soilData.getUserId() + ", Error: " + e.getMessage());
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

            String generalRecommendation = analysisJson.path("generalRecommendation").asText();
            List<FertilizerDetail> fertilizers = extractFertilizerDetails(analysisJson.path("fertilizers"));
            List<String> applicationTips = extractStringList(analysisJson.path("applicationTips"));
            List<String> seasonalAdvice = extractStringList(analysisJson.path("seasonalAdvice"));

            return FertilizerRecommendation.builder()
                    .userId(soilData.getUserId())
                    .soilType(soilData.getSoilType())
                    .cropType(soilData.getCropType())
                    .areaValue(soilData.getAreaValue())
                    .areaUnit(soilData.getAreaUnit().toString())
                    .season(soilData.getSeason().toString())
                    .generalRecommendation(generalRecommendation.isEmpty() ?
                            "AI recommendation for " + soilData.getCropType() + " in " + soilData.getSoilType() + " soil." :
                            generalRecommendation)
                    .fertilizers(fertilizers)
                    .applicationTips(applicationTips)
                    .seasonalAdvice(seasonalAdvice)
                    .build();

        } catch (Exception e) {
            System.err.println("Error processing AI response for soil data of user: " + soilData.getUserId() + ", Error: " + e.getMessage());
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

    private List<FertilizerDetail> extractFertilizerDetails(JsonNode fertilizersNode) {
        List<FertilizerDetail> fertilizers = new ArrayList<>();
        if (fertilizersNode.isArray()) {
            fertilizersNode.forEach(fertilizer -> {
                try {
                    FertilizerDetail detail = FertilizerDetail.builder()
                            .name(fertilizer.path("name").asText("Unknown Fertilizer"))
                            .company(fertilizer.path("company").asText("IFFCO"))
                            .quantity(fertilizer.path("quantity").asText("50 kg per acre"))
                            .applicationMethod(fertilizer.path("applicationMethod").asText("As per package instructions"))
                            .npkRatio(fertilizer.path("npkRatio").asText("20:20:20"))
                            .build();
                    fertilizers.add(detail);
                } catch (Exception e) {
                    System.out.println("Error parsing fertilizer detail: " + e.getMessage());
                }
            });
        }
        return fertilizers.isEmpty() ? getDefaultFertilizers("NULL") : fertilizers;
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
        return FertilizerRecommendation.builder()
                .userId(soilData.getUserId())
                .soilType(soilData.getSoilType())
                .cropType(soilData.getCropType())
                .areaValue(soilData.getAreaValue())
                .areaUnit(soilData.getAreaUnit().toString())
                .season(soilData.getSeason().toString())
                .generalRecommendation("Standard fertilizer recommendation for " + soilData.getCropType() +
                        " cultivation in " + soilData.getSoilType() + " soil during " + soilData.getSeason() + " season.")
                .fertilizers(getDefaultFertilizers(soilData.getCropType()))
                .applicationTips(getDefaultApplicationTips())
                .seasonalAdvice(getDefaultSeasonalAdvice(soilData.getSeason().toString()))
                .build();
    }

    private List<FertilizerDetail> getDefaultFertilizers(String cropType) {
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
                "Avoid application during windy conditions",
                "Keep fertilizers away from seeds during sowing"
        );
    }

    private List<String> getDefaultSeasonalAdvice(String season) {
        switch (season.toLowerCase()) {
            case "kharif":
                return Arrays.asList(
                        "Monitor monsoon patterns before fertilizer application",
                        "Apply nitrogen in split doses to prevent leaching during heavy rains",
                        "Consider using slow-release fertilizers during monsoon",
                        "Maintain proper drainage to avoid waterlogging"
                );
            case "rabi":
                return Arrays.asList(
                        "Apply phosphorus-rich fertilizers during cool weather",
                        "Reduce nitrogen doses in winter to prevent lodging",
                        "Consider micronutrient supplements during winter months",
                        "Adjust irrigation schedule with fertilizer application"
                );
            case "summer":
                return Arrays.asList(
                        "Increase potassium application for heat stress tolerance",
                        "Apply fertilizers with irrigation to prevent burning",
                        "Use mulching to reduce fertilizer loss due to evaporation",
                        "Monitor plant stress and adjust nutrient doses accordingly"
                );
            default:
                return Arrays.asList(
                        "Monitor weather conditions before application",
                        "Adjust quantity based on seasonal rainfall patterns",
                        "Split application for better nutrient uptake and efficiency"
                );
        }
    }

    private String createPromptForSoilAnalysis(SoilData soilData) {
        String soilType = soilData.getSoilType();
        String cropType = soilData.getCropType();
        Double areaValue = soilData.getAreaValue();
        String areaUnit = soilData.getAreaUnit().toString();
        String season = soilData.getSeason().toString();
        String location = soilData.getLocation() != null ? soilData.getLocation() : "India";

        return String.format("""
        You are an expert agricultural consultant specializing in soil analysis and fertilizer recommendations for Indian farming conditions. 
        
        Analyze the following soil and crop data and provide specific, actionable fertilizer recommendations in EXACT JSON format:

        {
          "generalRecommendation": "Brief overview of fertilizer strategy for this soil-crop combination",
          "fertilizers": [
            {
              "name": "Specific fertilizer name",
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
          ]
        }

        Crop and Soil Information:
        - Soil Type: %s
        - Crop Type: %s
        - Farm Area: %.1f %s
        - Growing Season: %s
        - Location: %s

        Requirements:
        1. Recommend Indian fertilizer brands (IFFCO, Coromandel, NFL, RCF, etc.)
        2. Calculate exact quantities for %.1f %s area
        3. Consider %s season requirements for %s crop
        4. Include both basal dose and top-dressing recommendations
        5. Use simple Hindi-English mixed language that farmers understand
        6. Focus on cost-effective and locally available fertilizers
        7. Consider soil type characteristics for nutrient availability

        Provide ONLY the JSON response above. Keep all recommendations practical and farmer-friendly.
        """,
                soilType, cropType, areaValue, areaUnit.toLowerCase(),
                season.toLowerCase(), location, areaValue, areaUnit.toLowerCase(),
                season.toLowerCase(), cropType.toLowerCase()
        );
    }
}