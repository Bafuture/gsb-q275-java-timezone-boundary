package com.example.gsb.tz;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodicTriggerTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private PeriodicTrigger trigger(DisambiguationStrategy strategy, LocalTime time) {
        TimeZoneStatistics stats = new TimeZoneStatistics();
        LocalDateTimeResolver resolver = new LocalDateTimeResolver(stats, strategy);
        return new PeriodicTrigger(NEW_YORK, time, resolver);
    }

    @Test
    void dailyJobInsideSpringGapIsShiftedForwardAndNotSkipped() {
        // 每天 02:30 触发；2025-03-09 该时段被跳过
        PeriodicTrigger schedule = trigger(DisambiguationStrategy.EARLIER, LocalTime.of(2, 30));

        ZonedDateTime before = schedule.triggerOn(LocalDate.of(2025, 3, 8));
        ZonedDateTime transitionDay = schedule.triggerOn(LocalDate.of(2025, 3, 9));
        ZonedDateTime after = schedule.triggerOn(LocalDate.of(2025, 3, 10));

        assertThat(before.getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(before.toLocalTime()).isEqualTo(LocalTime.of(2, 30));

        // 切换当天不跳过：前移到 03:00 EDT
        assertThat(transitionDay.toLocalTime()).isEqualTo(LocalTime.of(3, 0));
        assertThat(transitionDay.getOffset()).isEqualTo(ZoneOffset.ofHours(-4));

        assertThat(after.toLocalTime()).isEqualTo(LocalTime.of(2, 30));
        assertThat(after.getOffset()).isEqualTo(ZoneOffset.ofHours(-4));

        // 三个触发时刻严格递增、日期各不相同，无重复无跳过
        assertThat(before.toInstant()).isBefore(transitionDay.toInstant());
        assertThat(transitionDay.toInstant()).isBefore(after.toInstant());
    }

    @Test
    void dailyJobInFallOverlapFiresExactlyOncePerDay() {
        // 每天 01:30 触发；2025-11-02 01:30 出现两次
        PeriodicTrigger earlier = trigger(DisambiguationStrategy.EARLIER, LocalTime.of(1, 30));
        PeriodicTrigger later = trigger(DisambiguationStrategy.LATER, LocalTime.of(1, 30));

        ZonedDateTime first = earlier.triggerOn(LocalDate.of(2025, 11, 2));
        ZonedDateTime second = later.triggerOn(LocalDate.of(2025, 11, 2));

        assertThat(first.getOffset()).isEqualTo(ZoneOffset.ofHours(-4)); // EDT 第一次
        assertThat(second.getOffset()).isEqualTo(ZoneOffset.ofHours(-5)); // EST 第二次
        assertThat(first.toInstant()).isBefore(second.toInstant());

        // 无论哪种策略，从切换前一天到切换后一天都恰好 2 个步进、3 次触发
        List<ZonedDateTime> seqEarlier = walk(earlier, LocalDate.of(2025, 11, 1), 3);
        List<ZonedDateTime> seqLater = walk(later, LocalDate.of(2025, 11, 1), 3);
        assertStrictlyIncreasingAndDistinctDays(seqEarlier);
        assertStrictlyIncreasingAndDistinctDays(seqLater);
        assertThat(seqEarlier.get(1).toInstant().toString())
                .isEqualTo("2025-11-02T05:30:00Z");
        assertThat(seqLater.get(1).toInstant().toString())
                .isEqualTo("2025-11-02T06:30:00Z");
    }

    @Test
    void nextAfterWalksStrictlyForwardAcrossTransitionsForWholeYear() {
        PeriodicTrigger schedule = trigger(DisambiguationStrategy.EARLIER, LocalTime.of(0, 15));

        ZonedDateTime start = schedule.triggerOn(LocalDate.of(2025, 1, 1));
        List<ZonedDateTime> triggers = new ArrayList<>();
        triggers.add(start);
        ZonedDateTime cursor = start;
        for (int i = 1; i < 365; i++) {
            cursor = schedule.nextAfter(cursor);
            triggers.add(cursor);
        }

        // 恰好 365 次（2025 非闰年），每次对应一个不同的本地日期
        assertThat(triggers).hasSize(365);
        assertStrictlyIncreasingAndDistinctDays(triggers);
        // 最后一次落在 2025-12-31 00:15
        assertThat(triggers.get(364).toLocalDate()).isEqualTo(LocalDate.of(2025, 12, 31));

        // 相邻两次的真实间隔：普通天 24h，春季切换当天->次日 23h，秋季当天->次日 25h
        ZonedDateTime springA = triggers.get(indexOfDate(triggers, LocalDate.of(2025, 3, 9)));
        ZonedDateTime springB = triggers.get(indexOfDate(triggers, LocalDate.of(2025, 3, 10)));
        assertThat(java.time.Duration.between(springA, springB)).hasHours(23);

        ZonedDateTime fallA = triggers.get(indexOfDate(triggers, LocalDate.of(2025, 11, 2)));
        ZonedDateTime fallB = triggers.get(indexOfDate(triggers, LocalDate.of(2025, 11, 3)));
        assertThat(java.time.Duration.between(fallA, fallB)).hasHours(25);
    }

    @Test
    void nextAfterStrictlyAfterReferenceEvenSameDay() {
        PeriodicTrigger schedule = trigger(DisambiguationStrategy.EARLIER, LocalTime.of(10, 0));
        ZonedDateTime morning = LocalDate.of(2025, 6, 1).atTime(9, 0)
                .atZone(NEW_YORK);
        ZonedDateTime next = schedule.nextAfter(morning);
        assertThat(next.toLocalDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(next.toLocalTime()).isEqualTo(LocalTime.of(10, 0));

        ZonedDateTime afterScheduled = LocalDate.of(2025, 6, 1).atTime(10, 0)
                .atZone(NEW_YORK);
        ZonedDateTime nextAgain = schedule.nextAfter(afterScheduled);
        assertThat(nextAgain.toLocalDate()).isEqualTo(LocalDate.of(2025, 6, 2));
    }

    private static List<ZonedDateTime> walk(PeriodicTrigger schedule, LocalDate from, int count) {
        List<ZonedDateTime> result = new ArrayList<>();
        ZonedDateTime cursor = schedule.triggerOn(from);
        result.add(cursor);
        for (int i = 1; i < count; i++) {
            cursor = schedule.nextAfter(cursor);
            result.add(cursor);
        }
        return result;
    }

    private static void assertStrictlyIncreasingAndDistinctDays(List<ZonedDateTime> triggers) {
        for (int i = 1; i < triggers.size(); i++) {
            assertThat(triggers.get(i - 1).toInstant())
                    .as("trigger %d must be strictly after %d", i, i - 1)
                    .isBefore(triggers.get(i).toInstant());
            assertThat(triggers.get(i - 1).toLocalDate())
                    .as("each day must fire exactly once")
                    .isBefore(triggers.get(i).toLocalDate());
        }
    }

    private static int indexOfDate(List<ZonedDateTime> triggers, LocalDate date) {
        for (int i = 0; i < triggers.size(); i++) {
            if (triggers.get(i).toLocalDate().equals(date)) {
                return i;
            }
        }
        throw new AssertionError("date not found: " + date);
    }
}
