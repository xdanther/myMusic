# knowledge.md

## 项目是什么

myMusic：面向 Android 6.0+（手机 / 平板 / 折叠屏 / 电视）的音乐客户端，单 Activity + Jetpack Compose（Material 3 Expressive），
平台音源层是原生 Kotlin 实现（移植自 `third_party/music-lib`，不需要后端服务）。
包名 `com.mytv.mymusic`，许可证 AGPL-3.0。`README.md` 是功能与交互的权威说明（很长，按模块分节）。

## 模块

| 路径 | 作用 |
| --- | --- |
| `app/` | 全部应用代码、资源、清单、单元测试与设备测试 |
| `baselineprofile/` | `com.android.test` 模块：Baseline Profile 生成 + Macrobenchmark 帧时间测试 |
| `lib-decoder-flac/` | JNI 封装内嵌 libFLAC（系统解码器吃不下 24 位无损）；C++ 只在 `src/main/jni` |
| `lib-runtime-lx/` | 落雪自定义音源用的 QuickJS 运行时（`LxQuickJsInterrupt`） |
| `third_party/music-lib/` | 上游参考源码，只读，不参与构建 |

依赖版本统一在 `gradle/libs.versions.toml`（AGP 9.4.1、Kotlin 2.4.20、KSP 2.3.12、Gradle wrapper 9.8.0，
compileSdk/targetSdk 37，minSdk 23）。`settings.gradle.kts` 用 `FAIL_ON_PROJECT_REPOS`，仓库只在其中配置。

## 代码位置（`app/src/main/java/com/mytv/mymusic/`）

- `MainActivity.kt` / `MyMusicApp.kt` / `ForegroundActivity.kt`：入口与应用级组装。
- `ui/`：界面。`AppNavigation.kt` + `AppUi.kt` + `MusicViewModel.kt` 是主干；子目录按页面划分
  （`charts`、`playlists`、`search`、`library`、`local`、`player`（含 `stage` 全屏歌词动画、`share` 分享卡片）、
  `poster` 海报墙、`settings`、`tv`、`adaptive` 折叠/双屏、`theme`、`components`）。
- `data/`：`AppStore.kt` 是 Room 数据库（当前 `version = 12`，迁移是同文件里的 `MIGRATION_n_m` 对象，注册进 `ALL_MIGRATIONS`）；
  `MusicRepository.kt` 门面；`native/` 各平台音源 Provider；`cloud/`（网易云盘、QQ 云盘）、`sync/` 音乐库同步、
  `webdav/`、`transfer/` 局域网传输面板、`local/` 本地音乐扫描与匹配、`lx/` 落雪音源、`update/` 应用内更新。
- `playback/`：`PlaybackService`（Media3）及均衡器、缓存淘汰、切歌过渡（crossfade/automix）、节拍与频谱分析、睡眠定时。
- `aggregate/`：聚合搜索与换源（`AggregateSearchEngine`、`SourceResolver`、`TrackMatcher`）。
- `widget/`：Glance 桌面小组件（正在播放、歌词、榜单、歌单、听歌模式、快捷入口）；`lyricon/` 与词幕互通；`recognize/` 听歌识曲；`overlay/` 悬浮歌词。

## 命令

仓库没有 `local.properties`，Gradle 依赖环境变量定位 SDK：

```bash
export JAVA_HOME="/path/to/jdk"      # 目标字节码 17；CI 用 temurin 21
export ANDROID_HOME="$HOME/Android/Sdk"
```

```bash
./gradlew :app:assembleDebug          # 调试包
./gradlew test                        # JVM 单元测试（CI 在每次 push/PR 跑这个）
./gradlew assembleRelease             # 需 signing.properties/keystore.jks，否则自动退化为 debug 签名
./gradlew :app:connectedDebugAndroidTest   # 设备测试，需要模拟器/真机
ANDROID_SERIAL=<设备> ./gradlew :app:generateReleaseBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.class=com.mytv.mymusic.baselineprofile.BaselineProfileGenerator
```

CI：`.github/workflows/build.yml`，`main`/`dev` push 构建通用 APK 并发布 Release（附 `update.json` 与
`update.apk.sha256`），PR 只跑 `./gradlew test`。CI 不跑 Android lint（需要时手动 `./gradlew lint`）。

## 约定与坑

- **提交**：按 `CLAUDE.md` 直接提交到 `main`，中文、简短、带 `feat:` / `fix:` / `test:` 之类的类型前缀；
  只提交本次任务涉及的文件，别把工作区其它改动带上。代码注释与界面文案用中文。
- **安装包命名**：`app/build.gradle.kts` 的 `androidComponents.onVariants` 统一把输出改成
  `myMusic-<versionName>.apk`。`versionName` 固定为文件里的 `appVersionName`（1.1），`versionCode` 取
  `VERSION_CODE` 环境变量；CI 用 `v<versionName>.<versionCode>` 标签发布。应用内更新的落地文件名
  （`data/update/UpdateRepository.kt`）必须与发布资产同名。
- **minSdk 23 限制**：`material3` 被 `strictly` 钉在 `1.5.0-alpha25`（alpha26 起要求 minSdk 24，别随手升）；
  新 API 靠 core library desugaring；罗马音 / 繁简转换等仅在 Android 10+ 生效，需运行时判断。
- **Compose 实验 API** 已在模块层统一 opt-in（`ExperimentalMaterial3Api`、`ExperimentalMaterial3ExpressiveApi`），
  不必逐文件加注解。
- **改 Room 实体**必须同时 `version + 1` 并在 `AppStore.kt` 里补 `MIGRATION_n_m`、加进 `ALL_MIGRATIONS`，否则已有用户库升级会崩。
- **`res/xml` 网络配置**由 `NetworkSecurityConfigTest` 直接读原文校验；`testOptions` 已把该目录声明为测试输入，
  改配置后测试会重跑，不要删这段声明。
- **测试分层**：纯逻辑放 `app/src/test`（JUnit4 + `kotlinx-coroutines-test` + MockWebServer，样例 JSON 在
  `app/src/test/resources/`）；依赖 Android 框架、Room 迁移、手势、DRM 的放 `app/src/androidTest`；
  性能相关放 `baselineprofile`。命名 `XxxTest.kt`，UI 相关的放对应 `ui/<模块>/` 目录下。
- **只有英文**：界面文案全部进 `res/values/strings.xml`，代码里用 `stringResource` / `getString`；不再有
  `values-zh` 或应用内语言切换。格式化统一用 `ui/text/Formats.kt`（默认 `Locale.ENGLISH`，设备是中文时也不出现
  「10 月 20 日」「亿/万」）。存储值（`GROUP_ALL = "全部"`、平台 id、DataStore 枚举名、同步 JSON）保持原样，
  只在显示时映射成英文。
- **敏感数据**：同步会以明文写入 Cookie（私密 Gist / WebDAV），令牌与 WebDAV 密码只留在本机；
  签名文件（`*.jks`、`signing.properties`）与 `dist/` 已在 `.gitignore`，不要提交。
- `docs/` 除 Markdown 审计/性能报告外，还有大量 `*.html` 设计稿（播放页、海报墙、折叠屏、小组件等）；
  界面改动较大时，按 `docs/performance.md` 重新生成 Baseline Profile。
