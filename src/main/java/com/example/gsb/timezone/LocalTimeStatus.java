package com.example.gsb.timezone;

/**
 * 本地时间在某时区中的存在性状态。
 */
public enum LocalTimeStatus {
    /** 唯一存在：恰好对应一个 UTC 偏移。 */
    UNIQUE,
    /** 不存在：处于春季拨快被跳过的时段（gap）。 */
    GAP,
    /** 不唯一：处于秋季拨回被重复的时段（overlap），对应两个偏移。 */
    OVERLAP
}
