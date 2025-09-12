package com.ai.producer.entity;


import lombok.Data;
//import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
//@Entity
//@Table(name = "soil_data")
public class SoilData {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

//    @Column(name = "user_id", nullable = false)
    private String userId;

//    @Column(name = "soil_type", nullable = false)
    private String soilType; // clay, sandy, loamy, etc.

//    @Lob
//    @Column(name = "soil_image")
    private byte[] soilImage; // Store image as byte array

//    @Column(name = "area_value", nullable = false)
    private Double areaValue; // numeric value

//    @Column(name = "area_unit", nullable = false)
//    @Enumerated(EnumType.STRING)
    private AreaUnit areaUnit; // acre, bigha, hectare

//    @Column(name = "crop_type", nullable = false)
    private String cropType; // wheat, rice, corn, etc.

//    @Column(name = "location")
    private String location; // optional location info

//    @Column(name = "season", nullable = false)
//    @Enumerated(EnumType.STRING)
    private Season season; // kharif, rabi, summer

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }

    public enum AreaUnit {
        ACRE, BIGHA, HECTARE
    }

    public enum Season {
        KHARIF, RABI, SUMMER
    }
}
