package com.sigecin.business.web.form;

import com.sigecin.business.dto.DaySchedule;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Un renglón del horario semanal. Las horas solo son obligatorias si el día está abierto. */
public class DayForm {

    private DayOfWeek day;

    private boolean open;

    // <input type="time"> envía HH:mm
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    public DayForm() {
    }

    public DayForm(DayOfWeek day, boolean open, LocalTime openTime, LocalTime closeTime) {
        this.day = day;
        this.open = open;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public DaySchedule toData() {
        return new DaySchedule(day, open, openTime, closeTime);
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}
