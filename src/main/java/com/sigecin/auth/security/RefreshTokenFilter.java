package com.sigecin.auth.security;

import com.sigecin.auth.service.JwtService;
import com.sigecin.auth.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Renueva la sesión de forma transparente. Si el JWT de acceso falta o venció y hay
 * un refresh token válido, emite un par nuevo en las cookies y deja el JWT nuevo en
 * la petición para que la autentique el filtro del JWT. Si el refresh token no sirve,
 * borra ambas cookies y marca la sesión como expirada (la petición sigue como anónima).
 */
@Slf4j
public class RefreshTokenFilter extends OncePerRequestFilter {

    /** JWT recién emitido que debe usarse en lugar del de la cookie. */
    static final String RENEWED_ACCESS_TOKEN = RefreshTokenFilter.class.getName() + ".RENEWED";
    /** La sesión no pudo renovarse: se ignora la cookie de acceso y se avisa al redirigir a /login. */
    static final String SESSION_EXPIRED = RefreshTokenFilter.class.getName() + ".EXPIRED";

    private final AuthCookies cookies;
    private final JwtDecoder jwtDecoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final RequestMatcher ignored;

    public RefreshTokenFilter(AuthCookies cookies, JwtDecoder jwtDecoder, JwtService jwtService,
                              RefreshTokenService refreshTokens, RequestMatcher ignored) {
        this.cookies = cookies;
        this.jwtDecoder = jwtDecoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.ignored = ignored;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return ignored.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String refreshToken = cookies.readRefresh(request);
        if (refreshToken != null && !hasValidAccessToken(request)) {
            refreshTokens.renew(refreshToken).ifPresentOrElse(renewal -> {
                String accessToken = jwtService.issueToken(renewal.user());
                cookies.writeAccess(response, accessToken);
                if (renewal.newRefreshToken() != null) {
                    cookies.writeRefresh(response, renewal.newRefreshToken());
                }
                request.setAttribute(RENEWED_ACCESS_TOKEN, accessToken);
                log.debug("JWT de acceso renovado con el refresh token (usuario {})", renewal.user().getId());
            }, () -> {
                log.debug("No se pudo renovar la sesión: se borran las cookies");
                cookies.clearAll(response);
                request.setAttribute(SESSION_EXPIRED, Boolean.TRUE);
            });
        }
        chain.doFilter(request, response);
    }

    private boolean hasValidAccessToken(HttpServletRequest request) {
        String accessToken = cookies.readAccess(request);
        if (accessToken == null) {
            return false;
        }
        try {
            jwtDecoder.decode(accessToken);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
