package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeZoneConversionTest {

    private final TimeZoneBoundaryService service = new TimeZoneBoundaryService();

    @Test
    void 同一时刻在不同时区之间转换() {
        Instant instant = Instant.parse("2024-03-10T07:30:00Z");
        ZoneId newYork = ZoneId.of("America/New_York");
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        ZoneId berlin = ZoneId.of("Europe/Berlin");

        ZonedDateTime ny = service.convert(instant, newYork, newYork);
        ZonedDateTime sh = service.convert(instant, newYork, shanghai);
        ZonedDateTime be = service.convert(instant, newYork, berlin);

        assertThat(ny.toLocalDateTime()).isEqualTo(LocalDateTime.of(2024, 3, 10, 3, 30));
        assertThat(sh.toLocalDateTime()).isEqualTo(LocalDateTime.of(2024, 3, 10, 15, 30));
        assertThat(be.toLocalDateTime()).isEqualTo(LocalDateTime.of(2024, 3, 10, 8, 30));
        assertThat(sh.toInstant()).isEqualTo(instant);
        assertThat(be.toInstant()).isEqualTo(instant);
    }

    @Test
    void 跨夏令时边界往返一致() {
        Instant[] samples = {
                Instant.parse("2024-03-10T06:59:59Z"),
                Instant.parse("2024-03-10T07:00:00Z"),
                Instant.parse("2024-11-03T05:30:00Z"),
                Instant.parse("2024-11-03T06:30:00Z"),
                Instant.parse("1945-07-01T12:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z")
        };

        for (Instant instant : samples) {
            assertThat(service.verifyRoundTrip(instant,
                    ZoneId.of("America/New_York"), ZoneId.of("Asia/Shanghai")))
                    .as("往返一致: %s", instant).isTrue();
            assertThat(service.verifyRoundTrip(instant,
                    ZoneId.of("Europe/Berlin"), ZoneId.of("Pacific/Auckland")))
                    .as("往返一致: %s", instant).isTrue();
        }
    }

    @Test
    void 转换结果偏移符合规则() {
        Instant instant = Instant.parse("2024-07-01T12:00:00Z");
        ZonedDateTime ny = service.convert(instant, ZoneId.of("UTC"), ZoneId.of("America/New_York"));
        assertThat(ny.getOffset()).isEqualTo(ZoneOffset.ofHours(-4));
    }
}
