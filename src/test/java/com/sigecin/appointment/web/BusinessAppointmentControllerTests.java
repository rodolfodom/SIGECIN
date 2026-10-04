package com.sigecin.appointment.web;

import com.sigecin.IntegrationTest;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.repository.AppointmentRepository;
import com.sigecin.appointment.web.view.CalendarView;
import com.sigecin.support.AppointmentTestData;
import com.sigecin.support.AuthTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.sigecin.support.AppointmentTestData.CARLOS;
import static com.sigecin.support.AppointmentTestData.CORTE_CLASICO;
import static com.sigecin.support.AppointmentTestData.MASAJE;
import static com.sigecin.support.AppointmentTestData.futureMonday;
import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Calendario del negocio (semana y mes), detalle, confirmación y cancelación. */
@IntegrationTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, AppointmentTestData.class})
@Transactional
class BusinessAppointmentControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired AppointmentTestData data;
    @Autowired AppointmentRepository appointments;

    @Test
    void weekViewShowsMondayToSundayWithTheBusinessAppointments() throws Exception {
        LocalDate monday = futureMonday();
        data.create(CARLOS, CORTE_CLASICO, monday.plusDays(2).atTime(10, 0));
        data.create(CARLOS, MASAJE, monday.plusDays(2).atTime(12, 0));   // del Spa: no debe aparecer

        CalendarView view = calendar(auth.cookieFor(BARBERIA_OWNER), "week", monday.plusDays(3), null);
        assertThat(view.weeks()).hasSize(1);
        assertThat(view.firstDay()).isEqualTo(monday);
        assertThat(view.lastDay()).isEqualTo(monday.plusDays(6));
        var wednesday = view.weeks().getFirst().days().get(2);
        assertThat(wednesday.appointments()).extracting(a -> a.serviceName()).containsExactly("Corte clásico");
        assertThat(view.previous()).isEqualTo(monday.plusDays(3).minusWeeks(1));
    }

    @Test
    void monthViewLimitsEachDayAndLinksToTheWeek() throws Exception {
        LocalDate monday = futureMonday();
        for (int hour = 9; hour <= 13; hour++) {
            data.create(CARLOS, CORTE_CLASICO, monday.atTime(hour, 0));
        }
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);

        CalendarView view = calendar(owner, "month", monday, null);
        assertThat(view.firstDay().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(view.firstDay()).isBeforeOrEqualTo(monday.withDayOfMonth(1));
        var day = view.weeks().stream().flatMap(w -> w.days().stream())
                .filter(d -> d.date().equals(monday)).findFirst().orElseThrow();
        assertThat(day.appointments()).hasSize(CalendarView.MONTH_DAY_LIMIT);
        assertThat(day.hidden()).isEqualTo(2);
        mvc.perform(get("/business/appointments").cookie(owner).param("view", "month").param("date", monday.toString()))
                .andExpect(content().string(containsString("+2 más")))
                .andExpect(content().string(containsString("view=week&amp;date=" + monday)));
    }

    @Test
    void statusFilterKeepsOnlyThatStatus() throws Exception {
        LocalDate monday = futureMonday();
        Appointment pending = data.create(CARLOS, CORTE_CLASICO, monday.atTime(9, 0));
        data.create(CARLOS, CORTE_CLASICO, monday.atTime(10, 0)).setStatus(AppointmentStatus.CONFIRMED);
        appointments.flush();

        CalendarView view = calendar(auth.cookieFor(BARBERIA_OWNER), "week", monday, "PENDING");
        assertThat(view.weeks().getFirst().days().getFirst().appointments())
                .extracting(a -> a.appointmentId()).containsExactly(pending.getId());
    }

    @Test
    void ownerConfirmsPendingAppointmentOnlyOnce() throws Exception {
        Appointment appointment = data.create(CARLOS, CORTE_CLASICO, futureMonday().atTime(9, 0));
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);
        String url = "/business/appointments/" + appointment.getId();

        mvc.perform(get(url).cookie(owner))
                .andExpect(content().string(containsString("Carlos Ramírez Torres")))
                .andExpect(content().string(containsString("Confirmar cita")));
        mvc.perform(post(url + "/confirm").with(csrf()).cookie(owner))
                .andExpect(flash().attribute("successKey", "appointment.confirmed"));
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        mvc.perform(post(url + "/confirm").with(csrf()).cookie(owner))
                .andExpect(flash().attribute("errorKey", "appointment.error.not-confirmable"));
    }

    @Test
    void ownerCancelsWithABusinessReason() throws Exception {
        Appointment appointment = data.create(CARLOS, CORTE_CLASICO, futureMonday().atTime(9, 0));
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);
        String cancel = "/business/appointments/" + appointment.getId() + "/cancel";

        mvc.perform(post(cancel).with(csrf()).cookie(owner).param("reasonId", "1"))    // motivo de cliente
                .andExpect(flash().attribute("errorKey", "appointment.error.invalid-reason"));
        mvc.perform(post(cancel).with(csrf()).cookie(owner).param("reasonId", "6"))
                .andExpect(flash().attribute("successKey", "appointment.cancelled"));
        Appointment cancelled = appointments.findDetailedById(appointment.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledReason().getName()).isEqualTo("Conflicto de horario del negocio");
    }

    @Test
    void appointmentOfAnotherBusinessIsNotFound() throws Exception {
        Appointment spa = data.create(CARLOS, MASAJE, futureMonday().atTime(12, 0));
        Cookie owner = auth.cookieFor(BARBERIA_OWNER);

        mvc.perform(get("/business/appointments/" + spa.getId()).cookie(owner)).andExpect(status().isNotFound());
        mvc.perform(post("/business/appointments/" + spa.getId() + "/confirm").with(csrf()).cookie(owner))
                .andExpect(status().isNotFound());
        assertThat(appointments.findById(spa.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    private CalendarView calendar(Cookie owner, String view, LocalDate date, String status) throws Exception {
        var request = get("/business/appointments").cookie(owner).param("view", view).param("date", date.toString());
        if (status != null) {
            request.param("status", status);
        }
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return (CalendarView) result.getModelAndView().getModel().get("calendar");
    }
}
