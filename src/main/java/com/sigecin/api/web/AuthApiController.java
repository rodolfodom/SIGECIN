package com.sigecin.api.web;

import com.sigecin.api.dto.LoginRequest;
import com.sigecin.api.dto.RefreshRequest;
import com.sigecin.api.dto.TokenResponse;
import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.auth.service.AuthService;
import com.sigecin.auth.service.JwtService;
import com.sigecin.auth.service.RefreshTokenService;
import com.sigecin.config.AuthProperties;
import com.sigecin.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación de la API REST (pensada para Postman u otro cliente HTTP). A diferencia de
 * las vistas Thymeleaf, aquí no hay cookies: el JWT de acceso se envía en el encabezado
 * {@code Authorization: Bearer ...} y el refresh token viaja en el cuerpo de las peticiones.
 * Usa los mismos servicios que el login web, así que la rotación y la detección de
 * reutilización del refresh token son idénticas.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthApiController {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final AuthProperties properties;

    /** Credenciales inválidas: 401; cuenta inactiva: 403 (ver ApiExceptionHandler). */
    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticate(request.email(), request.password());
        log.info("Login por API del usuario {}", user.getId());
        return tokens(user, refreshTokens.create(user));
    }

    /** Rota el refresh token: el enviado queda revocado y se devuelve uno nuevo junto con otro JWT. */
    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokens.renew(request.refreshToken())
                .map(renewal -> tokens(renewal.user(), renewal.newRefreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido, vencido o revocado"));
    }

    /** Revoca la familia del refresh token. El JWT de acceso sigue siendo válido hasta que vence. */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Usuario dueño del JWT enviado. */
    @GetMapping("/me")
    public AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }

    private TokenResponse tokens(User user, String refreshToken) {
        return new TokenResponse(TOKEN_TYPE, jwtService.issueToken(user), properties.accessTokenTtl().toSeconds(),
                refreshToken, refreshToken == null ? null : properties.refreshTokenTtl().toSeconds());
    }
}
