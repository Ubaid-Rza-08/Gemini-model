package com.sih.farmer.service;

import com.sih.farmer.dto.RefreshResponseDto;
import com.sih.farmer.dto.SignUpRequestDto;
import com.sih.farmer.dto.SignupResponseDto;
import com.sih.farmer.entity.RefreshToken;
import com.sih.farmer.entity.UserEntity;
import com.sih.farmer.repository.RefreshTokenRepository;
import com.sih.farmer.repository.UserRepository;
import com.sih.farmer.security.AuthUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    @Value("${jwt.refresh-token-ms}")
    private long refreshTokenMillis;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final WeatherService weatherService;

    public SignupResponseDto signup(SignUpRequestDto signupRequestDto) throws IOException, InterruptedException {
        // Check if phone already exists
        UserEntity existingUser = userRepository.findByPhone(signupRequestDto.getPhone());
        if (existingUser != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        UserEntity user = UserEntity.builder()
                .name(signupRequestDto.getName())
                .phone(signupRequestDto.getPhone())
                .local(signupRequestDto.getLocal())
                .area(signupRequestDto.getArea())
                .city(signupRequestDto.getCity())
                .build();

        UserEntity savedUser = userRepository.save(user);
        return new SignupResponseDto(savedUser.getId(), savedUser.getPhone(),savedUser.getCity());
    }

    public RefreshResponseDto refreshAccessToken(String refreshTokenString) throws Exception {
        String userId = authUtil.getUserIdFromToken(refreshTokenString);
        Claims claims = authUtil.extractAllClaims(refreshTokenString);
        String oldJti = claims.get("refreshJti", String.class);
        Date tokenExpiry = claims.getExpiration();

        RefreshToken stored = refreshTokenRepository.findByJti(oldJti);
        if (stored == null) throw new IllegalArgumentException("Refresh token not found");
        if (stored.getRevoked()) {
            revokeAllTokensForUser(userId);
            throw new Exception("Refresh token reuse detected. All tokens revoked. Re-login required.");
        }
        if (tokenExpiry.before(new Date())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new Exception("Refresh token expired");
        }

        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new Exception("User not found"));

        String newRefreshJti = UUID.randomUUID().toString();
        String newRefreshToken = authUtil.generateRefreshToken(user, newRefreshJti);
        String newAccessToken = authUtil.generateAccessToken(user);

        stored.setRevoked(true);
        stored.setReplacedBy(newRefreshJti);
        refreshTokenRepository.save(stored);

        RefreshToken newStored = RefreshToken.builder()
                .jti(newRefreshJti)
                .userId(UUID.fromString(userId))
                .expiresAt(new Date(System.currentTimeMillis() + refreshTokenMillis))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newStored);

        return new RefreshResponseDto(newAccessToken, newRefreshToken);
    }

    private void revokeAllTokensForUser(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(UUID.fromString(userId));
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }

    @Scheduled(cron = "${jwt.cleanup.cron}")
    public void cleanExpired() {
        refreshTokenRepository.findAll().stream()
                .filter(t -> t.getExpiresAt().before(new Date(System.currentTimeMillis())))
                .forEach(t -> refreshTokenRepository.delete(t));
    }
}
