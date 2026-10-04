package com.example.gsb.tz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDateTimeResolverTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalDateTime AMBIGUOUS = LocalDateTime.of(2025, 11, 2, 1, 30);
    private static final LocalDateTime GAP = LocalDateTime.of(2025, 3, 9, 2, 30);

    private final TimeZoneStatistics statistics = new TimeZoneStatistics();

    @Test
    void earlierStrategyPicksFirstOccurrence() {
        LocalDateTimeResolver resolver =
                new LocalDateTimeResolver(statistics, DisambiguationStrategy.EARLIER);

        ZonedDateTime resolved = resolver.resolve(NEW_YORK, AMBIGUOUS);

        assertThat(resolved.getOffset()).isEqualTo(ZoneOffset.ofHours(-4)); // 第一次：EDT
        assertThat(resolved.toInstant().toString()).isEqualTo("2025-11-02T05:30:00Z");
    }

    @Test
    void laterStrategyPicksSecondOccurrence() {
        LocalDateTimeResolver resolver =
                new LocalDateTimeResolver(statistics, DisambiguationStrategy.LATER);

        ZonedDateTime resolved = resolver.resolve(NEW_YORK, AMBIGUOUS);

        assertThat(resolved.getOffset()).isEqualTo(ZoneOffset.ofHours(-5)); // 第二次：EST
        assertThat(resolved.toInstant().toString()).isEqualTo("2025-11-02T06:30:00Z");
    }

    @Test
    void rejectStrategyThrows() {
        LocalDateTimeResolver resolver =
                new LocalDateTimeResolver(statistics, DisambiguationStrategy.REJECT);

        assertThatThrownBy(() -> resolver.resolve(NEW_YORK, AMBIGUOUS))
                .isInstanceOf(AmbiguousLocalTimeException.class)
                .hasMessageContaining("不唯一")
                .hasMessageContaining("America/New_York");
    }

    @ParameterizedTest
    @EnumSource(value = DisambiguationStrategy.class)
    void nonExistentTimeIsShiftedToFirstValidInstantRegardlessOfStrategy(
            DisambiguationStrategy strategy) {
        LocalDateTimeResolver resolver = new LocalDateTimeResolver(statistics, strategy);

        ZonedDateTime resolved = resolver.resolve(NEW_YORK, GAP);

        // 02:30 不存在，前移到空档结束的 03:00 EDT
        assertThat(resolved.toLocalDateTime().toString()).isEqualTo("2025-03-09T03:00");
        assertThat(resolved.getOffset()).isEqualTo(ZoneOffset.ofHours(-4));
        assertThat(resolved.toInstant().toString()).isEqualTo("2025-03-09T07:00:00Z");
    }

    @Test
    void uniqueTimeIsUnchangedForEveryStrategy() {
        LocalDateTime unique = LocalDateTime.of(2025, 6, 1, 10, 0);
        for (DisambiguationStrategy strategy : DisambiguationStrategy.values()) {
            ZonedDateTime resolved =
                    new LocalDateTimeResolver(statistics, strategy).resolve(NEW_YORK, unique);
            assertThat(resolved.toInstant().toString()).isEqualTo("2025-06-01T14:00:00Z");
        }
    }

    @Test
    void boundaryCasesAreLoggedAndRejectIsLoggedAsWarning() {
        Logger logger = Logger.getLogger(LocalDateTimeResolver.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(capture);
        try {
            new LocalDateTimeResolver(statistics, DisambiguationStrategy.EARLIER)
                    .resolve(NEW_YORK, GAP);
            new LocalDateTimeResolver(statistics, DisambiguationStrategy.LATER)
                    .resolve(NEW_YORK, AMBIGUOUS);
            assertThatThrownBy(() ->
                    new LocalDateTimeResolver(statistics, DisambiguationStrategy.REJECT)
                            .resolve(NEW_YORK, AMBIGUOUS))
                    .isInstanceOf(AmbiguousLocalTimeException.class);

            assertThat(records).hasSize(3);
            assertThat(records.get(0).getMessage()).contains("不存在本地时间");
            assertThat(records.get(1).getMessage()).contains("不唯一本地时间消歧");
            assertThat(records.get(2).getMessage()).contains("REJECT");
        } finally {
            logger.removeHandler(capture);
        }
    }
}
