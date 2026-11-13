# ChatYUE

> 一款最低支持 **Android 1.5（API 3 / Cupcake）** 的原生 AI 对话应用。
> 零 AndroidX、零 Jetpack、零 Material、零第三方依赖；所有浮层、控件、动效均为 `View` + `Canvas` 手绘。
> 本应用基于 **Muse** 继续开发，按 **GNU GPL v3** 发布。

![icon](app/src/main/res/drawable/ic_launcher.png)

> **3.0（2026-11）**：
>
> - 修复发送时报 `1210 temperature 参数非法：限制小数点[2]位`：温度 / top_p 统一按两位小数写入 JSON；
> - 重绘「重新生成」矢量图标，箭头方向与圆弧缺口一致；
> - 三个**默认关闭**的实验性功能（设置或新手引导可开启）：反思、Agent 多轮任务、让 Agent 操控所有文件；
> - Agent 可创建 / 读取 / 修改 / 删除文件与文件夹，压缩 / 解压 zip，多轮思考，结束后用产物卡片展示；
> - 任意大小附件导入（流式复制）；HTML 预览支持同目录 CSS/JS；文本 / 代码在线预览；
> - 全局崩溃错误页（独立进程）：完整日志 + 复制 / 重试 / 返回；
> - 关于页加入原项目 Muse、本应用开源代码入口与致谢许可证页。
>
> **原项目：Muse** — <https://github.com/Zer0Qing/Muse>（Zer0Qing，GPL-3.0）。
> 本应用源码：<https://github.com/chumianyi/ChatYUE-3.0>（GPL-3.0）。

---

## 一、先看结论：硬性约束是怎么落地的

| 约束 | 落地方式 | 代码位置 |
| --- | --- | --- |
| minSdkVersion 3 | `defaultConfig.minSdkVersion 3` + Manifest `<uses-sdk>`；关闭 `NewApi` 等 lint 校验 | `app/build.gradle`、`AndroidManifest.xml` |
| 禁止 AndroidX / support / Material | `dependencies { }` 为空；`android.useAndroidX=false`；`enableJetifier=false` | `build.gradle`、`gradle.properties` |
| Java 1.6 语法 | 全仓无 lambda / 方法引用 / try-with-resources / 字符串 switch / 接口默认方法 / diamond | 全部 `*.java` |
| 无 Fragment / RecyclerView / ActionBar / Notification.Builder / ViewPager / 属性动画 | 页面用手动 View 栈 + 位移补间；列表用 `ListView` + `BaseAdapter`；动效自写 `Tween` | `ui/Page.java`、`MainActivity`、`core/Tween.java` |
| 流式 SSE | `HttpURLConnection` 取 `InputStream` → `BufferedReader` 逐行解析 `data:` | `net/ChatClient.java` |
| 可中断生成 | `Request.cancel()` 断开连接 + 关闭 wolfSSL 会话 | `net/Request.java` |
| wolfSSL TLS 兜底 | JNI 桥 + 纯手工 HTTP/1.1 流式解析；CA 包内置 | `jni/wolfssl_bridge.c`、`net/WolfTransport.java`、`assets/cacert.pem` |
| 禁止系统弹窗 / Toast / 选择器外观 | 自绘 `Sheet`、`Dialog`、`Toast`、`Loading`、`Segmented`、`MenuRow` | `ui/overlay/*`、`ui/widget/*` |
| ChatYUE 条目不展示 Base URL / API Key | 内置项固定为常量，UI 仅显示“内置 · 模型名” | `data/Providers.java`、`ui/screen/SettingsPage.java` |

### 2.1.0 修复的 5 个闪退入口

用户提交的 5 份 crash log 堆栈完全一致：

```
java.lang.IllegalArgumentException: Invalid Layer Save Flag - only ALL_SAVE_FLAGS is allowed
  at android.graphics.Canvas.checkValidSaveFlags(Canvas.java:412)
  at android.graphics.Canvas.saveLayerAlpha(Canvas.java:598)
  at com.chatyue.app.ui.widget.AlphaFrame.draw(AlphaFrame.java:49)
```

「···」、「设置项」、「首页搜索」、「设置搜索」、「知识库」五个入口都会打开浮层/新页面，
浮层淡入时走 `AlphaFrame.draw`。2.0.1 里 `saveLayerAlpha(rect, alpha, 0)` 的 flags 传了 0，
而硬件加速画布只接受 `ALL_SAVE_FLAGS`（31）。**一处修正，五个入口同时恢复。**

`AlphaFrame` 与 `IconButton` 现在都显式传 31，并注释了 API 1 里的名字差异
（`ALL_SAVE_FLAG` vs `ALL_SAVE_FLAGS`），保证既能对着 1.5 编译，也能在新系统上跑。

---

## 二、界面预览

`preview/design-preview.html` 是主要界面的静态视觉稿（浏览器打开即可），
用于核对配色、圆角、层级与信息密度。它不是运行时截图。

> 该稿对应 2.0 的三屏；2.2 的页面转场、M3 经典主题与弹窗动效是 Canvas 逐帧绘制，
> 静态 HTML 无法还原，请安装 APK 查看，或阅读 `ui/m3/` 与 `ui/widget/MorphFrame.java`。

---

## 三、原项目与许可证

ChatYUE 3.0 是在 **Muse**（<https://github.com/Zer0Qing/Muse>）基础上继续开发的衍生作品。
Muse 使用 **GNU General Public License v3.0**；因此本仓库同样以 **GPL-3.0** 发布，
完整许可证见根目录 `LICENSE`，或在应用内「关于 → 致谢与许可证」查看。

- 原项目：Muse，作者 Zer0Qing
- 本应用：ChatYUE 3.0，仓库 <https://github.com/chumianyi/ChatYUE-3.0>
- 可选 TLS 兜底：wolfSSL（GPL-2.0 或商业许可）
- 运行时：Android Framework / SDK（Apache-2.0 等）、org.json（JSON License）

没有引入 AndroidX、Jetpack、Material、support、Glide、OkHttp、Gson 等第三方库；
图标全部是 `core/Icons.java` 里的手绘 Path，字体使用设备系统字体。

---

## 四、构建与运行

### 直接安装（已构建好的产物）

仓库根目录的 `ChatYUE-3.0.apk` 是本次实际构建并签名的成品，可直接侧载：

| 项 | 值 |
| --- | --- |
| 包名 / 版本 | `com.chatyue.app` / 3.0 (versionCode 4) |
| minSdkVersion | 3（Android 1.5 Cupcake） |
| 体积 | 约 2.5 MB（含 8 个 ABI 的 wolfSSL bridge） |
| DEX | d8 生成，`--min-api 3` |
| 签名 | v1（JAR, SHA1 摘要）+ v2；Android 1.5–2.3 只认 v1，故必须带 SHA1 |
| SHA-256 | `6b6faa6ccb3afcdbe26d0747b29872a70fdb23db95a355cb3c3cb70cba6288c6` |

安装时系统会提示“未知来源”，允许即可。签名密钥是仓库内 `keystore/debug.keystore`
（口令 `android`，仅供调试安装，不要用于正式分发）。

### 不依赖 Gradle 的构建（推荐，也是本次出包用的方式）

```bash
ANDROID_SDK=/path/to/android-sdk sh scripts/build_apk.sh
# 产物：./ChatYUE-3.0.apk
```

脚本只调用 SDK 自带的 `aapt2 / javac / dx|d8 / zipalign / apksigner`，
不解析任何依赖、不需要联网：

* **JDK 17** 编译，`-source 1.7 -target 1.7`；源码本身仍保持 Java 1.6 语法（无 lambda / diamond / 字符串 switch）
* `aapt2 link --min-sdk-version 3` —— minSdk 真正写进清单
* **d8 --min-api 3** 产出 dex；脚本会优先使用 SDK 自带 d8
* `apksigner --min-sdk-version 3 --v1-signing-enabled true` —— 自动改用 SHA1 摘要

> **为什么不用 `./gradlew assembleDebug`？**
> AGP 7.x 要求 JDK 11/17，AGP 8.x 强制 `compileSdk ≥ 33`，而本沙箱是 JDK 20 + Gradle 9，
> AGP 7 在这套组合下无法启动。Gradle 工程仍然完整保留（`app/build.gradle`、wrapper 配置），
> 用 Android Studio 或 JDK 17 + Gradle 7.3.3 打开可正常 `assembleDebug`。
> 两条路径的产物等价，只是 Gradle 那条会把 minSdk 校验交给 AGP。

### 环境

* JDK 11（AGP 7.2.2 + Gradle 7.3.3）
* Android SDK Platform 31（`compileSdkVersion 31`）
* 不需要任何额外依赖下载

> **为什么 compileSdk 是 31 而不是 3？**
> 现代 AGP/AAPT2 已无法用 API 3 的平台包编译，但 `compileSdk` 只影响“编译期可见的 API 面”。
> 本项目所有实际调用的 API 都在 API 3 范围内（见第十节逐条清单），`minSdkVersion 3` 才是运行时契约。
> 同时关闭了 `NewApi` / `InlinedApi` lint，避免 Gradle 因低 minSdk 报错。

### 命令行

```bash
cd ChatYUE
./gradlew assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
```

若本机没有 gradle wrapper 的 jar，可直接用 Android Studio 打开工程（会自动补全 wrapper），
或使用本机 Gradle 7.3+：`gradle assembleDebug`。

### wolfSSL 兜底库（已随仓库提供 8 ABI）

`app/src/main/jniLibs/` 已包含以下 ABI 的 `libwolfssl_bridge.so`：

`armeabi` · `armeabi-v7a` · `arm64-v8a` · `x86` · `x86_64` · `mips` · `mips64` · `riscv64`

它们静态链接 wolfSSL 5.7.4，运行时只依赖 libc/libm/liblog/libdl，不依赖
`libwolfssl.so` 或 `libstdc++`。重新生成走 GitHub Actions（不占本地 CPU）：

```bash
# 触发 workflow：Build wolfSSL Bridge (all ABIs)
gh workflow run build-wolfssl.yml
# 或直接用仓库的 Actions 页面点 Run workflow
```

workflow 定义在 `.github/workflows/build-wolfssl.yml`，实际编译脚本是
`scripts/build_wolfssl_ci.sh`（legacy 7 ABI 用 NDK r10e，riscv64 用 NDK r27d）。
旧的 `app/src/main/jni/build_native.sh` 只覆盖 armeabi / armeabi-v7a / x86，
保留作为本机快速构建入口。
**没有 .so 时应用照常启动**，走 HTTP 端点完全正常；只有访问 HTTPS 且平台 TLS 握手失败时，
会明确提示「wolfSSL 兜底库未加载」。

---

## 五、目录结构

```
ChatYUE/
├── ChatYUE-2.0.1.apk                    # 已构建并签名的成品（可直接安装）
├── scripts/build_apk.sh                 # 不依赖 Gradle 的构建脚本（本次出包用它）
├── keystore/debug.keystore              # 调试签名密钥，口令 android
├── preview/design-preview.html|.png     # 三屏视觉稿
├── build.gradle / settings.gradle / gradle.properties
└── app/
    ├── build.gradle                     # minSdk 3、lint 关闭、jniLibs 配置
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/cacert.pem            # 内置 CA 包（wolfSSL 校验用）
        ├── jni/                         # wolfSSL JNI 桥 + ndk-build 脚本
        ├── res/drawable/ic_launcher.png # 单密度图标（不做多密度适配体系）
        └── java/com/chatyue/app/
            ├── ChatYueApp.java          # Application：初始化、释放 CA 包
            ├── MainActivity.java        # 唯一 Activity：页面栈 / 转场 / 返回键 / 选图
            ├── core/                    # Ui、Palette、Tween、Icons（矢量图标）、AlphaView（透明度协议）
            ├── data/                    # Message、Session、SessionStore、Prefs、Providers、Knowledge、Agents
            ├── md/                      # MdParser、MdBlock、MdInline、CodeHi（语法高亮）
            ├── net/                     # RequestQueue、HttpTransport、WolfTransport、ChatClient、WolfSsl
            ├── util/                    # Base64Codec、FileUtil、TimeUtil、ClipboardCompat
            └── ui/
                ├── RootLayout.java      # 页面层 / 浮层 / Toast / Loading 四层
                ├── Page.java
                ├── md/                  # MarkdownView、MdText、CodeBox、TableBox、QuoteText
                ├── overlay/             # OverlayHost、Sheet、Dialog、Toast、Loading
                ├── screen/              # HomePage、ChatPage、SettingsPage、ProviderEditPage、SessionListPage
                └── widget/              # Label、RoundBox、AlphaFrame、IconButton、SoftButton、MenuRow、Bubble…
```

---

## 六、架构与线程模型

```
UI 线程                         工作线程（RequestQueue 单线程串行）
───────                         ────────────────────────────────
点击发送
  └─ Message(user) 入队
  └─ Message(assistant, streaming) 占位
  └─ ChatClient.send() ────────►  buildBody(JSON)
                                   HttpTransport.open()  ← 平台 TLS
                                     └─ 失败且是 TLS → WolfTransport.open() ← wolfSSL
                                   逐行读 SSE
  ◄──── onDelta(content, reason)  解析 choices[0].delta
  累积 + 40ms 合批
  MarkdownView 增量重排
  ◄──── onDone / onError
```

* **请求队列**：`RequestQueue` 用一个常驻 `Thread` + `LinkedList` 实现串行队列，
  异常不会拖垮队列；UI 回调统一走主线程 `Handler`。
* **合批刷新**：每个 SSE delta 只累加到缓冲区，40ms 合并一次刷新，
  避免低端机每来一个字就重排整段 Markdown。
* **增量重排**：`MarkdownView.setMarkdown()` 发现前 n-1 个块没变化时只更新最后一个 `MdText`，
  流式输出不会整段闪烁。
* **持久化**：`filesDir/sessions/<id>.json` 一条会话一个文件，`index.json` 存轻量索引；
  图片压缩后存 `filesDir/images/`。没有 SQLite 依赖，损坏时降级为空列表而不崩溃。

---

## 七、自绘 UI 体系

### 设计语言

| 项 | 值 |
| --- | --- |
| 底色 | `#F6F3F5`（奶油灰粉） |
| 卡片 | `#FFFFFF`，圆角 16–24dp |
| 主色 | `#EF7FA8`，浅色底 `#FBE3EB` |
| 文字 | `#1C1B1D` / `#6E6B72` / `#9B979F` |
| 动效 | 统一 90–340ms，`cubic-bezier(.2,0,0,1)` 近似曲线 + 弹簧回弹 |

### 组件清单（全部可直接复用）

| 组件 | 作用 | 自绘要点 |
| --- | --- | --- |
| `Label` | 单行文本 | 自管基线、省略号，不继承 TextView |
| `RoundBox` | 圆角容器 | 支持四角独立圆角、描边、伪阴影 |
| `IconButton` | 圆形图标键 | 按压「缩小 + 加深」，矢量图标 |
| `SoftButton` | 胶囊文字键 | 图标+文字、描边、按压反馈 |
| `MenuRow` | 设置/菜单行 | 分组圆角、分隔线、勾选、危险色 |
| `Segmented` | 任务/Agent/群聊 | 滑动胶囊指示器 |
| `TopBar` | 顶栏 | 返回键 + 双行胶囊标题 + 更多键 |
| `BounceScrollView` | 滚动容器 | `dispatchDraw` 平移 + 弹簧补间实现边界回弹 |
| `Bubble` | 消息气泡 | 四角独立圆角、助手侧淡投影、长按判定 |
| `MarkdownView` / `MdText` / `CodeBox` / `TableBox` | 富文本 | `StaticLayout` 排版 + 自绘代码块（含复制键、横向滚动、限高纵向滚动） |
| `ThinkingView` | 思考过程卡片 | 可折叠，流式时显示「正在思考…」 |
| `TypingDots` / `Spinner` | 加载态 | 逐帧 Canvas 动画 |
| `Sheet` / `Dialog` / `Toast` / `Loading` | 浮层 | 遮罩淡入 + 位移补间，**替代全部系统弹窗** |
| `InputBar` | 输入区 | 胶囊底自绘，EditText 背景置空、内边距归零 |

### 交互细节

* **触摸按压反馈**：每个可点组件都有自己的 `pressed` 插值，不依赖 `state_pressed` drawable。
* **滚动边界回弹**：`BounceScrollView` 在顶部/底部继续下拉时对画布做阻尼位移，松手弹簧归位。
* **键盘避让**：`windowSoftInputMode=adjustResize` + `ChatPage.onSizeChanged` 自动把视线拉回最新消息。
* **长按 / 滑动判定**：`Bubble.onInterceptTouchEvent` 用 480ms 定时 + 8dp 位移阈值区分长按与滚动；
  `CodeBox` / `TableBox` 在横向拖动时 `requestDisallowInterceptTouchEvent(true)`，避免外层滚动抢事件。

---

## 八、TLS 双链路

```
              ┌── http:// ──────────────► HttpTransport（HttpURLConnection，SSE 流式）
chatUrl() ────┤
              └── https:// ──► HttpTransport
                                 │ 握手失败且判定为 TLS 问题
                                 ▼
                              WolfTransport
                                 ├─ WolfSsl.connect()  wolfSSL TLS 1.0–1.3 握手（内置 CA）
                                 ├─ 手写 HTTP/1.1 请求行 + 头 + body
                                 └─ 响应解析：status line → headers → chunked / content-length → BufferedReader
```

* 明文 HTTP 直连与 HTTPS 两条链路都能走通；都失败时弹出**自绘**错误面板，
  给出「TLS 握手失败 / 域名解析失败 / 连接被拒绝 / HTTP 401」等可读原因，
  并可展开技术细节与 TLS 诊断（显示 wolfSSL 是否加载、版本号）。
* 内置 `assets/cacert.pem`（Mozilla CA Bundle）由 `ChatYueApp` 释放到私有目录，
  绕开老系统过期的 `/system/etc/security/cacerts`。
* `WolfSsl` 的 `System.loadLibrary` 失败不会崩溃，`isAvailable()` 返回 false，上层自动降级并提示。

---

## 九、功能清单

**对话**
* 多轮上下文（默认带最近 40 条），会话级系统提示词覆盖全局设置
* 流式逐字输出（40ms 合批）、随时中断、重新生成、失败重试
* 长按消息：复制 / 重新生成 / 编辑后重发 / 删除
* 快捷条：复制 / 重生成 / 导出 Markdown / 清空
* 思考过程卡片（解析 `reasoning_content`，可折叠）

**Markdown**
* 标题、段落、有序/无序列表（含悬挂缩进）、引用、分隔线、表格、围栏代码块
* 行内粗体/斜体/删除线/行内代码/链接
* 代码块：语言标签、按字符的语法高亮（关键字/字符串/注释/数字/函数）、一键复制、双向滚动

**会话**
* 列表 / 新建 / 切换 / 重命名 / 删除，本地 JSON 持久化
* 首页「最近」卡片直达最近一次会话

**图片输入**
* 相册选择（`ACTION_GET_CONTENT`）与拍照（MediaStore 输出 URI）
* 采样解码 + JPEG 压缩 + base64 data URL，按 OpenAI vision 规范发送

**设置**
* 模型提供方：**ChatYUE 固定第一项**，只显示「内置 · GLM-4.1V-Thinking-Flash」，
  **不出现也不可编辑 Base URL / API Key**；第三方可添加、编辑、删除、设为默认
* 用户画像、全局系统提示词、记忆开关、字号、知识库片段、会话管理、关于

**其他**
* 输入框多行自适应（最多 5 行）、发送/停止态切换、附件预览
* 搜索会话（标题 + 正文）、复制到剪贴板（含 API 11 前后两套实现）

---

## 十、页面清单

没有占位页，也没有「点进去只有一个标题」的空壳。每一个入口都通向一个真实页面：

| 分组 | 页面 | 说明 |
| --- | --- | --- |
| 引导 | `OnboardingPage` | 8 步：欢迎 / 语言 / 称呼 / 记忆 / 模型 / 隐私 / 主题 / 准备就绪；2.2 已移除前面的开屏动画 |
| 经典主题 | `ui/m3/*` | 与默认页面完全独立的 Material 3 页面：首页 / 会话 / 知识 / 收藏 / 设置 / 外观 / 聊天 / 看图 / 模型编辑 |
| 首页 | `HomePage` | 问候、最近会话、快捷提问、Agent 入口 |
| 对话 | `ChatPage` | 流式逐字、Markdown、代码高亮与复制、图片、长按操作 |
| 会话 | `SessionListPage` / `SessionDetailPage` | 列表、重命名、导出、清空、删除 |
| 搜索 | `SearchPage` | 标题 + 正文全局搜索，结果直达原会话 |
| 看图 | `ImagePreviewPage` | 全屏、可缩放、保存到相册 |
| 设置 | `SettingsPage` / `M3SettingsPage` | 分组入口，含搜索；经典主题使用独立 M3 设置页 |
| 人设 | `ProfilePage` / `AssistantListPage` / `AssistantEditPage` | 画像、助手增删改、设为默认 |
| Agent | `AgentPage` | 四项能力开关 + 人设快捷入口 |
| 资源 | `AssetsPage` | 收藏 / 知识库 / 快捷消息 / 模板 / 技能 / 助手 |
| 收藏 | `FavoritePage` | 跨会话收藏，点回原会话 |
| 知识 | `KnowledgePage` | 增删改查，随请求发送 |
| 快捷消息 | `QuickMessagePage` | 增删，对话页一键填入 |
| 模板 | `PromptLibraryPage` | 5 个分类 14 个模板，点开带进新会话 |
| 技能 | `SkillPage` | 6 项约束开关，实时预览将要发送的文本 |
| 外观 | `AppearancePage` / `M3AppearancePage` | 13 套主题 + 字号 + 实时预览；经典主题使用独立 M3 外观页 |
| 聊天设置 | `ChatSettingsPage` | 字号、记忆、回车发送、自动滚动、上下文 |
| 模型参数 | `ModelParamsPage` | 温度、Top P、单次上限，真正写进请求体 |
| 语言 | `LanguagePage` | 5 种回答语言 |
| 数据 | `DataPage` | 占用统计、清图片缓存、删全部会话、重播引导 |
| 用量 | `UsagePage` | 会话/消息/字数/token 估算与排行 |
| 关于 | `AboutPage` / `ChangelogPage` / `HelpPage` | 版本、更新日志、FAQ 与反馈模板 |
| 协议 | `TermsPage` / `PrivacyPage` | 用户协议、隐私政策（长文 Markdown 渲染） |
| 模型提供方 | `ProviderEditPage` | 内置项只读，第三方可增删改 |

统一的页面骨架在 `BasePage`：顶栏 + 回弹滚动 + `section / card / row / toggle / stepper / note / markdown`
这套原语，保证 30 个页面的间距、圆角与层级完全一致。

---

## 十一、API 3 兼容性清单（逐条核对）

| 能力 | 使用的 API | 起始版本 |
| --- | --- | --- |
| 页面/容器 | `FrameLayout` `LinearLayout` `ScrollView` `ListView` `EditText` | 1 |
| 绘制 | `Canvas` `Paint` `Path` `RectF` `StaticLayout` `BitmapShader` | 1 |
| 文本 Span | `StyleSpan` `TypefaceSpan` `ForegroundColorSpan` `BackgroundColorSpan` `LeadingMarginSpan.Standard` | 1 |
| 滚动回弹 | `dispatchDraw` + `Canvas.translate`（不用 `overScrollBy`，它要 API 9） | 1 |
| 动画 | 自写 `Tween`（`Handler.postDelayed` + `SystemClock.uptimeMillis`） | 1 |
| 网络 | `HttpURLConnection` `URL` `InputStream` | 1 |
| JSON | `org.json.JSONObject / JSONArray` | 1 |
| 存储 | `SharedPreferences` `FileInputStream/FileOutputStream` | 1 |
| 剪贴板 | `android.text.ClipboardManager`；API 11+ 的 `ClipData` 全程反射调用，不静态引用 | 1 |
| 透明度 | 自写 `Ui.setAlpha` + `AlphaFrame`（`Canvas.saveLayerAlpha`）替代 API 11 的 `View.setAlpha` | 1 |
| 图片 | `BitmapFactory` `Bitmap.compress` `ACTION_GET_CONTENT` `MediaStore` | 1 |
| 版本判断 | `Build.VERSION.SDK`（字符串，API 1）**而非** `SDK_INT`（API 4） | 1 |

刻意规避的坑（全部经“API 3 原版 android.jar 交叉编译”验证，见第十一节）：
* **`View.setAlpha` 在 API 3 上根本不存在**（只有 `protected onSetAlpha`）。所有淡入淡出改走
  `Ui.setAlpha(view, 0..1)`：自绘控件实现 `AlphaView` 接口，容器继承 `AlphaFrame` 用
  `Canvas.saveLayerAlpha(RectF, int, int)` 做整棵子树的加权合成，平台控件留一条反射兜底。
* 不用 `setScaleX` / `setTranslationX` / `setRotation`（API 11）——位移一律 `offsetLeftAndRight`/`offsetTopAndBottom`
* 不用 `View.generateViewId`（API 17）、`java.util.Base64`（API 8，自写 `Base64Codec`）、`getActionMasked`（API 8）
* 不用 `Activity.onBackPressed`（API 5）为主路径——返回键在 `onKeyDown(KEYCODE_BACK)` 里拦截，
  新系统那层 `onBackPressed` 只做兜底且不加 `@Override`
* 布局参数统一用 `FILL_PARENT`（API 1），不用 `MATCH_PARENT`（API 8）——虽然两者数值都是 -1，
  但换掉之后整份源码能对着 1.5 的 `android.jar` 编译通过，可验证性更强

---

## 十二、已知边界（如实说明）

1. **wolfSSL `.so` 已随仓库提供 8 ABI**：legacy 7 ABI 由 NDK r10e 构建，riscv64 由
   NDK r27d 构建，均静态链接 wolfSSL 5.7.4。MIPS / RISC-V 属于极冷门 Android 设备，
   系统是否加载对应 ABI 取决于设备本身；常见 ARM / x86 设备直接可用。
   HTTP 链路完全不依赖它，HTTPS 在平台 TLS 握手失败时才会走 wolfSSL。
2. **状态栏底色**：API 21 才有 `setStatusBarColor`，1.5 上系统状态栏保持默认灰色，无法自绘。
3. **没有本地模型**：应用不含任何端侧推理能力，也没有「本地模型」这一类提供方。
   所有对话都走云端接口；离线时只能查看历史，不能生成。设置里也不会出现任何
   伪装成本地的开关。
4. **没有后台推送**：低版本没有 `Notification.Builder`，也没有常驻服务；
   应用退到后台后不会主动提醒。
5. **语言只影响模型回复**：界面文案保持中文，避免出现「半套翻译」的割裂感。
6. **compileSdk 31 编译、minSdk 3 运行**：AAPT2 产出的资源表在老系统上的极限兼容性
   取决于构建工具链版本；若要追求“真机 1.5 原教旨兼容”，需回退到 aapt v1 + AGP 1.x 工具链。

---

## 十三、校验记录

* **API 3 交叉编译**：把 `com.google.android:android:1.5_r4`（Android 1.5 原版 `android.jar`）
  当作 classpath 编译全部 111 个源文件（含新增的 25 个页面/组件），**0 error**。这一步是本工程最硬的兼容性证据：
  任何 API 4 以后才出现的类/方法都会在这里报 `cannot find symbol`。本次它抓出并修掉了
  4 处真实缺陷（`View.setAlpha`、`MATCH_PARENT`、`ClipData` 静态引用、`onBackPressed` 的 `@Override`）。
* 成品 APK 反查：`dexdump -f` 确认 **DEX version 035**（由 **dx** 生成，不是 d8）；
  `grep` 全 dex 无 `androidx/` `android/support/` `java/util/function/` `java/lang/invoke/` `ClipData` 引用。
* 五个闪退入口的根因已定位并修复：`Canvas.saveLayerAlpha` 的 flags 必须传 31（`ALL_SAVE_FLAGS`）。
* 签名：`apksigner verify --min-sdk-version 3` 通过，v1 为 SHA1 摘要（`MANIFEST.MF` 里是
  `SHA1-Digest`），Android 1.5 的 JarVerifier 能认。
* Markdown 解析器在 JVM 上跑了 9 组用例（完整文档、未闭合代码块、空串、null、只有围栏、
  半截表格、嵌套列表、CRLF、500 条列表）：不抛异常，块类型与数量符合预期；
  过程中修掉了“`- ` 无序列表被判成有序列表”的分支错误。
* 全仓静态扫描：无 `->`、无 `<>`、无 `try (`、无 `catch (A | B)`、无字符串 switch、
  无 `AlertDialog` / `Toast.makeText` / `PopupWindow` / `Fragment` / `RecyclerView` 引用。
* 图标资源仅提供 `res/drawable/ic_launcher.png` 单密度版本，不做多密度适配。
