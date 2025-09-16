package com.ubaid.ai_service.controller;

import com.ubaid.ai_service.model.FertilizerRecommendation;
import com.ubaid.ai_service.model.SoilData;
import com.ubaid.ai_service.service.SoilAIService;
import com.ubaid.ai_service.service.SoilHealthCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fertilizer")
@Slf4j
@CrossOrigin(origins = "*")
public class FertilizerController {

    private final SoilAIService soilAIService;
    private final SoilHealthCardService soilHealthCardService;

    @PostMapping("/analyze")
    public ResponseEntity<FertilizerRecommendation> analyzeSoilAndRecommend(
            @RequestParam("cropType") String cropType,
            @RequestParam("areaValue") Double areaValue,
            @RequestParam("areaUnit") SoilData.AreaUnit areaUnit,
            @RequestParam("season") SoilData.Season season,
            @RequestParam("language") String language,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "soilType", required = false) String soilType,
            @RequestParam(value = "soilImage", required = false) MultipartFile soilImage) {

        try {
            // Validate required fields
            if (cropType == null || areaValue == null || areaUnit == null ||
                    season == null || language == null) {
                throw new IllegalArgumentException("All required fields must be provided (cropType, areaValue, areaUnit, season, language)");
            }

            // Validate soilImage if provided
            if (soilImage != null && !soilImage.isEmpty()) {
                String contentType = soilImage.getContentType();
                if (contentType == null || (!contentType.startsWith("image/"))) {
                    throw new IllegalArgumentException("soilImage must be a valid image file (JPEG, PNG, etc.)");
                }

                // Validate file size (max 5MB)
                if (soilImage.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("Image size must be less than 5MB");
                }
            }

            // Create SoilData object
            SoilData soilData = new SoilData();
            soilData.setCropType(cropType);
            soilData.setAreaValue(areaValue);
            soilData.setAreaUnit(areaUnit);
            soilData.setSeason(season);
            soilData.setLanguage(language);
            soilData.setLocation(location != null && !location.trim().isEmpty() ? location.trim() : null);

            // Set soil type if provided (allow null/empty)
            if (soilType != null && !soilType.trim().isEmpty()) {
                soilData.setSoilType(soilType.trim());
            }

            // Handle soil image if provided
            if (soilImage != null && !soilImage.isEmpty()) {
                try {
                    soilData.setSoilImage(soilImage.getBytes());
                    log.info("Soil image received for analysis, size: {} bytes", soilImage.getSize());
                } catch (IOException e) {
                    log.error("Error processing soil image", e);
                    throw new IllegalArgumentException("Error processing soil image");
                }
            }

            log.info("Processing soil analysis request: CropType={}, SoilTypeSource={}, HasImage={}",
                    cropType, soilData.getSoilTypeSource(), soilData.getSoilImage() != null);

            // Generate recommendation using AI
            FertilizerRecommendation recommendation = soilAIService.generateFertilizerRecommendation(soilData);

            return ResponseEntity.ok(recommendation);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error generating fertilizer recommendation", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/analyze-health-card")
    public ResponseEntity<FertilizerRecommendation> analyzeSoilHealthCard(
            @RequestParam("healthCardImage") MultipartFile healthCardImage,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "overrideCropType", required = false) String overrideCropType,
            @RequestParam(value = "overrideAreaValue", required = false) Double overrideAreaValue,
            @RequestParam(value = "overrideAreaUnit", required = false) SoilData.AreaUnit overrideAreaUnit,
            @RequestParam(value = "overrideSeason", required = false) SoilData.Season overrideSeason) {

        try {
            // Validate health card image
            if (healthCardImage == null || healthCardImage.isEmpty()) {
                throw new IllegalArgumentException("Soil health card image is required");
            }

            String contentType = healthCardImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("healthCardImage must be a valid image file (JPEG, PNG, etc.)");
            }

            // Validate file size (max 10MB for health card as it may have more details)
            if (healthCardImage.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("Health card image size must be less than 10MB");
            }

            // Validate language parameter
            if (!isValidLanguage(language)) {
                throw new IllegalArgumentException("Invalid language code. Supported: en, hi, bn, te, ta, mr, gu, kn, ml, pa, or");
            }

            log.info("Processing soil health card analysis: ImageSize={} bytes, Language={}",
                    healthCardImage.getSize(), language);

            // Extract soil data from health card using Gemini vision
            byte[] imageBytes = healthCardImage.getBytes();
            SoilData extractedSoilData = soilHealthCardService.extractSoilDataFromHealthCard(imageBytes, language);

            // Apply overrides if provided
            if (overrideCropType != null && !overrideCropType.trim().isEmpty()) {
                extractedSoilData.setCropType(overrideCropType.trim());
                log.info("Overriding crop type with: {}", overrideCropType);
            }

            if (overrideAreaValue != null && overrideAreaValue > 0) {
                extractedSoilData.setAreaValue(overrideAreaValue);
                log.info("Overriding area value with: {}", overrideAreaValue);
            }

            if (overrideAreaUnit != null) {
                extractedSoilData.setAreaUnit(overrideAreaUnit);
                log.info("Overriding area unit with: {}", overrideAreaUnit);
            }

            if (overrideSeason != null) {
                extractedSoilData.setSeason(overrideSeason);
                log.info("Overriding season with: {}", overrideSeason);
            }

            log.info("Extracted soil data from health card: CropType={}, SoilType={}, Location={}, Area={} {}",
                    extractedSoilData.getCropType(), extractedSoilData.getSoilType(),
                    extractedSoilData.getLocation(), extractedSoilData.getAreaValue(),
                    extractedSoilData.getAreaUnit());

            // Generate recommendation using the extracted soil data
            FertilizerRecommendation recommendation = soilAIService.generateFertilizerRecommendation(extractedSoilData);

            return ResponseEntity.ok(recommendation);

        } catch (IllegalArgumentException e) {
            log.error("Validation error for health card analysis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorRecommendation(e.getMessage()));
        } catch (IOException e) {
            log.error("Error processing health card image", e);
            return ResponseEntity.badRequest().body(createErrorRecommendation("Error processing health card image"));
        } catch (Exception e) {
            log.error("Error analyzing soil health card", e);
            return ResponseEntity.internalServerError().body(createErrorRecommendation("Internal server error occurred"));
        }
    }

    @PostMapping("/analyze-json")
    public ResponseEntity<FertilizerRecommendation> analyzeSoilFromJson(
            @RequestBody SoilData soilData) {
        try {
            // Validate required fields
            if (soilData.getCropType() == null || soilData.getAreaValue() == null ||
                    soilData.getAreaUnit() == null || soilData.getSeason() == null ||
                    soilData.getLanguage() == null) {
                throw new IllegalArgumentException("All required fields must be provided (cropType, areaValue, areaUnit, season, language)");
            }

            log.info("Processing soil analysis from JSON: CropType={}, SoilTypeSource={}, HasImage={}",
                    soilData.getCropType(), soilData.getSoilTypeSource(), soilData.getSoilImage() != null);

            // Generate recommendation using AI
            FertilizerRecommendation recommendation = soilAIService.generateFertilizerRecommendation(soilData);

            return ResponseEntity.ok(recommendation);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error generating fertilizer recommendation from JSON", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/soil-types")
    public ResponseEntity<SoilData.SoilType[]> getSupportedSoilTypes() {
        return ResponseEntity.ok(SoilData.SoilType.values());
    }

    @GetMapping("/area-units")
    public ResponseEntity<SoilData.AreaUnit[]> getSupportedAreaUnits() {
        return ResponseEntity.ok(SoilData.AreaUnit.values());
    }

    @GetMapping("/seasons")
    public ResponseEntity<SoilData.Season[]> getSupportedSeasons() {
        return ResponseEntity.ok(SoilData.Season.values());
    }

    // Helper methods
    private boolean isValidLanguage(String language) {
        return language != null &&
                ("en".equals(language) || "hi".equals(language) || "bn".equals(language) ||
                        "te".equals(language) || "ta".equals(language) || "mr".equals(language) ||
                        "gu".equals(language) || "kn".equals(language) || "ml".equals(language) ||
                        "pa".equals(language) || "or".equals(language));
    }

    private FertilizerRecommendation createErrorRecommendation(String errorMessage) {
        return FertilizerRecommendation.builder()
                .detectedSoilType("Unknown")
                .cropType("Unknown")
                .areaValue(0.0)
                .areaUnit("ACRE")
                .season("KHARIF")
                .language("en")
                .generalRecommendation("Error: " + errorMessage)
                .fertilizers(java.util.Collections.emptyList())
                .applicationTips(java.util.Collections.emptyList())
                .seasonalAdvice(java.util.Collections.emptyList())
                .pesticideRecommendation(java.util.Collections.emptyList())
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }
}