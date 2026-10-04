package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatisticsAndLogTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Test
    void 统计参与时区与gap_overlap次数() {
        TimeZoneBoundaryService svc = new TimeZoneBoundaryService();

        svc.dayBounds(NEW_YORK, java.time.LocalDate.of(2024, 3, 10));
        svc.inspect(NEW_YORK, LocalDateTime.of(2024, 3, 10, 2, 30)); // gap
        svc.resolve(NEW_YORK, LocalDateTime.of(2024, 3, 10, 2, 45), AmbiguityStrategy.EARLIER); // gap
        svc.inspect(NEW_YORK, LocalDateTime.of(2024, 11, 3, 1, 30)); // overlap
        svc.resolve(NEW_YORK, LocalDateTime.of(2024, 11, 3, 1, 15), AmbiguityStrategy.LATER); // overlap
        svc.convert(Instant.parse("2024-06-01T00:00:00Z"), NEW_YORK, SHANGHAI);

        TimeZoneStatistics stats = svc.statistics();
        assertThat(stats.zoneCount()).isEqualTo(2);
        assertThat(stats.zonesUsed()).containsExactlyInAnyOrder(NEW_YORK, SHANGHAI);
        assertThat(stats.gapCount()).isEqualTo(2);
        assertThat(stats.overlapCount()).isEqualTo(2);
        assertThat(stats.toString()).contains("zones=2", "gaps=2", "overlaps=2");
    }

    @Test
    void 边界事件日志记录完整() {
        TimeZoneBoundaryService svc = new TimeZoneBoundaryService();

        svc.resolve(NEW_YORK, LocalDateTime.of(2024, 3, 10, 2, 30), AmbiguityStrategy.EARLIER);
        assertThatThrownBy(() ->
                svc.resolve(NEW_YORK, LocalDateTime.of(2024, 11, 3, 1, 30),
                        AmbiguityStrategy.FAIL))
                .isInstanceOf(AmbiguousLocalTimeException.class);

        assertThat(svc.events()).extracting(BoundaryEvent::type)
                .contains(
                        BoundaryEvent.Type.GAP_ENCOUNTERED,
                        BoundaryEvent.Type.GAP_SHIFTED_FORWARD,
                        BoundaryEvent.Type.OVERLAP_ENCOUNTERED);

        BoundaryEvent gapEvent = svc.events().stream()
                .filter(e -> e.type() == BoundaryEvent.Type.GAP_SHIFTED_FORWARD)
                .findFirst().orElseThrow();
        assertThat(gapEvent.zone()).isEqualTo(NEW_YORK);
        assertThat(gapEvent.localDateTime()).isEqualTo(LocalDateTime.of(2024, 3, 10, 2, 30));
        assertThat(gapEvent.detail()).contains("PT1H", "03:30");
        assertThat(gapEvent.loggedAt()).isNotNull();
    }

    @Test
    void 日志时间戳来自可注入时钟() {
        Instant fixed = Instant.parse("2030-01-01T00:00:00Z");
        TimeZoneBoundaryService svc =
                new TimeZoneBoundaryService(Clock.fixed(fixed, java.time.ZoneOffset.UTC));

        svc.nextTrigger(NEW_YORK, LocalTime.of(2, 30),
                Instant.parse("2024-03-09T20:00:00Z"), AmbiguityStrategy.EARLIER);

        assertThat(svc.events()).isNotEmpty();
        assertThat(svc.events()).allSatisfy(e -> assertThat(e.loggedAt()).isEqualTo(fixed));
    }
}
