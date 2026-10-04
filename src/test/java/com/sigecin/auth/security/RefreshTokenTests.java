package com.sigecin.auth.security;

import com.sigecin.IntegrationTest;
import com.sigecin.auth.service.RefreshTokenService;
import com.sigecin.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renovación transparente, rotación, detección de reutilización y revocación de refresh tokens. */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class RefreshTokenTests {

    private static final String ACCESS = "SIGECIN_TOKEN";
    private static final String REFRESH = "SIGECIN_REFRESH";
    private static final String BUSINESS_EMAIL = "barberia.estilo@unam.mx";
    private static final String BUSINESS_PASSWORD = "pass5678";
    private static final String DASHBOARD = "/business/dashboard";

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired EntityManager em;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired UserRepository users;

    @Test
    void loginStoresOnlyTheHashOfTheRefreshToken() throws Exception {
        MvcResult login = login();
        Cookie refresh = login.getResponse().getCookie(REFRESH);

        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getMaxAge()).isEqualTo(7 * 24 * 3600);
        assertThat(login.getResponse().getCookie(ACCESS).getMaxAge()).isEqualTo(15 * 60);
        assertThat(jdbc.sql("select count(*) from refresh_token where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(refresh.getValue())).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from refresh_token where token_hash = :raw")
                .param("raw", refresh.getValue()).query(Long.class).single()).isZero();
    }

    @Test
    void missingAccessTokenIsRenewedTransparentlyAndRefreshRotates() throws Exception {
        Cookie refresh = login().getResponse().getCookie(REFRESH);

        MvcResult renewed = mvc.perform(get(DASHBOARD).cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS))
                .andReturn();

        Cookie rotated = renewed.getResponse().getCookie(REFRESH);
        assertThat(rotated.getValue()).isNotEqualTo(refresh.getValue());
        assertThat(revokedAndReplaced(refresh)).isTrue();
        // El token nuevo también funciona
        mvc.perform(get(DASHBOARD).cookie(rotated)).andExpect(status().isOk());
    }

    @Test
    void expiredAccessTokenIsRenewed() throws Exception {
        Cookie refresh = login().getResponse().getCookie(REFRESH);

        mvc.perform(get(DASHBOARD).cookie(expiredAccessToken(), refresh))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS))
                .andExpect(cookie().exists(REFRESH));
    }

    @Test
    void reusedTokenWithinGraceIsAcceptedWithoutRotatingAgain() throws Exception {
        Cookie refresh = login().getResponse().getCookie(REFRESH);
        mvc.perform(get(DASHBOARD).cookie(refresh)).andExpect(status().isOk());

        // Segunda pestaña con el token anterior, justo después de la rotación
        mvc.perform(get(DASHBOARD).cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ACCESS))
                .andExpect(cookie().doesNotExist(REFRESH));
    }

    @Test
    void reusedTokenAfterGraceRevokesTheWholeFamily() throws Exception {
        Cookie refresh = login().getResponse().getCookie(REFRESH);
        Cookie rotated = mvc.perform(get(DASHBOARD).cookie(refresh))
                .andReturn().getResponse().getCookie(REFRESH);
        // La rotación ocurrió hace un minuto: fuera de la ventana de gracia (30 s)
        jdbc.sql("update refresh_token set revoked_at = :past where token_hash = :hash")
                .param("past", LocalDateTime.now().minusMinutes(1))
                .param("hash", RefreshTokenService.hash(refresh.getValue()))
                .update();
        em.clear();

        mvc.perform(get(DASHBOARD).cookie(refresh))
                .andExpect(redirectedUrl("/login?expired&redirect=" + DASHBOARD))
                .andExpect(cookie().maxAge(REFRESH, 0));
        // El token legítimo más reciente también quedó revocado
        mvc.perform(get(DASHBOARD).cookie(rotated))
                .andExpect(redirectedUrl("/login?expired&redirect=" + DASHBOARD));
    }

    @Test
    void expiredRefreshTokenEndsTheSession() throws Exception {
        Cookie refresh = login().getResponse().getCookie(REFRESH);
        jdbc.sql("update refresh_token set expires_at = :past where token_hash = :hash")
                .param("past", LocalDateTime.now().minusMinutes(1))
                .param("hash", RefreshTokenService.hash(refresh.getValue()))
                .update();
        em.clear();

        mvc.perform(get(DASHBOARD).cookie(refresh))
                .andExpect(redirectedUrl("/login?expired&redirect=" + DASHBOARD));
    }

    @Test
    void failedRenewalOnPublicPageContinuesAsAnonymous() throws Exception {
        mvc.perform(get("/businesses").cookie(expiredAccessToken(), new Cookie(REFRESH, "token-desconocido")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(ACCESS, 0))
                .andExpect(cookie().maxAge(REFRESH, 0));
    }

    @Test
    void deactivatedAccountCannotRenew() throws Exception {
        Cookie refresh = login().getResponse().getCookie(REFRESH);
        users.findByEmail(BUSINESS_EMAIL).orElseThrow().setActive(false);
        em.flush();
        em.clear();

        mvc.perform(get(DASHBOARD).cookie(refresh))
                .andExpect(redirectedUrl("/login?expired&redirect=" + DASHBOARD));
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        MvcResult login = login();
        Cookie access = login.getResponse().getCookie(ACCESS);
        Cookie refresh = login.getResponse().getCookie(REFRESH);

        mvc.perform(post("/logout").with(csrf()).cookie(access, refresh))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge(REFRESH, 0));
        em.clear();

        mvc.perform(get(DASHBOARD).cookie(refresh))
                .andExpect(redirectedUrl("/login?expired&redirect=" + DASHBOARD));
    }

    private MvcResult login() throws Exception {
        return mvc.perform(post("/login").with(csrf())
                        .param("email", BUSINESS_EMAIL)
                        .param("password", BUSINESS_PASSWORD))
                .andExpect(redirectedUrl(DASHBOARD))
                .andReturn();
    }

    private boolean revokedAndReplaced(Cookie refresh) {
        em.flush();
        return jdbc.sql("select revoked_at is not null and replaced_by_id is not null from refresh_token where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(refresh.getValue()))
                .query(Boolean.class).single();
    }

    /** JWT firmado correctamente pero vencido hace 5 minutos (más que la tolerancia de 60 s). */
    private Cookie expiredAccessToken() {
        Instant past = Instant.now().minus(20, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sigecin").subject("5")
                .issuedAt(past).expiresAt(past.plus(15, ChronoUnit.MINUTES))
                .claim("role", "BUSINESS").claim("name", "Barbería El Estilo").claim("email", BUSINESS_EMAIL)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Cookie(ACCESS, token);
    }
}
