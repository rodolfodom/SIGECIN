package com.sigecin.business.dto;

import com.sigecin.business.entity.Business;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Horario de un día tal como se guarda: si {@code open} es falso el día queda inactivo. */
public record DaySchedule(DayOfWeek day, boolean open, LocalTime openTime, LocalTime closeTime) {

    /**
     * Los siete días, de lunes a domingo. Un día sin registro va cerrado y sin horas; uno
     * desactivado va cerrado pero conserva sus horas. El horario del negocio debe estar cargado.
     */
    public static List<DaySchedule> weekOf(Business business) {
        List<DaySchedule> week = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            week.add(business.getSchedules().stream()
                    .filter(s -> s.getDayOfWeek() == day)
                    .findFirst()
                    .map(s -> new DaySchedule(day, s.isActive(), s.getOpenTime(), s.getCloseTime()))
                    .orElse(new DaySchedule(day, false, null, null)));
        }
        return week;
    }
}
