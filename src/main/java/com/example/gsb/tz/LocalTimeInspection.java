package com.example.gsb.tz;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 对某个本地时间在指定时区中存在性检查的结果。
 *
 * @param zone           时区
 * @param localDateTime  被检查的本地时间
 * @param status         存在性状态
 * @param validOffsets   该本地时间对应的所有有效偏移（0、1 或 2 个）
 * @param transition     若处于夏令时切换边界，则给出切换详情；否则为空
 */
public record LocalTimeInspection(
        ZoneId zone,
        LocalDateTime localDateTime,
        LocalTimeStatus status,
        List<ZoneOffset> validOffsets,
        Optional<ZoneOffsetTransition> transition) {

    public LocalTimeInspection {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(localDateTime, "localDateTime");
        Objects.requireNonNull(status, "status");
        validOffsets = List.copyOf(validOffsets);
        transition = transition == null ? Optional.empty() : transition;
    }

    public boolean exists() {
        return status != LocalTimeStatus.NON_EXISTENT;
    }

    public boolean isUnique() {
        return status == LocalTimeStatus.UNIQUE;
    }

    public boolean isAmbiguous() {
        return status == LocalTimeStatus.AMBIGUOUS;
    }
}
