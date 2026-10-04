package com.sigecin.appointment.web;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.support.AppointmentTestData;
import com.sigecin.support.AuthTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.sigecin.support.AppointmentTestData.CARLOS;
import static com.sigecin.support.AppointmentTestData.CORTE_CLASICO;
import static com.sigecin.support.AppointmentTestData.LAURA;
import static com.sigecin.support.AppointmentTestData.MANICURE_INACTIVO;
import static com.sigecin.support.AppointmentTestData.futureMonday;
import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reserva, lista, detalle y cancelación de citas del cliente, con cada resultado de validación. */
@IntegrationTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, AppointmentTestData.class})
@Transactional
class ClientAppointmentControllerTests {

    private static final String CARLOS_EMAIL = "carlos.ramirez@unam.mx";
    private static final String LAURA_EMAIL = "laura.sanchez@unam.mx";

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired AppointmentTestData data;
    @Autowired AppointmentRepository appointments;
    @Autowired EntityManager em;

    // --- Reserva ---

    @Test
    void bookingCreatesPendingAppointmentAndOpensItsDetail() throws Exception {
        LocalDateTime start = futureMonday().atTime(11, 0);
        MvcResult result = mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, start).param("clientNotes", "  Primera vez  "))
                .andExpect(flash().attribute("successKey", "booking.success"))
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/appointments/");
        Appointment created = appointments.findById(Long.valueOf(location.substring("/appointments/".length()))).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(created.getEndTime()).isEqualTo(start.plusMinutes(30));
        assertThat(created.getPriceAtBooking()).isEqualByComparingTo("120.00");
        assertThat(created.getClientNotes()).isEqualTo("Primera vez");

        mvc.perform(get(location).cookie(auth.cookieFor(CARLOS_EMAIL)))
                .andExpect(content().string(containsString("Pendiente")))
                .andExpect(content().string(containsString("Cancelar la cita")));
    }

    @Test
    void takenSlotReturnsToAvailabilityKeepingTheNotes() throws Exception {
        LocalDateTime start = futureMonday().atTime(12, 0);
        data.create(LAURA, CORTE_CLASICO, start.plusMinutes(15));   // 12:15–12:45 se traslapa con 12:00–12:30

        mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, start).param("clientNotes", "Sin prisa"))
                .andExpect(redirectedUrl("/businesses/1/availability?serviceId=1&date=" + start.toLocalDate()))
                .andExpect(flash().attribute("errorKey", "booking.error.taken"))
                .andExpect(flash().attribute("clientNotes", "Sin prisa"));
    }

    @Test
    void eachRejectionHasItsMessage() throws Exception {
        LocalDate monday = futureMonday();
        // Termina 19:15, después del cierre (19:00)
        mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, monday.atTime(18, 45)))
                .andExpect(flash().attribute("errorKey", "booking.error.outside-hours"));
        // Antes de abrir
        mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, monday.atTime(8, 30)))
                .andExpect(flash().attribute("errorKey", "booking.error.outside-hours"));
        // Domingo: la Barbería no abre
        mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, monday.plusDays(6).atTime(11, 0)))
                .andExpect(flash().attribute("errorKey", "booking.error.outside-hours"));
        mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, LocalDateTime.now().minusHours(1)))
                .andExpect(flash().attribute("errorKey", "booking.error.past"));
        mvc.perform(book(CARLOS_EMAIL, MANICURE_INACTIVO, monday.atTime(11, 0)))
                .andExpect(flash().attribute("errorKey", "booking.error.unavailable"));
        // Sin hora elegida
        mvc.perform(post("/appointments").with(csrf()).cookie(auth.cookieFor(CARLOS_EMAIL))
                        .param("businessId", "1").param("serviceId", "1").param("date", monday.toString()))
                .andExpect(redirectedUrl("/businesses/1/availability?serviceId=1&date=" + monday))
                .andExpect(flash().attribute("errorKey", "booking.error.no-slot"));
    }

    @Test
    void inactiveBusinessCannotBeBooked() throws Exception {
        em.find(com.sigecin.business.entity.Business.class, 1L).setActive(false);
        em.flush();
        mvc.perform(book(CARLOS_EMAIL, CORTE_CLASICO, futureMonday().atTime(11, 0)))
                .andExpect(flash().attribute("errorKey", "booking.error.unavailable"));
    }

    @Test
    void onlyClientsCanBook() throws Exception {
        mvc.perform(post("/appointments").with(csrf()).cookie(auth.cookieFor(BARBERIA_OWNER))
                        .param("serviceId", "1").param("startTime", futureMonday().atTime(11, 0).toString()))
                .andExpect(status().isForbidden());
    }

    // --- Lista y detalle ---

    @Test
    void listShowsOwnAppointmentsWithFilters() throws Exception {
        Appointment mine = data.create(CARLOS, CORTE_CLASICO, futureMonday().atTime(9, 0));
        data.create(LAURA, CORTE_CLASICO, futureMonday().atTime(13, 0));
        Cookie carlos = auth.cookieFor(CARLOS_EMAIL);
        String day = futureMonday().toString();

        MvcResult result = mvc.perform(get("/appointments").cookie(carlos).param("from", day).param("to", day))
                .andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked")
        var page = (org.springframework.data.domain.Page<Appointment>) result.getModelAndView().getModel().get("appointments");
        assertThat(page.getContent()).extracting(Appointment::getId).containsExactly(mine.getId());

        mvc.perform(get("/appointments").cookie(carlos).param("status", "CANCELLED").param("from", day))
                .andExpect(content().string(containsString("No tienes citas")));
    }

    @Test
    void appointmentOfAnotherClientIsNotFound() throws Exception {
        Appointment lauras = data.create(LAURA, CORTE_CLASICO, futureMonday().atTime(9, 0));
        mvc.perform(get("/appointments/" + lauras.getId()).cookie(auth.cookieFor(CARLOS_EMAIL)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/appointments/" + lauras.getId() + "/cancel").with(csrf())
                        .cookie(auth.cookieFor(CARLOS_EMAIL)).param("reasonId", "1"))
                .andExpect(status().isNotFound());
    }

    // --- Cancelación ---

    @Test
    void clientCancelsWithAClientReason() throws Exception {
        Appointment appointment = data.create(CARLOS, CORTE_CLASICO, futureMonday().atTime(9, 0));
        Cookie carlos = auth.cookieFor(CARLOS_EMAIL);
        String cancel = "/appointments/" + appointment.getId() + "/cancel";

        // Motivo 5 es de BUSINESS; sin motivo tampoco se permite
        mvc.perform(post(cancel).with(csrf()).cookie(carlos).param("reasonId", "5"))
                .andExpect(flash().attribute("errorKey", "appointment.error.invalid-reason"));
        mvc.perform(post(cancel).with(csrf()).cookie(carlos))
                .andExpect(flash().attribute("errorKey", "appointment.error.invalid-reason"));

        mvc.perform(post(cancel).with(csrf()).cookie(carlos).param("reasonId", "3"))
                .andExpect(redirectedUrl("/appointments/" + appointment.getId()))
                .andExpect(flash().attribute("successKey", "appointment.cancelled"));
        Appointment cancelled = appointments.findDetailedById(appointment.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledReason().getName()).isEqualTo("Cambio de planes");

        // CANCELLED es final; el detalle ya no ofrece cancelar
        mvc.perform(post(cancel).with(csrf()).cookie(carlos).param("reasonId", "3"))
                .andExpect(flash().attribute("errorKey", "appointment.error.not-cancellable"));
        mvc.perform(get("/appointments/" + appointment.getId()).cookie(carlos))
                .andExpect(content().string(not(containsString("Cancelar la cita"))))
                .andExpect(content().string(containsString("Cambio de planes")));
    }

    @Test
    void appointmentThatAlreadyStartedCannotBeCancelled() throws Exception {
        Appointment past = data.create(CARLOS, CORTE_CLASICO, LocalDateTime.now().minusHours(2));
        past.setStatus(AppointmentStatus.CONFIRMED);
        em.flush();

        mvc.perform(post("/appointments/" + past.getId() + "/cancel").with(csrf())
                        .cookie(auth.cookieFor(CARLOS_EMAIL)).param("reasonId", "1"))
                .andExpect(flash().attribute("errorKey", "appointment.error.not-cancellable"));
    }

    private MockHttpServletRequestBuilder book(String email, long serviceId, LocalDateTime start) {
        return post("/appointments").with(csrf()).cookie(auth.cookieFor(email))
                .param("businessId", serviceId >= 4 ? "2" : "1")
                .param("serviceId", String.valueOf(serviceId))
                .param("startTime", start.withSecond(0).withNano(0).toString());
    }
}
