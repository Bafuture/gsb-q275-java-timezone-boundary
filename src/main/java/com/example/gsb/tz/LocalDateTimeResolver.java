package com.example.gsb.tz;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 将本地时间解析为时区内的具体时刻（{@link ZonedDateTime}）。
 *
 * 规则：
 * - 唯一：直接解析；
 * - 不唯一（秋季 overlap）：按可配置的 {@link DisambiguationStrategy} 处理
 *   （取第一次 / 取第二次 / 报错）；
 * - 不存在（春季 gap）：向前平移到该空档之后的第一个有效时刻
 *   （等价于 java.time 默认的前移规则，保证任务不被跳过）。
 * 每次边界处理都会通过 JUL 输出边界日志，并计入统计。
 */
public final class LocalDateTimeResolver {

    private static final Logger LOG = Logger.getLogger(LocalDateTimeResolver.class.getName());

    private final LocalTimeInspector inspector;
    private final DisambiguationStrategy ambiguousStrategy;

    public LocalDateTimeResolver(TimeZoneStatistics statistics,
                                 DisambiguationStrategy ambiguousStrategy) {
        this(new LocalTimeInspector(statistics), ambiguousStrategy);
    }

    public LocalDateTimeResolver(LocalTimeInspector inspector,
                                 DisambiguationStrategy ambiguousStrategy) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.ambiguousStrategy = Objects.requireNonNull(ambiguousStrategy, "ambiguousStrategy");
    }

    /**
     * 解析本地时间。
     *
     * @param zone          时区
     * @param localDateTime 本地时间
     * @return 对应的带时区时刻；gap 时为前移到空档结束处的时刻
     * @throws AmbiguousLocalTimeException overlap 且策略为 REJECT 时抛出
     */
    public ZonedDateTime resolve(ZoneId zone, LocalDateTime localDateTime) {
        LocalTimeInspection inspection = inspector.inspect(zone, localDateTime);
        return switch (inspection.status()) {
            case UNIQUE -> ZonedDateTime.of(localDateTime, zone);
            case AMBIGUOUS -> resolveAmbiguous(inspection);
            case NON_EXISTENT -> resolveGap(inspection);
        };
    }

    private ZonedDateTime resolveAmbiguous(LocalTimeInspection inspection) {
        if (ambiguousStrategy == DisambiguationStrategy.REJECT) {
            LOG.log(Level.WARNING, "不唯一本地时间且策略为 REJECT: zone={0}, local={1}, offsets={2}",
                    new Object[]{inspection.zone().getId(), inspection.localDateTime(),
                            inspection.validOffsets()});
            throw new AmbiguousLocalTimeException(inspection.zone(), inspection.localDateTime());
        }

        List<ZoneOffset> offsets = inspection.validOffsets();
        ZoneOffsetTransition t = inspection.transition().orElseThrow();
        // 秋季回拨：transition.getOffsetBefore() 是第一次出现时的偏移，
        // getOffsetAfter() 是第二次出现时的偏移。
        ZoneOffset chosen = switch (ambiguousStrategy) {
            case EARLIER -> t.getOffsetBefore();
            case LATER -> t.getOffsetAfter();
            case REJECT -> throw new IllegalStateException("unreachable");
        };
        if (!offsets.contains(chosen)) {
            chosen = offsets.get(0);
        }
        ZonedDateTime result = ZonedDateTime.ofLocal(inspection.localDateTime(),
                inspection.zone(), chosen);
        LOG.log(Level.INFO,
                "不唯一本地时间消歧: zone={0}, local={1}, strategy={2}, offsets={3}, chosen={4}",
                new Object[]{inspection.zone().getId(), inspection.localDateTime(),
                        ambiguousStrategy, offsets, chosen});
        return result;
    }

    private ZonedDateTime resolveGap(LocalTimeInspection inspection) {
        ZoneOffsetTransition t = inspection.transition().orElseThrow();
        // 前移到空档结束后的第一个有效本地时间，保证任务不丢。
        // getInstant() 即切换发生的绝对时刻，也是空档结束后的第一个有效时刻
        ZonedDateTime result = ZonedDateTime.ofInstant(t.getInstant(), inspection.zone());
        LOG.log(Level.INFO,
                "不存在本地时间（春季跳过）前移: zone={0}, local={1}, gap={2}~{3}, resolved={4}",
                new Object[]{inspection.zone().getId(), inspection.localDateTime(),
                        t.getDateTimeBefore(), t.getDateTimeAfter(), result});
        return result;
    }

    public DisambiguationStrategy strategy() {
        return ambiguousStrategy;
    }
}
