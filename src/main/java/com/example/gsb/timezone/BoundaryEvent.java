package com.example.gsb.timezone;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 边界事件日志条目：组件在遇到 gap / overlap 等边界情况时记录。
 */
public record BoundaryEvent(Instant loggedAt,
                            Type type,
                            ZoneId zone,
                            LocalDateTime localDateTime,
                            String detail) {

    public enum Type {
        /** 遇到不存在的本地时间（gap）。 */
        GAP_ENCOUNTERED,
        /** 遇到不唯一的本地时间（overlap）。 */
        OVERLAP_ENCOUNTERED,
        /** gap 时间被前移修正。 */
        GAP_SHIFTED_FORWARD,
        /** overlap 时间按策略消歧。 */
        OVERLAP_RESOLVED
    }
}
