package com.sigecin.appointment.web.view;

import com.sigecin.appointment.dto.AppointmentDetailRow;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modelo del calendario de citas del negocio, armado en el servidor para que la plantilla
 * solo lo recorra. La semana va de lunes a domingo; el mes es una rejilla de semanas
 * completas (incluye días del mes anterior y del siguiente, marcados fuera del periodo).
 */
public record CalendarView(Mode mode, LocalDate date, LocalDate today, List<Week> weeks,
                           LocalDate previous, LocalDate next) {

    /** En la vista de mes cada día muestra como máximo estas citas y un enlace "+N más". */
    public static final int MONTH_DAY_LIMIT = 3;

    public enum Mode {
        WEEK, MONTH;

        /** Valor del parámetro {@code view}: "week" (por defecto) o "month". */
        public static Mode fromParam(String view) {
            return "month".equalsIgnoreCase(view) ? MONTH : WEEK;
        }

        public String param() {
            return name().toLowerCase();
        }
    }

    public record Week(List<Day> days) {
    }

    /** {@code hidden} cuenta las citas que no caben en la celda (solo en la vista de mes). */
    public record Day(LocalDate date, boolean inPeriod, boolean today,
                      List<AppointmentDetailRow> appointments, int hidden) {
    }

    /** Primer día del rango a consultar (inclusivo). */
    public static LocalDate rangeStart(Mode mode, LocalDate date) {
        LocalDate anchor = mode == Mode.MONTH ? date.withDayOfMonth(1) : date;
        return anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Día siguiente al último del rango (exclusivo). */
    public static LocalDate rangeEnd(Mode mode, LocalDate date) {
        if (mode == Mode.WEEK) {
            return rangeStart(mode, date).plusWeeks(1);
        }
        return date.with(TemporalAdjusters.lastDayOfMonth())
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .plusDays(1);
    }

    /** Arma el calendario con las citas del rango (ordenadas por hora de inicio). */
    public static CalendarView of(Mode mode, LocalDate date, LocalDate today, List<AppointmentDetailRow> rows) {
        Map<LocalDate, List<AppointmentDetailRow>> byDay = rows.stream()
                .collect(Collectors.groupingBy(row -> row.startTime().toLocalDate()));
        LocalDate start = rangeStart(mode, date);
        LocalDate end = rangeEnd(mode, date);
        List<Week> weeks = new ArrayList<>();
        for (LocalDate monday = start; monday.isBefore(end); monday = monday.plusWeeks(1)) {
            List<Day> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate day = monday.plusDays(i);
                List<AppointmentDetailRow> all = byDay.getOrDefault(day, List.of());
                boolean inPeriod = mode == Mode.WEEK || day.getMonth() == date.getMonth();
                int shown = mode == Mode.MONTH ? Math.min(all.size(), MONTH_DAY_LIMIT) : all.size();
                days.add(new Day(day, inPeriod, day.equals(today), all.subList(0, shown), all.size() - shown));
            }
            weeks.add(new Week(days));
        }
        LocalDate previous = mode == Mode.MONTH ? date.minusMonths(1) : date.minusWeeks(1);
        LocalDate next = mode == Mode.MONTH ? date.plusMonths(1) : date.plusWeeks(1);
        return new CalendarView(mode, date, today, weeks, previous, next);
    }

    /** Primer y último día mostrados (para el título de la vista de semana). */
    public LocalDate firstDay() {
        return weeks.get(0).days().get(0).date();
    }

    public LocalDate lastDay() {
        List<Day> lastWeek = weeks.get(weeks.size() - 1).days();
        return lastWeek.get(lastWeek.size() - 1).date();
    }
}
