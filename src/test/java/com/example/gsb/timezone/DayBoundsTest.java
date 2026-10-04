package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DayBoundsTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private final TimeZoneBoundaryService service = new TimeZoneBoundaryService();

    @Test
    void 春季拨快当天为23小时() {
        DayBounds bounds = service.dayBounds(NEW_YORK, LocalDate.of(2024, 3, 10));

        assertThat(bounds.start()).isEqualTo(Instant.parse("2024-03-10T05:00:00Z"));
        assertThat(bounds.end()).isEqualTo(Instant.parse("2024-03-11T04:00:00Z"));
        assertThat(bounds.actualDuration()).isEqualTo(Duration.ofHours(23));
        assertThat(bounds.actualHours()).isEqualTo(23.0);
    }

    @Test
    void 秋季拨回当天为25小时() {
        DayBounds bounds = service.dayBounds(NEW_YORK, LocalDate.of(2024, 11, 3));

        assertThat(bounds.start()).isEqualTo(Instant.parse("2024-11-03T04:00:00Z"));
        assertThat(bounds.end()).isEqualTo(Instant.parse("2024-11-04T05:00:00Z"));
        assertThat(bounds.actualDuration()).isEqualTo(Duration.ofHours(25));
        assertThat(bounds.actualHours()).isEqualTo(25.0);
    }

    @Test
    void 普通日期为24小时() {
        DayBounds bounds = service.dayBounds(NEW_YORK, LocalDate.of(2024, 6, 15));
        assertThat(bounds.actualDuration()).isEqualTo(Duration.ofHours(24));
    }
}
