package com.sigecin.business.web;

import com.sigecin.IntegrationTest;
import com.sigecin.business.entity.BusinessSchedule;
import com.sigecin.business.repository.BusinessScheduleRepository;
import com.sigecin.support.AuthTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Horario semanal: crear, actualizar y desactivar días, y validación de horas. */
@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
@Transactional
class BusinessScheduleControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired BusinessScheduleRepository schedules;
    @Autowired EntityManager em;

    @Test
    void savingTheWeekCreatesUpdatesAndDeactivatesDays() throws Exception {
        // Barbería: lunes a viernes 09-19, sábado 10-15, domingo sin registro
        mvc.perform(week(Map.of(
                        DayOfWeek.MONDAY, new String[]{"08:00", "20:00"},
                        DayOfWeek.SUNDAY, new String[]{"10:00", "14:00"})))
                .andExpect(redirectedUrl("/business/schedule"));
        em.flush();
        em.clear();

        assertThat(day(DayOfWeek.MONDAY).getOpenTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(day(DayOfWeek.MONDAY).getCloseTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(day(DayOfWeek.SUNDAY).isActive()).isTrue();
        // Los días cerrados se desactivan y conservan sus horas
        BusinessSchedule saturday = day(DayOfWeek.SATURDAY);
        assertThat(saturday.isActive()).isFalse();
        assertThat(saturday.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(schedules.findByBusinessIdOrderByDayOfWeek(1L)).hasSize(7);
    }

    @Test
    void closingTimeMustBeAfterOpeningTime() throws Exception {
        mvc.perform(week(Map.of(DayOfWeek.MONDAY, new String[]{"18:00", "09:00"})))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("scheduleForm", "days[0].closeTime",
                        "business.schedule.close-before-open"));
    }

    @Test
    void openDayNeedsBothTimes() throws Exception {
        mvc.perform(week(Map.of(DayOfWeek.TUESDAY, new String[]{"", "18:00"})))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("scheduleForm", "days[1].openTime", "NotNull"));
    }

    @Test
    void malformedTimeIsReported() throws Exception {
        mvc.perform(week(Map.of(DayOfWeek.MONDAY, new String[]{"9am", "18:00"})))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("scheduleForm", "days[0].openTime", "typeMismatch"));
    }

    /** Envía los siete días; los que no vienen en {@code open} van cerrados y sin horas. */
    private MockHttpServletRequestBuilder week(Map<DayOfWeek, String[]> open) {
        MockHttpServletRequestBuilder request = post("/business/schedule").with(csrf())
                .cookie(auth.cookieFor(BARBERIA_OWNER));
        for (DayOfWeek day : DayOfWeek.values()) {
            String prefix = "days[" + (day.getValue() - 1) + "].";
            request.param(prefix + "day", day.name()).param("_" + prefix + "open", "on");
            String[] times = open.get(day);
            if (times != null) {
                request.param(prefix + "open", "true")
                        .param(prefix + "openTime", times[0])
                        .param(prefix + "closeTime", times[1]);
            }
        }
        return request;
    }

    private BusinessSchedule day(DayOfWeek day) {
        return schedules.findByBusinessIdOrderByDayOfWeek(1L).stream()
                .filter(s -> s.getDayOfWeek() == day).findFirst().orElseThrow();
    }
}
