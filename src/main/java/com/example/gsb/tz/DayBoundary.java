package com.example.gsb.tz;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 某一天在指定时区中的时间区间。
 *
 * @param zone     时区
 * @param date     本地日期
 * @param start    当天 00:00（含）对应的时刻
 * @param end      次日 00:00（不含）对应的时刻
 * @param duration 实际时长，夏令时切换当天为 23 或 25 小时，其余为 24 小时
 */
public record DayBoundary(ZoneId zone, LocalDate date, Instant start, Instant end,
                          Duration duration) {

    public DayBoundary {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("day duration must be positive: " + duration);
        }
    }

    /** 实际小时数（23、24 或 25）。 */
    public long hours() {
        return duration.toHours();
    }

    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return !instant.isBefore(start) && instant.isBefore(end);
    }
}
