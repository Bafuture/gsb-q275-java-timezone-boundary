package com.example.gsb.timezone;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 本地时间在给定时区不存在（春季跳过的 gap 时段）时抛出。
 */
public class NonexistentLocalTimeException extends RuntimeException {

    private final LocalDateTime localDateTime;
    private final ZoneId zone;

    public NonexistentLocalTimeException(LocalDateTime localDateTime, ZoneId zone) {
        super("本地时间 " + localDateTime + " 在时区 " + zone + " 不存在（处于春季拨快被跳过的时段）");
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
