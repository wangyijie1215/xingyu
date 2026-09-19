# Android 实施记录（v3.0 → v2.1）

> 源工程：`src-android/EchoFlow-Android/`（用户提供的 v3.0 源码，Java，无 Kotlin）
> 当前产物：`dist/EchoFlow-v2.1-神社+日记信笺胶囊.apk`（4.35 MB，已签名，可直接安装）
> 验证：Android 14 模拟器实机跑通，全流程无崩溃，数据落库已逐项确认

---

## 一、本轮（v2.1）新增：她的日记 / 信笺 / 时间胶囊

三项功能全部落到**数据层 + 生成服务 + 页面 + 导航 + Prompt 注入**五层，不是摆设。

### 新增文件（13 个）

| 文件 | 行数 | 职责 |
|---|---|---|
| `DiaryEntry.java` | 84 | 日记模型（四段结构：LINE / BODY / UNSAID / TOMORROW） |
| `DiaryStore.java` | 190 | 日记持久化 + 心情曲线聚合 + 情绪→积极度/颜色映射 |
| `DiaryService.java` | 245 | 四段生成 + 容错解析 + 保底日记（无 Key 也能用） |
| `DiaryActivity.java` | 430 | 日记页（今天的日记 / 心情曲线 / 往期 / 重新生成） |
| `Letter.java` | 125 | 信件模型（含「生成依据」字段，用于可解释性卡面） |
| `LetterStore.java` | 130 | 信件持久化 + 未读数 |
| `LetterService.java` | 235 | 她回信（写字的语气，3~5 段）+ 容错解析 + 保底信 |
| `LettersActivity.java` | 480 | 信笺页（火漆列表 / 信纸 / 生成依据 / 写信） |
| `TimeCapsule.java` | 140 | 胶囊模型 + **isSealed() 封存判定**（隔离的核心） |
| `CapsuleStore.java` | 135 | 胶囊持久化 + `sealedTeasers()`（只给存在事实，绝不给 content） |
| `CapsuleService.java` | 220 | 她写胶囊 + **到期三重投放**（信 + 记忆 + 事件） |
| `CapsuleActivity.java` | 520 | 胶囊页（玻璃瓶 / 倒计时 / 新建 / 封存声明） |
| `tools/efdrive.ps1` | 100 | Android UI 自动化驱动器（文本/ID 定位 + 重试） |

### 修改文件（4 个）

| 文件 | 改动 |
|---|---|
| `AndroidManifest.xml` | 注册 3 个新 Activity |
| `activity_character_profile.xml` | 新增日记/信笺/胶囊三个入口磁贴 |
| `CharacterProfileActivity.java` | 入口状态回显（`信笺 1` / `胶囊 2`）+ 点击绑定 |
| `PromptBuilder.java` | 新增 `LifeContext` 与第八参重载：把日记 / 信 / 胶囊接进对话 |
| `CardChatActivity.java` | `buildLifeContext()` —— 决定哪些能进模型、哪些不能 |
| `LetterService.java` | 补 `excerptOf()` 供 Prompt 短摘要 |

---

## 二、三项功能的设计要点（为什么这样做）

### 她的日记 —— 题眼是「她没说出口的那句话」

日记固定四段，`UNSAID` 是刻意单独强调的一段：

```
LINE      今天的开场（具体有画面，禁止「今天天气很好」）
BODY      发生了什么 + 她当时的反应（要细节，不要总结）
UNSAID    ★ 她没说出口的那句话（只有日记里才写）
TOMORROW  明天的打算（可被次日的主动消息引用）
```

**为什么这样设计**：她的人设是「话少、不太会直说」。所以 BODY 里**刻意没有一句直接的情感表达**，情感全部压在 UNSAID 里。用户读到那句话时，才会真正感觉她是一个有内心的人。

实测落库的保底日记（未配 API Key 时的路径）：

> 今天没什么特别的。
> 巡夜，回来，擦铠甲。
> ……写完这句就后悔了，好像也不算没有。
> **她没说出口的：「明天要是也能见到就好了。」**

页面上的 UNSAID 用了金边强调框——它是全页唯一被框起来的东西。

**保底策略**：进页面**先落一份保底稿**再异步生成。这样即使没配 Key、或模型超时，日记页也永远不是空白。

### 信笺 —— 信里的她比聊天里的她更坦白

聊天受即时性约束（句子短、有来有回、不能停顿太久）。**信解除了这个约束。**

写进 prompt 的硬约束：
- 用**写字**的语气，不是说话的语气：句子可以更长，可以停顿，可以用省略号
- 必须**具体引用**一件记忆或事件（否则就是一段模板情话）
- 「这是你比较少有的能好好说话的机会，因此可以说出平时不会当面说的话」
- 允许写不下去：「……就写到这吧。」是好的结尾
- 必须有落款 + 一句 P.S.（P.S. 常常是最动人的一句）

**火漆的配色纪律**：`#B03A2E` 是全应用**唯一**的强红，**只允许出现在「有一封信在等你」这一处**。未拆的信带 1.7s 呼吸脉冲。这条规则让红色本身成为信息。

### 时间胶囊 —— 封存内容不参与任何检索

这是三个功能里唯一带**安全约束**的。

> **封存期间 content 不得进入记忆检索、Prompt 组装、朋友圈生成或任何 LLM 上下文。**

理由：如果她能在到期前引用胶囊内容，那它就不叫胶囊了，用户也不会真的写真正想说的话。

实现上做了三层防护：
1. `TimeCapsule.isSealed()` 是唯一判定入口
2. `CapsuleStore.sealedTeasers()` 只返回「有一个胶囊存在」这件事本身，**签名上就不可能带 content**
3. `CapsuleActivity` 里点封存中的胶囊只弹 Toast：「还不能打开。到 X 那天，它会自己来找你。」

**已实机验证（见 §四）**：注入一个已到期 + 一个未到期的胶囊后，未到期那条的内容在记忆库、信笺、事件簿三个文件里**都搜不到**。

**到期时的三重投放**（已实测触发）：
```
到期 → ① 生成一封特别信件（火漆封缄、未读）
      → ② 写入长期记忆（importance 5）
      → ③ 写入事件簿（milestone）
```

---

## 三、Prompt 注入的边界（本轮最关键的一处设计）

`CardChatActivity.buildLifeContext()` 决定了三件事各注入什么。**边界比内容更重要**：

| 来源 | 注入什么 | 为什么不注入更多 |
|---|---|---|
| 日记 | 只给「昨天没说出口的那句话」 | 让她今天可以接着说 —— 这是最好的连续性。但**必须告诉她「对方没看过」**，否则她会说「我日记里写了」 |
| 信 | 只给最近一封的 40 字摘要 | 全文太长，且信是私密的时点事件 |
| 胶囊 | ★ 只给「存在一个胶囊」这个事实 | 内容封存。她也只能说「到时候再给你看」 |

Prompt 里对应写死的行为约束：

> （这是你私下写的、对方并没有看过。今天可以顺着这句话往下说，但不要说「我日记里写了」——日记是只给未来的自己看的。）

> （你知道有一个胶囊存在，但不知道里面写了什么，对方也不知道你的。可以提到「到时候再给你看」，但不要试图猜内容。）

**旧签名全部保留**：`buildMessages` 现在有 3 / 6 / 7 / 8 参四个重载，`MainActivity` 与 `PostsActivity` 一行未改。

---

## 四、实机验证记录（Android 14 模拟器）

### 页面级

| 步骤 | 结果 |
|---|---|
| 角色主页 | ✅ 三个新入口显示，且**状态回显正确**：`信笺 1`、`胶囊 2` |
| 她的日记 | ✅ 四段渲染，UNSAID 金边强调，心情曲线出柱，往期与「重新写」都在 |
| 信笺 | ✅ 火漆脉冲 + 「未拆」标签、信纸渲染、生成依据卡、写信框 |
| 时间胶囊 | ✅ 玻璃瓶 + 呼吸光点、倒计时、封存声明、新建表单 |
| 神社（回归） | ✅ 未受影响，`今日「小吉」` + 朱印正常 |

### 数据级（逐项 cat 出设备私有目录确认）

```
files/
├── cards/      c_test_alice.json
├── fortunes/   c_test_alice.json      ← 签文
├── shrine/     c_test_alice.json      ← 连续参拜
├── ema/        c_test_alice.json      ← 绘马
├── diaries/    c_test_alice.json      ← 日记（本轮）
├── letters/    c_test_alice.json      ← 信笺（本轮）
├── capsules/   c_test_alice.json      ← 胶囊（本轮）
├── memories/   c_test_alice.json      ← 签文 + 绘马愿望 + 胶囊记忆
└── events/     c_test_alice.json      ← 胶囊到期事件
```

**胶囊到期投放实测结果**：

```json
// capsules：到期的已 opened，未到期的保持 false
{"id":"cap_test1","openAt":"2026-09-10","opened":true,"openedAt":1789656908875}
{"id":"cap_test2","openAt":"2026-12-25","opened":false}

// letters：自动生成了特别信件
{"id":"ltr_cap_cap_test1","direction":"from_her","greet":"这封信，是几天前的我写的。",
 "body":"到那天，你还在这条街上吗？\n如果不在也没关系，我只是想问问。",
 "sign":"爱丽丝","read":false}

// memories：importance 5
{"id":"mem_cap_cap_test1","importance":5,"category":"event",
 "content":"你打开了她在 2026-09-10 留给你的时间胶囊：「…」"}

// events：milestone
{"id":"evt_cap_cap_test1","title":"时间胶囊到期：爱丽丝写给你的胶囊","auto":true}
```

### 封存隔离验证（本轮最重要的一条）

注入「已到期」与「未到期」各一个胶囊后，在三个下游文件里搜未到期那条的内容：

```
--- 记忆库里是否出现封存内容？ ---      ✅ 未泄漏
--- 信笺里是否出现封存内容？ ---        ✅ 未泄漏
--- 事件簿里是否出现封存内容？ ---      ✅ 未泄漏

--- 到期胶囊的内容是否正常送达？ ---    ✅ 正常送达信笺
```

**封存边界是真的，不是写在文档里的承诺。**

---

## 五、途中修掉的第三个真 Bug

### 入口磁贴不可点击

**现象**：角色主页三个新入口（日记/信笺/胶囊）用 uiautomator 看是 `clickable=true`（那是父布局继承的），但点击无反应。

**根因**：三个磁贴用的是 `LinearLayout` 容器 + 内部 `TextView`。子 `TextView` 铺满了整个容器并消费了触摸事件，容器自身的 `OnClickListener` 永远收不到。`android:clickable` 属性只影响 uiautomator 的可访问性上报，**不改变触摸分发**。

**修复**：给容器补 `android:clickable="true" android:focusable="true"`，让容器主动参与触摸分发，不再依赖子 View 不消费事件这一假设。神社入口一并加固。

```xml
<LinearLayout
    android:id="@+id/btn_diary"
    android:clickable="true"
    android:focusable="true"
    ... >
```

> 这类 bug 在真机上「点着好像没反应」时最容易漏。用 uiautomator 的 dump 看 `clickable` 属性会得出错误结论——**必须实际点一次看 Activity 是否切换**。

### 另两个（本轮之前已修，记录在 v2.0 文档）
1. `FortuneService.hash01()` 的 int 溢出导致数组下标 −1 崩溃
2. PowerShell 5.1 无法解析含中文的 `.ps1`

---

## 六、构建与验证

```powershell
$env:JAVA_HOME="D:\android-toolchain\jdk17"
$env:ANDROID_HOME="D:\android-toolchain\sdk"
$env:GRADLE_USER_HOME="D:\android-toolchain\gradle-home"
cd src-android\EchoFlow-Android
gradle assembleRelease --no-daemon      # 必须联网
```

自动化验证脚本：`tools/efdrive.ps1`（文本/ID 定位点击，自动重试 dump）。

> **模拟器踩坑记录**：`uiautomator dump` 有两个坑 ——
> 1. 反复 dump 到**同一个远端文件**时会返回缓存（永远 8761 字节）。必须每次换文件名 + 先 `rm -f`。
> 2. 页面上有**无限动画**（胶囊页的呼吸光点）时会报 `could not get idle state` 而 dump 失败。这类页面只能靠 `screencap` + 人工看图确认。

---

## 七、产物

| 文件 | 说明 |
|---|---|
| `dist/EchoFlow-v2.1-神社+日记信笺胶囊.apk` | 4.35 MB，已签名，本轮最终产物 |
| `dist/EchoFlow-v2.0-神社.apk` | 上一版（仅神社），保留 |
| `dist/echoflow-release.jks` | 签名密钥（口令均为 `echoflow`，仅供分发测试） |
| `shots-android/*.png` | 实机截图 13 张 |
| `android-shot-wall.png` | 截图整墙 |

APK sha256：`6B97E3EB96E83DD5AE0FC0FFA89294C040C395747AA35C2268F819A01E06BDF3`（v2.2，含顶栏与空气泡修复）

### 本轮顺手做的设计系统收口

原工程残留了 v3.0 的「霓虹青紫」配色（`#4DD0E1` / `#7C4DFF` / `#00B8D4`），与新设计系统混在一起。
本轮**全部清掉**，涉及 6 个布局 + 3 个 drawable：

| 旧 | 新 |
|---|---|
| `bg_gradient_top`（青紫霓虹渐变） | `ef_bg_profile_hero`（深紫柔光三段渐变） |
| `accent_cyan` 文字/进度条 | `ef_primary_light` / `ef_primary` |
| `accent_purple`（情绪字样） | `ef_emotion`（语义正确：情绪=蔷薇） |
| `bg_user_bubble` 青蓝渐变 | `ef_primary → ef_primary_deep` 紫渐变 |
| `bg_send` 青蓝渐变 | 同上一并统一 |
| `bg_input` 旧色引用 | `ef_night_500` + `ef_hairline` |

另外修了一个**字形缺失**问题：`✉`（U+2709）在测试设备（Android 14 模拟器）的字体里没有字形，渲染成豆腐块。
换成汉字「信」——对中文 UI 来说反而更贴调性。**教训：App 内不要用冷门 Unicode 符号当图标**，
要么用内置汉字，要么用 vector drawable；`⛩`（U+26E9）在该设备上正常，但不保证所有机型都有。

---

## 八、v2.2：用户实测反馈修复

用户装上 v2.1 后发来截图，暴露出两个**只有真机才会发现**的问题。这两条都不是「设计没做」，
而是「原工程遗留 + 我没在真机上按真实路径走一遍」——记在这里当教训。

### 8.1 顶栏挤爆（严重，功能不可达）

**现象**：标题完全不可见，`设置` 按钮被挤出屏幕右缘，点不到。

**根因**：`activity_main.xml` 顶栏是横向 `LinearLayout`，里面是
`标题(weight=1)` + **5 个 `wrap_content` MaterialButton**，没有滚动容器。
5 个按钮的固有宽度加起来超过 1080px，`weight=1` 的标题被压到 0 宽，
最右的按钮直接落到屏幕外。

```
修复前：标题 不可见      ｜ 设置 [1003,165][1080,265] ← 右缘被截断
修复后：EchoFlow [0,165][1080,243]                  ← 标题回来了
        设置 [1040,269][1080,369]                   ← 可横向滑动到达
```

**修复**：拆两行——标题独占一行（`match_parent` + ellipsize），
5 个按钮放进 `HorizontalScrollView`。

> 更彻底的做法是照设计稿换成「标题 + ⋯ 溢出菜单」，但那是改信息架构。
> 当前改法优先保证**不丢功能**。

### 8.2 AI 空气泡（观感像渲染坏了）

**现象**：AI 回复失败时，列表留下一个空的深色圆角壳。

**根因**：`MessageAdapter.AiHolder.bind()` 对空 content 不做判断——
`MarkdownParser.parse("")` 返回空列表 → `for` 一次都不进 → **容器本身还在**，
于是渲染出一个空的 `bg_ai_bubble`。

**修复（三处）**：
1. 空内容 + 正在生成 → 显示 `● ● ●`
2. 空内容 + 不在生成 → `container.setVisibility(View.GONE)`
3. 请求失败且一个字都没吐出来 → 直接从 `messages` 移除该条并 `notifyItemRemoved`

新增 `MessageAdapter.setGenerating(Message)` 显式标记在生成的那条。

```
修复前：你好 / 你好 / (空壳) / 你好 / (空壳)
修复后：你好 / 你好                    ← 空壳完全消失
```

### 8.3 教训

维护别人的工程时，**不能只验证自己新写的功能**。
这两轮把神社、日记、信笺、胶囊都验得很细，却从没在真机上点过主界面顶栏——
那是原工程就有的代码，我默认它是好的。

**正确做法**：交付前把**主流程**从头走一遍（主页 → 每个入口 → 返回），
而不是只走自己改过的那几条路。

---

## 九、还没做的（按优先级）

| 项 | 说明 |
|---|---|
| 主动消息调度 | 设计已完备（`design/03` §3.8 的触发因子与五重限流），但需要 `WorkManager` + 通知权限，属独立工作量 |
| 朋友圈接生活系统 | 目前朋友圈是独立生成，未读取日记/信/胶囊 |
| 角色创建器（一句话生成） | `design/04` §29 |
| 语音 TTS / STT | `design/04` §17/§18/§19，需要接入具体 Provider |
| 回忆卡导出图片 | `design/04` §40，需要用 Canvas 画卡并走 MediaStore |
| 全局搜索 | `design/04` §48 |
