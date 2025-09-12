package com.ubaid.ai_service.model;


import lombok.Data;
//import jakarta.persistence.*;
import java.time.LocalDateTime;

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
    // Now allows analysis even without soil data for general recommendations
    public boolean canAnalyzeSoil() {
        return true; // Always allow analysis - AI can provide general recommendations
    }

    // Helper method to get soil type source
    public String getSoilTypeSource() {
        boolean hasSoilType = soilType != null && !soilType.trim().isEmpty();
        boolean hasImage = soilImage != null && soilImage.length > 0;

        if (hasSoilType && hasImage) {
            return "PROVIDED_AND_IMAGE";
        } else if (hasSoilType) {
            return "PROVIDED";
        } else if (hasImage) {
            return "IMAGE_ANALYSIS";
        }
        return "GENERAL"; // Changed from "NONE" to indicate general recommendations
    }

    // Helper method to check if we have any soil data
    public boolean hasSoilData() {
        return (soilType != null && !soilType.trim().isEmpty()) ||
                (soilImage != null && soilImage.length > 0);
    }
}