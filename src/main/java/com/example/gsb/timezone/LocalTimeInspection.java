package com.example.gsb.timezone;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * 本地时间存在性检查结果。
 *
 * @param status   UNIQUE / GAP / OVERLAP
 * @param zone     检查使用的时区
 * @param localDateTime 被检查的本地时间
 * @param validOffsets 合法偏移列表：UNIQUE 时 1 个，OVERLAP 时 2 个（按时间先后排列），GAP 时为空
 * @param gapDuration  GAP 被跳过的时长（通常 1 小时），非 GAP 时为空
 */
public record LocalTimeInspection(LocalTimeStatus status,
                                  ZoneId zone,
                                  LocalDateTime localDateTime,
                                  List<ZoneOffset> validOffsets,
                                  Optional<Duration> gapDuration) {

    /** 该本地时间是否唯一存在。 */
    public boolean exists() {
        return status == LocalTimeStatus.UNIQUE;
    }

    /** 该本地时间是否不唯一（对应两个时刻）。 */
    public boolean isAmbiguous() {
        return status == LocalTimeStatus.OVERLAP;
    }
}
