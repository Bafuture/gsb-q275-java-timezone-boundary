package com.example.gsb.tz;

import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 组件运行统计：参与计算的时区数、遇到的不存在时刻与不唯一时刻次数。
 * 线程安全，可在多个计算组件之间共享。
 */
public final class TimeZoneStatistics {

    private final Set<String> zonesUsed = ConcurrentHashMap.newKeySet();
    private final AtomicLong nonExistentCount = new AtomicLong();
    private final AtomicLong ambiguousCount = new AtomicLong();

    public void recordZone(ZoneId zone) {
        zonesUsed.add(zone.getId());
    }

    public void recordNonExistent() {
        nonExistentCount.incrementAndGet();
    }

    public void recordAmbiguous() {
        ambiguousCount.incrementAndGet();
    }

    /** 参与过计算的时区数量。 */
    public int zoneCount() {
        return zonesUsed.size();
    }

    /** 参与过计算的时区 ID 列表（只读快照）。 */
    public Set<String> zonesUsed() {
        return Set.copyOf(zonesUsed);
    }

    /** 累计遇到的不存在（春季跳过）本地时间次数。 */
    public long nonExistentCount() {
        return nonExistentCount.get();
    }

    /** 累计遇到的不唯一（秋季重复）本地时间次数。 */
    public long ambiguousCount() {
        return ambiguousCount.get();
    }

    public void reset() {
        zonesUsed.clear();
        nonExistentCount.set(0);
        ambiguousCount.set(0);
    }

    @Override
    public String toString() {
        return "TimeZoneStatistics{zones=" + zoneCount()
                + ", nonExistent=" + nonExistentCount()
                + ", ambiguous=" + ambiguousCount() + '}';
    }
}
