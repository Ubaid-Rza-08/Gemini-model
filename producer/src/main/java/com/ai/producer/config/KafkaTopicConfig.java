package com.ai.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic soilAnalysisTopic() {
        return new NewTopic("soil-analysis-topic", 3, (short) 1);
    }

//    @Bean
//    public NewTopic fertilizerRecommendationTopic() {
//        return new NewTopic("fertilizer-recommendation-topic", 3, (short) 1);
//    }
}
