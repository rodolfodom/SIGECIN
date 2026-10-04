package com.sigecin.business.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Horario de un día tal como se guarda: si {@code open} es falso el día queda inactivo. */
public record DaySchedule(DayOfWeek day, boolean open, LocalTime openTime, LocalTime closeTime) {
}
