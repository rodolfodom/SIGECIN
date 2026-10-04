package com.sigecin.report.dto;

import java.time.YearMonth;
import java.util.List;

/**
 * Reservas (citas no canceladas) por semana del mes: semana 1 = días 1–7, … semana 5 = días
 * 29 en adelante. Incluye las semanas sin citas con cero; febrero de 28 días tiene 4 semanas.
 */
public record WeeklyDistribution(YearMonth month, List<Long> counts) {

    public static int weeksIn(YearMonth month) {
        return (month.lengthOfMonth() + 6) / 7;
    }

    public long total() {
        return counts.stream().mapToLong(Long::longValue).sum();
    }

    public long max() {
        return counts.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    /** Primer día de la semana (1, 8, 15, 22, 29). */
    public static int firstDay(int week) {
        return (week - 1) * 7 + 1;
    }

    /** Último día de la semana, sin pasar del fin de mes. */
    public int lastDay(int week) {
        return Math.min(week * 7, month.lengthOfMonth());
    }
}
