package com.sigecin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Redirige a /login cuando se necesita sesión. Si la petición traía un token
 * (vencido o inválido) se borra la cookie y se avisa con ?expired. En peticiones
 * GET se conserva la página original en ?redirect para volver a ella tras el login.
 */
public class LoginRedirectEntryPoint implements AuthenticationEntryPoint {

    private final AuthCookies cookies;

    public LoginRedirectEntryPoint(AuthCookies cookies) {
        this.cookies = cookies;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        UriComponentsBuilder target = UriComponentsBuilder.fromPath(request.getContextPath() + "/login");
        if (cookies.read(request) != null) {
            cookies.clear(response);
            target.queryParam("expired");
        }
        if (HttpMethod.GET.matches(request.getMethod())) {
            String original = request.getRequestURI().substring(request.getContextPath().length());
            if (request.getQueryString() != null) {
                original += "?" + request.getQueryString();
            }
            target.queryParam("redirect", original);
        }
        response.sendRedirect(target.encode().build().toUriString());
    }
}
