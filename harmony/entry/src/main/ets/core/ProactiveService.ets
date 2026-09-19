import { ProactiveMessage, ProactivePolicy, RelationshipState, EmotionState, StoryEvent } from './Models';
import { TimeService } from '../platform/TimeService';
import { WeatherService } from '../platform/WeatherService';
import { ProviderRegistry } from '../provider/LlmProvider';
import { PromptBuilder } from './PromptBuilder';

/**
 * 主动性系统（需求 §2 / §22 / §49 / §54）
 *
 * 触发因子加权求和 > 阈值才发消息。阈值由「主动程度」滑杆控制：
 *   0 = 克制（阈值高、几乎不主动）   1 = 黏人（阈值低）
 *
 * 限流链路（必须全部通过）：
 *   全局开关 → 单角色开关 → 每日上限 → 勿扰时段 → 仅 Wi-Fi → 触发阈值
 */
export class ProactiveService {
  private readonly policy: ProactivePolicy;
  private sentToday: Map<string, number> = new Map();

  constructor(
    policy: ProactivePolicy,
    private readonly registry: ProviderRegistry,
    private readonly time: TimeService,
    private readonly weather: WeatherService
  ) {
    this.policy = policy;
  }

  // ==================== 限流闸门 ====================

  canSend(characterId: string, now: Date = new Date()): ProactiveDecision {
    if (!this.policy.enabled) return { allowed: false, reason: '全局开关已关闭' };
    if (this.policy.perCharacterDisabled.indexOf(characterId) >= 0) {
      return { allowed: false, reason: '该角色已单独关闭主动消息' };
    }
    if ((this.sentToday.get(characterId) ?? 0) >= this.policy.dailyLimit) {
      return { allowed: false, reason: `已达每日上限 ${this.policy.dailyLimit} 条` };
    }
    if (ProactiveService.inDnd(now, this.policy.dndStart, this.policy.dndEnd)) {
      return { allowed: false, reason: `勿扰时段 ${this.policy.dndStart}–${this.policy.dndEnd}` };
    }
    if (this.policy.wifiOnly && !this.time.isWifi()) {
      return { allowed: false, reason: '仅 Wi-Fi 模式下已暂停' };
    }
    return { allowed: true, reason: '' };
  }

  private static inDnd(now: Date, start: string, end: string): boolean {
    const cur = now.getHours() * 60 + now.getMinutes();
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    const s = sh * 60 + sm;
    const e = eh * 60 + em;
    return s <= e ? (cur >= s && cur < e) : (cur >= s || cur < e);   // 跨零点
  }

  // ==================== 触发因子 ====================

  /** 返回 0~1 的触发分；越高越该主动说话 */
  score(args: ProactiveArgs): number {
    let s = 0;
    // 空闲时长：3 天没开 App 是最强信号（对应 §54 的完整示例）
    s += Math.min(0.35, args.idleHours / 72 * 0.35);
    // 天气变化与记忆共振：下雨 + 「用户喜欢雨」的记忆 = 高价值时机
    if (args.weatherChanged && args.memoryResonance) s += 0.25;
    else if (args.weatherChanged) s += 0.10;
    // 未解决的剧情伏笔
    if (args.openPlot) s += 0.15;
    // 特殊日期（生日 / 纪念日 / 节日）
    if (args.specialDate) s += 0.25;
    // 她的情绪本身在催她开口
    if (args.emotion === '期待' || args.emotion === '孤独' || args.emotion === '担心') s += 0.15;
    // 关系越近越愿意主动
    s += args.relationship.bond / 100 * 0.15;
    // 刚刚聊过则显著降低（避免骚扰）
    if (args.idleHours < 2) s -= 0.4;
    return Math.max(0, Math.min(1, s));
  }

  /** 阈值随「主动程度」滑杆变化：0 → 0.75，1 → 0.35 */
  threshold(): number {
    return 0.75 - 0.4 * this.policy.clinginess;
  }

  async consider(args: ProactiveArgs): Promise<ProactiveMessage | null> {
    const gate = this.canSend(args.characterId);
    if (!gate.allowed) return null;
    if (this.score(args) < this.threshold()) return null;

    const text = await this.compose(args);
    if (!text) return null;

    this.sentToday.set(args.characterId, (this.sentToday.get(args.characterId) ?? 0) + 1);
    return {
      id: `pm_${Date.now()}`,
      characterId: args.characterId,
      createdAt: Date.now(),
      trigger: args.triggerLabel,
      text,
      read: false,
      deliveredVia: 'inapp'
    };
  }

  /**
   * 用角色自己的口吻写这条消息。
   * 关键：**必须基于真实记忆/事件/天气**，否则就成了随机问候。
   */
  private async compose(args: ProactiveArgs): Promise<string> {
    const lines: string[] = [];
    lines.push(`你现在是 ${args.characterName}。角色性格：${args.personality}`);
    lines.push(`【当前时间】${this.time.describe()}　【天气】${this.weather.summary()}`);
    lines.push(`【你们的关系】${args.relationship.level}，亲密度 ${Math.round(args.relationship.bond)}/100`);
    lines.push(`【你当前的情绪】${args.emotion}`);
    if (args.memories.length > 0) {
      lines.push(`【你想起了这些】\n${args.memories.map(m => `- ${m}`).join('\n')}`);
    }
    if (args.recentEvent) lines.push(`【最近发生的事】${args.recentEvent.title}：${args.recentEvent.body}`);
    if (args.openPlot) lines.push(`【还没说完的事】${args.openPlot}`);
    lines.push(`【任务】你有一阵子没和对方说话了。请主动发一条消息。`);
    lines.push(`要求：`);
    lines.push(`- 一到三句话，像真人随手发的消息，不要写信`);
    lines.push(`- 自然地从上面的记忆或天气切入，不要生硬地问「你好吗」`);
    lines.push(`- 不要说「我是AI」或任何出戏的话`);
    lines.push(`- 直接输出消息正文，不要引号、不要前缀`);

    try {
      const raw = await this.registry.chat().complete(
        [{ role: 'system', content: lines.join('\n') }, { role: 'user', content: '请输出这条消息。' }]
      );
      return raw.trim().replace(/^["「『]|["」』]$/g, '');
    } catch (e) {
      console.warn('[Proactive] 生成失败：', e);
      return '';
    }
  }

  /** 每日重置，由 TimeService 在零点调用 */
  resetDaily(): void {
    this.sentToday.clear();
  }
}

export interface ProactiveArgs {
  characterId: string;
  characterName: string;
  personality: string;
  idleHours: number;
  weatherChanged: boolean;
  memoryResonance: boolean;
  openPlot: string;
  specialDate: string;
  emotion: string;
  relationship: RelationshipState;
  memories: string[];
  recentEvent: StoryEvent | null;
  triggerLabel: string;
}

export interface ProactiveDecision {
  allowed: boolean;
  reason: string;
}
