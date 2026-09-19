import { Message, ChatSession } from './Models';
import { PromptBuilder } from './PromptBuilder';

/**
 * 上下文管理器（需求 §36 / §37）
 * 目标：聊天再长也不会「突然忘记剧情」。
 *
 * 分层预算：
 *   system（角色设定+世界观）  —— 常驻，不可裁剪
 *   memories（长期记忆 Top-K） —— 常驻，可锁
 *   locked（用户锁定消息）     —— 常驻
 *   summary（前情提要）        —— 可再压缩
 *   recent（最近原文）         —— 最先被压缩
 */

export interface ContextLayers {
  system: string;
  summary: string;
  recent: Message[];
  locked: Message[];
}

export interface ContextUsage {
  used: number;
  max: number;
  ratio: number;
  /** 是否已越过压缩阈值，UI 用橙色进度条提示 */
  shouldCompress: boolean;
}

export class ContextManager {
  private readonly maxTokens: number;
  private readonly reserveForReply: number;
  private readonly compressAt: number;
  /** 最近窗口保留的原文条数下限 */
  private readonly keepRecent: number;

  constructor(maxTokens: number = 128000, reserveForReply: number = 2000, keepRecent: number = 20) {
    this.maxTokens = maxTokens;
    this.reserveForReply = reserveForReply;
    this.keepRecent = keepRecent;
    this.compressAt = 0.75;
  }

  available(): number {
    return this.maxTokens - this.reserveForReply;
  }

  usage(system: string, summary: string, recent: Message[]): ContextUsage {
    let used = PromptBuilder.tokens(system) + PromptBuilder.tokens(summary);
    for (const m of recent) used += this.messageTokens(m);
    const ratio = used / this.available();
    return { used, max: this.maxTokens, ratio, shouldCompress: ratio >= this.compressAt };
  }

  /**
   * 生成实际发给模型的消息数组。
   * 返回 needCompress 与将要丢弃的消息，交由调用方异步做增量摘要后回写。
   */
  assemble(layers: ContextLayers): AssembleResult {
    const lockedTokens = layers.locked.reduce((n, m) => n + this.messageTokens(m), 0);
    let budget = this.available()
      - PromptBuilder.tokens(layers.system)
      - PromptBuilder.tokens(layers.summary)
      - lockedTokens;

    // 从最新往旧取，直到预算耗尽
    const kept: Message[] = [];
    const dropped: Message[] = [];
    for (let i = layers.recent.length - 1; i >= 0; i--) {
      const m = layers.recent[i];
      const cost = this.messageTokens(m);
      const mustKeep = kept.length < this.keepRecent;
      if (mustKeep || cost <= budget) {
        kept.unshift(m);
        budget -= cost;
      } else {
        // 剩余更旧的全部进入待压缩
        dropped.unshift(...layers.recent.slice(0, i + 1));
        break;
      }
    }

    return {
      messages: [...layers.locked, ...kept].sort((a, b) => a.createdAt - b.createdAt),
      dropped,
      needCompress: dropped.length > 0
    };
  }

  /** 供聊天顶栏显示：18,420 / 128,000 */
  format(usage: ContextUsage): string {
    return `${usage.used.toLocaleString()} / ${usage.max.toLocaleString()}`;
  }

  private messageTokens(m: Message): number {
    const v = m.variants[m.activeVariant];
    let n = v ? PromptBuilder.tokens(v.content) : 0;
    if (m.role !== 'user' && m.role !== 'assistant') n += 4;
    if (m.image) n += 320;   // 图片按低分辨率图块估算
    return n + 6;
  }
}

export interface AssembleResult {
  messages: Message[];
  dropped: Message[];
  needCompress: boolean;
}

/**
 * 压缩流水线：把 dropped 交给 summarize 模型做增量摘要，
 * 回写 session.summary。绝不重新生成整段摘要。
 */
export interface SummarizeFn {
  (prompt: string): Promise<string>;
}

export class Compressor {
  constructor(private readonly summarize: SummarizeFn) {}

  async run(previousSummary: string, dropped: Message[], session: ChatSession): Promise<string> {
    if (dropped.length === 0) return previousSummary;
    const transcript = dropped.map(m => {
      const v = m.variants[m.activeVariant];
      const who = m.role === 'user' ? '你' : session.characterId;
      return `${who}：${v ? v.content : ''}`;
    }).join('\n');
    const prompt = PromptBuilder.buildCompressPrompt(previousSummary, transcript);
    try {
      return await this.summarize(prompt);
    } catch (e) {
      // 压缩失败绝不阻塞对话：保留原摘要即可
      return previousSummary;
    }
  }
}
