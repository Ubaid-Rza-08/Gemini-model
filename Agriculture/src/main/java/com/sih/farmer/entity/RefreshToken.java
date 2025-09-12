package com.sih.farmer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String jti; // JWT ID - unique identifier for the refresh token

    @Column(nullable = false)
    private UUID userId; // Reference to the user who owns this token

    @Column(nullable = false)
    private Date expiresAt; // When this refresh token expires

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false; // Whether this token has been revoked

    @Column
    private String replacedBy; // JTI of the token that replaced this one (for rotation)

    @Column
    private Date createdAt;

    @Column
    private Date revokedAt; // When this token was revoked

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        if (revoked && revokedAt == null) {
            revokedAt = new Date();
        }
    }
}
