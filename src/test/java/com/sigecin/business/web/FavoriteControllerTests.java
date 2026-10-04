package com.sigecin.business.web;

import com.sigecin.IntegrationTest;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.support.AuthTestSupport;
import com.sigecin.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Favoritos del cliente: listar, marcar, quitar, negocios no disponibles y control de acceso. */
@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
@Transactional
class FavoriteControllerTests {

    // Carlos (cliente 1) tiene como favoritos la Barbería (1) y la Clínica (3)
    private static final String CARLOS = "carlos.ramirez@unam.mx";

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired EntityManager em;

    @Test
    void listShowsClientFavorites() throws Exception {
        mvc.perform(get("/favorites").cookie(auth.cookieFor(CARLOS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Barbería El Estilo")))
                .andExpect(content().string(containsString("Clínica Dental Sonrisa")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("No disponible"))));
    }

    @Test
    void addFromDetailReturnsToDetailAndIsIdempotent() throws Exception {
        Cookie carlos = auth.cookieFor(CARLOS);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/favorites/2").with(csrf()).cookie(carlos).param("redirect", "/businesses/2"))
                    .andExpect(redirectedUrl("/businesses/2"))
                    .andExpect(flash().attribute("successKey", "favorite.added"));
            em.flush();
        }
        assertThat(users.isFavorite(1L, 2L)).isTrue();
        assertThat(users.findFavorites(1L)).hasSize(3);
        mvc.perform(get("/businesses/2").cookie(carlos))
                .andExpect(content().string(containsString("En favoritos")));
    }

    @Test
    void removeDeletesFavorite() throws Exception {
        mvc.perform(post("/favorites/1/remove").with(csrf()).cookie(auth.cookieFor(CARLOS)))
                .andExpect(redirectedUrl("/favorites"))
                .andExpect(flash().attribute("successKey", "favorite.removed"));
        em.flush();
        assertThat(users.isFavorite(1L, 1L)).isFalse();
    }

    @Test
    void unavailableFavoriteIsKeptAndCanBeRemovedButNotAdded() throws Exception {
        Cookie carlos = auth.cookieFor(CARLOS);
        businesses.findById(3L).orElseThrow().setActive(false);
        em.flush();

        mvc.perform(get("/favorites").cookie(carlos))
                .andExpect(content().string(containsString("No disponible")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/businesses/3\""))));
        // Laura (cliente 2) no puede marcar un negocio que no es visible
        mvc.perform(post("/favorites/3").with(csrf()).cookie(auth.cookieFor("laura.sanchez@unam.mx")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/favorites/3/remove").with(csrf()).cookie(carlos))
                .andExpect(redirectedUrl("/favorites"));
        em.flush();
        assertThat(users.isFavorite(1L, 3L)).isFalse();
    }

    @Test
    void externalRedirectIsIgnored() throws Exception {
        mvc.perform(post("/favorites/2").with(csrf()).cookie(auth.cookieFor(CARLOS))
                        .param("redirect", "//sitio-malicioso.com"))
                .andExpect(redirectedUrl("/favorites"));
    }

    @Test
    void onlyClientsHaveFavorites() throws Exception {
        mvc.perform(get("/favorites").cookie(auth.cookieFor(BARBERIA_OWNER))).andExpect(status().isForbidden());
        mvc.perform(post("/favorites/2").with(csrf()).cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/favorites/2").with(csrf())).andExpect(redirectedUrl("/login"));
    }
}
