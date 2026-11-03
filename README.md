# 颖控 · 安卓调度仪表台

> 先看清，再动手。

颖控是一款面向安卓深度用户的性能与功耗工具箱：实时监控、帧率记录、A/B 对比、性能档位、刷新率、温控、充电控制、应用治理、进程管理、Magisk 模块和自动化，全部收在同一条时间线上。所有写操作先备份、再回读、失败自动还原。

**当前阶段：M1 可点原型 v0.3（模拟数据，不会修改系统）。**

## 原型

- 在线体验：[https://yingge-dev.github.io/yingkong/](https://yingge-dev.github.io/yingkong/)
- 本地打开：直接双击 `index.html`，或起一个静态服务器
- 手机访问体验最佳，建议在浏览器里「添加到主屏幕」全屏使用
- 支持 `#monitor` / `#records` / `#tools` / `#apps` / `#settings` 直达对应页面

### 现在能点

| 页面 | 内容 |
|---|---|
| 控制台 | FPS / 1% low / 抖动、CPU / GPU / 内存 / 温度 / 功耗 / 电池 6 项实时指标、8 核频率与负载、统一时间轴、后台唤醒 |
| 记录 | 调优前后 A/B 曲线、掉帧率 / 1% low / 温度差值、本局 / 今日 / 7 天、场次、自动归因 |
| 工具 | 18 个模块：性能档、刷新率、充电上限、温控墙、帧率记录、CPU 控制、SWAP、进程管理、活动/服务、应用偏见、耗电统计、充电统计、Magisk、附加模块、ORMS、检测工具、备份还原、节点编辑、自动化、导出 |
| 应用 | 搜索、筛选、冻结 / 解冻、待机分组、强制停止 |
| 设置 | 基础 / Shizuku / Root 权限模式、采样频率、设备与能力自检、救援模式 |

工具里的 CPU 控制、进程管理、帧率记录、耗电统计、充电统计、活动/服务已经有可点的数据页；其余模块先给说明与入口。

## 功能对标

Scene 的功能拆解、颖控对应实现和权限要求见 [FEATURES.md](FEATURES.md)。参考官方站 [omarea.com](https://omarea.com/) 与其 8 张实机界面，只学功能与信息结构，不复制界面与素材。

## Android APK（M2 真数据）

包名 `com.yingkong.app`，minSdk 26 / targetSdk 35，debug 签名。

- 下载：[GitHub Releases](https://github.com/yingge-dev/yingkong/releases)
- **零假数据**：所有数值来自 KernelSU root（`/proc/stat`、`cpufreq`、`kgsl`、`power_supply`、`thermal_zone`、`dumpsys cpuinfo`、`dumpsys gfxinfo framestats`）
- FPS / Jank / 99% 帧时来自前台应用的真实帧时间戳；取不到就显示 `—`
- 刷新率读取真实 Display 模式；温度、电压、电流、电池百分比都是真值
- 不含登录、收费、绑机器；全部本地运行
- **记录页已跑通**：前台服务 + 通知栏停止，无悬浮窗、无无障碍；场次保存平均 FPS / 1% low / Jank / 功耗 / 温度 / 时长，重启不丢
- **场次详情**：逐秒 FPS 曲线、Jank 标记、温度/功耗曲线、前 3 秒预热区间；最多保存 1200 个样本点
- **悬浮按钮**：系统级悬浮窗，可拖动；点按开始/结束记录，记录中显示真实 FPS；退回桌面/游戏后仍在，通知栏可隐藏
- 悬浮窗只是系统窗口，不注入、不读内存、不模拟点击；部分游戏可能检测到悬浮窗
- **可选采集模块 v0.5.0**：支持线程绑核（RenderThread/UnityMain/UnityGfx 等绑大核，可一键恢复全核）
- **可选采集模块 v0.4.0**：设置页可安装/卸载 Magisk / KernelSU 模块；原生 C 守护进程覆盖 arm64 / armv7 / x86_64，开机自启，每秒采集 CPU/核心/内存/温度/电池/频率/GPU，并按目标包名采集线程级 CPU（RenderThread/UnityMain 等），写入 `/data/adb/yingkong`；App 优先读模块数据并显示最忙线程，未装模块时回退 `/proc`；不改系统分区，卸载即删除
- 模块安装后建议重启；如果异常，可在 KSU/Magisk 管理器或安全模式下禁用模块
- **FPS 双通道**：普通 App 用 gfxinfo 帧时间；游戏/视频走 SurfaceView 等图层时，用 SurfaceFlinger 图层 frame 计数差值回退，避免只有 0 或 165Hz 合成率
- 已知限制：SurfaceFlinger 回退路径暂时没有 Jank/帧时，需要后续补 FrameTimeline
- **Shizuku 免 root 通道**：安装并启动 Shizuku 后可授权颖控；root 优先，未 root 时控制台/记录/进程/应用管理走 Shizuku shell；已验证 adb/shell 模式 `uid=2000(shell)` 下 `id`、`pm suspend`、`am force-stop` 可用
- **工具 / 进程**：真实进程列表（PID/UID/内存），按内存/名称排序、搜索、结束用户进程；系统进程只读保护
- **应用管理**：用户应用列表、搜索、强停、`pm suspend` 冻结/解冻、待机分组 restricted/active；系统应用不列入，避免误操作
- 应用 Tab 暂未接页面，先不放假数据
- 构建环境：JDK 17 + Android SDK 35 + Gradle 8.9，`android/` 为 Compose 工程
- 构建命令（Windows）：

```powershell
$env:JAVA_HOME='<jdk17>'
$env:ANDROID_HOME='<android-sdk>'
gradle -p android assembleDebug
```

## 设计

- 视觉「晨光」：浅色、玻璃、连续圆角、单一信号色，参考 Xiaomi HyperOS 与 Apple HIG 的方法。
- 字体：Inter（OFL）+ Noto Sans SC。
- 颜色：`#F2F2F7` / `#FFFFFF` / `#1D1D1F` / `#FF6900` / `#0A84FF`。
- 动效：数字滚动、曲线生长、底部弹层、触感反馈。

## 为什么不是又一个 Scene

1. **一条时间轴**：帧率、温度、功耗、充电、后台事件落在同一根轴上。
2. **能验证**：写入 → 回读 → 标「已核验 / 未生效」，失败自动回退。
3. **能对比**：调优前后两条曲线 + 差值 + 分享长图。
4. **配方存语义**：刷新率写「面板最高」，不写死 120；换机自动跟着走。
5. **Shizuku 免 Root**：看数据、管应用不需要 Root；只有写内核节点才要 Root。

## 路线

- **M1** 可点原型（当前）
- **M2** Android 工程 + 控制台只读（Shizuku 接真数据）
- **M3** 记录 / A-B / 导出
- **M4** 调优写入 + 回读核验 + 还原
- **M5** 应用治理 + 进程 / 活动 / 自动化
- **M6** Magisk / 节点 / ORMS / 检测 + 悬浮 HUD
- **M7** 官网
- **M8** 发布

## 说明

- 原型数据全部为模拟值，不会修改任何系统设置。
- 正式版所有写操作都会备份原值、回读核验、支持一键还原。
- 旧项目「真·全局高刷」保持独立：[yingge-dev/global-hfr](https://github.com/yingge-dev/global-hfr)，本仓库不会改动它。

## 目录

```
index.html      原型入口
styles.css      设计系统与页面样式
app.js          交互与模拟数据
FEATURES.md     Scene 功能对标
assets/         字体、图标
```

License: TBD















