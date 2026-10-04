package com.sigecin.auth.security;

import com.sigecin.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/** Escribe, lee y borra las cookies HttpOnly del JWT de acceso y del refresh token. */
@Component
public class AuthCookies {

    private final AuthProperties properties;

    public AuthCookies(AuthProperties properties) {
        this.properties = properties;
    }

    public void writeAccess(HttpServletResponse response, String token) {
        add(response, properties.accessCookieName(), token, properties.accessTokenTtl());
    }

    public void writeRefresh(HttpServletResponse response, String token) {
        add(response, properties.refreshCookieName(), token, properties.refreshTokenTtl());
    }

    public void clearAccess(HttpServletResponse response) {
        add(response, properties.accessCookieName(), "", Duration.ZERO);
    }

    public void clearAll(HttpServletResponse response) {
        clearAccess(response);
        add(response, properties.refreshCookieName(), "", Duration.ZERO);
    }

    public String readAccess(HttpServletRequest request) {
        return read(request, properties.accessCookieName());
    }

    public String readRefresh(HttpServletRequest request) {
        return read(request, properties.refreshCookieName());
    }

    private static String read(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }

    private void add(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
