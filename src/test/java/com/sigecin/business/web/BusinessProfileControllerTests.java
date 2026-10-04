package com.sigecin.business.web;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.business.entity.Business;
import com.sigecin.business.repository.BusinessRepository;
import com.sigecin.support.AuthTestSupport;
import com.sigecin.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

/** Alta, perfil, baja y reactivación del negocio, y el desvío a /business/setup. */
@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
@Transactional
class BusinessProfileControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired BusinessRepository businesses;
    @Autowired AppointmentRepository appointments;
    @Autowired EntityManager em;

    // --- Alta ---

    @Test
    void ownerWithoutBusinessIsSentToSetupFromAnyBusinessRoute() throws Exception {
        Cookie owner = auth.cookieFor(auth.newBusinessOwner("nuevo@ejemplo.mx"));

        for (String route : new String[]{"/business/profile", "/business/services", "/business/schedule", "/business/dashboard"}) {
            mvc.perform(get(route).cookie(owner)).andExpect(redirectedUrl("/business/setup"));
        }
        mvc.perform(get("/business/setup").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registra tu negocio")));
    }

    @Test
    void setupCreatesBusinessAndExplainsWhatIsMissingToBeVisible() throws Exception {
        User user = auth.newBusinessOwner("nuevo@ejemplo.mx");
        Cookie owner = auth.cookieFor(user);

        mvc.perform(post("/business/setup").with(csrf()).cookie(owner)
                        .param("name", "  Estética Luna  ")
                        .param("categoryId", "2")
                        .param("description", "Cortes y peinados")
                        .param("phone", "   ")
                        .param("address", ""))
                .andExpect(redirectedUrl("/business/profile"))
                .andExpect(flash().attribute("successKey", "business.setup.success"));

        Business created = businesses.findByOwnerId(user.getId()).orElseThrow();
        assertThat(created.getName()).isEqualTo("Estética Luna");
        assertThat(created.getCategory().getName()).isEqualTo("Salón de belleza");
        assertThat(created.getPhone()).isNull();
        assertThat(created.isActive()).isTrue();

        mvc.perform(get("/business/profile").cookie(owner))
                .andExpect(content().string(containsString("Tu negocio aún no es visible")))
                .andExpect(content().string(containsString("Definir horario")))
                .andExpect(content().string(containsString("Agregar servicio")));
        // Ya tiene negocio: no puede volver al alta
        mvc.perform(get("/business/setup").cookie(owner)).andExpect(redirectedUrl("/business/profile"));
    }

    @Test
    void setupValidatesFields() throws Exception {
        Cookie owner = auth.cookieFor(auth.newBusinessOwner("nuevo@ejemplo.mx"));

        mvc.perform(post("/business/setup").with(csrf()).cookie(owner)
                        .param("name", " ")
                        .param("categoryId", "")
                        .param("phone", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("businessForm", "name", "categoryId", "phone"))
                .andExpect(content().string(containsString("Escribe un teléfono válido")));
    }

    @Test
    void secondSetupIsRejected() throws Exception {
        mvc.perform(post("/business/setup").with(csrf()).cookie(auth.cookieFor(BARBERIA_OWNER))
                        .param("name", "Otro negocio").param("categoryId", "1"))
                .andExpect(redirectedUrl("/business/profile"));
        assertThat(businesses.count()).isEqualTo(3);
    }

    // --- Perfil ---

    @Test
    void profileShowsDataScheduleAndVisibility() throws Exception {
        mvc.perform(get("/business/profile").cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Barbería El Estilo")))
                .andExpect(content().string(containsString("10:00 – 15:00")))   // sábado
                .andExpect(content().string(containsString("Cerrado")))         // domingo
                .andExpect(content().string(containsString("Tu negocio es visible para los clientes.")));
    }

    @Test
    void editUpdatesBusinessData() throws Exception {
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);
        mvc.perform(get("/business/profile/edit").cookie(owner))
                .andExpect(model().attribute("businessForm", hasProperty("name", is("Barbería El Estilo"))));

        mvc.perform(post("/business/profile/edit").with(csrf()).cookie(owner)
                        .param("name", "Barbería El Estilo Premium")
                        .param("categoryId", "1")
                        .param("phone", "(55) 1234-5678")
                        .param("address", "Av. Reforma 200, CDMX"))
                .andExpect(redirectedUrl("/business/profile"))
                .andExpect(flash().attribute("successKey", "business.profile.updated"));

        Business updated = businesses.findById(1L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Barbería El Estilo Premium");
        assertThat(updated.getPhone()).isEqualTo("(55) 1234-5678");
        assertThat(updated.getDescription()).isNull();
    }

    // --- Baja y reactivación ---

    @Test
    void deactivationCancelsFutureAppointmentsAndActivationDoesNotRestoreThem() throws Exception {
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);
        // Garantiza una cita futura aunque el script se ejecute a fin de mes
        User client = em.find(User.class, 1L);
        Long futureId = appointments.save(new Appointment(client,
                em.find(com.sigecin.serviceoffering.entity.ServiceOffering.class, 1L),
                LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0), null)).getId();
        long expected = appointments.countFutureActiveByBusiness(1L, LocalDateTime.now());

        mvc.perform(get("/business/profile/deactivate").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(model().attribute("appointmentsToCancel", expected));

        mvc.perform(post("/business/profile/deactivate").with(csrf()).cookie(owner))
                .andExpect(redirectedUrl("/business/profile"))
                .andExpect(flash().attribute("successArg", String.valueOf(expected)));
        em.flush();
        em.clear();

        assertThat(businesses.findById(1L).orElseThrow().isActive()).isFalse();
        Appointment cancelled = appointments.findById(futureId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledReason().getName()).isEqualTo(CancellationReason.BUSINESS_DEACTIVATED);
        assertThat(appointments.countFutureActiveByBusiness(1L, LocalDateTime.now())).isZero();
        // Ya inactivo: la confirmación regresa al perfil, que ofrece reactivar
        mvc.perform(get("/business/profile/deactivate").cookie(owner)).andExpect(redirectedUrl("/business/profile"));
        mvc.perform(get("/business/profile").cookie(owner))
                .andExpect(content().string(containsString("Reactivar el negocio")))
                .andExpect(content().string(not(containsString("Tu negocio es visible"))));

        mvc.perform(post("/business/profile/activate").with(csrf()).cookie(owner))
                .andExpect(redirectedUrl("/business/profile"));
        em.flush();
        em.clear();
        assertThat(businesses.findById(1L).orElseThrow().isActive()).isTrue();
        assertThat(appointments.findById(futureId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }
}
