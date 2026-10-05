package com.sigecin.config;

import com.sigecin.auth.security.AuthCookies;
import com.sigecin.auth.security.CookieBearerTokenResolver;
import com.sigecin.auth.security.LoginRedirectEntryPoint;
import com.sigecin.auth.security.RefreshTokenFilter;
import com.sigecin.auth.service.JwtService;
import com.sigecin.auth.service.RefreshTokenService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

/**
 * Autenticación sin estado: un JWT de acceso de corta duración viaja en una cookie
 * HttpOnly y se valida en cada petición; al vencer, {@link RefreshTokenFilter} lo
 * renueva con el refresh token (otra cookie HttpOnly, guardado como hash en la BD).
 * Como la autenticación va en cookie, CSRF permanece habilitado (también en cookie).
 * Las rutas /api/** tienen su propia cadena, con el JWT en el encabezado Authorization.
 * <p>
 * El filtro del JWT se registra a mano en lugar de usar {@code oauth2ResourceServer()}:
 * ese DSL excluye de CSRF toda petición con token, lo cual es correcto para el
 * encabezado Authorization pero dejaría sin protección a la cookie.
 */
@Configuration
public class SecurityConfig {

    // Rutas donde no se leen las cookies de sesión (ver CookieBearerTokenResolver y RefreshTokenFilter)
    private static final String[] TOKEN_IGNORED = {
            "/login", "/register", "/vendor/**", "/webjars/**", "/css/**", "/js/**", "/img/**", "/favicon.ico"
    };

    /**
     * API REST para Postman u otros clientes: el JWT llega en el encabezado
     * {@code Authorization: Bearer} (no en cookie) y lo valida el resource server de Spring.
     * CSRF se desactiva solo aquí, porque el navegador nunca agrega ese encabezado por su cuenta.
     * Va primero ({@code @Order(1)}): las rutas /api/** no pasan por la cadena de las vistas.
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/businesses/**").permitAll()
                        .requestMatchers("/api/client/**").hasRole("CLIENT")
                        .requestMatchers("/api/business/**").hasRole("BUSINESS")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtService::toAuthentication)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    /** Vistas Thymeleaf: JWT y refresh token en cookies HttpOnly, con CSRF. */
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthCookies cookies, JwtService jwtService,
                                            JwtDecoder jwtDecoder, RefreshTokenService refreshTokens,
                                            AuthProperties properties) throws Exception {
        var entryPoint = new LoginRedirectEntryPoint(cookies);
        // sendError(403) para que se muestre templates/error/403.html
        var accessDenied = new AccessDeniedHandlerImpl();

        var jwtProvider = new JwtAuthenticationProvider(jwtDecoder);
        jwtProvider.setJwtAuthenticationConverter(jwtService::toAuthentication);
        var jwtFilter = new BearerTokenAuthenticationFilter(new ProviderManager(jwtProvider));
        jwtFilter.setBearerTokenResolver(new CookieBearerTokenResolver(cookies, ignoredByTokenResolver()));
        var refreshFilter = new RefreshTokenFilter(cookies, jwtDecoder, jwtService, refreshTokens,
                ignoredByTokenResolver());
        // Token vencido o inválido: se borra la cookie y se redirige a /login?expired
        jwtFilter.setAuthenticationEntryPoint(entryPoint);

        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/business/**").hasRole("BUSINESS")
                        .requestMatchers("/appointments/**", "/favorites/**").hasRole("CLIENT")
                        .anyRequest().permitAll())
                .addFilterAfter(refreshFilter, LogoutFilter.class)
                .addFilterAfter(jwtFilter, RefreshTokenFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository(properties)))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler((request, response, auth) -> {
                            refreshTokens.revoke(cookies.readRefresh(request));
                            cookies.clearAll(response);
                        })
                        .logoutSuccessUrl("/login?logout"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static RequestMatcher ignoredByTokenResolver() {
        var builder = PathPatternRequestMatcher.withDefaults();
        return new OrRequestMatcher(Arrays.stream(TOKEN_IGNORED).map(builder::matcher).toArray(RequestMatcher[]::new));
    }

    private static CookieCsrfTokenRepository csrfTokenRepository(AuthProperties properties) {
        var repository = new CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie -> cookie.secure(properties.cookieSecure()).sameSite("Lax"));
        return repository;
    }
}
