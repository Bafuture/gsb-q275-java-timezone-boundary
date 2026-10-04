# 时区与夏令时边界处理组件

Pair-wise GSB 标注任务仓库（第 17 批 / 275）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java, Maven, JUnit 5 |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

> 本仓库是**初始环境快照**：只有工程骨架，不含任何实现代码。
> 分支说明：`main` 为初始环境；`A`、`B` 为两次独立执行各自的工作分支，均从 `main` 的同一个提交拉出。

## 运行方式

```bash
./mvnw -q verify
```

## 实现说明

组件位于 `src/main/java/com/example/gsb/tz/`，零第三方依赖（仅 JDK `java.time`）：

| 类 | 职责 |
|----|------|
| `DayBoundaryCalculator` / `DayBoundary` | 按给定时区计算某天的半开区间 `[start, end)`，夏令时切换当天自动为 23/25 小时 |
| `LocalTimeInspector` / `LocalTimeInspection` / `LocalTimeStatus` | 判断本地时间唯一 / 不存在（春季 gap）/ 不唯一（秋季 overlap），返回三态明确结果及有效偏移、切换详情 |
| `LocalDateTimeResolver` / `DisambiguationStrategy` | 本地时间 → 时刻解析；overlap 按可配置策略 `EARLIER`（取第一次）/ `LATER`（取第二次）/ `REJECT`（抛 `AmbiguousLocalTimeException`）消歧；gap 前移到空档结束后第一个有效时刻 |
| `PeriodicTrigger` | 每日本地时间周期任务的下一次触发计算；跨切换不重复、不跳过，触发序列严格单调递增 |
| `TimeZoneConverter` | 同一时刻在不同时区间转换（`withZoneSameInstant`），并提供往返一致性校验 |
| `TimeZoneStatistics` | 线程安全统计：参与计算的时区数、不存在时刻与不唯一时刻累计次数 |

边界行为（gap 前移、overlap 消歧、切换当天 23/25 小时）均通过 JUL（`java.util.logging`）输出边界日志，测试中用自定义 `Handler` 捕获验证。

### 历史时区规则的数据来源

历史规则（如 1900 年上海的 LMT `+08:05:43`、1945 年美国的 War Time、1941 年英国的 BDST `+02:00`、1945 年上海的夏令时 `+09:00`）全部来自 **JDK 内置的 IANA Time Zone Database（tzdata）**，即 JRE `lib/tzdb.dat`，由 `java.time.zone.ZoneRulesProvider` 加载；tzdata 版本随 JDK 更新，也可用 Oracle 的 TZUpdater 工具单独升级。因此组件无需任何外部数据文件即可正确计算过去日期的起止与偏移。

### 测试

`src/test/java/com/example/gsb/tz/` 共 32 个用例，覆盖：切换当天起止与 23/25 小时、存在性三态判断、三种消歧策略、周期任务跨春季/秋季切换（含全年 365 次单调性校验）、时区转换往返一致、历史规则（上海 LMT、美国 War Time、英国 BDST）与边界日志捕获。

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的定时任务与统计区间在全球多个时区运行，夏令时切换当天经常出现重复执行或漏执行。请从零实现一个时区与夏令时边界处理组件。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持按给定时区计算某一天的起止时刻，夏令时切换当天要正确反映实际小时数（23 或 25 小时）；2) 支持判断某个本地时间是否存在（春季跳过的时段）或不唯一（秋季重复的时段），两者都要能返回明确结果；3) 支持不唯一时间的消歧策略（取第一次、取第二次或报错），策略可配置；4) 支持按本地时间的周期任务计算下一次触发时刻，跨夏令时切换时不得重复或跳过；5) 支持时区转换：把同一时刻在不同时区之间转换，需验证往返一致；6) 支持历史时区规则：使用给定时区的历史规则计算过去的日期，需说明依赖的数据来源；7) 提供统计：参与计算的时区数、遇到的不存在时刻与不唯一时刻次数；8) 测试覆盖夏令时切换当天起止、存在性判断、消歧策略、周期任务跨切换、往返一致与边界日志；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。
