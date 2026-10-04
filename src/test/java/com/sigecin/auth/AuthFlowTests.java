package com.sigecin.auth;

import com.sigecin.TestcontainersConfiguration;
import com.sigecin.user.Role;
import com.sigecin.user.User;
import com.sigecin.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Registro, inicio y cierre de sesión, y control de acceso por rol con el JWT en cookie. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTests {

    private static final String TOKEN_COOKIE = "SIGECIN_TOKEN";
    // Datos de prueba de ddl_sigecin.sql
    private static final String CLIENT_EMAIL = "carlos.ramirez@unam.mx";
    private static final String CLIENT_PASSWORD = "pass1234";
    private static final String BUSINESS_EMAIL = "barberia.estilo@unam.mx";
    private static final String BUSINESS_PASSWORD = "pass5678";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    // --- Registro ---

    @Test
    void registerCreatesUserWithHashedPassword() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ana Pérez")
                        .param("email", "  Ana.Perez@Ejemplo.MX ")
                        .param("password", "secreta123")
                        .param("confirmPassword", "secreta123")
                        .param("role", "BUSINESS"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successKey", "auth.register.success"));

        User ana = users.findByEmail("ana.perez@ejemplo.mx").orElseThrow();
        assertThat(ana.getRole()).isEqualTo(Role.BUSINESS);
        assertThat(ana.getPassword()).startsWith("$2a$").isNotEqualTo("secreta123");
        assertThat(passwordEncoder.matches("secreta123", ana.getPassword())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Otro Carlos")
                        .param("email", CLIENT_EMAIL.toUpperCase())
                        .param("password", "secreta123")
                        .param("confirmPassword", "secreta123")
                        .param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrorCode("registerForm", "email", "auth.register.email-taken"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Este correo ya está registrado.")));
    }

    @Test
    void registerValidatesFields() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "")
                        .param("email", "no-es-correo")
                        .param("password", "corta")
                        .param("confirmPassword", "distinta")
                        .param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerForm", "fullName", "email", "password"))
                .andExpect(model().attributeHasFieldErrorCode("registerForm", "confirmPassword", "auth.register.password-mismatch"));
    }

    // --- Inicio de sesión ---

    @Test
    void clientLoginSetsHttpOnlyCookieAndGoesToBusinesses() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", CLIENT_EMAIL)
                        .param("password", CLIENT_PASSWORD))
                .andExpect(redirectedUrl("/businesses"))
                .andExpect(cookie().exists(TOKEN_COOKIE))
                .andExpect(cookie().httpOnly(TOKEN_COOKIE, true))
                .andExpect(cookie().sameSite(TOKEN_COOKIE, "Lax"));
    }

    @Test
    void businessLoginGoesToDashboard() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", BUSINESS_EMAIL)
                        .param("password", BUSINESS_PASSWORD))
                .andExpect(redirectedUrl("/business/dashboard"));
    }

    @Test
    void loginReturnsToOriginalPageButNotToExternalSites() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", CLIENT_EMAIL).param("password", CLIENT_PASSWORD)
                        .param("redirect", "/businesses/1/availability?serviceId=1"))
                .andExpect(redirectedUrl("/businesses/1/availability?serviceId=1"));
        mvc.perform(post("/login").with(csrf())
                        .param("email", CLIENT_EMAIL).param("password", CLIENT_PASSWORD)
                        .param("redirect", "//sitio-malicioso.com"))
                .andExpect(redirectedUrl("/businesses"));
    }

    @Test
    void wrongPasswordAndUnknownEmailShowSameGenericMessage() throws Exception {
        for (String email : new String[]{CLIENT_EMAIL, "nadie@unam.mx"}) {
            mvc.perform(post("/login").with(csrf())
                            .param("email", email)
                            .param("password", "incorrecta"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("errorKey", "auth.login.bad-credentials"))
                    .andExpect(cookie().doesNotExist(TOKEN_COOKIE));
        }
    }

    @Test
    void inactiveAccountCannotLogIn() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", "sofia.herrera@unam.mx")
                        .param("password", CLIENT_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorKey", "auth.login.disabled"))
                .andExpect(cookie().doesNotExist(TOKEN_COOKIE));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/login")
                        .param("email", CLIENT_EMAIL)
                        .param("password", CLIENT_PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedPostWithoutCsrfTokenIsRejected() throws Exception {
        // La cookie del JWT no debe eximir de CSRF (a diferencia del encabezado Authorization)
        Cookie client = loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        mvc.perform(post("/logout").cookie(client))
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist(TOKEN_COOKIE));
    }

    // --- Control de acceso ---

    @Test
    void protectedPageWithoutSessionRedirectsToLoginKeepingTheTarget() throws Exception {
        mvc.perform(get("/appointments?status=PENDING"))
                .andExpect(redirectedUrl("/login?redirect=/appointments?status%3DPENDING"));
    }

    @Test
    void tokenGivesAccessAccordingToRole() throws Exception {
        Cookie client = loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);
        Cookie business = loginAs(BUSINESS_EMAIL, BUSINESS_PASSWORD);

        mvc.perform(get("/business/dashboard").cookie(business)).andExpect(status().isOk());
        mvc.perform(get("/business/dashboard").cookie(client))
                .andExpect(status().isForbidden());
        mvc.perform(get("/businesses").cookie(client))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Carlos Ramírez Torres")));
    }

    @Test
    void invalidTokenClearsCookieAndWarnsSessionExpired() throws Exception {
        mvc.perform(get("/business/dashboard").cookie(new Cookie(TOKEN_COOKIE, "token.alterado.invalido")))
                .andExpect(redirectedUrl("/login?expired&redirect=/business/dashboard"))
                .andExpect(cookie().maxAge(TOKEN_COOKIE, 0));
    }

    @Test
    void logoutClearsCookie() throws Exception {
        Cookie client = loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        mvc.perform(post("/logout").with(csrf()).cookie(client))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge(TOKEN_COOKIE, 0));
    }

    @Test
    void flashMessageSurvivesTheRedirectWithoutSession() throws Exception {
        MvcResult registered = mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Luis Gómez")
                        .param("email", "luis.gomez@ejemplo.mx")
                        .param("password", "secreta123")
                        .param("confirmPassword", "secreta123")
                        .param("role", "CLIENT"))
                .andExpect(redirectedUrl("/login"))
                .andReturn();
        assertThat(registered.getRequest().getSession(false)).isNull();
        Cookie flash = registered.getResponse().getCookie("SIGECIN_FLASH");
        assertThat(flash).isNotNull();

        mvc.perform(get("/login").cookie(flash))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tu cuenta se creó correctamente")))
                .andExpect(cookie().maxAge("SIGECIN_FLASH", 0));
    }

    @Test
    void unknownPageShowsCustom404() throws Exception {
        mvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound());
    }

    private Cookie loginAs(String email, String password) throws Exception {
        return mvc.perform(post("/login").with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andReturn().getResponse().getCookie(TOKEN_COOKIE);
    }
}
