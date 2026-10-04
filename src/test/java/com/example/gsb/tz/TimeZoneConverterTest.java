package com.example.gsb.tz;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeZoneConverterTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    private final TimeZoneStatistics statistics = new TimeZoneStatistics();
    private final TimeZoneConverter converter = new TimeZoneConverter(statistics);

    @Test
    void sameInstantHasDifferentLocalRepresentations() {
        ZonedDateTime ny = ZonedDateTime.of(2025, 7, 1, 8, 0, 0, 0, NEW_YORK);

        ZonedDateTime sh = converter.convert(ny, SHANGHAI);
        ZonedDateTime tk = converter.convert(ny, TOKYO);

        assertThat(sh.toLocalDateTime()).isEqualTo(LocalDateTime.of(2025, 7, 1, 20, 0));
        assertThat(tk.toLocalDateTime()).isEqualTo(LocalDateTime.of(2025, 7, 1, 21, 0));
        assertThat(ny.toInstant()).isEqualTo(sh.toInstant()).isEqualTo(tk.toInstant());
    }

    @Test
    void roundTripIsConsistentAcrossDstTransition() {
        // 跨越纽约春季切换的时刻
        ZonedDateTime ny = ZonedDateTime.of(2025, 3, 9, 1, 30, 0, 0, NEW_YORK);

        assertThat(converter.verifyRoundTrip(ny, SHANGHAI)).isTrue();
        assertThat(converter.verifyRoundTrip(ny, TOKYO)).isTrue();
        assertThat(converter.verifyRoundTrip(ny, LONDON)).isTrue();

        ZonedDateTime roundTripped =
                converter.convert(converter.convert(ny, SHANGHAI), NEW_YORK);
        assertThat(roundTripped.toInstant()).isEqualTo(ny.toInstant());
        assertThat(roundTripped.getOffset()).isEqualTo(ny.getOffset());
    }

    @Test
    void convertInstantDirectly() {
        Instant instant = Instant.parse("2025-11-02T05:30:00Z");
        ZonedDateTime ny = converter.convert(instant, NEW_YORK);
        assertThat(ny.toLocalDateTime()).isEqualTo(LocalDateTime.of(2025, 11, 2, 1, 30));
        assertThat(ny.getOffset()).isEqualTo(ZoneOffset.ofHours(-4));
    }

    @Test
    void conversionRecordsZonesInStatistics() {
        ZonedDateTime ny = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, NEW_YORK);
        converter.convert(ny, SHANGHAI);
        converter.convert(ny, TOKYO);

        assertThat(statistics.zoneCount()).isEqualTo(3);
        assertThat(statistics.zonesUsed())
                .containsExactlyInAnyOrder("America/New_York", "Asia/Shanghai", "Asia/Tokyo");
    }
}
