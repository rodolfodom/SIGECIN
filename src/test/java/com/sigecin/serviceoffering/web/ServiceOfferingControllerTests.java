package com.sigecin.serviceoffering.web;

import com.sigecin.IntegrationTest;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.repository.ServiceOfferingRepository;
import com.sigecin.support.AuthTestSupport;
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
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Servicios del negocio: lista propia, alta, edición, activar/desactivar y pertenencia (404). */
@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
@Transactional
class ServiceOfferingControllerTests {

    // Servicio 4 = "Masaje relajante" del Spa (negocio 2)
    private static final long OTHER_BUSINESS_SERVICE = 4L;

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired ServiceOfferingRepository services;
    @Autowired EntityManager em;

    @Test
    void listShowsOnlyOwnServices() throws Exception {
        mvc.perform(get("/business/services").cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Corte clásico")))
                .andExpect(content().string(containsString("$350.00")))
                .andExpect(content().string(not(containsString("Masaje relajante"))));
    }

    @Test
    void createAddsActiveService() throws Exception {
        mvc.perform(post("/business/services").with(csrf()).cookie(auth.cookieFor(BARBERIA_OWNER))
                        .param("name", " Afeitado clásico ")
                        .param("description", "")
                        .param("durationMin", "25")
                        .param("price", "95.50"))
                .andExpect(redirectedUrl("/business/services"))
                .andExpect(flash().attribute("successKey", "service.created"));

        ServiceOffering created = services.findByBusinessIdOrderByName(1L).stream()
                .filter(s -> s.getName().equals("Afeitado clásico")).findFirst().orElseThrow();
        assertThat(created.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
        assertThat(created.getDescription()).isNull();
        assertThat(created.getPrice()).isEqualByComparingTo("95.50");
    }

    @Test
    void createValidatesFields() throws Exception {
        mvc.perform(post("/business/services").with(csrf()).cookie(auth.cookieFor(BARBERIA_OWNER))
                        .param("name", "")
                        .param("durationMin", "0")
                        .param("price", "10.555"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("serviceForm", "name", "durationMin", "price"))
                .andExpect(content().string(containsString("Usa como máximo 2 decimales.")));
        mvc.perform(post("/business/services").with(csrf()).cookie(auth.cookieFor(BARBERIA_OWNER))
                        .param("name", "Corte").param("durationMin", "30").param("price", "-1"))
                .andExpect(model().attributeHasFieldErrorCode("serviceForm", "price", "DecimalMin"));
    }

    @Test
    void editUpdatesOwnService() throws Exception {
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);
        mvc.perform(get("/business/services/1/edit").cookie(owner))
                .andExpect(model().attribute("serviceForm", hasProperty("name", is("Corte clásico"))));

        mvc.perform(post("/business/services/1").with(csrf()).cookie(owner)
                        .param("name", "Corte clásico")
                        .param("durationMin", "40")
                        .param("price", "150"))
                .andExpect(redirectedUrl("/business/services"));
        em.flush();
        em.clear();

        ServiceOffering updated = services.findById(1L).orElseThrow();
        assertThat(updated.getDurationMin()).isEqualTo(40);
        assertThat(updated.getPrice()).isEqualByComparingTo("150");
    }

    @Test
    void toggleStatusDeactivatesAndReactivates() throws Exception {
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);

        mvc.perform(post("/business/services/1/status").with(csrf()).cookie(owner))
                .andExpect(flash().attribute("successKey", "service.deactivated"));
        em.flush();
        em.clear();
        assertThat(services.findById(1L).orElseThrow().getStatus()).isEqualTo(ServiceStatus.INACTIVE);

        mvc.perform(post("/business/services/1/status").with(csrf()).cookie(owner))
                .andExpect(flash().attribute("successKey", "service.activated"));
        em.flush();
        em.clear();
        assertThat(services.findById(1L).orElseThrow().getStatus()).isEqualTo(ServiceStatus.ACTIVE);
    }

    @Test
    void servicesOfAnotherBusinessAreNotFound() throws Exception {
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);
        String other = "/business/services/" + OTHER_BUSINESS_SERVICE;

        mvc.perform(get(other + "/edit").cookie(owner)).andExpect(status().isNotFound());
        mvc.perform(post(other).with(csrf()).cookie(owner)
                        .param("name", "Robado").param("durationMin", "30").param("price", "1"))
                .andExpect(status().isNotFound());
        mvc.perform(post(other + "/status").with(csrf()).cookie(owner)).andExpect(status().isNotFound());
        em.flush();
        em.clear();
        ServiceOffering untouched = services.findById(OTHER_BUSINESS_SERVICE).orElseThrow();
        assertThat(untouched.getName()).isEqualTo("Masaje relajante");
        assertThat(untouched.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
    }
}
