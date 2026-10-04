package com.example.gsb.tz;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 按给定时区计算某一天的起止时刻。
 * 起止时刻直接取自本地午夜在该时区的真实映射，因此夏令时切换当天
 * 的区间时长自动反映实际小时数（春季 23 小时、秋季 25 小时）。
 */
public final class DayBoundaryCalculator {

    private static final Logger LOG = Logger.getLogger(DayBoundaryCalculator.class.getName());

    private final TimeZoneStatistics statistics;

    public DayBoundaryCalculator(TimeZoneStatistics statistics) {
        this.statistics = Objects.requireNonNull(statistics, "statistics");
    }

    /**
     * 计算 {@code date} 在 {@code zone} 中的半开区间 [start, end)。
     */
    public DayBoundary boundaryOf(ZoneId zone, LocalDate date) {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(date, "date");
        statistics.recordZone(zone);

        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        DayBoundary boundary = new DayBoundary(zone, date, start, end,
                java.time.Duration.between(start, end));

        long hours = boundary.hours();
        if (hours != 24) {
            LOG.info(() -> "夏令时切换当天: zone=" + zone.getId() + ", date=" + date
                    + ", actualHours=" + hours + ", start=" + start + ", end=" + end);
        }
        return boundary;
    }
}
