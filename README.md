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


## 组件说明

核心入口为 `com.example.gsb.timezone.TimeZoneBoundaryService`：

- `dayBounds(zone, date)`：某时区一天的起止时刻，夏令时切换当天实际为 23/25 小时。
- `inspect(zone, localDateTime)`：判断本地时间唯一存在（UNIQUE）、不存在（GAP，春季跳过）或不唯一（OVERLAP，秋季重复）。
- `resolve(zone, localDateTime, strategy)`：解析本地时间；OVERLAP 按 `AmbiguityStrategy`（EARLIER / LATER / FAIL）消歧，GAP 统一前移被跳过的时长。
- `nextTrigger(zone, timeOfDay, after, strategy)`：本地时间周期任务的下一次触发时刻，跨夏令时切换不重复、不跳过。
- `convert` / `verifyRoundTrip`：同一时刻的时区转换与往返一致性验证。
- `statistics()`：参与计算的时区数、gap 与 overlap 次数。
- `events()`：边界事件日志（gap/overlap 的遇到与处理）。

### 历史时区规则的数据来源

历史与未来时区规则均来自 **JDK 内置的 IANA Time Zone Database（tzdb）**，即 JDK 安装目录下的 `lib/tzdb.dat`。数据版本可通过 `TimeZoneBoundaryService.tzdbVersion()` 查询（如 `2024a`）；如需更新规则（例如某国临时调整夏令时），可使用 Oracle TZUpdater 工具升级 JDK 的 tzdb 数据，无需改动本组件代码。

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的定时任务与统计区间在全球多个时区运行，夏令时切换当天经常出现重复执行或漏执行。请从零实现一个时区与夏令时边界处理组件。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持按给定时区计算某一天的起止时刻，夏令时切换当天要正确反映实际小时数（23 或 25 小时）；2) 支持判断某个本地时间是否存在（春季跳过的时段）或不唯一（秋季重复的时段），两者都要能返回明确结果；3) 支持不唯一时间的消歧策略（取第一次、取第二次或报错），策略可配置；4) 支持按本地时间的周期任务计算下一次触发时刻，跨夏令时切换时不得重复或跳过；5) 支持时区转换：把同一时刻在不同时区之间转换，需验证往返一致；6) 支持历史时区规则：使用给定时区的历史规则计算过去的日期，需说明依赖的数据来源；7) 提供统计：参与计算的时区数、遇到的不存在时刻与不唯一时刻次数；8) 测试覆盖夏令时切换当天起止、存在性判断、消歧策略、周期任务跨切换、往返一致与边界日志；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。
