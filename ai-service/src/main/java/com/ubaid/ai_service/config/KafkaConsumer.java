package com.ubaid.ai_service.config;
import com.ubaid.ai_service.model.FertilizerRecommendation;
import com.ubaid.ai_service.model.SoilData;
import com.ubaid.ai_service.service.SoilAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final SoilAIService soilAIService;

    @KafkaListener(topics = "soil-analysis-topic", groupId = "agriculture-group")
    public void processSoilData(SoilData soilData) {
        try {
            System.out.println("Processing soil data for user: " + soilData.getUserId());

            FertilizerRecommendation recommendation = soilAIService.generateFertilizerRecommendation(soilData);

            System.out.println("Recommendation generated for user: " + soilData.getUserId());

        } catch (Exception e) {
            System.err.println("Error processing soil data for user: " + soilData.getUserId() + ", Error: " + e.getMessage());
        }
    }
}