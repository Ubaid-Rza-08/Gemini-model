package com.sih.farmer.service;

import com.sih.farmer.config.TwilioConfig;
import com.sih.farmer.dto.LoginResponseDto;
import com.sih.farmer.entity.RefreshToken;
import com.sih.farmer.entity.UserEntity;
import com.sih.farmer.repository.RefreshTokenRepository;
import com.sih.farmer.repository.UserRepository;
import com.sih.farmer.security.AuthUtil;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    @Value("${jwt.refresh-token-ms}")
    private long refreshTokenMillis;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TwilioConfig twilioConfig;
    private final StringRedisTemplate redisTemplate;
    private final AuthUtil authUtil;

    @Value("${otp.expiration.minutes:5}")
    private int otpExpirationMinutes;

    private static final SecureRandom secureRandom = new SecureRandom();

    public String generateAndSendOtp(String phoneNumber) {
        try {
            // Check if user exists with this phone number
            UserEntity user = userRepository.findByPhone(phoneNumber);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Phone number not registered. Please sign up first.");
            }

            String otp = generateOtp();
            sendOtpSms(phoneNumber, otp);

            // Store OTP in Redis with expiration
            redisTemplate.opsForValue().set(phoneNumber, otp, otpExpirationMinutes, TimeUnit.MINUTES);

            log.info("OTP sent successfully to {}", phoneNumber);
            return "OTP sent successfully to " + phoneNumber;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }

    public LoginResponseDto verifyOtpAndGenerateToken(String phoneNumber, String otp) {
        try {
            String storedOtp = redisTemplate.opsForValue().get(phoneNumber);

            if (storedOtp == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired or not found");
            }

            if (!storedOtp.equals(otp)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
            }

            // Get user by phone number
            UserEntity user = userRepository.findByPhone(phoneNumber);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            // Generate tokens
            String accessToken = authUtil.generateAccessToken(user);
            String refreshJti = UUID.randomUUID().toString();
            String refreshToken = authUtil.generateRefreshToken(user, refreshJti);

            // Save refresh token
            RefreshToken r = RefreshToken.builder()
                    .jti(refreshJti)
                    .userId(user.getId())
                    .expiresAt(new Date(System.currentTimeMillis() + refreshTokenMillis))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(r);

            // Delete OTP from Redis after successful verification
            redisTemplate.delete(phoneNumber);

            log.info("OTP verified successfully for {}", phoneNumber);
            return new LoginResponseDto(accessToken, refreshToken, user.getId());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying OTP for {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Error verifying OTP: " + e.getMessage());
        }
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // 6-digit OTP
        return String.valueOf(otp);
    }

    private void sendOtpSms(String phoneNumber, String otp) {
        try {
            String messageBody = String.format(
                    "Hi, your verification code is: %s. This code will expire in %d minutes. " +
                            "Please do not share this code with anyone.",
                    otp, otpExpirationMinutes
            );

            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioConfig.getTwilioPhoneNumber()),
                    messageBody
            ).create();

            log.info("SMS sent successfully to {} with message SID: {}", phoneNumber, message.getSid());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS: " + e.getMessage());
        }
    }



    public String sendWeatherAlert(String alert, String city, String phoneNumber) {
        try {
            String messageBody = String.format(
                    "Weather Alert for %s: %s. Please take necessary precautions. Stay safe!",
                    city, alert
            );

            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioConfig.getTwilioPhoneNumber()),
                    messageBody
            ).create();

            log.info("Weather alert SMS sent successfully to {} with message SID: {}", phoneNumber, message.getSid());
            return "Weather alert sent successfully";

        } catch (Exception e) {
            log.error("Failed to send weather alert SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send weather alert SMS: " + e.getMessage());
        }
    }
}