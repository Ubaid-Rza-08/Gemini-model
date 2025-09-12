package com.sih.farmer.security;

import com.sih.farmer.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class AuthUtil {
    @Value("${jwt.secret}")
    private String jwtSecretKey;
    @Value("${jwt.access-token-ms}")
    private long accessTokenMillis;
    @Value("${jwt.refresh-token-ms}")
    private long refreshTokenMillis;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("phone", user.getPhone());
        claims.put("name", user.getName());

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(user.getPhone()) // Use phone as subject
                .issuer("auth-service")
                .audience().add("auth-service-backend").and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenMillis))
                .and()
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(UserEntity user, String refreshJti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("phone", user.getPhone());
        claims.put("name", user.getName());
        claims.put("refreshJti", refreshJti);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getPhone())
                .issuer("auth-service")
                .audience().add("auth-service-backend").and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenMillis))
                .signWith(getSecretKey())
                .compact();
    }

    public String getUserIdFromToken(String token) throws Exception {
        if(isTokenExpired(token)){
            throw new Exception("Token expired");
        }
        Claims claims = extractAllClaims(token);
        return claims.get("userId", String.class);
    }

    public String getPhoneFromToken(String token) throws Exception {
        if(isTokenExpired(token)){
            throw new Exception("Token expired");
        }
        Claims claims = extractAllClaims(token);
        return claims.get("phone", String.class);
    }

    public Claims extractAllClaims(String token) throws Exception {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new Exception("Invalid or expired token");
        }
    }

    private boolean isTokenExpired(String token) throws Exception {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) throws Exception {
        return extractClaims(token, Claims::getExpiration);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimResolver) throws Exception {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }
}
