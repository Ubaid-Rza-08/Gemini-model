package com.ubaid.ai_service.model;

import lombok.*;
//import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

//@Entity
//@Table(name = "fertilizer_recommendations")

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FertilizerRecommendation {

    // Removed userId and soilDataId
    private String detectedSoilType; // AI-detected soil type from image
    private String cropType;
    private Double areaValue;
    private String areaUnit;
    private String season;
    private String language; // Language used for response
    private String generalRecommendation;

    // Limited to exactly 2 fertilizer recommendations
    private List<FertilizerDetail> fertilizers; // Max 2 items

    private List<String> applicationTips;
    private List<String> seasonalAdvice;
    private LocalDateTime createdAt;
}