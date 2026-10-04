package com.example.gsb.timezone;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 某时区某一天的起止时刻。
 *
 * <p>start 为该日 00:00 对应的实际时刻；end 为次日 00:00 对应的实际时刻。
 * 夏令时切换当天的 {@code actualDuration} 为 23 或 25 小时
 * （历史上还存在更长的偏移，例如 2011 年萨摩亚直接跳过一整天）。</p>
 */
public record DayBounds(ZoneId zone,
                        LocalDate date,
                        Instant start,
                        Instant end,
                        Duration actualDuration) {

    /** 该日实际小时数，例如 23.0 / 24.0 / 25.0。 */
    public double actualHours() {
        return actualDuration.toMillis() / 3_600_000.0;
    }
}
