package com.sigecin.report.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.report.dto.Dashboard;
import com.sigecin.report.dto.ServiceRankingRow;
import com.sigecin.report.dto.WeeklyDistribution;
import com.sigecin.report.service.ReportDataService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard del negocio: pantalla de inicio del dueño. Los datos de las gráficas se incrustan
 * en la página al renderizarla (no hay endpoints JSON).
 */
@Controller
public class DashboardController {

    /** La gráfica de servicios muestra como máximo este número de barras. */
    static final int CHART_SERVICES = 5;

    private final ReportDataService reportData;
    private final MessageSource messages;

    public DashboardController(ReportDataService reportData, MessageSource messages) {
        this.reportData = reportData;
        this.messages = messages;
    }

    @GetMapping("/business/dashboard")
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        Dashboard dashboard = reportData.dashboard(user.id());
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("charts", Map.of(
                "weekly", weeklyChart(dashboard.weekly()),
                "services", servicesChart(dashboard.ranking())));
        return "business/dashboard";
    }

    private Map<String, Object> weeklyChart(WeeklyDistribution weekly) {
        List<String> labels = new ArrayList<>();
        for (int week = 1; week <= weekly.counts().size(); week++) {
            labels.add(text("dashboard.chart.week", week, WeeklyDistribution.firstDay(week), weekly.lastDay(week)));
        }
        return Map.of("labels", labels, "data", weekly.counts(), "label", text("dashboard.chart.bookings"));
    }

    private Map<String, Object> servicesChart(List<ServiceRankingRow> ranking) {
        List<ServiceRankingRow> top = ranking.stream().limit(CHART_SERVICES).toList();
        return Map.of(
                "labels", top.stream().map(ServiceRankingRow::serviceName).toList(),
                "data", top.stream().map(ServiceRankingRow::bookings).toList(),
                "label", text("dashboard.chart.bookings"));
    }

    private String text(String key, Object... args) {
        return messages.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
