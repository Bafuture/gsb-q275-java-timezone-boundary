package com.example.gsb.tz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * 按本地时间的每日周期任务：每天在指定时区的某个本地时间触发一次。
 *
 * 跨夏令时切换的保证：
 * - 春季跳过：触发时间落在空档内时，前移到空档结束后的第一个有效时刻，
 *   任务不丢失；
 * - 秋季重复：按可配置策略只取一次（第一次 / 第二次 / 报错），任务不重复；
 * - 下一次触发时刻严格大于上一次，序列单调递增。
 */
public final class PeriodicTrigger {

    private final ZoneId zone;
    private final LocalTime localTime;
    private final LocalDateTimeResolver resolver;

    public PeriodicTrigger(ZoneId zone, LocalTime localTime,
                           LocalDateTimeResolver resolver) {
        this.zone = Objects.requireNonNull(zone, "zone");
        this.localTime = Objects.requireNonNull(localTime, "localTime");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * 计算严格晚于 {@code after} 的下一次触发时刻。
     *
     * @param after 参考时刻（通常为上一次触发时刻或当前时间）
     */
    public ZonedDateTime nextAfter(ZonedDateTime after) {
        Objects.requireNonNull(after, "after");
        ZonedDateTime reference = after.withZoneSameInstant(zone);

        LocalDate date = reference.toLocalDate();
        ZonedDateTime candidate = triggerOn(date);
        while (!candidate.toInstant().isAfter(after.toInstant())) {
            date = date.plusDays(1);
            candidate = triggerOn(date);
        }
        return candidate;
    }

    /** 某一天（本地日期）的触发时刻，已应用 gap 前移与 overlap 消歧。 */
    public ZonedDateTime triggerOn(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return resolver.resolve(zone, LocalDateTime.of(date, localTime));
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalTime localTime() {
        return localTime;
    }
}
