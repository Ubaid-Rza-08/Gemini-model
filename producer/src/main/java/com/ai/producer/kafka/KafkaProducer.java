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
            kafkaTemplate.send("soil-analysis-topic", soilData);
            log.info("Sent soil data for analysis: User={}, SoilType={}, CropType={}",
                    soilData.getUserId(), soilData.getSoilType(), soilData.getCropType());
            return "Soil analysis request sent successfully for user: " + soilData.getUserId();
        } catch (Exception e) {
            log.error("Error sending soil data to Kafka", e);
            return "Failed to send soil analysis request: " + e.getMessage();
        }
    }

    @PostMapping("/soil-analysis-form")
    public String sendSoilDataWithImage(
            @RequestParam("userId") String userId,
            @RequestParam("soilType") String soilType,
            @RequestParam("cropType") String cropType,
            @RequestParam("areaValue") Double areaValue,
            @RequestParam("areaUnit") String areaUnit,
            @RequestParam("season") String season,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "soilImage", required = false) MultipartFile soilImage) {

        try {
            SoilData soilData = new SoilData();
            soilData.setUserId(userId);
            soilData.setSoilType(soilType);
            soilData.setCropType(cropType);
            soilData.setAreaValue(areaValue);

            // Convert string to enum
            try {
                soilData.setAreaUnit(SoilData.AreaUnit.valueOf(areaUnit.toUpperCase()));
                soilData.setSeason(SoilData.Season.valueOf(season.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return "Invalid area unit or season. Use: ACRE/BIGHA/HECTARE for area unit and KHARIF/RABI/SUMMER for season";
            }

            soilData.setLocation(location);

            // Handle image if provided
            if (soilImage != null && !soilImage.isEmpty()) {
                try {
                    soilData.setSoilImage(soilImage.getBytes());
                    log.info("Soil image received, size: {} bytes", soilImage.getSize());
                } catch (IOException e) {
                    log.error("Error processing soil image", e);
                    return "Error processing soil image: " + e.getMessage();
                }
            }

            kafkaTemplate.send("soil-analysis-topic", soilData);
            log.info("Sent soil data with image for analysis: User={}, SoilType={}, CropType={}",
                    userId, soilType, cropType);

            return "Soil analysis request sent successfully for user: " + userId;

        } catch (Exception e) {
            log.error("Error sending soil data to Kafka", e);
            return "Failed to send soil analysis request: " + e.getMessage();
        }
    }
}
