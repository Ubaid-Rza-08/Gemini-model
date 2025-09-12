package com.ai.producer.kafka;

import com.ai.producer.entity.SoilData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/producer")
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, SoilData> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, SoilData> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/soil-analysis")
    public String sendSoilDataForAnalysis(@RequestBody SoilData soilData) {
        try {
            // Validate required fields
            if (soilData.getCropType() == null || soilData.getAreaValue() == null ||
                    soilData.getAreaUnit() == null || soilData.getSeason() == null ||
                    soilData.getLanguage() == null || soilData.getSoilImage() == null) {
                return "Error: All required fields must be provided including soil image, crop type, area, season, and language";
            }

            kafkaTemplate.send("soil-analysis-topic", soilData);
            log.info("Sent soil data for analysis: CropType={}, Language={}, ImageSize={} bytes",
                    soilData.getCropType(), soilData.getLanguage(),
                    soilData.getSoilImage() != null ? soilData.getSoilImage().length : 0);

            return "Soil analysis request sent successfully for crop: " + soilData.getCropType();
        } catch (Exception e) {
            log.error("Error sending soil data to Kafka", e);
            return "Failed to send soil analysis request: " + e.getMessage();
        }
    }

    @PostMapping("/soil-analysis-form")
    public String sendSoilDataWithImage(
            @RequestParam("cropType") String cropType,
            @RequestParam("areaValue") Double areaValue,
            @RequestParam("areaUnit") String areaUnit,
            @RequestParam("season") String season,
            @RequestParam("language") String language,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam("soilImage") MultipartFile soilImage) {

        try {
            // Validate required fields
            if (cropType == null || areaValue == null || areaUnit == null ||
                    season == null || language == null || soilImage == null || soilImage.isEmpty()) {
                return "Error: All required fields must be provided including soil image";
            }

            SoilData soilData = new SoilData();
            soilData.setCropType(cropType);
            soilData.setAreaValue(areaValue);
            soilData.setLanguage(language);

            // Convert string to enum
            try {
                soilData.setAreaUnit(SoilData.AreaUnit.valueOf(areaUnit.toUpperCase()));
                soilData.setSeason(SoilData.Season.valueOf(season.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return "Invalid area unit or season. Use: ACRE/BIGHA/HECTARE for area unit and KHARIF/RABI/SUMMER for season";
            }

            soilData.setLocation(location);

            // Handle soil image - required for soil type detection
            try {
                soilData.setSoilImage(soilImage.getBytes());
                log.info("Soil image received, size: {} bytes", soilImage.getSize());
            } catch (IOException e) {
                log.error("Error processing soil image", e);
                return "Error processing soil image: " + e.getMessage();
            }

            kafkaTemplate.send("soil-analysis-topic", soilData);
            log.info("Sent soil data with image for analysis: CropType={}, Language={}, Season={}",
                    cropType, language, season);

            return "Soil analysis request sent successfully for crop: " + cropType + " in " + language + " language";

        } catch (Exception e) {
            log.error("Error sending soil data to Kafka", e);
            return "Failed to send soil analysis request: " + e.getMessage();
        }
    }
}