package com.sigecin.business.web.form;

import com.sigecin.business.dto.DaySchedule;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Un renglón del horario semanal. Las horas solo son obligatorias si el día está abierto. */
@Getter
@Setter
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
}
