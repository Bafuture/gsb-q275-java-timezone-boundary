package com.example.gsb.timezone;

import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 组件运行统计：参与计算的时区集合、遇到的 gap 与 overlap 次数。线程安全。
 */
public final class TimeZoneStatistics {

    private final Set<ZoneId> zonesUsed = ConcurrentHashMap.newKeySet();
    private final AtomicLong gapCount = new AtomicLong();
    private final AtomicLong overlapCount = new AtomicLong();

    void recordZone(ZoneId zone) {
        zonesUsed.add(zone);
    }

    void recordGap() {
        gapCount.incrementAndGet();
    }

    void recordOverlap() {
        overlapCount.incrementAndGet();
    }

    /** 参与计算的时区数量。 */
    public int zoneCount() {
        return zonesUsed.size();
    }

    /** 参与计算的时区（只读快照）。 */
    public Set<ZoneId> zonesUsed() {
        return Set.copyOf(zonesUsed);
    }

    /** 遇到的不存在（gap）时刻次数。 */
    public long gapCount() {
        return gapCount.get();
    }

    /** 遇到的不唯一（overlap）时刻次数。 */
    public long overlapCount() {
        return overlapCount.get();
    }

    @Override
    public String toString() {
        return "TimeZoneStatistics{zones=" + zoneCount()
                + ", gaps=" + gapCount()
                + ", overlaps=" + overlapCount() + '}';
    }
}
