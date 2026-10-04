package com.sigecin.report.dto;

import com.sigecin.appointment.dto.AppointmentDetailRow;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Contenido del reporte mensual en PDF. */
public record MonthlyReport(String businessName, YearMonth period, MonthlySummary summary,
                            List<AppointmentDetailRow> appointments, WeeklyDistribution weekly,
                            List<ServiceRankingRow> ranking, LocalDateTime generatedAt) {
}
