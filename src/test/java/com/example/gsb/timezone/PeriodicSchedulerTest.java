package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodicSchedulerTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private final TimeZoneBoundaryService service = new TimeZoneBoundaryService();

    private List<ZonedDateTime> triggerSequence(LocalTime timeOfDay, Instant from,
                                                AmbiguityStrategy strategy, int count) {
        List<ZonedDateTime> triggers = new ArrayList<>();
        Instant cursor = from;
        for (int i = 0; i < count; i++) {
            ZonedDateTime next = service.nextTrigger(NEW_YORK, timeOfDay, cursor, strategy);
            triggers.add(next);
            cursor = next.toInstant();
        }
        return triggers;
    }

    @Test
    void 跨春季拨快不跳过不重复() {
        List<ZonedDateTime> triggers = triggerSequence(
                LocalTime.of(2, 30), Instant.parse("2024-03-08T00:00:00Z"),
                AmbiguityStrategy.EARLIER, 4);

        assertThat(triggers).extracting(t -> t.toInstant().toString())
                .containsExactly(
                        "2024-03-08T07:30:00Z", // 周五 EST
                        "2024-03-09T07:30:00Z", // 周六 EST
                        "2024-03-10T07:30:00Z", // 周日：02:30 不存在，前移为 03:30 EDT
                        "2024-03-11T06:30:00Z"  // 周一 EDT
                );
        assertThat(triggers).extracting(ZonedDateTime::toInstant).doesNotHaveDuplicates();
        for (int i = 1; i < triggers.size(); i++) {
            assertThat(triggers.get(i).toInstant())
                    .isAfter(triggers.get(i - 1).toInstant());
        }
    }

    @Test
    void 跨秋季拨回不重复触发() {
        List<ZonedDateTime> triggers = triggerSequence(
                LocalTime.of(1, 30), Instant.parse("2024-11-01T00:00:00Z"),
                AmbiguityStrategy.EARLIER, 4);

        assertThat(triggers).extracting(t -> t.toInstant().toString())
                .containsExactly(
                        "2024-11-01T05:30:00Z", // 周五 EDT
                        "2024-11-02T05:30:00Z", // 周六 EDT
                        "2024-11-03T05:30:00Z", // 周日：01:30 出现两次，EARLIER 只取第一次
                        "2024-11-04T06:30:00Z"  // 周一 EST
                );
        assertThat(triggers).extracting(ZonedDateTime::toInstant).doesNotHaveDuplicates();
    }

    @Test
    void LATER策略在秋季取第二次出现() {
        ZonedDateTime next = service.nextTrigger(NEW_YORK, LocalTime.of(1, 30),
                Instant.parse("2024-11-03T00:00:00Z"), AmbiguityStrategy.LATER);

        assertThat(next.toInstant()).isEqualTo(Instant.parse("2024-11-03T06:30:00Z"));
    }

    @Test
    void 下一次触发严格晚于参照时刻() {
        Instant after = Instant.parse("2024-06-15T16:00:00Z"); // 当天 12:00 EDT 已过
        ZonedDateTime next = service.nextTrigger(NEW_YORK, LocalTime.NOON, after,
                AmbiguityStrategy.EARLIER);

        assertThat(next.toInstant()).isEqualTo(Instant.parse("2024-06-16T16:00:00Z"));
    }
}
