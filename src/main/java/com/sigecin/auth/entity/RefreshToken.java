package com.sigecin.auth.entity;

import com.sigecin.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Refresh token de una sesión. Solo se guarda el hash del valor; el token en claro
 * existe únicamente en la cookie del navegador.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Todos los tokens de un mismo inicio de sesión comparten familia
    @Column(name = "family_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String familyId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // Token que lo reemplazó al rotar; vacío si se revocó por logout o por reutilización
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    private RefreshToken replacedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String familyId, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Se reemplazó al rotar (no por logout ni por robo) después de {@code limit}. */
    public boolean wasRotatedAfter(LocalDateTime limit) {
        return replacedBy != null && revokedAt.isAfter(limit);
    }

    public void rotateTo(RefreshToken successor, LocalDateTime now) {
        this.replacedBy = successor;
        this.revokedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public RefreshToken getReplacedBy() {
        return replacedBy;
    }
}
