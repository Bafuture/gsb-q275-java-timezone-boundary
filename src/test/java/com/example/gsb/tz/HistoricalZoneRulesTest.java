package com.example.gsb.tz;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 历史时区规则测试。
 *
 * 数据来源：JDK 内置的 IANA Time Zone Database（tzdata），
 * 位于 JRE 的 lib/tzdb.dat，由 java.time.ZoneRulesProvider 加载。
 * 历史偏移（如 1900 年上海的 LMT +08:05:43、二战期间美国的 War Time）
 * 均来自该数据库，可用 JDK 的 TZUpdater 工具升级。
 */
class HistoricalZoneRulesTest {

    private final TimeZoneStatistics statistics = new TimeZoneStatistics();
    private final DayBoundaryCalculator calculator = new DayBoundaryCalculator(statistics);

    @Test
    void shanghaiUsedLocalMeanTimeBefore1928() {
        // tzdb：Asia/Shanghai 在 1928 年前使用 LMT，UTC+08:05:43
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        ZonedDateTime noon1900 =
                ZonedDateTime.of(1900, 1, 1, 12, 0, 0, 0, shanghai);

        assertThat(noon1900.getOffset()).isEqualTo(ZoneOffset.of("+08:05:43"));
        // 现代上海固定 UTC+8
        assertThat(ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, shanghai).getOffset())
                .isEqualTo(ZoneOffset.ofHours(8));
    }

    @Test
    void newYorkObservedWarTimeIn1945() {
        // 1942-02-09 至 1945-09-30 美国全年实行 War Time（EDT，UTC-4）
        ZoneId newYork = ZoneId.of("America/New_York");
        ZonedDateTime vjDay = ZonedDateTime.of(1945, 8, 14, 19, 0, 0, 0, newYork);

        assertThat(vjDay.getOffset()).isEqualTo(ZoneOffset.ofHours(-4));
        assertThat(newYork.getRules().isDaylightSavings(vjDay.toInstant())).isTrue();
        // 1945-10-01 起恢复标准时 UTC-5
        assertThat(ZonedDateTime.of(1945, 10, 1, 12, 0, 0, 0, newYork).getOffset())
                .isEqualTo(ZoneOffset.ofHours(-5));
    }

    @Test
    void londonUsedDoubleSummerTimeIn1941() {
        // 二战期间英国实行 BDST（UTC+2）
        ZoneId london = ZoneId.of("Europe/London");
        ZonedDateTime summer1941 = ZonedDateTime.of(1941, 7, 1, 12, 0, 0, 0, london);

        assertThat(summer1941.getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    void historicalDayBoundaryUsesHistoricalRules() {
        // 用历史规则计算过去的日期：1945-09-30 是 War Time 结束日，纽约当天 25 小时
        DayBoundary day = calculator.boundaryOf(
                ZoneId.of("America/New_York"), LocalDate.of(1945, 9, 30));
        assertThat(day.hours()).isEqualTo(25);

        // 1883-11-18 美国铁路标准时启用前，纽约使用 LMT（UTC-04:56:02）
        ZoneId newYork = ZoneId.of("America/New_York");
        ZonedDateTime beforeRailwayTime =
                ZonedDateTime.of(1883, 11, 17, 12, 0, 0, 0, newYork);
        assertThat(beforeRailwayTime.getOffset())
                .isEqualTo(ZoneOffset.of("-04:56:02"));
    }

    @Test
    void historicalInstantConvertsConsistently() {
        // 1945-08-14T23:00Z（对日胜利时刻）在各时区的历史表示
        Instant vjDay = Instant.parse("1945-08-14T23:00:00Z");
        TimeZoneConverter converter = new TimeZoneConverter(statistics);

        ZonedDateTime ny = converter.convert(vjDay, ZoneId.of("America/New_York"));
        ZonedDateTime sh = converter.convert(vjDay, ZoneId.of("Asia/Shanghai"));

        assertThat(ny.getOffset()).isEqualTo(ZoneOffset.ofHours(-4)); // EWT
        assertThat(ny.toLocalDateTime().toString()).isEqualTo("1945-08-14T19:00");
        // tzdb：1945 年夏季中国实行夏令时 CDT（UTC+9），与现代固定 UTC+8 不同
        assertThat(sh.getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(sh.toLocalDateTime().toString()).isEqualTo("1945-08-15T08:00");
        assertThat(converter.verifyRoundTrip(ny, ZoneId.of("Asia/Shanghai"))).isTrue();
    }
}
