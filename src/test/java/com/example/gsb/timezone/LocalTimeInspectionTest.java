package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LocalTimeInspectionTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private final TimeZoneBoundaryService service = new TimeZoneBoundaryService();

    @Test
    void 普通本地时间唯一存在() {
        LocalTimeInspection result =
                service.inspect(NEW_YORK, LocalDateTime.of(2024, 6, 15, 12, 30));

        assertThat(result.status()).isEqualTo(LocalTimeStatus.UNIQUE);
        assertThat(result.exists()).isTrue();
        assertThat(result.isAmbiguous()).isFalse();
        assertThat(result.validOffsets()).containsExactly(ZoneOffset.ofHours(-4));
        assertThat(result.gapDuration()).isEmpty();
    }

    @Test
    void 春季跳过的本地时间不存在() {
        LocalTimeInspection result =
                service.inspect(NEW_YORK, LocalDateTime.of(2024, 3, 10, 2, 30));

        assertThat(result.status()).isEqualTo(LocalTimeStatus.GAP);
        assertThat(result.exists()).isFalse();
        assertThat(result.validOffsets()).isEmpty();
        assertThat(result.gapDuration()).contains(Duration.ofHours(1));
    }

    @Test
    void 秋季重复的本地时间不唯一() {
        LocalTimeInspection result =
                service.inspect(NEW_YORK, LocalDateTime.of(2024, 11, 3, 1, 30));

        assertThat(result.status()).isEqualTo(LocalTimeStatus.OVERLAP);
        assertThat(result.isAmbiguous()).isTrue();
        assertThat(result.validOffsets())
                .containsExactly(ZoneOffset.ofHours(-4), ZoneOffset.ofHours(-5));
    }
}
