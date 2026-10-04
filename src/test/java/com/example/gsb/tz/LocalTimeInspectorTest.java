package com.example.gsb.tz;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LocalTimeInspectorTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private final TimeZoneStatistics statistics = new TimeZoneStatistics();
    private final LocalTimeInspector inspector = new LocalTimeInspector(statistics);

    @Test
    void skippedSpringTimeIsNonExistent() {
        // 2025-03-09 02:00~03:00 在纽约不存在
        LocalTimeInspection result =
                inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 3, 9, 2, 30));

        assertThat(result.status()).isEqualTo(LocalTimeStatus.NON_EXISTENT);
        assertThat(result.exists()).isFalse();
        assertThat(result.isAmbiguous()).isFalse();
        assertThat(result.validOffsets()).isEmpty();
        assertThat(result.transition()).isPresent();
        assertThat(statistics.nonExistentCount()).isEqualTo(1);
    }

    @Test
    void repeatedFallTimeIsAmbiguousWithTwoOffsets() {
        // 2025-11-02 01:00~02:00 在纽约出现两次（EDT -04:00 与 EST -05:00）
        LocalTimeInspection result =
                inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 11, 2, 1, 30));

        assertThat(result.status()).isEqualTo(LocalTimeStatus.AMBIGUOUS);
        assertThat(result.exists()).isTrue();
        assertThat(result.isAmbiguous()).isTrue();
        assertThat(result.validOffsets())
                .containsExactly(ZoneOffset.ofHours(-4), ZoneOffset.ofHours(-5));
        assertThat(result.transition()).isPresent();
        assertThat(statistics.ambiguousCount()).isEqualTo(1);
    }

    @Test
    void regularTimeIsUnique() {
        LocalTimeInspection summer =
                inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 7, 1, 12, 0));
        assertThat(summer.status()).isEqualTo(LocalTimeStatus.UNIQUE);
        assertThat(summer.validOffsets()).containsExactly(ZoneOffset.ofHours(-4));
        assertThat(summer.transition()).isEmpty();

        LocalTimeInspection winter =
                inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 1, 1, 12, 0));
        assertThat(winter.status()).isEqualTo(LocalTimeStatus.UNIQUE);
        assertThat(winter.validOffsets()).containsExactly(ZoneOffset.ofHours(-5));
    }

    @Test
    void boundaryEndpointsAreNotAmbiguous() {
        // 03:00（切换后）唯一，02:00（切换前最后一刻起点）不存在
        assertThat(inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 3, 9, 3, 0)).status())
                .isEqualTo(LocalTimeStatus.UNIQUE);
        assertThat(inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 3, 9, 1, 59)).status())
                .isEqualTo(LocalTimeStatus.UNIQUE);
        // 秋季：02:00 已唯一（只属于 EST），00:30 唯一（只属于 EDT）
        assertThat(inspector.inspect(NEW_YORK, LocalDateTime.of(2025, 11, 2, 2, 0)).status())
                .isEqualTo(LocalTimeStatus.UNIQUE);
    }
}
