package com.sigecin.report.dto;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Semanas del mes según v_weekly_distribution: 1 = días 1–7, … 5 = días 29 en adelante. */
class WeeklyDistributionTests {

    @Test
    void weeksAndDayRanges() {
        assertThat(WeeklyDistribution.weeksIn(YearMonth.of(2026, 2))).isEqualTo(4);   // 28 días
        assertThat(WeeklyDistribution.weeksIn(YearMonth.of(2028, 2))).isEqualTo(5);   // bisiesto: 29 días
        assertThat(WeeklyDistribution.weeksIn(YearMonth.of(2026, 10))).isEqualTo(5);

        WeeklyDistribution october = new WeeklyDistribution(YearMonth.of(2026, 10), List.of(3L, 0L, 5L, 1L, 2L));
        assertThat(WeeklyDistribution.firstDay(5)).isEqualTo(29);
        assertThat(october.lastDay(5)).isEqualTo(31);
        assertThat(october.total()).isEqualTo(11);
        assertThat(october.max()).isEqualTo(5);
    }
}
