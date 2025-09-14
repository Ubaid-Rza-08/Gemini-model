package com.ubaid.ai_service.controller;

import com.ubaid.ai_service.model.FertilizerRecommendation;
import com.ubaid.ai_service.model.SoilData;
import com.ubaid.ai_service.service.SoilAIService;
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

            // Generate recommendation using AI - no longer requires soil data
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
}