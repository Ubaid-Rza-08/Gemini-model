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
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

//    @Column(name = "soil_data_id")
    private Long soilDataId;

//    @Column(name = "user_id", nullable = false)
    private String userId;

//    @Column(name = "soil_type")
    private String soilType;

//    @Column(name = "crop_type")
    private String cropType;

//    @Column(name = "area_value")
    private Double areaValue;

//    @Column(name = "area_unit")
    private String areaUnit;

//    @Column(name = "season")
    private String season;

//    @Column(name = "general_recommendation", columnDefinition = "TEXT")
    private String generalRecommendation;

//    @ElementCollection
//    @CollectionTable(name = "fertilizer_details",
//            joinColumns = @JoinColumn(name = "recommendation_id"))
    private List<FertilizerDetail> fertilizers;

//    @ElementCollection
//    @CollectionTable(name = "application_tips",
//            joinColumns = @JoinColumn(name = "recommendation_id"))
//    @Column(name = "tip", columnDefinition = "TEXT")
    private List<String> applicationTips;

//    @ElementCollection
//    @CollectionTable(name = "seasonal_advice",
//            joinColumns = @JoinColumn(name = "recommendation_id"))
//    @Column(name = "advice", columnDefinition = "TEXT")
    private List<String> seasonalAdvice;

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
////    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//    }
}

//@Embeddable
//@Data
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//class FertilizerDetail {
//    @Column(name = "fertilizer_name")
//    private String name;
//
//    @Column(name = "company")
//    private String company;
//
//    @Column(name = "quantity")
//    private String quantity; // e.g., "50 kg", "25 bags"
//
//    @Column(name = "application_method")
//    private String applicationMethod;
//
//    @Column(name = "npk_ratio")
//    private String npkRatio; // e.g., "20:20:20"
//}