package com.example.gsb.tz;

/**
 * 不唯一（秋季重复）本地时间的消歧策略。
 */
public enum DisambiguationStrategy {
    /** 取第一次出现（转换前的偏移，通常是夏令时偏移）。 */
    EARLIER,
    /** 取第二次出现（转换后的偏移，通常是标准时偏移）。 */
    LATER,
    /** 拒绝消歧，抛出 {@link AmbiguousLocalTimeException}。 */
    REJECT
}
