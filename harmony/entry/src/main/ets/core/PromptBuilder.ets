import {
  CharacterCard, Memory, MemoryTier, GRADE_WEIGHT, Lorebook, LoreEntry,
  RelationshipState, EmotionState
} from './Models';
import { Emotions } from '../theme/Tokens';

/**
 * 生命感核心：把角色卡 / Persona / 世界观 / 长期记忆 / 关系 / 情绪 / 时间 / 天气 / 剧本
 * 装配成一次请求的系统提示词。这是整个 App 的中枢（需求 §53）。
 *
 *   角色卡 → Persona → World → Memory → Relationship → Emotion
 *        → Current Time → Weather → Scenario → Prompt
 */

export interface TimeContext {
  /** yyyy年MM月dd日 */
  date: string;
  /** 周四 */
  weekday: string;
  /** HH:mm */
  time: string;
  timezone: string;
  /** 节日/纪念日，命中时非空 */
  festival: string;
}

export interface WeatherContext {
  text: string;      // 小雨
  temp: string;      // 18℃
  city: string;
  allowed: boolean;  // 用户是否授权
}

export interface PromptContext {
  card: CharacterCard;
  persona: string;
  memories: Memory[];
  lorebooks: Lorebook[];
  relationship: RelationshipState;
  emotion: EmotionState;
  time: TimeContext;
  weather: WeatherContext;
  scenarioDirective: string;
  /** 本轮用户输入，用于 Lorebook 关键词匹配 */
  userInput: string;
  /** 最近 N 条上下文，同样参与关键词匹配 */
  recentContext: string;
}

export interface PromptBundle {
  system: string;
  /** 命中的 Lore 条目，用于「世界观一致性」与调试面板 */
  loreHits: LoreEntry[];
  /** 实际注入的记忆 id，用于来源追溯与该轮命中计数 */
  memoryIds: string[];
  estimatedTokens: number;
}

export class PromptBuilder {
  /** Lorebook 注入总预算（token），超出按 priority 截断 */
  private static readonly LORE_BUDGET = 1200;
  /** 记忆默认注入条数 */
  static readonly MEMORY_TOPK = 12;

  // ==================== 记忆检索 ====================

  /**
   * score = 0.5*importance + 0.3*keyword_match + 0.2*recency_decay
   * 只取 Top-K，绝不整表拼接（需求 §36 / §52）
   */
  static retrieveMemories(all: Memory[], userInput: string, k: number = PromptBuilder.MEMORY_TOPK): Memory[] {
    const now = Date.now();
    const text = userInput.toLowerCase();
    const scored = all
      .filter(m => !m.disabled && m.tier !== MemoryTier_SHORT_ONLY)
      .map(m => {
        const kw: number = m.keywords.length === 0 ? 0
          : (m.keywords.some(x => text.includes(x.toLowerCase())) ? 1 : 0);
        const days = Math.max(0, (now - m.lastUsedAt) / 86400000);
        const recency = 1 / (1 + days / 7);
        return { m, s: 0.5 * m.importance + 0.3 * kw + 0.2 * recency };
      })
      .sort((a, b) => b.s - a.s);
    return scored.slice(0, k).map(x => x.m);
  }

  // ==================== Lorebook 关键词触发 ====================

  static matchLore(books: Lorebook[], text: string): LoreEntry[] {
    const hay = text.toLowerCase();
    const hits: LoreEntry[] = [];
    for (const b of books) {
      for (const e of b.entries) {
        if (e.constant) { hits.push(e); continue; }
        const found = e.keys.some(k => {
          const key = e.caseSensitive ? k : k.toLowerCase();
          return e.caseSensitive ? text.includes(key) : hay.includes(key);
        });
        if (found) hits.push(e);
      }
    }
    hits.sort((a, b) => (b.priority - a.priority) || (a.insertionOrder - b.insertionOrder));
    // 预算裁剪
    let used = 0;
    const out: LoreEntry[] = [];
    for (const e of hits) {
      const cost = PromptBuilder.tokens(e.content);
      if (used + cost > PromptBuilder.LORE_BUDGET) break;
      used += cost;
      out.push(e);
    }
    return out;
  }

  // ==================== 主装配 ====================

  static build(ctx: PromptContext): PromptBundle {
    const d = ctx.card.data;
    const loreHits = PromptBuilder.matchLore(ctx.lorebooks, ctx.userInput + '\n' + ctx.recentContext);
    const memories = PromptBuilder.retrieveMemories(ctx.memories, ctx.userInput);

    const p: string[] = [];

    // ---- 角色与沉浸约束（放在最前，权重最高） ----
    p.push(`【角色】你现在是 ${d.name}。`);
    p.push(`请完全代入角色，用第一人称回复，不要跳出角色，不要以旁白或作者身份说话。`);
    p.push(`【角色设定】${d.description}`);
    if (d.personality) p.push(`【性格】${d.personality}`);
    if (d.scenario) p.push(`【场景】${d.scenario}`);
    if (d.mesExample) p.push(`【示例对话】${d.mesExample}`);

    // ---- 世界观（关键词命中才注入，减少长期上下文压力） ----
    if (loreHits.length > 0) {
      const before = loreHits.filter(e => e.position === 'before_char').map(e => e.content).join('\n');
      const after = loreHits.filter(e => e.position === 'after_char').map(e => e.content).join('\n');
      if (before) p.push(`【世界观】\n${before}`);
      if (after) p.push(`【补充设定】\n${after}`);
    }

    // ---- 长期记忆 ----
    if (memories.length > 0) {
      p.push(`【关于用户和你们之间的长期记忆】`);
      p.push(memories.map(m => `- ${m.content}`).join('\n'));
      p.push(`（这些是你应该记住的事实，自然地融入对话，不要逐条复述。）`);
    }

    // ---- 关系 ----
    p.push(`【你们的关系】${ctx.relationship.level}，亲密度 ${Math.round(ctx.relationship.bond)}/100`);
    p.push(`（根据关系亲密度调整你的语气和亲近程度，不要机械提及数字。）`);

    // ---- 情绪 ----
    p.push(`【你当前的情绪】${ctx.emotion.current}`);
    p.push(`（${Emotions.directive(ctx.emotion.current)}不要每句都描述自己的情绪。）`);

    // ---- 时间感知（§23：绝不能让角色把时间搞错） ----
    p.push(`【当前时间】${ctx.time.date} ${ctx.time.time}（${ctx.time.weekday}，${ctx.time.timezone}）`);
    if (ctx.time.festival) p.push(`【今日】${ctx.time.festival}`);

    // ---- 天气：只在授权且与对话相关时使用，不每轮播报 ----
    if (ctx.weather.allowed) {
      p.push(`【天气】${ctx.weather.city} ${ctx.weather.text} ${ctx.weather.temp}`);
      p.push(`（天气仅在与对话相关时自然提及，不要每轮主动播报。）`);
    }

    // ---- Persona ----
    if (ctx.persona) p.push(`【用户设定】${ctx.persona}`);

    // ---- 剧本 / 剧情导演 ----
    if (ctx.scenarioDirective) p.push(`【剧情要求】${ctx.scenarioDirective}`);

    // ---- 角色卡自带的 system prompt 与后置指令 ----
    if (d.systemPrompt) p.push(d.systemPrompt);
    if (d.postHistoryInstructions) p.push(d.postHistoryInstructions);

    const system = p.join('\n\n');
    return {
      system,
      loreHits,
      memoryIds: memories.map(m => m.id),
      estimatedTokens: PromptBuilder.tokens(system)
    };
  }

  // ==================== 状态分析器提示词（§5 / §53） ====================

  static buildAnalyzerPrompt(recentDialogue: string, characterName: string): string {
    return [
      `你是角色记忆与状态分析器。基于以下对话，输出严格的 JSON，不要其他文字。`,
      `对话：\n${recentDialogue}`,
      `要求：`,
      `1. memories：只提取真正重要、值得长期记住的信息。普通寒暄、日常琐事不要记忆。`,
      `   每条给出 content / importance(0~1) / grade(core_identity|strong_preference|important_fact|meaningful|general|trivial) / keywords。`,
      `2. relation_delta：这次对话让关系亲近了多少（0-5），普通对话1，深入对话2-3，特殊时刻4-5。`,
      `3. emotion：基于角色性格和对话内容，判断 ${characterName} 当前的情绪状态。`,
      `   只能取：${Emotions.all.join(' / ')}。给出 name 与 confidence(0~1)。`,
      `4. event：只有当这次对话发生了值得记录的重要事情时才填，否则填 null。`,
      `   字段 title / body / milestone / bond_delta。`,
      `5. consistency：检查角色是否忘记自己是谁、忘记世界观、错误称呼、自相矛盾、时间线错误、角色能力错误。`,
      `输出格式：`,
      `{"memories":[],"relation_delta":1,"emotion":{"name":"平静","confidence":0.6},` +
      `"event":null,"consistency":{"character_ok":true,"world_ok":true,"timeline_ok":true,"memory_ok":true,"issues":[]}}`,
      `只输出 JSON，绝对不要输出解释、markdown代码块或其他文字。`
    ].join('\n');
  }

  /** 朋友圈动态生成（§朋友圈） */
  static buildPostPrompt(name: string, personality: string, emotion: string, ctxLine: string): string {
    return [
      `你现在是 ${name}。角色性格：${personality}`,
      ctxLine,
      `请你以 ${name} 的第一人称，发一条朋友圈动态。要求：`,
      `- 一两句话，像真人随手发的朋友圈`,
      `- 可以分享日常、心情、所见所感`,
      `- 完全符合角色性格和身份`,
      `- 让你的语气符合当前情绪「${emotion}」`,
      `- 不要解释，不要引号，直接输出动态正文`,
      `- 不要加「发了一条朋友圈」这类前缀`
    ].join('\n');
  }

  /** 剧情总结（§16） */
  static buildSummaryPrompt(transcript: string): string {
    return [
      `请总结以下角色扮演对话，输出以下字段（中文）：`,
      `本章标题 / 主要人物 / 发生事件 / 关系变化 / 重要记忆 / 未解决剧情`,
      `对话：\n${transcript}`
    ].join('\n');
  }

  /** 上下文压缩（§37）：增量摘要，不重新生成 */
  static buildCompressPrompt(previousSummary: string, droppedMessages: string): string {
    return [
      `你正在维护一段长期角色扮演的「前情提要」。`,
      previousSummary ? `已有前情提要：\n${previousSummary}` : `目前还没有前情提要。`,
      `请把以下将要被压缩掉的对话合并进前情提要，保留：人物关系变化、已发生的关键事件、未解决的伏笔。`,
      `不要保留寒暄和闲聊。输出更新后的完整前情提要（不超过 300 字）。`,
      `对话：\n${droppedMessages}`
    ].join('\n');
  }

  /** 粗略 token 估算：中文按 1 字 ≈ 1 token，英文按 4 字符 ≈ 1 token */
  static tokens(s: string): number {
    if (!s) return 0;
    let cjk = 0;
    let other = 0;
    for (let i = 0; i < s.length; i++) {
      const c = s.charCodeAt(i);
      if (c >= 0x4e00 && c <= 0x9fff) cjk++; else other++;
    }
    return cjk + Math.ceil(other / 4);
  }
}

/** short 层是当前上下文，不进长期检索 */
const MemoryTier_SHORT_ONLY: MemoryTier = 'short';
