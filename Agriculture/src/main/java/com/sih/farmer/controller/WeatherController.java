package com.sih.farmer.controller;

import com.sih.farmer.security.AuthUtil;
import com.sih.farmer.service.WeatherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class WeatherController {

    private final WeatherService weatherService;
    private final AuthUtil authUtil;

    @GetMapping("/location")
    public String searchWeather(@RequestParam String city, HttpServletRequest request)
            throws Exception {
        log.info("Searching weather for city: {}", city);

        // Extract JWT token from request
        String token = getTokenFromRequest(request);

        // Get phone number from token
        String phoneNumber = authUtil.getPhoneFromToken(token);

        return weatherService.getWeatherByCity(city, phoneNumber);
    }

    private String getTokenFromRequest(HttpServletRequest request) throws RuntimeException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("No valid token found");
    }
}
