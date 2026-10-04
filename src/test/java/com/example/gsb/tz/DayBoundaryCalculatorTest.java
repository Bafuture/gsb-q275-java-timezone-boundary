package com.example.gsb.tz;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DayBoundaryCalculatorTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final DayBoundaryCalculator calculator =
            new DayBoundaryCalculator(new TimeZoneStatistics());

    @Test
    void springForwardDayIsTwentyThreeHoursInNewYork() {
        // 2025-03-09 02:00 拨到 03:00，当天只有 23 小时
        DayBoundary day = calculator.boundaryOf(NEW_YORK, LocalDate.of(2025, 3, 9));

        assertThat(day.hours()).isEqualTo(23);
        assertThat(day.duration()).isEqualTo(Duration.ofHours(23));
        // 00:00 仍为 EST(-05:00)，次日 00:00 已为 EDT(-04:00)
        assertThat(day.start().toString()).isEqualTo("2025-03-09T05:00:00Z");
        assertThat(day.end().toString()).isEqualTo("2025-03-10T04:00:00Z");
        assertThat(day.contains(day.start())).isTrue();
        assertThat(day.contains(day.end())).isFalse();
    }

    @Test
    void fallBackDayIsTwentyFiveHoursInNewYork() {
        // 2025-11-02 02:00 拨回 01:00，当天有 25 小时
        DayBoundary day = calculator.boundaryOf(NEW_YORK, LocalDate.of(2025, 11, 2));

        assertThat(day.hours()).isEqualTo(25);
        assertThat(day.duration()).isEqualTo(Duration.ofHours(25));
        assertThat(day.start().toString()).isEqualTo("2025-11-02T04:00:00Z");
        assertThat(day.end().toString()).isEqualTo("2025-11-03T05:00:00Z");
    }

    @Test
    void normalDayIsTwentyFourHours() {
        DayBoundary day = calculator.boundaryOf(NEW_YORK, LocalDate.of(2025, 6, 15));
        assertThat(day.hours()).isEqualTo(24);
        assertThat(day.duration()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void berlinTransitionsAreTwentyThreeAndTwentyFiveHours() {
        assertThat(calculator.boundaryOf(BERLIN, LocalDate.of(2025, 3, 30)).hours())
                .isEqualTo(23);
        assertThat(calculator.boundaryOf(BERLIN, LocalDate.of(2025, 10, 26)).hours())
                .isEqualTo(25);
    }

    @Test
    void nonDstZoneIsAlwaysTwentyFourHours() {
        // 上海自 1991 年起不再实行夏令时
        assertThat(calculator.boundaryOf(SHANGHAI, LocalDate.of(2025, 3, 9)).hours())
                .isEqualTo(24);
        assertThat(calculator.boundaryOf(SHANGHAI, LocalDate.of(2025, 11, 2)).hours())
                .isEqualTo(24);
    }
}
