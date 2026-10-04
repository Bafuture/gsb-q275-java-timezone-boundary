package com.example.gsb.timezone;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 本地时间在给定时区不唯一（秋季重复的 overlap 时段）且策略为 FAIL 时抛出。
 */
public class AmbiguousLocalTimeException extends RuntimeException {

    private final LocalDateTime localDateTime;
    private final ZoneId zone;

    public AmbiguousLocalTimeException(LocalDateTime localDateTime, ZoneId zone) {
        super("本地时间 " + localDateTime + " 在时区 " + zone + " 不唯一（处于秋季拨回被重复的时段），且消歧策略为 FAIL");
        this.localDateTime = localDateTime;
        this.zone = zone;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public ZoneId getZone() {
        return zone;
    }
}
