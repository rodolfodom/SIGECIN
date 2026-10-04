package com.sigecin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuración de la autenticación con JWT de acceso y refresh token en cookies (prefijo sigecin.auth). */
@ConfigurationProperties("sigecin.auth")
public record AuthProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration refreshReuseGrace,
        String accessCookieName,
        String refreshCookieName,
        boolean cookieSecure) {

    public AuthProperties {
        // HS256 exige una llave de al menos 256 bits
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("sigecin.auth.jwt-secret debe tener al menos 32 caracteres");
        }
    }
}
