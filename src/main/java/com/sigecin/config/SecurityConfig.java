package com.sigecin.config;

import com.sigecin.auth.AuthCookies;
import com.sigecin.auth.CookieBearerTokenResolver;
import com.sigecin.auth.JwtService;
import com.sigecin.auth.LoginRedirectEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Autenticación sin estado: el JWT viaja en una cookie HttpOnly y se valida en
 * cada petición. Como la autenticación va en cookie, CSRF permanece habilitado
 * (también guardado en cookie, sin sesión).
 * <p>
 * El filtro del JWT se registra a mano en lugar de usar {@code oauth2ResourceServer()}:
 * ese DSL excluye de CSRF toda petición con token, lo cual es correcto para el
 * encabezado Authorization pero dejaría sin protección a la cookie.
 */
@Configuration
public class SecurityConfig {

    // Rutas donde no se lee la cookie del JWT (ver CookieBearerTokenResolver)
    private static final String[] TOKEN_IGNORED = {
            "/login", "/register", "/vendor/**", "/webjars/**", "/css/**", "/js/**", "/img/**", "/favicon.ico"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthCookies cookies, JwtService jwtService,
                                            JwtDecoder jwtDecoder, AuthProperties properties) throws Exception {
        var entryPoint = new LoginRedirectEntryPoint(cookies);
        // sendError(403) para que se muestre templates/error/403.html
        var accessDenied = new AccessDeniedHandlerImpl();

        var jwtProvider = new JwtAuthenticationProvider(jwtDecoder);
        jwtProvider.setJwtAuthenticationConverter(jwtService::toAuthentication);
        var jwtFilter = new BearerTokenAuthenticationFilter(new ProviderManager(jwtProvider));
        jwtFilter.setBearerTokenResolver(new CookieBearerTokenResolver(cookies, ignoredByTokenResolver()));
        // Token vencido o inválido: se borra la cookie y se redirige a /login?expired
        jwtFilter.setAuthenticationEntryPoint(entryPoint);

        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/business/**").hasRole("BUSINESS")
                        .requestMatchers("/appointments/**", "/favorites/**").hasRole("CLIENT")
                        .anyRequest().permitAll())
                .addFilterAfter(jwtFilter, LogoutFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository(properties)))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler((request, response, auth) -> cookies.clear(response))
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
