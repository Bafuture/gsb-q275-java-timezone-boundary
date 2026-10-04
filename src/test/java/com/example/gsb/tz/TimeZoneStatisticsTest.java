package com.example.gsb.tz;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeZoneStatisticsTest {

    @Test
    void sharedStatisticsAccumulateAcrossComponents() {
        TimeZoneStatistics statistics = new TimeZoneStatistics();
        LocalTimeInspector inspector = new LocalTimeInspector(statistics);
        DayBoundaryCalculator calculator = new DayBoundaryCalculator(statistics);

        ZoneId newYork = ZoneId.of("America/New_York");
        ZoneId berlin = ZoneId.of("Europe/Berlin");

        calculator.boundaryOf(newYork, java.time.LocalDate.of(2025, 3, 9));
        calculator.boundaryOf(berlin, java.time.LocalDate.of(2025, 3, 30));
        inspector.inspect(newYork, LocalDateTime.of(2025, 3, 9, 2, 30));   // 不存在
        inspector.inspect(newYork, LocalDateTime.of(2025, 11, 2, 1, 30));  // 不唯一
        inspector.inspect(berlin, LocalDateTime.of(2025, 10, 26, 2, 30));  // 不唯一
        inspector.inspect(newYork, LocalDateTime.of(2025, 6, 1, 12, 0));   // 唯一

        assertThat(statistics.zoneCount()).isEqualTo(2);
        assertThat(statistics.zonesUsed())
                .containsExactlyInAnyOrder("America/New_York", "Europe/Berlin");
        assertThat(statistics.nonExistentCount()).isEqualTo(1);
        assertThat(statistics.ambiguousCount()).isEqualTo(2);
    }

    @Test
    void resetClearsAllCounters() {
        TimeZoneStatistics statistics = new TimeZoneStatistics();
        LocalTimeInspector inspector = new LocalTimeInspector(statistics);
        inspector.inspect(ZoneId.of("America/New_York"),
                LocalDateTime.of(2025, 11, 2, 1, 30));

        statistics.reset();

        assertThat(statistics.zoneCount()).isZero();
        assertThat(statistics.nonExistentCount()).isZero();
        assertThat(statistics.ambiguousCount()).isZero();
    }
}
