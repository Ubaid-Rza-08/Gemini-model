package com.ai.producer.entity;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
//import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class SoilData {

    private String soilType; // Optional: User can provide soil type (clay, sandy, loamy, etc.)
    private byte[] soilImage; // Optional: Store image as byte array for soil type detection
    private Double areaValue; // numeric value
    private AreaUnit areaUnit; // acre, bigha, hectare
    private String cropType; // wheat, rice, corn, etc.
    private String location; // Optional location info
    private Season season; // kharif, rabi, summer
    private String language; // en, hi, bn, te, ta, etc. for response language

    public enum AreaUnit {
        ACRE, BIGHA, HECTARE
    }

    public enum Season {
        KHARIF, RABI, SUMMER
    }

    public enum SoilType {
        CLAY, SANDY, LOAMY, SILT, RED_SOIL, BLACK_SOIL, ALLUVIAL, LATERITE, MOUNTAIN_SOIL, DESERT_SOIL
    }

    // Helper method to check if soil analysis is possible
    public boolean canAnalyzeSoil() {
        return soilType != null || soilImage != null;
    }

    // Helper method to get soil type source
    public String getSoilTypeSource() {
        if (soilType != null && soilImage != null) {
            return "PROVIDED_AND_IMAGE";
        } else if (soilType != null) {
            return "PROVIDED";
        } else if (soilImage != null) {
            return "IMAGE_ANALYSIS";
        }
        return "NONE";
    }
}