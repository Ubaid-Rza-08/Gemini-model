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

    private String userId;
    private String soilType; // clay, sandy, loamy, etc.
    private byte[] soilImage; // Store image as byte array
    private Double areaValue; // numeric value
    private AreaUnit areaUnit; // acre, bigha, hectare
    private String cropType; // wheat, rice, corn, etc.
    private String location; // optional location info
    private Season season; // kharif, rabi, summer

    public enum AreaUnit {
        ACRE, BIGHA, HECTARE
    }

    public enum Season {
        KHARIF, RABI, SUMMER
    }
}