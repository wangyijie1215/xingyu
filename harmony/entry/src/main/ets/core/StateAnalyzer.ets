import { AnalysisResult, AnalyzedMemory, Memory, MemoryGrade, GRADE_WEIGHT, StoryEvent } from './Models';
import { PromptBuilder } from './PromptBuilder';
import { ProviderRegistry } from '../provider/LlmProvider';

/**
 * 状态分析器（需求 §5 / §6 / §8 / §9 / §28）
 * 在回复流**结束之后**异步执行，绝不阻塞 UI，也绝不让分析失败影响对话。
 *
 * 输出契约（严格 JSON）：
 *   memories[] / relation_delta / emotion{name,confidence} / event|null / consistency
 */
export class StateAnalyzer {
  constructor(private readonly registry: ProviderRegistry) {}

  async analyze(recentDialogue: string, characterName: string): Promise<AnalysisResult | null> {
    const provider = this.registry.analyze();
    const prompt = PromptBuilder.buildAnalyzerPrompt(recentDialogue, characterName);
    try {
      const raw = await provider.complete(
        [{ role: 'system', content: prompt }, { role: 'user', content: '请输出 JSON。' }],
        true
      );
      return StateAnalyzer.parse(raw);
    } catch (e) {
      // 分析链路的任何失败都在这里被吞掉：对话体验优先
      console.warn('[StateAnalyzer] 分析失败，已跳过本轮状态更新：', e);
      return null;
    }
  }

  /** 容错解析：先直解，失败则剥离 ```json 围栏重试一次 */
  static parse(raw: string): AnalysisResult | null {
    const direct = StateAnalyzer.tryJson(raw);
    if (direct) return direct;
    const fenced = raw.replace(/```json/gi, '').replace(/```/g, '').trim();
    const start = fenced.indexOf('{');
    const end = fenced.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return StateAnalyzer.tryJson(fenced.substring(start, end + 1));
    }
    return null;
  }

  private static tryJson(s: string): AnalysisResult | null {
    try {
      const j = JSON.parse(s) as Record<string, Object>;
      const memoriesRaw = (j['memories'] ?? []) as Array<Record<string, Object>>;
      const memories: AnalyzedMemory[] = memoriesRaw.map(m => ({
        content: String(m['content'] ?? ''),
        importance: Number(m['importance'] ?? 0.4),
        grade: (String(m['grade'] ?? 'general') as MemoryGrade),
        keywords: (m['keywords'] ?? []) as string[]
      })).filter(m => m.content.length > 0);

      const emo = (j['emotion'] ?? {}) as Record<string, Object>;
      const ev = j['event'] as Record<string, Object> | null;
      const cons = (j['consistency'] ?? {}) as Record<string, Object>;

      return {
        memories,
        relationDelta: Number(j['relation_delta'] ?? 0),
        emotion: {
          name: String(emo['name'] ?? '平静'),
          confidence: Number(emo['confidence'] ?? 0)
        },
        event: ev && ev['title'] ? {
          title: String(ev['title']),
          body: String(ev['body'] ?? ''),
          milestone: Boolean(ev['milestone'] ?? false),
          bondDelta: Number(ev['bond_delta'] ?? 0)
        } : null,
        consistency: {
          characterOk: Boolean(cons['character_ok'] ?? true),
          worldOk: Boolean(cons['world_ok'] ?? true),
          timelineOk: Boolean(cons['timeline_ok'] ?? true),
          memoryOk: Boolean(cons['memory_ok'] ?? true),
          issues: (cons['issues'] ?? []) as string[]
        }
      };
    } catch (e) {
      return null;
    }
  }
}

/**
 * 记忆写入策略（需求 §5：避免记忆库无限膨胀）
 * - trivial / importance < 0.25 → 只进 short 层，7 天后清理
 * - 与既有记忆高度相似 → 合并（保留高重要度、累加命中、更新来源）
 */
export class MemoryWriter {
  private static readonly SIMILARITY_MERGE = 0.88;
  private static readonly TRIVIAL_TTL = 7 * 86400000;

  /** 返回需要落库的新增记忆 */
  static toNewMemories(incoming: AnalyzedMemory[], characterId: string, source: Memory['source']): Memory[] {
    const now = Date.now();
    const out: Memory[] = [];
    for (const a of incoming) {
      const imp = Math.max(GRADE_WEIGHT[a.grade] ?? 0.4, a.importance);
      if (imp < 0.25) {
        out.push({
          id: `mem_${now}_${out.length}`,
          characterId,
          tier: 'short',
          content: a.content,
          importance: imp,
          grade: 'trivial',
          keywords: a.keywords,
          source,
          locked: false,
          disabled: false,
          hitCount: 0,
          createdAt: now,
          lastUsedAt: now
        });
        continue;
      }
      out.push({
        id: `mem_${now}_${out.length}`,
        characterId,
        tier: 'long',
        content: a.content,
        importance: imp,
        grade: a.grade,
        keywords: a.keywords,
        source,
        locked: false,
        disabled: false,
        hitCount: 0,
        createdAt: now,
        lastUsedAt: now
      });
    }
    return out;
  }

  /** 与既有记忆合并：保留更高重要度，累加命中，更新来源 */
  static shouldMerge(candidate: Memory, existing: Memory[]): Memory | null {
    for (const e of existing) {
      if (e.characterId !== candidate.characterId || e.disabled) continue;
      const score = MemoryWriter.jaccard(candidate.content, e.content);
      if (score >= MemoryWriter.SIMILARITY_MERGE) return e;
    }
    return null;
  }

  static merge(target: Memory, incoming: Memory): Memory {
    target.importance = Math.max(target.importance, incoming.importance);
    target.hitCount += 1;
    target.lastUsedAt = Date.now();
    target.source = incoming.source;
    if (target.content.length < incoming.content.length) target.content = incoming.content;
    return target;
  }

  private static jaccard(a: string, b: string): number {
    const sa = new Set<string>(a.split(''));
    const sb = new Set<string>(b.split(''));
    let inter = 0;
    sa.forEach(c => { if (sb.has(c)) inter++; });
    const union = sa.size + sb.size - inter;
    return union === 0 ? 0 : inter / union;
  }
}

/**
 * 关系成长（需求 §6）
 * 单次增量 0~5，来源可追溯，跨等级时额外产出一次可展示的「关系升级」提示。
 */
export class RelationUpdater {
  static apply(bond: number, delta: number, thresholds: number[], levels: string[]): {
    bond: number; levelIndex: number; level: string; leveledUp: boolean;
  } {
    const next = Math.max(0, Math.min(100, bond + delta));
    let idx = 0;
    for (let i = 0; i < thresholds.length; i++) if (next >= thresholds[i]) idx = i;
    return { bond: next, levelIndex: idx, level: levels[idx], leveledUp: idx > RelationUpdater.indexOf(bond, thresholds) };
  }

  private static indexOf(bond: number, thresholds: number[]): number {
    let idx = 0;
    for (let i = 0; i < thresholds.length; i++) if (bond >= thresholds[i]) idx = i;
    return idx;
  }
}

/**
 * 情绪状态机（需求 §8）
 * 不是随机值：需要置信度 > 0.6 且与当前不同才切换，并记录 history。
 */
export class EmotionUpdater {
  private static readonly SWITCH_CONFIDENCE = 0.6;

  static apply(current: string, next: string, confidence: number): { emotion: string; changed: boolean } {
    if (next === current) return { emotion: current, changed: false };
    if (confidence < EmotionUpdater.SWITCH_CONFIDENCE) return { emotion: current, changed: false };
    return { emotion: next, changed: true };
  }

  /** 情绪 → 回复长度系数，供 UI 预期与 Prompt 调节 */
  static lengthFactor(emotion: string): number {
    switch (emotion) {
      case '开心': case '期待': return 1.15;
      case '悲伤': case '疲惫': case '孤独': return 0.7;
      case '生气': case '紧张': return 0.85;
      default: return 1.0;
    }
  }
}

/** 事件簿写入：只有真正重要的事情才落表（§9） */
export class EventWriter {
  static toEvent(a: NonNullable<AnalysisResult['event']>, characterId: string, messageIds: string[]): StoryEvent {
    const d = new Date();
    const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return {
      id: `evt_${Date.now()}`,
      characterId,
      date,
      title: a.title,
      body: a.body,
      milestone: a.milestone,
      bondDelta: a.bondDelta,
      images: [],
      sourceMessageIds: messageIds,
      note: ''
    };
  }
}
