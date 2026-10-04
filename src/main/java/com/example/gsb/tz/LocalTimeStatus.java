package com.example.gsb.tz;

/**
 * 本地时间在某个时区中的存在性状态。
 */
public enum LocalTimeStatus {
    /** 恰好对应一个有效偏移，唯一确定一个时刻。 */
    UNIQUE,
    /** 春季拨快时被跳过，该本地时间不存在（gap）。 */
    NON_EXISTENT,
    /** 秋季拨回时重复出现，对应两个有效偏移（overlap）。 */
    AMBIGUOUS
}
