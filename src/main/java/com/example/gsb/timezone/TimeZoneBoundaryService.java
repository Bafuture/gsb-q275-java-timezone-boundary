package com.example.gsb.timezone;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 时区与夏令时边界处理组件。
 *
 * <p>历史与未来的时区规则均来自 JDK 内置的 IANA Time Zone Database（tzdb，
 * 位于 JDK 的 {@code lib/tzdb.dat}），可通过 {@link #tzdbVersion()} 查看版本，
 * 也可使用 Oracle TZUpdater 工具升级数据。</p>
 */
public final class TimeZoneBoundaryService {

    private static final int MAX_SCHEDULE_SCAN_DAYS = 3700;

    private final TimeZoneStatistics statistics = new TimeZoneStatistics();
    private final List<BoundaryEvent> eventLog = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public TimeZoneBoundaryService() {
        this(Clock.systemUTC());
    }

    public TimeZoneBoundaryService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // ------------------------------------------------------------------
    // 1) 某时区某一天的起止时刻
    // ------------------------------------------------------------------

    /**
     * 计算 {@code zone} 时区中 {@code date} 当天的起止时刻。
     * 夏令时切换当天的实际时长为 23 或 25 小时。
     */
    public DayBounds dayBounds(ZoneId zone, LocalDate date) {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(date, "date");
        statistics.recordZone(zone);
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        return new DayBounds(zone, date, start, end, Duration.between(start, end));
    }

    // ------------------------------------------------------------------
    // 2) 本地时间存在性 / 唯一性判断
    // ------------------------------------------------------------------

    /**
     * 判断本地时间在给定时区中唯一存在、不存在（gap）还是不唯一（overlap）。
     */
    public LocalTimeInspection inspect(ZoneId zone, LocalDateTime localDateTime) {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(localDateTime, "localDateTime");
        statistics.recordZone(zone);

        ZoneRules rules = zone.getRules();
        List<java.time.ZoneOffset> offsets = rules.getValidOffsets(localDateTime);

        if (offsets.size() == 1) {
            return new LocalTimeInspection(LocalTimeStatus.UNIQUE, zone, localDateTime,
                    List.copyOf(offsets), Optional.empty());
        }
        if (offsets.size() == 2) {
            statistics.recordOverlap();
            log(BoundaryEvent.Type.OVERLAP_ENCOUNTERED, zone, localDateTime,
                    "合法偏移: " + offsets);
            return new LocalTimeInspection(LocalTimeStatus.OVERLAP, zone, localDateTime,
                    List.copyOf(offsets), Optional.empty());
        }
        statistics.recordGap();
        ZoneOffsetTransition transition = rules.getTransition(localDateTime);
        Duration gap = transition != null ? transition.getDuration() : Duration.ZERO;
        log(BoundaryEvent.Type.GAP_ENCOUNTERED, zone, localDateTime,
                "被跳过的时长: " + gap);
        return new LocalTimeInspection(LocalTimeStatus.GAP, zone, localDateTime,
                List.of(), Optional.of(gap));
    }

    // ------------------------------------------------------------------
    // 3) 带消歧策略的本地时间解析
    // ------------------------------------------------------------------

    /**
     * 把本地时间解析为确定的时刻。
     *
     * <p>overlap 按 {@code strategy} 消歧（EARLIER / LATER / FAIL）；
     * gap 统一向前平移被跳过的时长（例如 02:30 不存在时落到 03:30），
     * 保证周期任务不会因春季拨快而丢失触发。</p>
     */
    public ZonedDateTime resolve(ZoneId zone, LocalDateTime localDateTime,
                                 AmbiguityStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        LocalTimeInspection inspection = inspect(zone, localDateTime);

        switch (inspection.status()) {
            case UNIQUE:
                return ZonedDateTime.of(localDateTime, zone);
            case OVERLAP:
                return resolveOverlap(zone, localDateTime, inspection, strategy);
            case GAP:
            default:
                Duration gap = inspection.gapDuration().orElse(Duration.ZERO);
                LocalDateTime shifted = localDateTime.plus(gap);
                log(BoundaryEvent.Type.GAP_SHIFTED_FORWARD, zone, localDateTime,
                        "前移 " + gap + " 至 " + shifted);
                return ZonedDateTime.ofLocal(shifted, zone, null);
        }
    }

    private ZonedDateTime resolveOverlap(ZoneId zone, LocalDateTime localDateTime,
                                         LocalTimeInspection inspection,
                                         AmbiguityStrategy strategy) {
        List<java.time.ZoneOffset> offsets = inspection.validOffsets();
        switch (strategy) {
            case EARLIER:
                log(BoundaryEvent.Type.OVERLAP_RESOLVED, zone, localDateTime,
                        "策略 EARLIER，取第一次出现 " + offsets.get(0));
                return ZonedDateTime.ofLocal(localDateTime, zone, offsets.get(0));
            case LATER:
                log(BoundaryEvent.Type.OVERLAP_RESOLVED, zone, localDateTime,
                        "策略 LATER，取第二次出现 " + offsets.get(1));
                return ZonedDateTime.ofLocal(localDateTime, zone, offsets.get(1));
            case FAIL:
            default:
                throw new AmbiguousLocalTimeException(localDateTime, zone);
        }
    }

    // ------------------------------------------------------------------
    // 4) 本地时间周期任务的下一次触发
    // ------------------------------------------------------------------

    /**
     * 计算“每天在 {@code zone} 的本地时间 {@code timeOfDay} 触发”的任务，
     * 严格晚于 {@code after} 的下一次触发时刻。
     *
     * <p>在本地日期域逐日推进再转换为时刻，并严格要求结果晚于上次触发，
     * 因此跨夏令时切换时既不重复（秋季 25 小时天只触发一次）也不跳过
     * （春季不存在的时刻被前移修正）。</p>
     */
    public ZonedDateTime nextTrigger(ZoneId zone, LocalTime timeOfDay, Instant after,
                                     AmbiguityStrategy strategy) {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(timeOfDay, "timeOfDay");
        Objects.requireNonNull(after, "after");
        statistics.recordZone(zone);

        LocalDate startDate = after.atZone(zone).toLocalDate();
        for (int i = 0; i < MAX_SCHEDULE_SCAN_DAYS; i++) {
            LocalDateTime candidateLocal = LocalDateTime.of(startDate.plusDays(i), timeOfDay);
            ZonedDateTime candidate = resolve(zone, candidateLocal, strategy);
            if (candidate.toInstant().isAfter(after)) {
                return candidate;
            }
        }
        throw new IllegalStateException("在 " + MAX_SCHEDULE_SCAN_DAYS + " 天内未找到下一次触发时刻");
    }

    // ------------------------------------------------------------------
    // 5) 时区转换与往返一致性
    // ------------------------------------------------------------------

    /** 把同一时刻从 {@code from} 时区表示转换为 {@code to} 时区表示。 */
    public ZonedDateTime convert(Instant instant, ZoneId from, ZoneId to) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        statistics.recordZone(from);
        statistics.recordZone(to);
        return instant.atZone(from).withZoneSameInstant(to);
    }

    /**
     * 验证往返一致：from → to → from 后仍是同一时刻。
     */
    public boolean verifyRoundTrip(Instant instant, ZoneId from, ZoneId to) {
        ZonedDateTime there = convert(instant, from, to);
        ZonedDateTime back = convert(there.toInstant(), to, from);
        return back.toInstant().equals(instant) && back.getOffset().equals(instant.atZone(from).getOffset());
    }

    // ------------------------------------------------------------------
    // 6) 历史时区规则数据来源
    // ------------------------------------------------------------------

    /**
     * 返回 JDK 内置 IANA tzdb 的数据版本（例如 "2024a"）。
     * 历史日期（如 1991 年中国夏令时、1945 年美国战时时间）均按该数据计算。
     */
    public String tzdbVersion() {
        return ZoneRulesProvider.getVersions("UTC").lastKey();
    }

    // ------------------------------------------------------------------
    // 7) 统计与边界日志
    // ------------------------------------------------------------------

    public TimeZoneStatistics statistics() {
        return statistics;
    }

    /** 边界事件日志（只读快照）。 */
    public List<BoundaryEvent> events() {
        return List.copyOf(eventLog);
    }

    private void log(BoundaryEvent.Type type, ZoneId zone,
                     LocalDateTime localDateTime, String detail) {
        eventLog.add(new BoundaryEvent(Instant.now(clock), type, zone, localDateTime, detail));
    }
}
