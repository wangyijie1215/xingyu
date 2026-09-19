# EchoFlow · HarmonyOS NEXT 实现（参考实现）

这是设计规格 `design/02-设计系统与UI规格.md` 的可编译参考实现骨架。
代码按 **HarmonyOS NEXT（ArkTS / ArkUI，API 12+）** 编写；Android 侧的对应改造见 `design/05-实施路线与Android改造.md`。

## 目录

```
harmony/
├── entry/src/main/
│   ├── resources/base/element/color.json   设计 token 全部色值（$r('app.color.ef_*')）
│   └── ets/
│       ├── theme/Tokens.ets                间距/字阶/圆角/动效 + 语义色分组 + 情绪定义
│       ├── components/Ef.ets               EfChip / EfCard / SectionHead / BondRing / EfMeter
│       │                                   / EfListRow / SheetMenuItem
│       ├── core/Models.ets                 角色卡(Tavern V2) / 记忆 / 关系 / 情绪 / 事件
│       │                                   / 会话 / Lorebook / 主动消息 / Provider / 分析结果
│       ├── core/PromptBuilder.ets          ★ 生命感 Prompt 装配 + 记忆检索 + Lorebook 触发
│       ├── core/ContextManager.ets         上下文预算分层 + 增量压缩
│       ├── core/StateAnalyzer.ets          状态分析器 + 记忆写入策略 + 关系/情绪/事件更新
│       ├── core/ProactiveService.ets       主动消息触发评分 + 限流闸门 + 消息生成
│       ├── core/ChatOrchestrator.ets       ★ 一次完整回合的编排（§53 全链路）
│       ├── core/FortuneService.ets         ★ 神社：确定性运签 + 状态倾斜 + 御守 + 绘马
│       ├── core/Diary.ets                  角色日记四段生成 + 心情曲线 + 纪念日倒数
│       ├── core/Letter.ets                 信笺（她回信）+ 时间胶囊（到期投放）
│       ├── provider/LlmProvider.ets        OpenAI-compatible SSE + 能力协商 + 模型路由
│       ├── platform/SecureStore.ets        Asset Store 级密钥保护
│       ├── platform/TimeService.ets        时间感知 + 节日 + 相对时间 + 网络类型
│       ├── pages/CharacterHome.ets         角色主页（生命感仪表盘）
│       ├── pages/Chat.ets                  聊天（swipe / 分支 / 语音 / 图片 / 上下文条）
│       ├── pages/MemoryManager.ets         记忆管理器
│       ├── pages/VoiceCall.ets             实时语音通话
│       ├── pages/Shrine.ets                神社主页（朱印 / 御守 / 绘马 / 七日 / 她的日记）
│       └── pages/Fortune.ets               和纸签文 + 六维分段条 + 宜忌 + 角色解读
```

## 接线顺序（建议照着这个顺序落地，每一步都能跑起来）

### 第 1 步 · 资源与主题

1. 把 `color.json` 放进 `entry/src/main/resources/base/element/`。若与已有资源冲突，改前缀即可。
2. 页面中**只允许**使用 `$r('app.color.ef_*')` 与 `Tokens.ets` 的常量，禁止写死色值。CI 里可以加一条 grep 检查。

### 第 2 步 · 数据与持久化

`RepoPort`（`core/ChatOrchestrator.ets` 末尾）是存储层的唯一契约。用关系型数据库（`@kit.ArkData` 的 `relationalStore`）实现它，建索引：

```sql
CREATE INDEX idx_msg_chat_ts   ON messages(chat_id, created_at);
CREATE INDEX idx_mem_char_imp  ON memories(character_id, importance DESC);
CREATE INDEX idx_evt_char_date ON events(character_id, date DESC);
CREATE INDEX idx_char_cat      ON characters(category, hot_score DESC);
```

只有 `short` 层记忆允许 7 天后清理，`long` 与 `relation` 层永不过期。

### 第 3 步 · Provider

```ts
const registry = new ProviderRegistry();
registry.register(new LlmProvider({
  id: 'grok', label: 'Grok', kind: 'grok',
  baseUrl: 'https://api.x.ai/v1', model: 'grok-4',
  secretRef: 'grok', headers: {}, params: { temperature: 0.85 },
  capabilities: ['stream', 'tools', 'vision', 'json']
}));
// 成本优化：分析与总结走便宜模型（§35）
registry.setRoutes('grok', 'cheap', 'cheap');
await SecureStore.put('grok', 'API_KEY');   // 明文只进密钥库
```

**能力协商**：`LlmProvider.supports('tools')` 为 false 时，角色技能区块与 tools 参数都不会出现——不要向模型发送它无法处理的定义。

### 第 4 步 · 编排器

```ts
const orchestrator = new ChatOrchestrator(registry, repo, time, weather, proactivePolicy);

orchestrator.send(runtime, userText, {
  onDelta: (d) => chatPage.onStreamDelta(d),       // 内部已做 16ms 节流
  onDone: (full) => chatPage.finishStream(full),
  onStateApplied: (result, note) => chatPage.showBondNote(note),  // 「羁绊 +2 · 情绪 期待 → 安心」
  onError: (e) => chatPage.showError(e.message)
});
```

### 第 5 步 · 主动消息

在 `EntryAbility.onForeground()` 与后台任务里各调一次：

```ts
const weatherBefore = weather.summary();
await weather.refresh();                       // 静默刷新，失败沿旧值
const msg = await orchestrator.tryProactive(runtime, weatherBefore);
if (msg) notifier.push(msg);                   // 点击直达 chat/{characterId}
```

`ProactiveService.canSend()` 是唯一闸门：全局开关 → 单角色开关 → 每日上限 → 勿扰时段 → 仅 Wi-Fi → 触发阈值。**任何一条不过就不发**。

### 第 6 步 · 系统集成

- **通知**：`@kit.NotificationKit`，通知内容为主动消息正文，`wantAgent` 指向 `pages/Chat`。
- **服务卡片**：`form_config` 提供头像、名字、当前情绪、最近一条主动消息，点击直达聊天。
- **快捷方式**：`shortcuts` 配置常用角色。
- **语音**：TTS 走 `@kit.CoreSpeechKit` 或第三方 Provider（保存 `voiceId`）；STT 同理。`VoiceCall` 页只消费状态，不感知具体实现。

## 与设计稿的对应关系

| 设计稿（`design/shots/`） | 实现文件 |
|---|---|
| `01-home.png` 陪伴首页 | 待补：`pages/Home.ets`（结构见设计规格 §4.1） |
| `02-charhome.png` 角色主页 | `pages/CharacterHome.ets` |
| `03-chat.png` 聊天 | `pages/Chat.ets` |
| `04-memory.png` 记忆库 | `pages/MemoryManager.ets` |
| `09-call.png` 语音通话 | `pages/VoiceCall.ets` |
| 其余 8 屏 | 结构与 token 已在设计规格 §4 中给定，按同样模式实现 |

## 注意事项

1. **流式渲染**：`Chat.onStreamDelta` 里的 16ms 节流不可去掉。ArkUI 的 `ForEach` 以 `message.id` 为 key，只改最后一条的 `variants[activeVariant].content`，不要重建列表。
2. **分析链路必须异步**：`ChatOrchestrator.updateStateAsync` 不 `await`。记忆提取失败时对话必须照常完成。
3. **羁绊提示的频率**：`bondNote` 为空时 UI 不占位。不要为了「有反馈」而每轮都提示。
4. **情绪切换需要置信度**：`EmotionUpdater.SWITCH_CONFIDENCE = 0.6`，低于此值保持不变——情绪抖动比没有情绪更出戏。
5. **API Key**：`SecureStore` 之外的任何序列化路径都必须经 `SecureStore.isSecretField()` 过滤。导出与备份里出现 Key 属于严重缺陷。
