package com.sigecin.business.web.form;

import com.sigecin.business.dto.DaySchedule;
import com.sigecin.business.entity.Business;
import org.springframework.validation.Errors;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Horario de la semana completa: siempre siete renglones, de lunes a domingo. */
public class ScheduleForm {

    // Horas sugeridas para un día que nunca se ha configurado
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(18, 0);

    private List<DayForm> days = new ArrayList<>();

    public static ScheduleForm from(Business business) {
        ScheduleForm form = new ScheduleForm();
        for (DaySchedule day : DaySchedule.weekOf(business)) {
            form.days.add(day.openTime() == null
                    ? new DayForm(day.day(), false, DEFAULT_OPEN, DEFAULT_CLOSE)
                    : new DayForm(day.day(), day.open(), day.openTime(), day.closeTime()));
        }
        return form;
    }

    /** Reglas que dependen de varios campos: un día abierto necesita horas y el cierre va después de la apertura. */
    public void validate(Errors errors) {
        for (int i = 0; i < days.size(); i++) {
            DayForm day = days.get(i);
            if (!day.isOpen() || errors.hasFieldErrors("days[" + i + "].*")) {
                continue;
            }
            if (day.getOpenTime() == null) {
                errors.rejectValue("days[" + i + "].openTime", "NotNull");
            }
            if (day.getCloseTime() == null) {
                errors.rejectValue("days[" + i + "].closeTime", "NotNull");
            }
            if (day.getOpenTime() != null && day.getCloseTime() != null
                    && !day.getCloseTime().isAfter(day.getOpenTime())) {
                errors.rejectValue("days[" + i + "].closeTime", "business.schedule.close-before-open");
            }
        }
    }

    public List<DaySchedule> toData() {
        return days.stream().map(DayForm::toData).toList();
    }

    public List<DayForm> getDays() {
        return days;
    }

    public void setDays(List<DayForm> days) {
        this.days = days;
    }
}
