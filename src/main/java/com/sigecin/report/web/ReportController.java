package com.sigecin.report.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.report.pdf.MonthlyReportPdf;
import com.sigecin.report.service.ReportDataService;
import com.sigecin.report.web.form.ReportPeriodForm;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.stream.IntStream;

/** Reporte mensual: selector de año y mes, y descarga del PDF. */
@Controller
@RequestMapping("/business/reports")
public class ReportController {

    /** El selector ofrece el año en curso, los tres anteriores y el siguiente. */
    private static final int YEARS_BACK = 3;

    private final ReportDataService reportData;
    private final MonthlyReportPdf pdf;

    public ReportController(ReportDataService reportData, MonthlyReportPdf pdf) {
        this.reportData = reportData;
        this.pdf = pdf;
    }

    @GetMapping
    public String selector(Model model) {
        int current = Year.now().getValue();
        model.addAttribute("reportPeriodForm", ReportPeriodForm.of(YearMonth.now()));
        model.addAttribute("years", IntStream.rangeClosed(current - YEARS_BACK, current + 1).boxed().toList());
        model.addAttribute("months", Month.values());
        return "business/reports";
    }

    /** Descarga el PDF; un periodo inválido regresa al selector. Un mes sin citas genera un PDF con ceros. */
    @GetMapping("/monthly")
    public Object monthly(@AuthenticationPrincipal AuthenticatedUser user,
                          @Valid @ModelAttribute ReportPeriodForm reportPeriodForm, BindingResult result,
                          RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorKey", "report.error.invalid-period");
            return "redirect:/business/reports";
        }
        YearMonth period = reportPeriodForm.toYearMonth();
        byte[] content = pdf.render(reportData.monthlyReport(user.id(), period));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("reporte-sigecin-" + period + ".pdf").build().toString())
                .body(content);
    }
}
