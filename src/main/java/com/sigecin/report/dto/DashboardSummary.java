package com.sigecin.report.dto;

import java.math.BigDecimal;

/** Fila de v_dashboard_summary: citas de hoy, ingreso estimado del mes en curso y favoritos. */
public record DashboardSummary(long todayAppointments, BigDecimal monthEstimatedIncome, long favoritesCount) {

    public static final DashboardSummary EMPTY = new DashboardSummary(0, BigDecimal.ZERO, 0);
}
