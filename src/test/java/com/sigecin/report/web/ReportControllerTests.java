package com.sigecin.report.web;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.sigecin.IntegrationTest;
import com.sigecin.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.YearMonth;

import static com.sigecin.support.AuthTestSupport.BARBERIA_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Selector y descarga del reporte mensual en PDF. */
@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
@Transactional
class ReportControllerTests {

    @Autowired MockMvc mvc;
    @Autowired AuthTestSupport auth;
    @Autowired JdbcClient jdbc;

    @Test
    void selectorDefaultsToTheCurrentMonth() throws Exception {
        mvc.perform(get("/business/reports").cookie(auth.cookieFor(BARBERIA_OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Descargar PDF")))
                .andExpect(content().string(containsString("value=\"" + YearMonth.now().getMonthValue() + "\" selected")));
    }

    @Test
    void monthlyReportIsAPdfWithTheBusinessData() throws Exception {
        YearMonth month = YearMonth.now();
        MvcResult result = download(month)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("reporte-sigecin-" + month + ".pdf")))
                .andReturn();

        String text = pdfText(result.getResponse().getContentAsByteArray());
        long total = jdbc.sql("""
                        select count(*) from appointment a join service s on s.id = a.service_id
                         where s.business_id = 1 and a.start_time >= :from and a.start_time < :to
                        """)
                .param("from", month.atDay(1).atStartOfDay()).param("to", month.plusMonths(1).atDay(1).atStartOfDay())
                .query(Long.class).single();

        assertThat(text).contains("Barbería El Estilo", "Resumen ejecutivo", "Total de citas", String.valueOf(total),
                "Reservas por semana", "Semana 1 (días 1–7)", "Servicios más solicitados", "Citas del periodo",
                "Corte clásico", "Confirmada");
        // Solo datos de este negocio
        assertThat(text).doesNotContain("Masaje relajante", "Limpieza dental");
    }

    @Test
    void monthWithoutAppointmentsProducesAReportWithZeros() throws Exception {
        String text = pdfText(download(YearMonth.of(2020, 1)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());

        assertThat(text).contains("Enero de 2020", "No hay citas en este periodo.", "No hubo reservas en este periodo.",
                "$0.00", "Total del mes: 0");
    }

    @Test
    void historicalMonthUsesTheFrozenPrice() throws Exception {
        // Abril 2026 (script): Corte clásico confirmado a $120.00
        jdbc.sql("update service set price = 999 where id = 1").update();
        String text = pdfText(download(YearMonth.of(2026, 4)).andReturn().getResponse().getContentAsByteArray());
        assertThat(text).contains("$120.00").doesNotContain("$999.00");
    }

    @Test
    void invalidPeriodReturnsToTheSelector() throws Exception {
        mvc.perform(get("/business/reports/monthly").cookie(auth.cookieFor(BARBERIA_OWNER))
                        .param("year", "2026").param("month", "13"))
                .andExpect(redirectedUrl("/business/reports"))
                .andExpect(flash().attribute("errorKey", "report.error.invalid-period"));
    }

    @Test
    void clientsCannotDownloadReports() throws Exception {
        mvc.perform(get("/business/reports/monthly").cookie(auth.cookieFor("carlos.ramirez@unam.mx"))
                        .param("year", "2026").param("month", "10"))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions download(YearMonth month) throws Exception {
        return mvc.perform(get("/business/reports/monthly").cookie(auth.cookieFor(BARBERIA_OWNER))
                .param("year", String.valueOf(month.getYear()))
                .param("month", String.valueOf(month.getMonthValue())));
    }

    private static String pdfText(byte[] pdf) throws Exception {
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        StringBuilder text = new StringBuilder();
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(page))).append('\n');
            }
        }
        return text.toString();
    }
}
