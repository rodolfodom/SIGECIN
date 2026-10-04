package com.sigecin.report.dto;

import java.math.BigDecimal;

/** Fila de v_monthly_summary: totales del mes por estado e ingreso estimado (citas CONFIRMED). */
public record MonthlySummary(long totalAppointments, long pendingAppointments, long confirmedAppointments,
                             long cancelledAppointments, BigDecimal estimatedIncome) {

    /** Un mes sin citas no genera fila en la vista: se interpreta como ceros. */
    public static final MonthlySummary EMPTY = new MonthlySummary(0, 0, 0, 0, BigDecimal.ZERO);
}
