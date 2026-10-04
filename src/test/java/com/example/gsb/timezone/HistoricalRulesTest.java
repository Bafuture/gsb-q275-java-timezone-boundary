package com.example.gsb.timezone;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 历史时区规则测试。规则数据来自 JDK 内置 IANA tzdb（lib/tzdb.dat），
 * 见 {@code TimeZoneBoundaryService.tzdbVersion()}，可通过 Oracle TZUpdater 升级。
 */
class HistoricalRulesTest {

    private final TimeZoneBoundaryService service = new TimeZoneBoundaryService();

    @Test
    void 纽约1945年战时时间与战后恢复() {
        ZoneId ny = ZoneId.of("America/New_York");

        // 1942-1945 美国实行全年 War Time（东部为 -4），1945-09-30 结束
        assertThat(service.inspect(ny, LocalDateTime.of(1945, 2, 1, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(-4));
        // 1945 年冬天恢复 EST（-5）
        assertThat(service.inspect(ny, LocalDateTime.of(1945, 12, 1, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(-5));
    }

    @Test
    void 伦敦1947年双夏令时切换当天23小时() {
        ZoneId london = ZoneId.of("Europe/London");

        // 1947-04-13 从 BST(+1) 拨快至 DBST(+2)
        assertThat(service.dayBounds(london, LocalDate.of(1947, 4, 13)).actualHours())
                .isEqualTo(23.0);
        assertThat(service.inspect(london, LocalDateTime.of(1947, 7, 1, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(service.inspect(london, LocalDateTime.of(1947, 1, 15, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(0));
    }

    @Test
    void 中国1991年最后一次夏令时() {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");

        // 1986-1991 年中国实行夏令时，夏季 +9，冬季标准 +8
        assertThat(service.inspect(shanghai, LocalDateTime.of(1991, 6, 15, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(service.inspect(shanghai, LocalDateTime.of(1991, 1, 15, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(8));
        // 1992 年起取消夏令时，全年 +8
        assertThat(service.inspect(shanghai, LocalDateTime.of(1992, 6, 15, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(8));
    }

    @Test
    void 历史与未来按同一套tzdb规则计算() {
        ZoneId ny = ZoneId.of("America/New_York");
        // 1918 年纽约首次实行夏令时：7 月为 EDT(-4)，1 月为 EST(-5)
        assertThat(service.inspect(ny, LocalDateTime.of(1918, 7, 1, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(-4));
        assertThat(service.inspect(ny, LocalDateTime.of(1918, 1, 1, 12, 0))
                .validOffsets().get(0)).isEqualTo(ZoneOffset.ofHours(-5));
    }

    @Test
    void tzdb版本可查询() {
        assertThat(service.tzdbVersion()).matches("20\\d{2}[a-z]?");
    }
}
