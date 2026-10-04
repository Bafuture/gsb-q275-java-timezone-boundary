package com.example.gsb.tz;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 判断某个本地时间在指定时区中是否存在、是否唯一。
 * 所有检查结果都会计入共享的 {@link TimeZoneStatistics}。
 */
public final class LocalTimeInspector {

    private final TimeZoneStatistics statistics;

    public LocalTimeInspector(TimeZoneStatistics statistics) {
        this.statistics = Objects.requireNonNull(statistics, "statistics");
    }

    /**
     * 检查本地时间在给定时区中的存在性。
     *
     * @return 明确的三态结果：唯一 / 不存在 / 不唯一
     */
    public LocalTimeInspection inspect(ZoneId zone, LocalDateTime localDateTime) {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(localDateTime, "localDateTime");
        statistics.recordZone(zone);

        ZoneRules rules = zone.getRules();
        List<ZoneOffset> offsets = rules.getValidOffsets(localDateTime);
        ZoneOffsetTransition transition = rules.getTransition(localDateTime);

        LocalTimeStatus status;
        if (offsets.isEmpty()) {
            status = LocalTimeStatus.NON_EXISTENT;
            statistics.recordNonExistent();
        } else if (offsets.size() == 2) {
            status = LocalTimeStatus.AMBIGUOUS;
            statistics.recordAmbiguous();
        } else {
            status = LocalTimeStatus.UNIQUE;
        }
        return new LocalTimeInspection(zone, localDateTime, status, offsets, Optional.ofNullable(transition));
    }

    /** 本地时间在该时区中是否存在（春季跳过的时段返回 false）。 */
    public boolean exists(ZoneId zone, LocalDateTime localDateTime) {
        return inspect(zone, localDateTime).exists();
    }

    /** 本地时间在该时区中是否不唯一（秋季重复的时段返回 true）。 */
    public boolean isAmbiguous(ZoneId zone, LocalDateTime localDateTime) {
        return inspect(zone, localDateTime).isAmbiguous();
    }
}
