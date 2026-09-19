# 实施路线 · 与 v3.0 Android 客户端的对接

`EchoFlow-v3.0` 是 Android/Kotlin 程序化 UI 构建（无 xml 布局），本设计系统与它在语义上完全对齐，
因此 **可以先在现有 Android 端落地视觉与交互升级，再把服务层迁移到 HarmonyOS**，两端的 token 与数据模型共用一套定义。

---

## 一、现状 → 目标：逐类改造清单

| v3.0 类 | 保留 / 改造 | 说明 |
|---|---|---|
| `ApiClient` | **保留，补能力** | 已是 SSE 流式。新增：`supports(capability)` 能力协商、非流式 `complete(jsonMode)` 供分析器使用、`ProviderRegistry` 任务路由 |
| `PromptBuilder` | **保留，按 §53 扩展** | 现有已含记忆/关系/情绪/时间四段；补：Lorebook 关键词注入（带 1200 token 预算与 priority 截断）、Persona、剧本指令、天气（仅授权且相关时） |
| `StateAnalyzer` | **保留，补容错** | 已输出严格 JSON。补：剥离 ```json 围栏重试、解析失败静默丢弃（绝不阻塞回复）、一致性检查字段 |
| `SecureStore` | **保留** | Keystore 语义与 HarmonyOS Asset Store 等价，迁移时只换实现 |
| `CharacterCard` / `PngChara` | **保留** | 标准 Tavern V2。新增字段全部放 `extensions.echoflow`（voice / appearanceLock / visual / skillIds / relationLevels / worldIds / schedule），保证导回 SillyTavern 不破结构 |
| `CardStore` / `CardListActivity` | **改造为角色库** | 列表项加亲密度徽标、关系等级、情绪 Chip；加分类横轨、搜索、排行（对应 `discover` 屏） |
| `CardEditActivity` | **保留 + 扩字段** | 增加：声音设置（Provider/音色/语速/情绪/音调/Voice ID）、外貌锁定（prompt+seed+refImage）、聊天背景、技能勾选、关系等级自定义 |
| `CharacterProfileActivity` | **重写为角色主页** | 从信息页改为生命感仪表盘，结构见设计规格 §4.2。这是本次改造**收益最高、风险最低**的一步 |
| `CardChatActivity` + `MessageAdapter` | **重构消息模型** | `Message` → `variants[] + activeVariant`（swipe 不覆盖）、`kind ∈ text/voice/image/event/call`、`locked`、`favorite`；RecyclerView 用 `DiffUtil` + 只通知最后一项的 payload 变更，保证流式不卡 |
| `ChatStore` | **扩展为分支树** | 增加 `branch{id,parent,parentMessageId}`；回溯 = 新建分支，重启 = 新建会话但复用角色卡/Persona/世界观/记忆 |
| `MemoryStore` / `Memory` | **补管理能力** | 加 `grade` / `keywords` / `hitCount` / `locked` / `source{chatId,messageId,excerpt}`；检索改为 `0.5*importance+0.3*keyword+0.2*recency` Top-K |
| `CharacterStateStore` / `RelationshipState` | **保留，补来源** | `sources{chat,event,gift,moments}` 追溯成长来源；等级阈值可由角色卡覆盖 |
| `EmotionState` | **保留，补状态机** | 加 `confidence` 阈值（<0.6 不切换）、`history`、`lengthFactor` 影响回复长度 |
| `EventStore` / `StoryEvent` | **保留，补回忆卡** | 加 `milestone` / `bondDelta` / `images` / `sourceMessageIds` / `card`（星念卡） |
| `PostStore` / `PostsActivity` | **保留，补世界感** | 动态加：情绪、天气、时间、图片；角色姓名按角色主题色；支持「角色们正在发动态…」批量生成 |
| `MarkdownParser` | **保留** | 角色旁白用斜体、动作补白用 `（）`，与设计稿一致 |
| `MainActivity` | **改造为 Tab 容器** | 4 Tab：陪伴 / 世界 / 发现 / 我的；角色主页与聊天为二级页（隐藏 TabBar） |
| `Persona` | **保留** | 在「我的」页提供编辑入口与「发送给 AI 的内容预览」 |

---

## 二、视觉层落地顺序（不依赖服务层重构，可先做）

1. **建立 token**：把 `harmony/entry/src/main/resources/base/element/color.json` 的值转成 Kotlin 常量对象，接入 Material 主题。
2. **组件库**：实现 `EfChip` / `EfCard` / `SectionHead` / `BondRing`（用 `Canvas` 或自绘 View + `LinearGradient` 描边）/ `EfMeter` / `EfListRow`。
3. **逐屏替换**：角色主页 → 聊天 → 记忆 → 事件 → 朋友圈 → 发现 → 我的 → Provider。每替换一屏立刻真机看效果。
4. **动效**：统一 260ms `FastOutSlowInInterpolator`（≈ `cubic-bezier(.22,.61,.36,1)`）；脉冲、光点、波形三类无限动画；系统「减弱动效」开启时全部降级。

> 对照物：`design/prototype/index.html` 是 1:1 视觉基线，`design/shots/*.png` 是每屏的静态基准。

---

## 三、需要新增的页面

| 页面 | 优先级 | 说明 |
|---|---|---|
| 首页「陪伴」 | P0 | 主动消息条 + 英雄卡 + 角色轨道 + 今晚的世界 |
| 角色主页重写 | P0 | 见上 |
| 记忆管理器 | P0 | §4 |
| 事件簿 | P0 | §9 / §16 |
| 关系与情绪 | P0 | §6 / §7 / §8 / §27 / §28 |
| 剧情 / 世界观 | P1 | §10 / §11 / §14 / §15 |
| 发现 | P2 | §41 / §42 |
| Provider 管理 | P1 | §34 / §35 / §36 |
| 语音通话 | P2 | §19 |
| 全局搜索 | P2 | §48 |
| 角色创建器（一句话生成） | P2 | §29 |
| 礼物 / 回忆卡 | P2 | §25 / §26 / §40 |

---

## 四、里程碑

| 阶段 | 内容 | 验收标准 |
|---|---|---|
| **M1 视觉基线**（1–2 周） | token + 组件库 + 角色主页重写 | 角色主页与 `02-charhome.png` 一致；深色观感达标 |
| **M2 记忆与事件**（2 周） | 记忆管理器 + 事件簿 + 关系情绪面板 + 分析器容错 | 记忆可增删改禁锁搜；来源可回溯；「羁绊 +N」只在真正变化时出现 |
| **M3 主动性**（1–2 周） | ProactiveService + 通知 + 全套限流 | 关掉开关后**完全静默**；每日上限与勿扰时段生效 |
| **M4 剧情与世界观**（2 周） | Lorebook + 分支 + 回溯 + 重启 + 剧情总结 | 关键词命中能注入且 token 预算不超；回溯不删原剧情 |
| **M5 声音与图像**（2 周） | TTS 气泡 + 语音输入 + 角色照片 + 外貌锁定 | 跨消息声线一致；同一角色照片人脸稳定 |
| **M6 语音通话**（2 周） | 实时通话 + 转录 + 总结 | 转录与文字聊天关联；通话可总结为事件 |
| **M7 生态**（持续） | 发现 / 社区 / 导入导出 / 备份 / 搜索 / 服务卡片 | 角色卡与 SillyTavern 双向无损；备份不含 API Key |
| **M8 HarmonyOS 迁移** | 按 `harmony/README.md` 接线顺序 | 两端数据可互导 |

---

## 五、容易踩的坑（来自本次设计推演）

1. **数值播报**：StateAnalyzer 每轮都会返回 `relation_delta`，直接渲染会变成「亲密度 +1」刷屏。必须只在 `delta > 0` 且与上一次不同时显示一行，且不要写进消息列表的持久数据里。
2. **流式卡顿**：RecyclerView/ForEach 若每次 delta 都 `notifyDataSetChanged`，长回复必掉帧。用 payload 只更新最后一项的 TextView。
3. **Lorebook 撑爆上下文**：不设预算时，关键词命中会一次注入十几条。必须按 priority 截断（默认 1200 token）。
4. **情绪抖动**：让模型每轮重判情绪会导致语气反复横跳。加置信度阈值 + 最短持续时间（建议 ≥ 10 分钟）。
5. **记忆爆炸**：不做 grade/importance 过滤，一个月后记忆表会有几千条琐碎记录。`trivial` 只进 short 层并 7 天清理，是必须的。
6. **Key 泄漏进备份**：导出时最容易顺手把 Provider 配置整段序列化。在序列化入口按字段名过滤（`key` / `token` / `secret` / `password`）。
7. **主动消息打扰**：这是陪伴类产品最容易翻车的地方。「关闭」必须真的关闭，包括后台任务与通知通道。
8. **外貌漂移**：不锁 seed + prompt 的图片生成，同一个角色每次生成都是另一个人。`appearanceLock` 必须在生成时强制注入。
9. **回溯误删**：把「从这里继续」实现成删除后续消息，用户会永久丢失剧情。必须是新建分支。
10. **重启清太多**：「重启本次故事」只清聊天历史，角色卡 / Persona / 世界观 / 长期记忆必须保留——界面上要写清楚，否则用户不敢点。

---

## 六、与 v3.0 的向后兼容

- 旧的角色卡（无 `extensions.echoflow`）：全部字段给默认值（默认声线、默认主题紫、默认 6 级关系、无 Lorebook），**不需要用户做任何迁移**。
- 旧的聊天记录：`Message` 迁移为单 variant 的 `variants[0]`，`activeVariant = 0`，内容不变。
- 旧的记忆：缺 `grade` 的按 `importance` 反推（≥0.8→important_fact，≥0.5→meaningful，其余→general）。
- 数据库升级走 `ALTER TABLE ADD COLUMN`，不做破坏性重建。
