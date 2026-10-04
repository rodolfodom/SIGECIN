package com.sigecin.report.repository;

import com.sigecin.report.dto.DashboardSummary;
import com.sigecin.report.dto.MonthlySummary;
import com.sigecin.report.dto.ServiceRankingRow;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lectura de las vistas del dashboard y del reporte (v_dashboard_summary, v_monthly_summary,
 * v_service_ranking_monthly y v_weekly_distribution). Las vistas no reciben parámetros: se
 * filtran por negocio, año y mes en el WHERE. Un negocio o mes sin citas no tiene fila.
 */
@Repository
public class ReportRepository {

    private final JdbcClient jdbc;

    public ReportRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DashboardSummary> dashboardSummary(Long businessId) {
        return jdbc.sql("""
                        select today_appointments, month_estimated_income, favorites_count
                          from v_dashboard_summary
                         where business_id = :businessId
                        """)
                .param("businessId", businessId)
                .query(DashboardSummary.class)
                .optional();
    }

    public Optional<MonthlySummary> monthlySummary(Long businessId, YearMonth month) {
        return jdbc.sql("""
                        select total_appointments, pending_appointments, confirmed_appointments,
                               cancelled_appointments, estimated_income
                          from v_monthly_summary
                         where business_id = :businessId and period_year = :year and period_month = :month
                        """)
                .param("businessId", businessId)
                .param("year", month.getYear())
                .param("month", month.getMonthValue())
                .query(MonthlySummary.class)
                .optional();
    }

    /** Servicios del mes ordenados por número de reservas (los empates, por nombre). */
    public List<ServiceRankingRow> serviceRanking(Long businessId, YearMonth month) {
        return jdbc.sql("""
                        select service_id, service_name, bookings, ranking
                          from v_service_ranking_monthly
                         where business_id = :businessId and period_year = :year and period_month = :month
                         order by ranking, service_name
                        """)
                .param("businessId", businessId)
                .param("year", month.getYear())
                .param("month", month.getMonthValue())
                .query(ServiceRankingRow.class)
                .list();
    }

    /** Reservas por semana del mes (solo las semanas con citas: las demás valen cero). */
    public Map<Integer, Long> weeklyDistribution(Long businessId, YearMonth month) {
        return jdbc.sql("""
                        select week_of_month, appointments
                          from v_weekly_distribution
                         where business_id = :businessId and period_year = :year and period_month = :month
                        """)
                .param("businessId", businessId)
                .param("year", month.getYear())
                .param("month", month.getMonthValue())
                .query((rs, row) -> Map.entry(rs.getInt("week_of_month"), rs.getLong("appointments")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
