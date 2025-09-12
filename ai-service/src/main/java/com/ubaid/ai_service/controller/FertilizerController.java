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
public class FertilizerController {

    private final SoilAIService soilAIService;

    @PostMapping("/analyze")
    public ResponseEntity<FertilizerRecommendation> analyzeSoilAndRecommend(
            @RequestParam("userId") String userId,
            @RequestParam("soilType") String soilType,
            @RequestParam("cropType") String cropType,
            @RequestParam("areaValue") Double areaValue,
            @RequestParam("areaUnit") SoilData.AreaUnit areaUnit,
            @RequestParam("season") SoilData.Season season,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "soilImage", required = false) MultipartFile soilImage) {

        try {
            // Validate required fields
            if (userId == null || soilType == null || cropType == null ||
                    areaValue == null || areaUnit == null || season == null) {
                throw new IllegalArgumentException("All required fields must be provided");
            }

            // Create SoilData object
            SoilData soilData = new SoilData();
            soilData.setUserId(userId);
            soilData.setSoilType(soilType);
            soilData.setCropType(cropType);
            soilData.setAreaValue(areaValue);
            soilData.setAreaUnit(areaUnit);
            soilData.setSeason(season);
            soilData.setLocation(location);

            // Handle image if provided
            if (soilImage != null && !soilImage.isEmpty()) {
                try {
                    soilData.setSoilImage(soilImage.getBytes());
                    log.info("Soil image received, size: {} bytes", soilImage.getSize());
                } catch (IOException e) {
                    log.error("Error processing soil image", e);
                    throw new IllegalArgumentException("Error processing soil image");
                }
            }

            // Generate recommendation using AI
            FertilizerRecommendation recommendation = soilAIService.generateFertilizerRecommendation(soilData);

            return ResponseEntity.ok(recommendation);

        } catch (Exception e) {
            log.error("Error generating fertilizer recommendation", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/analyze-json")
    public ResponseEntity<FertilizerRecommendation> analyzeSoilFromJson(
            @RequestBody SoilData soilData) {
        try {
            // Validate required fields
            if (soilData.getUserId() == null || soilData.getSoilType() == null ||
                    soilData.getCropType() == null || soilData.getAreaValue() == null ||
                    soilData.getAreaUnit() == null || soilData.getSeason() == null) {
                throw new IllegalArgumentException("All required fields must be provided");
            }

            // Generate recommendation using AI
            FertilizerRecommendation recommendation = soilAIService.generateFertilizerRecommendation(soilData);

            return ResponseEntity.ok(recommendation);

        } catch (Exception e) {
            log.error("Error generating fertilizer recommendation from JSON", e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}