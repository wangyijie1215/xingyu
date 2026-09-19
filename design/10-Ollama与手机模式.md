# 本地 Ollama 接入 · 手机模式

> 对应 `dist/EchoFlow-v2.4-Ollama+手机模式.apk`
> 本轮两件事：**接上本地 Ollama**，**给每个角色一部手机（微信风 UI）**。

---

## 一、本地 Ollama 接入

### 1.1 为什么不是「再加一个 baseUrl 输入框」那么简单

原工程的 `ApiClient` 有一行硬编码：

```java
URL url = new URL("https://" + cleanBase + "/v1/chat/completions");
```

它**无条件补 `https://`**，并且总是发 `Authorization: Bearer <key>`。
这对云端服务是对的，但对本地 Ollama 是致命的——Ollama 默认不启 TLS，只说 http。

加了 Provider 抽象后，`ApiClient` 现在按类型决定协议：

| | 云端 | 本地 Ollama |
|---|---|---|
| 协议 | `https://`（用户写了就尊重） | `http://` |
| 鉴权 | `Authorization: Bearer` | 有 Key 才发，没有就跳过 |
| 连接超时 | 30s | 15s |
| 读超时 | 60s | **300s**（本地模型首次要加载权重） |
| 错误提示 | 「检查 API 地址」 | 「确认电脑上已运行 ollama serve」 |

端点拼接也更宽容：用户填的地址带了 `/v1`、`/v1/chat/completions`、末尾斜杠、
甚至误加了 `https://` 前缀，都能正确归一化。

### 1.2 明文流量必须显式放行

Android 9+ 默认禁止一切 http 明文流量。不配 `network_security_config` 的话，
连接会失败且报错信息非常不直观（只说 `Cleartext HTTP traffic not permitted`）。

新增 `res/xml/network_security_config.xml`：

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>   <!-- 模拟器→宿主机 -->
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
    <!-- 局域网：手机连电脑上的 Ollama 时是 192.168.x.x 之类，没法逐个列举 -->
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors><certificates src="system" /></trust-anchors>
    </base-config>
</network-security-config>
```

> 取舍说明：`base-config` 放开明文意味着**任意** http 地址都能访问。
> 考虑到 Ollama 的典型用法就是局域网 http，且公网服务仍受系统证书链约束，
> 这个口子是可接受的。如果只允许本地，把 `base-config` 改回 `false` 即可，
> 但那样真机连局域网 Ollama 就会失败。

### 1.3 一键检测

设置页的「检测并拉取模型列表」会：

1. 清洗用户填的 host（去空白、去协议前缀、去路径、去末尾斜杠）
2. 按顺序试候选地址：**用户填的 → `10.0.2.2:11434` → `127.0.0.1:11434` → `localhost:11434`**
3. 命中后 `GET /api/tags` 拉模型列表
4. 把实际可用的 host **回填到输入框**（用户填错了会被纠正）
5. 模型渲染成可点列表，**自动预选第一个**

**实测**（模拟器 → 宿主机真 Ollama）：

```
✓ 已连接 · 发现 6 个模型
地址：10.0.2.2:11434
  huihui_ai/qwen3-abliterated-sylvette:latest   ← 自动预选
  huihui_ai/qwen3-abliterated-explicit:latest
  huihui_ai/qwen3-abliterated:8b
  Qwen3.5-9B-explicit:latest
  Qwen3.5-9B-adult:latest
  Qwen3.5-9B:latest
```

配置落盘（`shared_prefs/echoflow_provider.xml`）：

```xml
<string name="provider_kind">ollama</string>
<string name="provider_base">10.0.2.2:11434</string>
<string name="provider_model">huihui_ai/qwen3-abliterated-sylvette:latest</string>
```

### 1.4 云端配置保持兼容

`ProviderStore.save()` 在云端模式下会**同时写回 `ChatStore`**，
所以 `MainActivity`、`PostsActivity` 这些还在读旧配置的地方不受影响——
和之前几轮一样，不破坏旧路径。

---

## 二、手机模式

### 2.1 它是什么

给每个角色一部「手机」。点进角色主页的 **📱 手机模式**，或首页顶栏的 **手机**，
看到的是**微信风格的会话列表**——每个角色是一个联系人。
点进去是微信风格的聊天页：浅灰底、白气泡 / 绿气泡、底部输入栏、四条 Tab。

### 2.2 核心差异：只说话，不要动作

这是本轮最重要的一条设计约束。

角色扮演的写法允许（甚至鼓励）用（）写动作：

> （她把盾往墙边一靠，往旁边挪了半步）……你也是被雨困住的？

但手机模式模拟的是**真的在用微信和她聊天**，所以要剥掉所有表演成分。
写进 system prompt 的硬约束：

```
【当前场景：手机聊天】
对方正在用手机和你聊天，就像发微信一样。
重要规则（必须严格遵守）：
1. 只输出你会打出来的文字。不要写任何动作、神态、心理描写。
2. 不要用（）或*号写旁白，例如「（她顿了顿）」是绝对禁止的。
3. 不要用第三人称描述自己。
4. 回复要短。通常一到两句，最多不超过三句。
5. 可以只回一个字或一个词，真人发消息就是这样。
...
```

**实测验证**（本地 Qwen3 8B）：

| | 回复 |
|---|---|
| 主聊天模式 | `*轻快地整了整银发环* 陛下可在长廊中？臣下奉命守候多时了。*眼睛微弯*` |
| 手机模式 | `在。` |

同一个模型、同一个角色卡，只因为追加了那段约束，输出完全不同。
这不是「加了点提示词」，是**把表演降到了零**。

### 2.3 开场白要单独写

角色卡的 `first_mes` 是**角色扮演风格**的，带动作描写，直接放进手机模式就破功了。

所以 `PhonePromptGreeting` 为 6 个内置角色各写了一句「像真人发的第一条消息」：

| 角色 | 手机模式开场白 |
|---|---|
| 月铃 | `今天又下雨了。\n你那边呢。` |
| 凛 | `明天早上七点，道场。\n别迟到。` |
| 白夜 | `今晚云很少。\n你要不要上来看一眼。` |
| 洛可 | `喂！你上次那个东西我修好啦\n明天来拿？还是我给你送过去` |
| 艾莲 | `今天新到的一批里，有本你可能会喜欢的。\n什么时候过来坐坐。` |
| 爱丽丝 | `……是我。\n今天巡夜路过你们那条街，就想着说一声。` |

自定义角色没有对应开场白时，`stripStageDirections()` 会用正则把
`（……）` / `(……)` / `*……*` 三种动作描写剥掉，只留会说出口的部分。

### 2.4 与主聊天的关系

```
                    ┌── 主聊天（CardChatActivity）── 角色扮演写法，允许动作描写
角色卡 / 关系 / 情绪 / 记忆 ─┤
                    └── 手机模式（PhoneChatActivity）── 微信写法，纯对话
```

- **聊天历史完全独立**（`PhoneStore` vs `ChatStore` / `files/chats/`），两条时间线互不干扰
- **但状态是共用的**：手机模式回复结束后同样跑 `StateAnalyzer`，
  所以在手机上聊的内容，主聊天里她也记得
- 手机模式右上角 `···` 可以一键切到主聊天

这个设计的意图是：同一个角色，两种相处方式。
想认真推进剧情时用主聊天，想随手说两句时用手机模式。

### 2.5 微信 UI 的还原度

| 元素 | 实现 |
|---|---|
| 会话列表 | 白底、50dp 方头像、名字 + 预览、右侧时间 |
| 未读红点 | 圆形红底白字角标（当前未启用，预留给主动消息） |
| 气泡 | 对方白底左气泡 / 自己微信绿 `#95EC69` 右气泡，圆角不对称 |
| 输入栏 | 浅灰底 + 语音按钮 + 圆角输入框 + 绿色发送 |
| 顶部栏 | 浅灰 `#EDEDED` + 居中标题 + 左右操作 |
| 底部 Tab | 微信 / 通讯录 / 发现 / 我（后三个是占位） |
| 时间格式 | 今天 `HH:mm`、昨天「昨天」、本周星期几、更早 `yyyy/M/d` |

---

## 三、验证记录

| 项 | 结果 |
|---|---|
| 设置页 Provider 切换 | ✅ 切到本地后自动隐藏 API Key 字段 |
| 检测本地 Ollama | ✅ `✓ 已连接 · 发现 6 个模型`，自动预选第一个 |
| 配置持久化 | ✅ `echoflow_provider.xml` 三个字段正确 |
| 会话列表 | ✅ 6 个角色带头像，微信风 |
| 手机模式聊天页 | ✅ 微信风气泡，开场白无动作描写 |
| **真实对话** | ✅ 发「you there」→ 本地 Qwen3 回复「在。」 |
| 回复不带动作 | ✅ 与主聊天模式对比明显 |
| 无 Key 也能用 | ✅ 本地模式跳过密钥校验 |
| 崩溃 | ✅ 全程无 |

---

## 四、踩到的坑

### 顶栏按钮已经多到需要横向滑动

上一轮为了修「设置按钮被挤出屏幕」把按钮行改成了 `HorizontalScrollView`。
这轮又加了「手机」按钮，现在**默认视野里看不到「设置」**，必须滑一下。

这是当时那个修复的合理后果（优先保证功能可达），但按钮数还在涨。
**下一轮该做的**：照设计稿换成「标题 + 一个 ⋯ 溢出菜单」，
把那 6 个按钮收进菜单里。这属于改信息架构，不该混在 bug 修复里做。

### 键位遮挡导致自动化点击失败

测试发消息时，`input text` 弹出键盘后布局上移，按固定坐标点「发送」点空了。
**处理**：先 `keyevent 111`（ESC）收键盘让布局复位，再重新 dump 拿坐标。
这条也补进了 `tools/efdrive.ps1` 的使用说明。

---

## 五、真机怎么连本地 Ollama

1. 电脑上启动：`ollama serve`（默认监听 `127.0.0.1:11434`）
2. **让 Ollama 监听所有网卡**（否则手机连不上）：
   ```powershell
   $env:OLLAMA_HOST="0.0.0.0:11434"; ollama serve
   ```
3. 查电脑局域网 IP：`ipconfig` → 找 `IPv4 地址`，例如 `192.168.1.100`
4. 手机与电脑连**同一个 Wi-Fi**
5. App 里填 `192.168.1.100:11434`，点检测
6. 若连不上：Windows 防火墙放行 11434 端口

> 模拟器不用管这些，直接用 `10.0.2.2:11434`（模拟器映射到宿主机）。
