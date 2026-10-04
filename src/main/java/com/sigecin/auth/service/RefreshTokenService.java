package com.sigecin.auth.service;

import com.sigecin.auth.entity.RefreshToken;
import com.sigecin.auth.repository.RefreshTokenRepository;
import com.sigecin.config.AuthProperties;
import com.sigecin.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite, rota y revoca refresh tokens.
 * <ul>
 *   <li>Cada uso rota el token: el anterior queda revocado y apunta a su reemplazo.</li>
 *   <li>Si se presenta un token ya rotado fuera de la ventana de gracia, se asume
 *       robo y se revoca toda la familia (el usuario debe iniciar sesión de nuevo).</li>
 *   <li>Dentro de la ventana de gracia (peticiones simultáneas) se acepta sin rotar.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final AuthProperties properties;

    public RefreshTokenService(RefreshTokenRepository tokens, AuthProperties properties) {
        this.tokens = tokens;
        this.properties = properties;
    }

    /**
     * Resultado de una renovación válida. {@code newRefreshToken} es null cuando el token
     * se aceptó por la ventana de gracia: el navegador ya recibió su reemplazo en otra respuesta.
     */
    public record Renewal(User user, String newRefreshToken) {
    }

    /** Inicia una familia nueva al iniciar sesión; devuelve el token en claro para la cookie. */
    @Transactional
    public String create(User user) {
        return issue(user, UUID.randomUUID().toString(), LocalDateTime.now()).raw();
    }

    /**
     * READ COMMITTED evita los gap locks de InnoDB: con REPEATABLE READ, insertar el token
     * nuevo en el índice único podía chocar con el bloqueo que espera una renovación
     * simultánea del mismo token y provocar un deadlock.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<Renewal> renew(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = tokens.findByHashForUpdate(hash(rawToken)).orElse(null);
        if (current == null || current.isExpired(now)) {
            return Optional.empty();
        }
        if (current.isRevoked()) {
            if (current.wasRotatedAfter(now.minus(properties.refreshReuseGrace()))) {
                return Optional.of(new Renewal(current.getUser(), null));
            }
            log.warn("Reutilización de refresh token (familia {}); se revoca la familia", current.getFamilyId());
            tokens.revokeFamily(current.getFamilyId(), now);
            return Optional.empty();
        }
        if (!current.getUser().isActive()) {
            tokens.revokeFamily(current.getFamilyId(), now);
            return Optional.empty();
        }
        Issued next = issue(current.getUser(), current.getFamilyId(), now);
        current.rotateTo(next.token(), now);
        return Optional.of(new Renewal(current.getUser(), next.raw()));
    }

    /** Logout: revoca la familia del token presentado (si existe). */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null) {
            return;
        }
        tokens.findByTokenHash(hash(rawToken))
                .ifPresent(token -> tokens.revokeFamily(token.getFamilyId(), LocalDateTime.now()));
    }

    /** Limpieza diaria de tokens vencidos (ya no sirven ni para detectar reutilización). */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteExpired() {
        int deleted = tokens.deleteExpiredBefore(LocalDateTime.now());
        log.info("Refresh tokens vencidos eliminados: {}", deleted);
    }

    private record Issued(RefreshToken token, String raw) {
    }

    private Issued issue(User user, String familyId, LocalDateTime now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = tokens.save(
                new RefreshToken(user, familyId, hash(raw), now.plus(properties.refreshTokenTtl())));
        return new Issued(token, raw);
    }

    // SHA-256 basta: el token es aleatorio de 256 bits, no una contraseña
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
