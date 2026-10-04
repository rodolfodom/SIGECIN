package com.sigecin.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Obtiene el JWT de la cookie en lugar del encabezado Authorization.
 * En las rutas ignoradas (login, registro, recursos estáticos) no lee la cookie,
 * para que un token vencido no interrumpa el inicio de sesión ni la carga de CSS/JS.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final AuthCookies cookies;
    private final RequestMatcher ignored;

    public CookieBearerTokenResolver(AuthCookies cookies, RequestMatcher ignored) {
        this.cookies = cookies;
        this.ignored = ignored;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        return ignored.matches(request) ? null : cookies.read(request);
    }
}
