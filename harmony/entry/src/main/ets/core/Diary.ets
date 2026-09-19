import { CharacterCard, RelationshipState, EmotionState, Memory, StoryEvent, MemoryCard } from './Models';
import { ProviderRegistry } from '../provider/LlmProvider';
import { TimeService } from '../platform/TimeService';

/**
 * 角色日记（扩展功能）
 *
 * 为什么需要它：聊天是「你找她」，日记是「她在你没看的时候也在生活」。
 * 这是陪伴类产品里性价比最高的一件「生命感」装置——成本低，情感密度极高。
 *
 * 每篇日记固定三段：
 *   line      当天的一句开场（具体、有画面）
 *   body      发生了什么 + 她当时的反应
 *   unsaid    ★ 她没说出口的那句话（只有日记里才写）
 *   tomorrow  明天的打算（可被次日的主动消息引用）
 */

export interface DiaryEntry {
  id: string;
  characterId: string;
  /** yyyy-MM-dd */
  date: string;
  /** 生成时的情境快照，用于「按心情筛选」 */
  emotion: string;
  weather: string;
  bond: number;
  line: string;
  body: string;
  /** 她没说出口的话 —— 日记的题眼 */
  unsaid: string;
  tomorrow: string;
  /** 由谁写成：auto = 系统按当天记录生成；user = 用户代写后编辑 */
  author: 'auto' | 'user';
  /** 当天是否有里程碑事件 */
  milestone: boolean;
  createdAt: number;
}

export interface DiaryContext {
  card: CharacterCard;
  relationship: RelationshipState;
  emotion: EmotionState;
  /** 当天的事件（用于让日记有具体的事可写） */
  todayEvents: StoryEvent[];
  /** 最近被检索到的记忆 */
  memories: Memory[];
  /** 当天对话的压缩摘要 */
  dialogueSummary: string;
  weather: string;
  /** 连续参拜天数等生活细节，让日记更「有生活」 */
  shrineStreak: number;
}

export class DiaryService {
  constructor(
    private readonly registry: ProviderRegistry,
    private readonly time: TimeService,
    private readonly repo: DiaryRepoPort
  ) {}

  async todayOrGenerate(ctx: DiaryContext): Promise<{ entry: DiaryEntry; fresh: boolean }> {
    const date = this.dateKey();
    const cached = await this.repo.getDiary(ctx.card.data.name, date);
    if (cached) return { entry: cached, fresh: false };
    const entry = await this.generate(ctx);
    await this.repo.saveDiary(entry);
    return { entry, fresh: true };
  }

  async generate(ctx: DiaryContext): Promise<DiaryEntry> {
    const d = ctx.card.data;
    const evLines = ctx.todayEvents.length > 0
      ? ctx.todayEvents.map(e => `- ${e.title}：${e.body}`).join('\n')
      : '- （今天没有特别的事发生）';
    const memLines = ctx.memories.slice(0, 5).map(m => `- ${m.content}`).join('\n');

    const prompt = [
      `你现在是 ${d.name}。角色性格：${d.personality}`,
      `【场景设定】${d.scenario}`,
      `【你与对方的关系】${ctx.relationship.level}，亲密度 ${Math.round(ctx.relationship.bond)}/100`,
      `【你今天的情绪】${ctx.emotion.current}`,
      `【今天的时间与天气】${this.time.describe()}　${ctx.weather}`,
      ctx.shrineStreak > 0 ? `【最近的生活】你已经连续第 ${ctx.shrineStreak} 天陪对方去神社参拜了。` : '',
      `【今天发生的事】\n${evLines}`,
      ctx.dialogueSummary ? `【今天和对方聊到的】\n${ctx.dialogueSummary}` : '',
      memLines ? `【你记得的事】\n${memLines}` : '',
      ``,
      `【任务】写今天的日记。用第一人称，像真的在写给自己看。`,
      `严格输出以下四行（每行以字段名开头，不要多余的解释、不要 markdown）：`,
      `LINE: 今天的开场，一句具体有画面的话（不要写「今天天气很好」这种空话）`,
      `BODY: 发生了什么，以及你当时的反应。2~3 句。要有细节，不要总结。`,
      `UNSAID: 你有一句话没有对对方说出口。只有日记里才写。一句。`,
      `TOMORROW: 明天打算做的一件小事。一句。`,
      ``,
      `要求：`,
      `- 完全符合角色性格与说话习惯，不要变成通用的温柔 AI`,
      `- 允许语句不完整、允许省略号，人写日记不会文绉绉`,
      `- 不要提到「AI」「模型」「用户」「系统」`,
      `- 不要写「亲爱的日记」这类套路开头`
    ].filter(x => x.length > 0).join('\n');

    let raw = '';
    try {
      raw = await this.registry.chat().complete([
        { role: 'system', content: prompt },
        { role: 'user', content: '请写今天的日记。' }
      ]);
    } catch (e) {
      raw = '';
    }

    const parsed = DiaryService.parse(raw);
    return {
      id: `diary_${this.dateKey()}_${ctx.card.data.name}`,
      characterId: ctx.card.data.name,
      date: this.dateKey(),
      emotion: ctx.emotion.current,
      weather: ctx.weather,
      bond: Math.round(ctx.relationship.bond),
      line: parsed.line,
      body: parsed.body,
      unsaid: parsed.unsaid,
      tomorrow: parsed.tomorrow,
      author: 'auto',
      milestone: ctx.todayEvents.some(e => e.milestone),
      createdAt: Date.now()
    };
  }

  /** 容错解析：模型偶尔会加引号或漏字段，这里全部兜住 */
  static parse(raw: string): { line: string; body: string; unsaid: string; tomorrow: string } {
    const pick = (key: string): string => {
      const re = new RegExp(`${key}\\s*[:：]\\s*([\\s\\S]*?)(?=\\n\\s*(?:LINE|BODY|UNSAID|TOMORROW)\\s*[:：]|$)`, 'i');
      const m = raw.match(re);
      return m ? m[1].trim().replace(/^["「『]|["」』]$/g, '') : '';
    };
    const line = pick('LINE');
    const body = pick('BODY');
    const unsaid = pick('UNSAID');
    const tomorrow = pick('TOMORROW');

    // 全字段缺失（例如模型完全跑偏）时，退回一段符合角色气质的保底日记
    if (!line && !body) {
      return {
        line: '今天没什么特别的。',
        body: '……写完这句就后悔了，好像也不算没有。',
        unsaid: '明天要是也能见到就好了。',
        tomorrow: '把披风上的线补一补。'
      };
    }
    return { line, body, unsaid, tomorrow };
  }

  /**
   * 心情曲线：把情绪历史按天聚合成柱状图（角色日记页的「最近十四天的心情」）
   * 值 = 积极度 * 100，颜色由主导情绪决定
   */
  static moodCurve(history: EmotionState['history'], days: number): Array<{ date: string; v: number; emotion: string }> {
    const POS: Record<string, number> = {
      '开心': 1.0, '期待': 0.88, '害羞': 0.78, '惊讶': 0.62,
      '平静': 0.55, '紧张': 0.38, '疲惫': 0.3, '孤独': 0.26, '悲伤': 0.2, '生气': 0.22
    };
    const out: Array<{ date: string; v: number; emotion: string }> = [];
    const today = new Date();
    for (let i = days - 1; i >= 0; i--) {
      const day = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i);
      const start = day.getTime();
      const end = start + 86400000;
      const seg = history.filter(h => h.to >= start && h.from < end);
      const dominant = seg.length > 0 ? seg[seg.length - 1].emotion : '平静';
      out.push({
        date: `${day.getMonth() + 1}/${day.getDate()}`,
        v: Math.round((POS[dominant] ?? 0.5) * 100),
        emotion: dominant
      });
    }
    return out;
  }

  dateKey(d: Date = new Date()): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}

export interface DiaryRepoPort {
  getDiary(characterId: string, date: string): Promise<DiaryEntry | null>;
  saveDiary(e: DiaryEntry): Promise<void>;
}

/**
 * 纪念日 / 倒数（扩展功能）
 * 让「时间感知」从「知道今天是几号」升级为「她记得那些对你重要的日子」。
 */
export type AnniversaryKind = 'met' | 'birthday' | 'festival' | 'custom' | 'milestone';

export interface Anniversary {
  id: string;
  characterId: string;
  kind: AnniversaryKind;
  title: string;
  /** 目标日期 yyyy-MM-dd（每年重复的用 MM-dd） */
  date: string;
  repeatYearly: boolean;
  /** 提前多少天开始由角色提起 */
  remindDaysBefore: number;
  /** 她是否已经在对话中提过 */
  mentioned: boolean;
}

export interface AnniversaryView extends Anniversary {
  daysLeft: number;
  /** 倒计时文案：2 天 / 今天 / 已过 3 天 */
  label: string;
  /** 是否已进入她的提醒窗口 */
  inRemindWindow: boolean;
}

export class AnniversaryService {
  constructor(private readonly time: TimeService) {}

  upcoming(list: Anniversary[], withinDays: number = 60): AnniversaryView[] {
    const today = new Date();
    const base = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
    const views: AnniversaryView[] = [];

    for (const a of list) {
      const target = AnniversaryService.nextOccurrence(a, today);
      const daysLeft = Math.round((target.getTime() - base) / 86400000);
      if (daysLeft > withinDays) continue;
      views.push({
        ...a,
        daysLeft,
        label: daysLeft === 0 ? '今天' : daysLeft > 0 ? `${daysLeft} 天` : `已过 ${-daysLeft} 天`,
        inRemindWindow: daysLeft >= 0 && daysLeft <= a.remindDaysBefore
      });
    }
    return views.sort((x, y) => x.daysLeft - y.daysLeft);
  }

  private static nextOccurrence(a: Anniversary, today: Date): Date {
    const parts = a.date.split('-').map(Number);
    if (a.repeatYearly) {
      let d = new Date(today.getFullYear(), parts[parts.length - 2] - 1, parts[parts.length - 1]);
      const t0 = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      if (d.getTime() < t0.getTime()) d = new Date(today.getFullYear() + 1, parts[parts.length - 2] - 1, parts[parts.length - 1]);
      return d;
    }
    return new Date(parts[0], parts[1] - 1, parts[2]);
  }

  /** 注入主动性评分：身处提醒窗口的纪念日会给主动消息加成 */
  reminderBoost(views: AnniversaryView[]): number {
    if (views.some(v => v.daysLeft === 0)) return 0.25;
    if (views.some(v => v.inRemindWindow)) return 0.12;
    return 0;
  }

  /** 生成「今天该提的日子」提示，交给 PromptBuilder 的【今日】段 */
  todayHint(views: AnniversaryView[]): string {
    const today = views.filter(v => v.daysLeft === 0);
    if (today.length > 0) return today.map(v => `${v.title}（就是今天）`).join('、');
    const soon = views.filter(v => v.inRemindWindow).slice(0, 1);
    if (soon.length > 0) return `${soon[0].title} 还有 ${soon[0].daysLeft} 天`;
    return '';
  }
}

/** 回忆卡生成（§40）：把一天里最值得留下的东西固化成一张卡 */
export class MemoryCardFactory {
  static fromEvent(e: StoryEvent): MemoryCard {
    return {
      title: `《${e.title}》`,
      subtitle: e.date.replace(/-/g, '.'),
      story: e.body,
      dialogue: [],
      image: e.images.length > 0 ? e.images[0] : '',
      exported: false
    };
  }

  static fromDiary(d: DiaryEntry): MemoryCard {
    return {
      title: `《${d.date.replace(/-/g, '.')} · 她没说出口的那句话》`,
      subtitle: d.date.replace(/-/g, '.'),
      story: `${d.line}\n${d.body}\n\n她没说出口的：「${d.unsaid}」`,
      dialogue: [`（没说出口）${d.unsaid}`],
      image: '',
      exported: false
    };
  }
}
