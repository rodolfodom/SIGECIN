package com.sigecin.business.web;

import com.sigecin.IntegrationTest;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.support.AuthTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Búsqueda y detalle públicos: filtros, paginación y regla de visibilidad. */
@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
@Transactional
class BusinessDirectoryControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired BusinessRepository businesses;
    @Autowired EntityManager em;

    @Test
    void homeRedirectsToSearchThatListsVisibleBusinessesWithoutSession() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/businesses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("3 negocios encontrados")))
                .andExpect(content().string(containsString("Barbería El Estilo")))
                .andExpect(content().string(containsString("Spa Serenidad")))
                .andExpect(content().string(containsString("Clínica Dental Sonrisa")));
    }

    @Test
    void searchFiltersByNameAndCategory() throws Exception {
        assertThat(names(mvc.perform(get("/businesses").param("name", "  SPA ")).andReturn()))
                .containsExactly("Spa Serenidad");
        assertThat(names(mvc.perform(get("/businesses").param("categoryId", "4")).andReturn()))
                .containsExactly("Clínica Dental Sonrisa");
        assertThat(names(mvc.perform(get("/businesses").param("name", "spa").param("categoryId", "4")).andReturn()))
                .isEmpty();
    }

    @Test
    void likeWildcardsAreSearchedLiterally() throws Exception {
        for (String term : new String[]{"%", "_", "!"}) {
            mvc.perform(get("/businesses").param("name", term))
                    .andExpect(content().string(containsString("No encontramos negocios")));
        }
    }

    @Test
    void resultsArePaginatedKeepingFilters() throws Exception {
        MvcResult first = mvc.perform(get("/businesses").param("size", "2")).andReturn();
        assertThat(names(first)).containsExactly("Barbería El Estilo", "Clínica Dental Sonrisa");
        assertThat(first.getResponse().getContentAsString()).contains("page=1");

        assertThat(names(mvc.perform(get("/businesses").param("size", "2").param("page", "1")).andReturn()))
                .containsExactly("Spa Serenidad");
        // Página negativa o tamaño fuera de rango se ajustan en lugar de fallar
        mvc.perform(get("/businesses").param("page", "-3").param("size", "1000")).andExpect(status().isOk());
    }

    @Test
    void detailShowsScheduleAndOnlyActiveServices() throws Exception {
        mvc.perform(get("/businesses/2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Masaje relajante")))
                .andExpect(content().string(not(containsString("Manicure + pedicure"))))   // inactivo
                .andExpect(content().string(containsString("10:00 – 20:00")))
                .andExpect(content().string(containsString("/businesses/2/availability?serviceId=4")))
                .andExpect(content().string(containsString("Inicia sesión para guardar en favoritos")));
    }

    @Test
    void invisibleOrUnknownBusinessIsNotFound() throws Exception {
        businesses.findById(2L).orElseThrow().setActive(false);
        em.flush();

        mvc.perform(get("/businesses/2")).andExpect(status().isNotFound());
        mvc.perform(get("/businesses/999")).andExpect(status().isNotFound());
        mvc.perform(get("/businesses")).andExpect(content().string(not(containsString("Spa Serenidad"))));
    }

    @Test
    void businessOwnerSeesDetailWithoutBookingOrFavoriteButtons() throws Exception {
        mvc.perform(get("/businesses/1").cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Reservar"))))
                .andExpect(content().string(not(containsString("favoritos"))));
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<String> names(MvcResult result) {
        Page<com.sigecin.business.entity.Business> page =
                (Page<com.sigecin.business.entity.Business>) result.getModelAndView().getModel().get("results");
        return page.getContent().stream().map(com.sigecin.business.entity.Business::getName).toList();
    }
}
