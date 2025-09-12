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

    // Removed userId and soilType
    private byte[] soilImage; // Store image as byte array - required for soil type detection
    private Double areaValue; // numeric value
    private AreaUnit areaUnit; // acre, bigha, hectare
    private String cropType; // wheat, rice, corn, etc.
    private String location; // optional location info
    private Season season; // kharif, rabi, summer
    private String language; // en, hi, bn, te, ta, etc. for response language

    public enum AreaUnit {
        ACRE, BIGHA, HECTARE
    }

    public enum Season {
        KHARIF, RABI, SUMMER
    }
}