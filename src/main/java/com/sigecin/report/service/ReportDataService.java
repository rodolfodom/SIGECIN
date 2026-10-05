package com.sigecin.report.service;

import com.sigecin.appointment.repository.AppointmentDetailRepository;
import com.sigecin.business.entity.Business;
import com.sigecin.business.service.BusinessService;
import com.sigecin.report.dto.Dashboard;
import com.sigecin.report.dto.DashboardSummary;
import com.sigecin.report.dto.MonthlyReport;
import com.sigecin.report.dto.MonthlySummary;
import com.sigecin.report.dto.ServiceRankingRow;
import com.sigecin.report.dto.WeeklyDistribution;
import com.sigecin.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Arma los datos del dashboard y del reporte mensual del negocio del dueño, a partir de las
 * vistas del esquema. "Hoy" y "mes en curso" se calculan en la BD (CURDATE) y en la JVM;
 * ambas usan la zona America/Mexico_City.
 */
@Service
@RequiredArgsConstructor
public class ReportDataService {

    public static final int UPCOMING_LIMIT = 5;

    private final BusinessService businessService;
    private final ReportRepository reports;
    private final AppointmentDetailRepository appointments;

    @Transactional(readOnly = true)
    public Dashboard dashboard(Long ownerId) {
        Long businessId = businessService.getByOwner(ownerId).getId();
        YearMonth month = YearMonth.now();
        List<ServiceRankingRow> ranking = reports.serviceRanking(businessId, month);
        return new Dashboard(
                reports.dashboardSummary(businessId).orElse(DashboardSummary.EMPTY),
                ranking.stream().filter(row -> row.ranking() == 1).toList(),
                appointments.findUpcomingToday(businessId, LocalDateTime.now(), UPCOMING_LIMIT),
                weekly(businessId, month),
                ranking);
    }

    @Transactional(readOnly = true)
    public MonthlyReport monthlyReport(Long ownerId, YearMonth month) {
        Business business = businessService.getByOwner(ownerId);
        Long businessId = business.getId();
        return new MonthlyReport(
                business.getName(),
                month,
                reports.monthlySummary(businessId, month).orElse(MonthlySummary.EMPTY),
                appointments.findForBusiness(businessId, month.atDay(1).atStartOfDay(),
                        month.plusMonths(1).atDay(1).atStartOfDay(), null),
                weekly(businessId, month),
                reports.serviceRanking(businessId, month),
                LocalDateTime.now());
    }

    private WeeklyDistribution weekly(Long businessId, YearMonth month) {
        Map<Integer, Long> byWeek = reports.weeklyDistribution(businessId, month);
        List<Long> counts = new ArrayList<>();
        for (int week = 1; week <= WeeklyDistribution.weeksIn(month); week++) {
            counts.add(byWeek.getOrDefault(week, 0L));
        }
        return new WeeklyDistribution(month, counts);
    }
}
