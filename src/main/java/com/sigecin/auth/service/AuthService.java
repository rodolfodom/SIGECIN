package com.sigecin.auth.service;

import com.sigecin.auth.exception.EmailAlreadyRegisteredException;
import com.sigecin.auth.web.form.RegisterForm;
import com.sigecin.user.entity.User;
import com.sigecin.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    // Hash de referencia para que un correo inexistente tarde lo mismo que uno válido
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("sigecin-dummy-password");
    }

    @Transactional
    public User register(RegisterForm form) {
        String email = normalizeEmail(form.getEmail());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(form.getRole(), form.getFullName().trim(), email,
                passwordEncoder.encode(form.getPassword()));
        try {
            User saved = users.saveAndFlush(user);
            log.info("Usuario {} registrado con rol {}", saved.getId(), saved.getRole());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Otro registro con el mismo correo se adelantó (uq_user_email)
            throw new EmailAlreadyRegisteredException();
        }
    }

    /**
     * Verifica las credenciales. Lanza BadCredentialsException si el correo o la
     * contraseña no coinciden (mensaje genérico) y DisabledException si la cuenta
     * está inactiva.
     */
    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        User user = users.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            log.warn("Inicio de sesión fallido: correo no registrado");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Inicio de sesión fallido: contraseña incorrecta para el usuario {}", user.getId());
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!user.isActive()) {
            log.warn("Inicio de sesión rechazado: la cuenta {} está inactiva", user.getId());
            throw new DisabledException("Cuenta inactiva");
        }
        log.info("Inicio de sesión del usuario {} ({})", user.getId(), user.getRole());
        return user;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
