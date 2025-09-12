package com.sih.farmer.repository;

import com.sih.farmer.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by JTI (JWT ID)
     */
    RefreshToken findByJti(String jti);

    /**
     * Find all refresh tokens for a specific user
     */
    List<RefreshToken> findByUserId(UUID userId);

    /**
     * Find all non-revoked refresh tokens for a specific user
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Find all expired refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :currentDate")
    List<RefreshToken> findExpiredTokens(@Param("currentDate") Date currentDate);

    /**
     * Find all revoked refresh tokens
     */
    List<RefreshToken> findByRevokedTrue();

    /**
     * Delete all expired refresh tokens (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :currentDate")
    void deleteExpiredTokens(@Param("currentDate") Date currentDate);

    /**
     * Revoke all tokens for a specific user
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllTokensForUser(@Param("userId") UUID userId, @Param("revokedAt") Date revokedAt);

    /**
     * Count active (non-revoked, non-expired) tokens for a user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :currentDate")
    long countActiveTokensForUser(@Param("userId") UUID userId, @Param("currentDate") Date currentDate);

    /**
     * Check if a JTI exists and is not revoked
     */
    boolean existsByJtiAndRevokedFalse(String jti);

    /**
     * Find refresh token by JTI that is not revoked and not expired
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.jti = :jti AND rt.revoked = false AND rt.expiresAt > :currentDate")
    RefreshToken findValidTokenByJti(@Param("jti") String jti, @Param("currentDate") Date currentDate);
}
