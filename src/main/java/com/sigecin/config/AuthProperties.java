package com.sigecin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuración de la autenticación con JWT en cookie (prefijo sigecin.auth). */
@ConfigurationProperties("sigecin.auth")
public record AuthProperties(
        String jwtSecret,
        Duration tokenTtl,
        String cookieName,
        boolean cookieSecure) {

    public AuthProperties {
        // HS256 exige una llave de al menos 256 bits
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("sigecin.auth.jwt-secret debe tener al menos 32 caracteres");
        }
    }
}
