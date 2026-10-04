package com.example.gsb.tz;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** 消歧策略为 REJECT 且本地时间不唯一时抛出。 */
public class AmbiguousLocalTimeException extends RuntimeException {

    public AmbiguousLocalTimeException(ZoneId zone, LocalDateTime localDateTime) {
        super("本地时间 " + localDateTime + " 在时区 " + zone.getId()
                + " 中不唯一（秋季重复时段），消歧策略为 REJECT");
    }
}
