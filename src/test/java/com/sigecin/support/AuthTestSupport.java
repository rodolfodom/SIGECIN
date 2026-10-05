package com.sigecin.support;

import com.sigecin.auth.service.JwtService;
import com.sigecin.user.entity.User;
import com.sigecin.user.enums.Role;
import com.sigecin.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Component;

/** Atajos de autenticación para pruebas con MockMvc: emite el JWT sin pasar por /login. */
@Component
public class AuthTestSupport {

    public static final String ACCESS_COOKIE = "SIGECIN_TOKEN";

    // Dueños de negocio de ddl_sigecin.sql
    public static final String BARBERIA_OWNER = "barberia.estilo@unam.mx";
    public static final String SPA_OWNER = "spa.serenidad@unam.mx";
    public static final String CLINICA_OWNER = "clinica.sonrisa@unam.mx";

    private final JwtService jwtService;
    private final UserRepository users;

    public AuthTestSupport(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    public Cookie cookieFor(String email) {
        return cookieFor(users.findByEmail(email).orElseThrow());
    }

    public Cookie cookieFor(User user) {
        return new Cookie(ACCESS_COOKIE, jwtService.issueToken(user));
    }

    /** Valor del encabezado Authorization para la API REST. */
    public String bearerFor(String email) {
        return "Bearer " + jwtService.issueToken(users.findByEmail(email).orElseThrow());
    }

    /** Usuario BUSINESS recién registrado, todavía sin negocio. */
    public User newBusinessOwner(String email) {
        return users.save(new User(Role.BUSINESS, "Dueño Nuevo", email, "$2a$10$sinUsoEnEstasPruebas"));
    }
}
