package com.example.gsb.tz;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * 时区转换：把同一时刻在不同时区之间转换。
 * 转换只改变本地表示，不改变对应的绝对时刻，因此往返转换必然一致。
 */
public final class TimeZoneConverter {

    private final TimeZoneStatistics statistics;

    public TimeZoneConverter(TimeZoneStatistics statistics) {
        this.statistics = Objects.requireNonNull(statistics, "statistics");
    }

    /** 将时刻从源时区表示转换为目标时区表示，绝对时刻不变。 */
    public ZonedDateTime convert(ZonedDateTime source, ZoneId targetZone) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetZone, "targetZone");
        statistics.recordZone(source.getZone());
        statistics.recordZone(targetZone);
        return source.withZoneSameInstant(targetZone);
    }

    /** 将绝对时刻转换为目标时区的本地表示。 */
    public ZonedDateTime convert(Instant instant, ZoneId targetZone) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(targetZone, "targetZone");
        statistics.recordZone(targetZone);
        return instant.atZone(targetZone);
    }

    /**
     * 往返一致性校验：source -> target -> 原时区，必须回到同一时刻。
     *
     * @return true 当且仅当往返后的时刻与源时刻指向同一 Instant
     */
    public boolean verifyRoundTrip(ZonedDateTime source, ZoneId targetZone) {
        ZonedDateTime roundTripped = convert(convert(source, targetZone), source.getZone());
        return roundTripped.toInstant().equals(source.toInstant());
    }
}
