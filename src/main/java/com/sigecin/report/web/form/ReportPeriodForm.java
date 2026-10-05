package com.sigecin.report.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

/** Año y mes del reporte mensual (parámetros de GET /business/reports/monthly). */
@Getter
@Setter
public class ReportPeriodForm {

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    public static ReportPeriodForm of(YearMonth period) {
        ReportPeriodForm form = new ReportPeriodForm();
        form.year = period.getYear();
        form.month = period.getMonthValue();
        return form;
    }

    public YearMonth toYearMonth() {
        return YearMonth.of(year, month);
    }
}
