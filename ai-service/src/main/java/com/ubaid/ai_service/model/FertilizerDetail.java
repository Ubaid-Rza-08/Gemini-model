package com.ubaid.ai_service.model;



import lombok.*;

//@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FertilizerDetail {
//    @Column(name = "fertilizer_name")
    private String name;

//    @Column(name = "company")
    private String company;

//    @Column(name = "quantity")
    private String quantity; // e.g., "50 kg", "25 bags"

//    @Column(name = "application_method")
    private String applicationMethod;

//    @Column(name = "npk_ratio")
    private String npkRatio; // e.g., "20:20:20"
}
