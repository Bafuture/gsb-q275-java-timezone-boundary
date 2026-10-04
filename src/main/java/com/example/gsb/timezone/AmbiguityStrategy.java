package com.example.gsb.timezone;

/**
 * 不唯一（秋季重复时段）本地时间的消歧策略。
 */
public enum AmbiguityStrategy {
    /** 取第一次出现（即偏移较大的那个，夏令时一侧）。 */
    EARLIER,
    /** 取第二次出现（即偏移较小的那个，标准时一侧）。 */
    LATER,
    /** 直接抛出 {@link AmbiguousLocalTimeException}。 */
    FAIL
}
