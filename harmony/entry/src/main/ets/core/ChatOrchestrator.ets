import {
  CharacterCard, Memory, MemoryTier, Lorebook, RelationshipState, EmotionState,
  StoryEvent, ChatSession, Message, MessageVariant, AnalysisResult, ProactivePolicy
} from './Models';
import { PromptBuilder, PromptContext, TimeContext, WeatherContext } from './PromptBuilder';
import { ContextManager, Compressor, AssembleResult } from './ContextManager';
import { StateAnalyzer, MemoryWriter, RelationUpdater, EmotionUpdater, EventWriter } from './StateAnalyzer';
import { ProactiveService } from './ProactiveService';
import { ProviderRegistry, ChatTurn } from '../provider/LlmProvider';
import { TimeService, WeatherService } from '../platform/TimeService';
import { Emotions } from '../theme/Tokens';

/**
 * 生命感核心编排器（需求 §53）
 *
 *   Character Card → Persona → World/Lore → Memory → Relationship → Emotion
 *        → Current Time → Weather → Scenario → Prompt → LLM → Response
 *        → Memory Extraction → Event Detection → Relationship Update → Emotion Update
 *
 * 关键设计：**分析链路永远在回复流结束之后异步执行，且失败不影响对话**。
 */

export interface CharacterRuntime {
  card: CharacterCard;
  characterId: string;
  persona: string;
  lorebooks: Lorebook[];
  memories: Memory[];
  relationship: RelationshipState;
  emotion: EmotionState;
  events: StoryEvent[];
  session: ChatSession;
  scenarioDirective: string;
  lastInteractionAt: number;
}

export interface TurnCallbacks {
  /** 流式增量，UI 侧需做 16ms 节流（见 Chat.onStreamDelta） */
  onDelta: (text: string) => void;
  /** 流结束 */
  onDone: (full: string) => void;
  /** 状态更新完成后回调，用于展示「羁绊 +2」与刷新角色主页 */
  onStateApplied: (result: AnalysisResult | null, note: string) => void;
  onError: (e: Error) => void;
}

export class ChatOrchestrator {
  private readonly context: ContextManager;
  private readonly compressor: Compressor;
  private readonly analyzer: StateAnalyzer;
  private readonly proactive: ProactiveService;
  private readonly time: TimeService;
  private readonly weather: WeatherService;

  constructor(
    private readonly registry: ProviderRegistry,
    private readonly repo: RepoPort,
    time: TimeService,
    weather: WeatherService,
    policy: ProactivePolicy
  ) {
    this.time = time;
    this.weather = weather;
    this.context = new ContextManager(128000, 2000, 20);
    this.compressor = new Compressor(p => this.registry.summarize().complete([
      { role: 'system', content: p }, { role: 'user', content: '请输出更新后的前情提要。' }
    ]));
    this.analyzer = new StateAnalyzer(registry);
    this.proactive = new ProactiveService(policy, registry, time, weather);
  }

  // ==================================================================
  // 一次完整回合
  // ==================================================================
  async send(rt: CharacterRuntime, userText: string, cb: TurnCallbacks): Promise<void> {
    // ---- 1. 落库用户消息 ----
    const userMsg = ChatOrchestrator.newMessage(rt.session.chatId, 'user', userText, 'text');
    await this.repo.appendMessage(userMsg);
    rt.lastInteractionAt = Date.now();

    // ---- 2. 组装 Prompt（生命感链路的全部输入在这里汇合） ----
    const promptCtx: PromptContext = {
      card: rt.card,
      persona: rt.persona,
      memories: rt.memories,
      lorebooks: rt.lorebooks,
      relationship: rt.relationship,
      emotion: rt.emotion,
      time: this.buildTime(),
      weather: this.buildWeather(),
      scenarioDirective: rt.scenarioDirective,
      userInput: userText,
      recentContext: rt.session.chatId ? await this.repo.recentTranscript(rt.session.chatId, 12) : ''
    };
    const bundle = PromptBuilder.build(promptCtx);

    // ---- 3. 上下文预算裁剪 + 必要时增量压缩（§36 / §37） ----
    const history = await this.repo.recentMessages(rt.session.chatId, 200);
    const layers = {
      system: bundle.system,
      summary: await this.repo.summary(rt.session.chatId),
      recent: history,
      locked: history.filter(m => m.locked)
    };
    const assembled: AssembleResult = this.context.assemble(layers);
    if (assembled.needCompress) {
      const updated = await this.compressor.run(layers.summary, assembled.dropped, rt.session);
      await this.repo.saveSummary(rt.session.chatId, updated);
    }

    const usage = this.context.usage(bundle.system, layers.summary, assembled.messages);
    this.repo.emit('context', { used: usage.used, max: usage.max, ratio: usage.ratio, compress: this.context.format(usage) });

    // ---- 4. 组装 messages 并流式请求 ----
    const turns: ChatTurn[] = [{ role: 'system', content: bundle.system }];
    for (const m of assembled.messages) {
      if (m.role === 'system') continue;
      const v = m.variants[m.activeVariant];
      if (!v) continue;
      turns.push({ role: m.role as 'user' | 'assistant', content: v.content });
    }
    turns.push({ role: 'user', content: userText });

    const assistant = ChatOrchestrator.newMessage(rt.session.chatId, 'assistant', '', 'text');
    assistant.variants = [{ content: '', createdAt: Date.now(), providerId: 'chat', model: this.registry.chat().config.model }];
    assistant.activeVariant = 0;

    let full = '';
    this.repo.emit('stream:start', assistant);

    await this.registry.chat().stream(turns, {
      onDelta: (d: string) => { full += d; cb.onDelta(d); },
      onDone: (text: string) => {
        assistant.variants[0].content = text || full;
        cb.onDone(text || full);
      },
      onError: (e: Error) => cb.onError(e)
    });

    if (full.length === 0) return;   // 出错路径已在 onError 处理

    // ---- 5. 落库回复（先给用户看到，再做后台状态更新） ----
    await this.repo.appendMessage(assistant);

    // ---- 6. 异步状态更新链路（§5/§6/§8/§9/§28） ----
    //    绝不 await 到用户可感知的位置；失败只记日志。
    this.updateStateAsync(rt, userText, full, cb);
  }

  private updateStateAsync(rt: CharacterRuntime, userText: string, reply: string, cb: TurnCallbacks): void {
    const dialogue = `用户：${userText}\n${rt.card.data.name}：${reply}`;
    this.analyzer.analyze(dialogue, rt.card.data.name).then(async (result: AnalysisResult | null) => {
      if (!result) { cb.onStateApplied(null, ''); return; }

      const notes: string[] = [];

      // --- 记忆提取与去重 ---
      const incoming = MemoryWriter.toNewMemories(
        result.memories, rt.characterId,
        { chatId: rt.session.chatId, messageId: '', ts: Date.now(), excerpt: userText.substring(0, 40) }
      );
      for (const m of incoming) {
        const target = MemoryWriter.shouldMerge(m, rt.memories);
        if (target) {
          await this.repo.saveMemory(MemoryWriter.merge(target, m));
        } else {
          rt.memories.push(m);
          await this.repo.saveMemory(m);
        }
      }

      // --- 关系成长（UI 只在真正变化时显示一行轻提示） ---
      const levels = rt.card.data.extensions.echoflow?.relationLevels ?? Emotions.defaultLevels;
      const thresholds = rt.card.data.extensions.echoflow?.relationThresholds ?? Emotions.defaultThresholds;
      const rel = RelationUpdater.apply(rt.relationship.bond, result.relationDelta, thresholds, levels);
      if (result.relationDelta > 0) {
        notes.push(`羁绊 +${result.relationDelta}`);
        rt.relationship.sources['chat'] = (rt.relationship.sources['chat'] ?? 0) + result.relationDelta;
      }
      if (rel.leveledUp) notes.push(`关系升级为「${rel.level}」`);
      rt.relationship.bond = rel.bond;
      rt.relationship.level = rel.level;
      rt.relationship.levelIndex = rel.levelIndex;
      rt.relationship.lastDelta = result.relationDelta;
      rt.relationship.updatedAt = Date.now();
      await this.repo.saveRelationship(rt.relationship);

      // --- 情绪状态机（置信度不足则不切换） ---
      const emo = EmotionUpdater.apply(rt.emotion.current, result.emotion.name, result.emotion.confidence);
      if (emo.changed) {
        rt.emotion.history.push({ emotion: rt.emotion.current, from: rt.emotion.since, to: Date.now() });
        notes.push(`情绪 ${rt.emotion.current} → ${emo.emotion}`);
        rt.emotion.prev = rt.emotion.current;
        rt.emotion.current = emo.emotion;
        rt.emotion.since = Date.now();
        await this.repo.saveEmotion(rt.emotion);
      }

      // --- 事件簿：只有值得记录的事才落表 ---
      if (result.event) {
        const ev = EventWriter.toEvent(result.event, rt.characterId, []);
        rt.events.push(ev);
        await this.repo.saveEvent(ev);
        if (ev.milestone) notes.push(`解锁事件「${ev.title}」`);
      }

      // --- 人设一致性（§28）：只在异常时提示，正常不打扰 ---
      const c = result.consistency;
      const bad = (!c.characterOk) || (!c.worldOk) || (!c.timelineOk) || (!c.memoryOk);
      if (bad) {
        console.warn('[Consistency] 检测到异常：', c.issues);
        notes.push('角色一致性待复核');
      }

      cb.onStateApplied(result, notes.join(' · '));
    }).catch(e => {
      console.warn('[Orchestrator] 状态更新异常：', e);
      cb.onStateApplied(null, '');
    });
  }

  // ==================================================================
  // 主动消息（§2 / §54）
  // ==================================================================

  /**
   * 完整示例：用户三天没打开 App + 今天下雨 + 记忆「用户喜欢雨」+ 事件「曾在雨天聊天」
   * → 角色主动发来一条消息。
   */
  async tryProactive(rt: CharacterRuntime, weatherBefore: string): Promise<Message | null> {
    const idle = this.time.idleHours(rt.lastInteractionAt);
    const weatherChanged = this.weather.changedSince(weatherBefore);
    const recentEvent = rt.events.length > 0 ? rt.events[rt.events.length - 1] : null;

    // 记忆共振：检索与当前天气相关的记忆（§54 的「用户喜欢雨」）
    const weatherText = this.weather.current()?.text ?? '';
    const resonant = rt.memories
      .filter(m => !m.disabled && weatherText.length > 0 && m.content.includes(weatherText.substring(0, 1)))
      .slice(0, 3);

    const triggerLabel = [
      weatherChanged ? `weather_${weatherText}` : '',
      resonant.length > 0 ? 'memory_resonance' : '',
      recentEvent && recentEvent.milestone ? `event_${recentEvent.id}` : '',
      `idle_${Math.round(idle)}h`
    ].filter(x => x.length > 0).join('+');

    const pm = await this.proactive.consider({
      characterId: rt.characterId,
      characterName: rt.card.data.name,
      personality: rt.card.data.personality,
      idleHours: idle,
      weatherChanged,
      memoryResonance: resonant.length > 0,
      openPlot: rt.scenarioDirective,
      specialDate: this.time.festival(),
      emotion: rt.emotion.current,
      relationship: rt.relationship,
      memories: resonant.map(m => m.content),
      recentEvent,
      triggerLabel
    });
    if (!pm) return null;

    await this.repo.saveProactive(pm);
    const msg = ChatOrchestrator.newMessage(rt.session.chatId, 'assistant', pm.text, 'text');
    await this.repo.appendMessage(msg);
    return msg;
  }

  // ==================================================================

  private buildTime(): TimeContext {
    const d = this.time.now();
    return {
      date: this.time.dateStr(d),
      weekday: this.time.weekdayStr(d),
      time: this.time.timeStr(d),
      timezone: this.time.tz(),
      festival: this.time.festival(d)
    };
  }

  private buildWeather(): WeatherContext {
    const w = this.weather.current();
    return {
      text: w?.text ?? '',
      temp: w?.temp ?? '',
      city: w?.city ?? '',
      allowed: w !== null
    };
  }

  static newMessage(chatId: string, role: 'user' | 'assistant', content: string, kind: Message['kind']): Message {
    return {
      id: `msg_${Date.now()}_${Math.floor(Math.random() * 1000)}`,
      chatId,
      role,
      variants: [{ content, createdAt: Date.now(), providerId: '', model: '' }],
      activeVariant: 0,
      kind,
      locked: false,
      favorite: false,
      createdAt: Date.now()
    };
  }
}

/** 持久化端口：由 storage 层实现，core 不感知 SQLite / 文件细节 */
export interface RepoPort {
  appendMessage(m: Message): Promise<void>;
  recentMessages(chatId: string, limit: number): Promise<Message[]>;
  recentTranscript(chatId: string, limit: number): Promise<string>;
  summary(chatId: string): Promise<string>;
  saveSummary(chatId: string, summary: string): Promise<void>;
  saveMemory(m: Memory): Promise<void>;
  saveRelationship(r: RelationshipState): Promise<void>;
  saveEmotion(e: EmotionState): Promise<void>;
  saveEvent(e: StoryEvent): Promise<void>;
  saveProactive(p: { id: string; text: string; createdAt: number }): Promise<void>;
  emit(channel: string, payload: Object): void;
}
