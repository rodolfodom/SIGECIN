package com.sigecin.report.dto;

import com.sigecin.appointment.dto.AppointmentDetailRow;

import java.util.List;

/**
 * Datos del dashboard del negocio. {@code topServices} son los servicios con ranking 1 del mes
 * en curso (varios si hay empate); {@code upcoming} son las próximas citas de hoy.
 */
public record Dashboard(DashboardSummary summary, List<ServiceRankingRow> topServices,
                        List<AppointmentDetailRow> upcoming, WeeklyDistribution weekly,
                        List<ServiceRankingRow> ranking) {
}
