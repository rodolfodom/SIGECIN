package com.sigecin.appointment.web;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.dto.Availability;
import com.sigecin.support.AppointmentTestData;
import com.sigecin.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.sigecin.support.AppointmentTestData.CARLOS;
import static com.sigecin.support.AppointmentTestData.CORTE_CLASICO;
import static com.sigecin.support.AppointmentTestData.MANICURE_INACTIVO;
import static com.sigecin.support.AppointmentTestData.futureMonday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Horas disponibles: horario del día, duración del servicio, traslapes y citas canceladas. */
@IntegrationTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, AppointmentTestData.class})
@Transactional
class AvailabilityControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired AppointmentTestData data;

    @Test
    void slotsEvery15MinutesThatFitBeforeClosing() throws Exception {
        List<LocalTime> slots = slots(1L, CORTE_CLASICO, futureMonday());
        // Lunes 09:00–19:00, servicio de 30 min: de 09:00 a 18:30 cada 15 min
        assertThat(slots).hasSize(39).startsWith(LocalTime.of(9, 0)).endsWith(LocalTime.of(18, 30));
    }

    @Test
    void takenSlotsAreExcludedAndCancelledOnesFreed() throws Exception {
        LocalDate monday = futureMonday();
        data.create(CARLOS, CORTE_CLASICO, monday.atTime(10, 0));        // 10:00–10:30
        var cancelled = data.create(CARLOS, CORTE_CLASICO, monday.atTime(15, 0));
        cancelled.setStatus(com.sigecin.appointment.enums.AppointmentStatus.CANCELLED);

        List<LocalTime> slots = slots(1L, CORTE_CLASICO, monday);
        assertThat(slots).contains(LocalTime.of(9, 30), LocalTime.of(10, 30), LocalTime.of(15, 0))
                .doesNotContain(LocalTime.of(9, 45), LocalTime.of(10, 0), LocalTime.of(10, 15));
    }

    @Test
    void closedDayAndPastDate() throws Exception {
        mvc.perform(get("/businesses/1/availability").param("serviceId", "1")
                        .param("date", futureMonday().plusDays(6).toString()))   // domingo
                .andExpect(content().string(containsString("El negocio no abre este día")));
        // Una fecha pasada muestra hoy
        MvcResult past = mvc.perform(get("/businesses/1/availability").param("serviceId", "1").param("date", "2020-01-01"))
                .andReturn();
        assertThat(((Availability) past.getModelAndView().getModel().get("availability")).date()).isEqualTo(LocalDate.now());
    }

    @Test
    void inactiveOrForeignServiceIsNotFound() throws Exception {
        mvc.perform(get("/businesses/2/availability").param("serviceId", String.valueOf(MANICURE_INACTIVO)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/businesses/2/availability").param("serviceId", "1"))   // servicio de la Barbería
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserIsInvitedToLogInAndClientGetsTheForm() throws Exception {
        String url = "/businesses/1/availability?serviceId=1&date=" + futureMonday();
        mvc.perform(get(url))
                .andExpect(content().string(containsString("Inicia sesión para reservar")))
                .andExpect(content().string(containsString("redirect=/businesses/1/availability")));
        mvc.perform(get(url).cookie(auth.cookieFor("carlos.ramirez@unam.mx")))
                .andExpect(content().string(containsString("Confirmar reserva")))
                .andExpect(content().string(containsString("value=\"" + futureMonday() + "T09:00\"")));
    }

    private List<LocalTime> slots(long businessId, long serviceId, LocalDate date) throws Exception {
        MvcResult result = mvc.perform(get("/businesses/{id}/availability", businessId)
                        .param("serviceId", String.valueOf(serviceId)).param("date", date.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return ((Availability) result.getModelAndView().getModel().get("availability")).slots();
    }
}
