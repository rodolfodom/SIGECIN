package com.sigecin.report.web;

import com.sigecin.IntegrationTest;
import com.sigecin.report.dto.Dashboard;
import com.sigecin.support.AppointmentTestData;
import com.sigecin.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.sigecin.support.AppointmentTestData.CARLOS;
import static com.sigecin.support.AppointmentTestData.CORTE_CLASICO;
import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dashboard de la Barbería (negocio 1). Los valores esperados se calculan con SQL directo
 * sobre las tablas, sin pasar por las vistas que usa la aplicación.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, AppointmentTestData.class})
@Transactional
class DashboardControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired AppointmentTestData data;
    @Autowired JdbcClient jdbc;

    @Test
    void indicatorsMatchTheAppointmentsTable() throws Exception {
        Dashboard dashboard = dashboard();
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.now();

        long todayCount = jdbc.sql("""
                        select count(*) from appointment a join service s on s.id = a.service_id
                         where s.business_id = 1 and a.status_id <> 3
                           and a.start_time >= :from and a.start_time < :to
                        """)
                .param("from", today.atStartOfDay()).param("to", today.plusDays(1).atStartOfDay())
                .query(Long.class).single();
        BigDecimal income = jdbc.sql("""
                        select coalesce(sum(a.price_at_booking), 0) from appointment a join service s on s.id = a.service_id
                         where s.business_id = 1 and a.status_id = 2
                           and a.start_time >= :from and a.start_time < :to
                        """)
                .param("from", month.atDay(1).atStartOfDay()).param("to", month.plusMonths(1).atDay(1).atStartOfDay())
                .query(BigDecimal.class).single();
        List<String> mostBooked = jdbc.sql("""
                        select s.name from appointment a join service s on s.id = a.service_id
                         where s.business_id = 1 and a.status_id <> 3
                           and a.start_time >= :from and a.start_time < :to
                         group by s.id, s.name
                        having count(*) = (select max(c) from (select count(*) c from appointment a2 join service s2 on s2.id = a2.service_id
                                            where s2.business_id = 1 and a2.status_id <> 3
                                              and a2.start_time >= :from and a2.start_time < :to
                                            group by s2.id) t)
                        """)
                .param("from", month.atDay(1).atStartOfDay()).param("to", month.plusMonths(1).atDay(1).atStartOfDay())
                .query(String.class).list();

        assertThat(dashboard.summary().todayAppointments()).isEqualTo(todayCount);
        assertThat(dashboard.summary().monthEstimatedIncome()).isEqualByComparingTo(income);
        assertThat(dashboard.summary().favoritesCount()).isEqualTo(2);   // Carlos y Laura
        assertThat(dashboard.topServices()).extracting(r -> r.serviceName()).containsExactlyInAnyOrderElementsOf(mostBooked);
        assertThat(dashboard.weekly().counts()).hasSize(5 - (month.lengthOfMonth() == 28 ? 1 : 0));
    }

    @Test
    void upcomingShowsTodaysNextAppointments() throws Exception {
        LocalDateTime soon = LocalDateTime.now().plusMinutes(30).truncatedTo(ChronoUnit.MINUTES);
        assumeThat(soon.toLocalDate()).as("la prueba necesita que la cita caiga hoy").isEqualTo(LocalDate.now());
        Long id = data.create(CARLOS, CORTE_CLASICO, soon).getId();

        assertThat(dashboard().upcoming()).extracting(a -> a.appointmentId()).contains(id);
    }

    @Test
    void pageEmbedsChartDataAndLoadsChartJsFromTheWebJar() throws Exception {
        String html = mvc.perform(get("/business/dashboard").cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(content().string(containsString("window.SIGECIN_CHARTS")))
                // Thymeleaf escapa lo que no es ASCII al incrustar JSON en un <script>: "–" queda como \u2013
                .andExpect(content().string(containsString("\"Sem. 1 (1\\u20137)\"")))
                .andReturn().getResponse().getContentAsString();
        // El enlace sale con la versión del WebJar (/webjars/chart.js/4.5.1/...) y el archivo se sirve
        java.util.regex.Matcher script = java.util.regex.Pattern
                .compile("src=\"(/webjars/chart\\.js/[^\"]+/chart\\.umd\\.min\\.js)\"").matcher(html);
        assertThat(script.find()).isTrue();
        mvc.perform(get(script.group(1))).andExpect(status().isOk());
    }

    @Test
    void clientsCannotSeeTheDashboard() throws Exception {
        mvc.perform(get("/business/dashboard").cookie(auth.cookieFor("carlos.ramirez@unam.mx")))
                .andExpect(status().isForbidden());
    }

    private Dashboard dashboard() throws Exception {
        MvcResult result = mvc.perform(get("/business/dashboard").cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(status().isOk()).andReturn();
        return (Dashboard) result.getModelAndView().getModel().get("dashboard");
    }
}
