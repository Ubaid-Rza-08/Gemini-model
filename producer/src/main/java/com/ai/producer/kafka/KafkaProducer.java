package com.ai.producer.kafka;

import com.ai.producer.entity.SoilData;
import com.ai.producer.util.ImageCompressionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/producer")
@Slf4j
@CrossOrigin(origins = "*")
public class KafkaProducer {

    private final KafkaTemplate<String, SoilData> kafkaTemplate;
    private final ImageCompressionUtil imageCompressionUtil;

    public KafkaProducer(KafkaTemplate<String, SoilData> kafkaTemplate,
                         ImageCompressionUtil imageCompressionUtil) {
        this.kafkaTemplate = kafkaTemplate;
        this.imageCompressionUtil = imageCompressionUtil;
    }

    @PostMapping("/soil-analysis")
    public ResponseEntity<String> sendSoilDataForAnalysis(@RequestBody SoilData soilData) {
        try {
            // Validate required fields
            if (soilData.getCropType() == null || soilData.getAreaValue() == null ||
                    soilData.getAreaUnit() == null || soilData.getSeason() == null ||
                    soilData.getLanguage() == null) {
                return ResponseEntity.badRequest()
                        .body("Error: All required fields must be provided (cropType, areaValue, areaUnit, season, language)");
            }

            // No longer requiring soilType or soilImage - AI can provide general recommendations
            kafkaTemplate.send("soil-analysis-topic", soilData);
            log.info("Sent soil data for analysis: CropType={}, Language={}, SoilTypeSource={}, ImageSize={} bytes",
                    soilData.getCropType(), soilData.getLanguage(), soilData.getSoilTypeSource(),
                    soilData.getSoilImage() != null ? soilData.getSoilImage().length : 0);

            return ResponseEntity.ok("Soil analysis request sent successfully for crop: " + soilData.getCropType());
        } catch (Exception e) {
            log.error("Error sending soil data to Kafka", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send soil analysis request: " + e.getMessage());
        }
    }

    @PostMapping(value = "/soil-analysis-form", consumes = {"multipart/form-data"})
    public ResponseEntity<String> sendSoilDataWithForm(
            @RequestParam("cropType") String cropType,
            @RequestParam("areaValue") Double areaValue,
            @RequestParam("areaUnit") String areaUnit,
            @RequestParam("season") String season,
            @RequestParam("language") String language,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "soilType", required = false) String soilType,
            @RequestParam(value = "soilImage", required = false) MultipartFile soilImage) {

        try {
            // Validate required fields
            if (cropType == null || cropType.trim().isEmpty() ||
                    areaValue == null || areaUnit == null || areaUnit.trim().isEmpty() ||
                    season == null || season.trim().isEmpty() ||
                    language == null || language.trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body("Error: All required fields must be provided (cropType, areaValue, areaUnit, season, language)");
            }

            // Validate soilImage if provided
            if (soilImage != null && !soilImage.isEmpty()) {
                String contentType = soilImage.getContentType();
                if (contentType == null || (!contentType.startsWith("image/"))) {
                    return ResponseEntity.badRequest()
                            .body("Error: soilImage must be a valid image file (JPEG, PNG, etc.)");
                }

                // Validate file size (max 5MB before compression)
                if (soilImage.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body("Error: Image size must be less than 5MB");
                }
            }

            // Validate soilType if provided (but not required)
            if (soilType != null && !soilType.trim().isEmpty()) {
                try {
                    // Clean and validate soil type
                    String cleanSoilType = soilType.toUpperCase().trim().replace(" ", "_");
                    SoilData.SoilType.valueOf(cleanSoilType);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body("Invalid soil type. Valid types: CLAY, SANDY, LOAMY, SILT, RED_SOIL, BLACK_SOIL, ALLUVIAL, LATERITE, MOUNTAIN_SOIL, DESERT_SOIL");
                }
            }

            SoilData soilData = new SoilData();
            soilData.setCropType(cropType.trim());
            soilData.setAreaValue(areaValue);
            soilData.setLanguage(language.trim());
            soilData.setLocation(location != null && !location.trim().isEmpty() ? location.trim() : null);

            // Set soil type if provided (allow empty/null values)
            if (soilType != null && !soilType.trim().isEmpty()) {
                soilData.setSoilType(soilType.trim());
            }

            // Convert string to enum with validation
            try {
                soilData.setAreaUnit(SoilData.AreaUnit.valueOf(areaUnit.toUpperCase().trim()));
                soilData.setSeason(SoilData.Season.valueOf(season.toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body("Invalid area unit or season. Valid areaUnit: ACRE, BIGHA, HECTARE. Valid season: KHARIF, RABI, SUMMER");
            }

            // Process and compress image if provided
            if (soilImage != null && !soilImage.isEmpty()) {
                try {
                    byte[] originalImageBytes = soilImage.getBytes();
                    byte[] compressedImageBytes = imageCompressionUtil.compressImage(originalImageBytes);
                    soilData.setSoilImage(compressedImageBytes);

                    log.info("Image processed - Original: {} KB, Compressed: {} KB, Filename: {}",
                            originalImageBytes.length / 1024,
                            compressedImageBytes.length / 1024,
                            soilImage.getOriginalFilename());

                } catch (IOException e) {
                    log.error("Error processing/compressing soil image", e);
                    return ResponseEntity.badRequest()
                            .body("Error processing soil image: " + e.getMessage());
                }
            }

            // Send to Kafka
            kafkaTemplate.send("soil-analysis-topic", soilData);

            String analysisMethod = soilData.getSoilTypeSource();
            log.info("Sent soil data for analysis: CropType={}, Language={}, Season={}, AnalysisMethod={}",
                    cropType, language, season, analysisMethod);

            String responseMessage = String.format(
                    "Soil analysis request sent successfully for crop: %s in %s language. Analysis method: %s",
                    cropType, language, getAnalysisMethodDescription(analysisMethod));

            return ResponseEntity.ok(responseMessage);

        } catch (Exception e) {
            log.error("Error sending soil data to Kafka", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send soil analysis request: " + e.getMessage());
        }
    }

    private String getAnalysisMethodDescription(String analysisMethod) {
        switch (analysisMethod) {
            case "PROVIDED_AND_IMAGE":
                return "User-provided soil type with image verification";
            case "PROVIDED":
                return "User-provided soil type";
            case "IMAGE_ANALYSIS":
                return "AI image analysis";
            case "GENERAL":
                return "General crop-based recommendations";
            default:
                return "Unknown";
        }
    }
}