package com.sih.farmer.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestTemplate restTemplate;
    private final OtpService otpService;

    @Value("${weather.api.key:4c3fb3dc336e3f3a2ccfc656bf857318}")
    private String apiKey;

    @Value("${weather.api.base-url:https://api.openweathermap.org/data/2.5/weather}")
    private String baseUrl;

    // Weather conditions that need alerts
    private static final Set<String> ALERT_CONDITIONS = Set.of(
            "Rain", "Thunderstorm", "Snow", "Drizzle", "Mist", "Fog", "Tornado", "Clouds"
    );

    public String getWeatherByCity(String city, String phoneNumber) throws IOException {
        log.info("Fetching weather data for city: {} for phone: {}", city, phoneNumber);

        String url = String.format("%s?units=metric&q=%s&appid=%s", baseUrl, city, apiKey);

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && response.has("weather") &&
                    response.get("weather").isArray() &&
                    response.get("weather").size() > 0) {

                String weatherCondition = response.get("weather").get(0).get("main").asText();
                String description = response.get("weather").get(0).get("description").asText();

                log.info("Weather condition for {}: {}", city, weatherCondition);

                // Check if alert needed
                if (ALERT_CONDITIONS.contains(weatherCondition)) {
                    String alertMessage = String.format("Weather Alert for %s: %s", city, description);
                    otpService.sendWeatherAlert(alertMessage, city, phoneNumber);
                    return String.format("Weather alert sent! %s in %s: %s", weatherCondition, city, description);
                }

                return String.format("Weather in %s: %s - %s", city, weatherCondition, description);
            }

            return "Weather data not available for " + city;

        } catch (Exception e) {
            log.error("Error fetching weather data for city: {}", city, e);
            throw new IOException("Failed to fetch weather data: " + e.getMessage());
        }
    }
}
