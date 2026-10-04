package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmbiguityStrategyTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalDateTime OVERLAP = LocalDateTime.of(2024, 11, 3, 1, 30);
    private static final LocalDateTime GAP = LocalDateTime.of(2024, 3, 10, 2, 30);

    private final TimeZoneBoundaryService service = new TimeZoneBoundaryService();

    @Test
    void EARLIER取第一次出现() {
        ZonedDateTime resolved = service.resolve(NEW_YORK, OVERLAP, AmbiguityStrategy.EARLIER);

        assertThat(resolved.toInstant()).isEqualTo(Instant.parse("2024-11-03T05:30:00Z"));
        assertThat(resolved.getOffset().getTotalSeconds()).isEqualTo(-4 * 3600);
    }

    @Test
    void LATER取第二次出现() {
        ZonedDateTime resolved = service.resolve(NEW_YORK, OVERLAP, AmbiguityStrategy.LATER);

        assertThat(resolved.toInstant()).isEqualTo(Instant.parse("2024-11-03T06:30:00Z"));
        assertThat(resolved.getOffset().getTotalSeconds()).isEqualTo(-5 * 3600);
    }

    @Test
    void FAIL策略抛出异常() {
        assertThatThrownBy(() -> service.resolve(NEW_YORK, OVERLAP, AmbiguityStrategy.FAIL))
                .isInstanceOf(AmbiguousLocalTimeException.class)
                .hasMessageContaining("2024-11-03T01:30");
    }

    @Test
    void 不存在的时刻统一前移被跳过的时长() {
        ZonedDateTime resolved = service.resolve(NEW_YORK, GAP, AmbiguityStrategy.EARLIER);

        assertThat(resolved.toLocalDateTime()).isEqualTo(LocalDateTime.of(2024, 3, 10, 3, 30));
        assertThat(resolved.toInstant()).isEqualTo(Instant.parse("2024-03-10T07:30:00Z"));
    }

    @Test
    void 唯一存在的时间不受策略影响() {
        LocalDateTime normal = LocalDateTime.of(2024, 6, 15, 12, 0);
        for (AmbiguityStrategy strategy : AmbiguityStrategy.values()) {
            assertThat(service.resolve(NEW_YORK, normal, strategy).toInstant())
                    .isEqualTo(Instant.parse("2024-06-15T16:00:00Z"));
        }
    }
}
