import { CharacterCard, RelationshipState, EmotionState, Memory, StoryEvent } from './Models';
import { ProviderRegistry } from '../provider/LlmProvider';
import { TimeService } from '../platform/TimeService';

/**
 * 神社 · 每日运势（需求扩展）
 *
 * 设计要点：
 *  1) **每日一签不可刷**：结果由 (日期 + 角色 + 固定盐) 确定性推导，抽到后落库并回读。
 *     同一角色同一天永远是同一张签——这既是神社的仪式感，也是反作弊。
 *  2) **不是纯随机**：关系亲密度、当前情绪、御守会倾斜吉凶分布，但只倾斜一次（抽签那一刻）。
 *  3) **签文要被人解读**：由角色用她自己的口吻解读，这是「生命感」的落点，而不是一段静态文案。
 *  4) **结果要留下痕迹**：写入长期记忆、关系微增、连续参拜计数、可能解锁事件与御守。
 */

// ==================== 模型 ====================

export type FortuneRank = '大吉' | '吉' | '中吉' | '小吉' | '末吉' | '凶';

export interface FortuneDimension {
  key: string;          // 恋爱 / 学业 / 健康 / 财运 / 人际 / 灵感
  value: number;        // 0 ~ 100
  note: string;
}

export interface FortuneLuck {
  item: string;         // 幸运物
  colorName: string;    // 幸运色
  colorHex: string;
  direction: string;    // 幸运方位
  number: number;
}

export interface Fortune {
  id: string;
  characterId: string;
  /** yyyy-MM-dd —— 同一天只存在一张 */
  date: string;
  slipNo: number;       // 第 N 番
  rank: FortuneRank;
  score: number;        // 综合 0~100
  summary: string;      // 雨过天青 · 宜与人共行
  dimensions: FortuneDimension[];
  luck: FortuneLuck;
  good: string[];       // 宜
  bad: string[];        // 忌
  /** 缘签：与这位角色的缘分指数 */
  bondScore: number;
  bondRank: FortuneRank;
  /** 角色用自己口吻写的解读（LLM 生成） */
  reading: string;
  drawnAt: number;
}

export type OmamoriId = 'enmusubi' | 'shizune' | 'yumemi' | 'gakushi' | 'koe' | 'kage' | 'toki' | 'tabi';

export interface Omamori {
  id: OmamoriId;
  name: string;
  effect: string;
  unlocked: boolean;
  /** 解锁条件的人类可读描述 */
  unlockHint: string;
  /** 对被佩戴者生效的一次性/当日修正 */
  modifiers: {
    bondGainMul?: number;      // 羁绊增长倍率
    emotionStabilize?: number; // 负面情绪切换阈值提升
    proactiveBoost?: number;   // 主动消息触发分加成
    memoryExtra?: number;      // 额外记忆提取条数
    voiceChance?: number;      // 语音回复概率
    imageChance?: number;      // 发照片概率
    festivalAware?: boolean;   // 更主动提起节日
    branchBonus?: number;      // 剧情分支数量加成
  };
}

export interface Ema {
  id: string;
  characterId: string;
  text: string;
  author: 'user' | 'character';
  createdAt: number;
  /** 她的绘马何时被用户看见 */
  seenByUser: boolean;
  /** 用户的绘马是否已被她提起 */
  mentioned: boolean;
  mentionedAt?: number;
}

export interface ShrineState {
  characterId: string;
  /** 连续参拜天数 */
  streak: number;
  /** 上次参拜日期 yyyy-MM-dd */
  lastVisit: string;
  /** 累计参拜 */
  totalVisits: number;
  /** 已佩戴的御守 */
  equipped: OmamoriId | null;
  unlocked: OmamoriId[];
  /** 前三天的签运，用于「最近七日」图 */
  history: Array<{ date: string; rank: FortuneRank }>;
}

// ==================== 常量表 ====================

const RANKS: FortuneRank[] = ['大吉', '吉', '中吉', '小吉', '末吉', '凶'];

/** 基础累计概率（未受任何修正时） */
const BASE_CUMULATIVE: number[] = [0.05, 0.23, 0.50, 0.75, 0.93, 1.00];

/** 名次 → 分项基础区间 */
const RANK_RANGE: Record<string, number[]> = {
  '大吉': [84, 100], '吉': [70, 92], '中吉': [60, 86],
  '小吉': [50, 78], '末吉': [40, 68], '凶': [26, 58]
};

const DIMENSIONS: string[] = ['恋爱', '学业', '健康', '财运', '人际', '灵感'];

const DIM_NOTES: Record<string, string[]> = {
  '恋爱': ['良缘渐近', '宜坦率', '静待', '有人一直在看你', '不必急于确认'],
  '学业': ['稳步', '宜复习', '瓶颈将破', '有新的方法', '记得留余地'],
  '健康': ['稍倦', '宜早睡', '状态回升', '注意肩颈', '多喝热水'],
  '财运': ['平', '守成', '有小惊喜', '不宜冲动消费', '宜囤积'],
  '人际': ['有贵人', '宜主动', '旧识重逢', '小事化了', '多听少说'],
  '灵感': ['泉涌', '宜记录', '卡住了就出门', '换个角度', '半夜最清醒']
};

const LUCKY_ITEMS = ['银白的小物件', '一枚旧硬币', '带在身上的书', '温热的饮品', '一块手帕', '雨伞', '写了字的纸条', '小铃铛', '玻璃珠'];
const LUCKY_COLORS: Array<{ n: string; hex: string }> = [
  { n: '紫藤', hex: '#9B7BFF' }, { n: '灯火橙', hex: '#FFA657' }, { n: '蔷薇', hex: '#FF9BB0' },
  { n: '薄荷', hex: '#7FE0C4' }, { n: '天青', hex: '#79D3F5' }, { n: '新芽', hex: '#B9E07F' },
  { n: '鸟居朱', hex: '#E5544B' }, { n: '御守金', hex: '#E8C87A' }, { n: '银白', hex: '#D8D8E8' }
];
const DIRECTIONS = ['东', '东南', '南', '西南', '西', '西北', '北', '东北'];

const GOOD_POOL = [
  '与人同行', '说出一直没说出口的话', '把想法写下来', '早一点休息',
  '接受别人的好意', '重新走一遍熟悉的路', '做一件很久没做的小事'
];
const BAD_POOL = [
  '独自淋雨', '深夜做决定', '把话憋着', '勉强自己撑到最后',
  '翻旧账', '在很累的时候答应别人', '一个人走没走过的小路'
];

const SUMMARY_POOL: Record<string, string[]> = {
  '大吉': ['云开月明 · 凡事皆宜', '风和日暖 · 心想事成'],
  '吉': ['雨过天青 · 宜与人共行', '渐入佳境 · 顺其自然'],
  '中吉': ['不急不缓 · 稳中向好', '有得有失 · 好在有得'],
  '小吉': ['小有顺遂 · 守之则安', '细微之处 · 藏着好意'],
  '末吉': ['稍显阻滞 · 后必转晴', '先难后易 · 忍一时'],
  '凶': ['今日宜静 · 明日必转', '风大 · 但有人在旁边']
};

/** 御守表（需求：御守要真的改行为，不能只是装饰） */
export const OMAMORI_TABLE: Omamori[] = [
  { id: 'enmusubi', name: '缘结守', effect: '羁绊增长 +20%', unlocked: true, unlockHint: '初始持有',
    modifiers: { bondGainMul: 1.2 } },
  { id: 'shizune', name: '心宁守', effect: '她不容易转负面情绪', unlocked: true, unlockHint: '初始持有',
    modifiers: { emotionStabilize: 0.15 } },
  { id: 'yumemi', name: '梦见守', effect: '主动消息概率提升', unlocked: true, unlockHint: '初始持有',
    modifiers: { proactiveBoost: 0.12 } },
  { id: 'gakushi', name: '学思守', effect: '每次多记一条', unlocked: false, unlockHint: '连续参拜 3 天',
    modifiers: { memoryExtra: 1 } },
  { id: 'koe', name: '声守', effect: '更常发语音', unlocked: false, unlockHint: '连续参拜 7 天',
    modifiers: { voiceChance: 0.25 } },
  { id: 'kage', name: '影守', effect: '更常发照片', unlocked: false, unlockHint: '关系达「信赖」',
    modifiers: { imageChance: 0.2 } },
  { id: 'toki', name: '时守', effect: '更记得节日与纪念日', unlocked: false, unlockHint: '连续参拜 14 天',
    modifiers: { festivalAware: true } },
  { id: 'tabi', name: '旅守', effect: '剧情分支更丰富', unlocked: false, unlockHint: '完成一次剧情总结',
    modifiers: { branchBonus: 1 } }
];

// ==================== 服务 ====================

export class FortuneService {
  constructor(
    private readonly registry: ProviderRegistry,
    private readonly time: TimeService,
    private readonly repo: FortuneRepoPort
  ) {}

  // ---------- 每日一签（不可刷） ----------

  async todayOrDraw(rt: FortuneRuntime): Promise<{ fortune: Fortune; fresh: boolean }> {
    const date = this.dateKey();
    const cached = await this.repo.getFortune(rt.characterId, date);
    if (cached) return { fortune: cached, fresh: false };
    const fortune = await this.draw(rt, date);
    await this.repo.saveFortune(fortune);
    await this.applyConsequences(rt, fortune);
    return { fortune, fresh: true };
  }

  /** 真正的抽签：确定性 hash + 状态倾斜 */
  private async draw(rt: FortuneRuntime, date: string): Promise<Fortune> {
    const salt = `${date}|${rt.characterId}|silvermoon`;
    const v = FortuneService.hash01(salt);

    // —— 状态倾斜：只在这一刻生效一次 ——
    const bondBias = (rt.relationship.bond / 100) * 0.15;                 // 关系越近越吉
    const omamori = this.equippedModifiers(rt.shrine);
    const luckBias = bondBias + (omamori.bondGainMul ? 0.06 : 0);
    const emotionBias = (rt.emotion.current === '开心' || rt.emotion.current === '期待') ? 0.05
      : (rt.emotion.current === '悲伤' || rt.emotion.current === '疲惫') ? -0.04 : 0;

    const shifted = Math.min(0.999, Math.max(0, v + luckBias + emotionBias));
    const rank = FortuneService.pickRank(shifted);
    const range = RANK_RANGE[rank];
    const score = Math.round(range[0] + (range[1] - range[0]) * FortuneService.hash01(salt + '|score'));

    const dimensions: FortuneDimension[] = DIMENSIONS.map((k, i) => {
      const r = FortuneService.hash01(`${salt}|dim|${k}`);
      // 分项在名次区间内摆动，恋爱项受羁绊额外加成
      let val = range[0] + (range[1] - range[0]) * r;
      if (k === '恋爱') val += (rt.relationship.bond - 50) * 0.16;
      const notes = DIM_NOTES[k];
      return { key: k, value: Math.round(Math.min(99, Math.max(20, val))), note: notes[Math.floor(r * notes.length) % notes.length] };
    });

    const color = LUCKY_COLORS[Math.floor(FortuneService.hash01(salt + '|c') * LUCKY_COLORS.length)];
    const luck: FortuneLuck = {
      item: LUCKY_ITEMS[Math.floor(FortuneService.hash01(salt + '|i') * LUCKY_ITEMS.length)],
      colorName: color.n, colorHex: color.hex,
      direction: DIRECTIONS[Math.floor(FortuneService.hash01(salt + '|d') * DIRECTIONS.length)],
      number: 1 + Math.floor(FortuneService.hash01(salt + '|n') * 9)
    };

    const pool = SUMMARY_POOL[rank];
    const summary = pool[Math.floor(FortuneService.hash01(salt + '|s') * pool.length) % pool.length];

    const good = FortuneService.pickN(GOOD_POOL, salt + '|good', 2);
    const bad = FortuneService.pickN(BAD_POOL, salt + '|bad', 2);

    // 缘签：与这位角色的缘分指数（关系权重最高）
    const bondScore = Math.round(Math.min(99, rt.relationship.bond * 0.7 + score * 0.3));
    const bondRank = FortuneService.pickRank(bondScore / 100);

    const fortune: Fortune = {
      id: `frt_${date}_${rt.characterId}`,
      characterId: rt.characterId,
      date,
      slipNo: 1 + Math.floor(FortuneService.hash01(salt + '|no') * 108),
      rank, score, summary, dimensions, luck, good, bad,
      bondScore, bondRank,
      reading: '',
      drawnAt: Date.now()
    };

    fortune.reading = await this.composeReading(rt, fortune);
    return fortune;
  }

  /**
   * 角色解读（生命感落点）
   * 关键：解读必须**与她的性格、当前情绪、你们的关系、你们的记忆**一致，
   * 且凶签时她要安慰人，而不是念一段模板。
   */
  private async composeReading(rt: FortuneRuntime, f: Fortune): Promise<string> {
    const isBad = f.rank === '凶' || f.rank === '末吉';
    const d = rt.card.data;
    const memories = rt.memories.filter(m => !m.disabled).slice(0, 4).map(m => `- ${m.content}`).join('\n');

    const prompt = [
      `你现在是 ${d.name}。角色性格：${d.personality}`,
      `【你们的关系】${rt.relationship.level}，亲密度 ${Math.round(rt.relationship.bond)}/100`,
      `【你当前的情绪】${rt.emotion.current}`,
      memories ? `【你记得的事】\n${memories}` : '',
      `【当前时间】${this.time.describe()}`,
      `【背景】你陪对方在银月神社求了一支签。`,
      `签文：${f.rank}　综合 ${f.score}　"${f.summary}"`,
      `分项：${f.dimensions.map(x => `${x.key} ${x.value}`).join('　')}`,
      `宜：${f.good.join('、')}　忌：${f.bad.join('、')}`,
      `【任务】用你的口吻，为对方解读这张签。`,
      `要求：`,
      `- 两三句话，口语，带一点动作或神态的描写（用（）括起来）`,
      `- 必须提到签文里的一两个具体点（分项或宜忌），不要泛泛而谈`,
      isBad
        ? `- 这张签偏弱。不要安慰式地说「没关系的」——你是个不太会说漂亮话的骑士，\n  用你自己的方式表达「今天我在」，比如提出陪对方做点什么。`
        : `- 这张签不错。可以带一点克制的、她自己的高兴，但不要夸张`,
      `- 不要出戏，不要提「AI」「签文系统」「模型」`,
      `- 直接输出解读正文`
    ].filter(x => x.length > 0).join('\n');

    try {
      const raw = await this.registry.chat().complete([
        { role: 'system', content: prompt },
        { role: 'user', content: '请输出你的解读。' }
      ]);
      return raw.trim();
    } catch (e) {
      // 解读失败不能挡住签文本身：退回一句符合情境的保底话术
      return isBad
        ? '（她把签纸收进袖口）……今天风大。要走哪条路，我陪你。'
        : '（她把签纸折好递还给你）签不错。那就按签上说的走。';
    }
  }

  // ---------- 抽签后的连锁反应 ----------

  private async applyConsequences(rt: FortuneRuntime, f: Fortune): Promise<void> {
    const today = this.dateKey();

    // 1) 连续参拜计数
    const s = rt.shrine;
    const yesterday = FortuneService.shiftDate(today, -1);
    s.streak = (s.lastVisit === yesterday) ? s.streak + 1 : 1;
    s.lastVisit = today;
    s.totalVisits += 1;

    // 2) 御守解锁
    for (const om of OMAMORI_TABLE) {
      if (s.unlocked.indexOf(om.id) >= 0) continue;
      const ok =
        (om.id === 'gakushi' && s.streak >= 3) ||
        (om.id === 'koe' && s.streak >= 7) ||
        (om.id === 'toki' && s.streak >= 14) ||
        (om.id === 'kage' && rt.relationship.levelIndex >= 3) ||
        (om.id === 'tabi' && rt.completedStoryCount >= 1);
      if (ok) {
        s.unlocked.push(om.id);
        await this.repo.saveEvent({
          id: `evt_omamori_${om.id}`,
          characterId: rt.characterId,
          date: today,
          title: `获得御守「${om.name}」`,
          body: `${om.effect}。连参拜第 ${s.streak} 天，她在神社替你求来的。`,
          milestone: true, bondDelta: 2, images: [], sourceMessageIds: [], note: ''
        });
      }
    }
    await this.repo.saveShrine(s);

    // 3) 写入长期记忆 —— 她以后会记得这一天
    const mem: Memory = {
      id: `mem_frt_${f.id}`,
      characterId: rt.characterId,
      tier: 'long',
      content: `${f.date} 在银月神社求了一支签：「${f.rank}」，签文是「${f.summary}」。`,
      importance: f.rank === '大吉' || f.rank === '凶' ? 0.78 : 0.55,
      grade: f.rank === '大吉' || f.rank === '凶' ? 'meaningful' : 'general',
      keywords: ['神社', '签', f.rank],
      source: { chatId: '', messageId: '', ts: Date.now(), excerpt: `银月神社 · 第${f.slipNo}番 · ${f.rank}` },
      locked: false, disabled: false, hitCount: 0,
      createdAt: Date.now(), lastUsedAt: Date.now()
    };
    await this.repo.saveMemory(mem);

    // 4) 关系微增（参拜本身也是一次互动）
    const gain = Math.round(1 * (this.equippedModifiers(rt.shrine).bondGainMul ?? 1));
    rt.relationship.bond = Math.min(100, rt.relationship.bond + gain);
    rt.relationship.lastDelta = gain;
    rt.relationship.sources['shrine'] = (rt.relationship.sources['shrine'] ?? 0) + gain;
    rt.relationship.updatedAt = Date.now();
    await this.repo.saveRelationship(rt.relationship);

    // 5) 连续参拜里程碑
    if (s.streak > 0 && s.streak % 7 === 0) {
      await this.repo.saveEvent({
        id: `evt_streak_${s.streak}`,
        characterId: rt.characterId,
        date: today,
        title: `连续参拜 ${s.streak} 天`,
        body: '她说：「……你居然真的每天都来。」（说完自己先转过身去了）',
        milestone: true, bondDelta: 5, images: [], sourceMessageIds: [], note: ''
      });
    }
  }

  // ---------- 绘马 ----------

  async writeEma(rt: FortuneRuntime, text: string): Promise<Ema> {
    const ema: Ema = {
      id: `ema_${Date.now()}`,
      characterId: rt.characterId,
      text,
      author: 'user',
      createdAt: Date.now(),
      seenByUser: true,
      mentioned: false
    };
    await this.repo.saveEma(ema);

    // 愿望进入她的长期记忆 —— 这是绘马真正的价值
    await this.repo.saveMemory({
      id: `mem_ema_${ema.id}`,
      characterId: rt.characterId,
      tier: 'relation',
      content: `你把愿望写在绘马上了：「${text}」。`,
      importance: 0.72, grade: 'important_fact',
      keywords: ['绘马', '愿望'],
      source: { chatId: '', messageId: '', ts: Date.now(), excerpt: '银月神社 · 绘马架' },
      locked: false, disabled: false, hitCount: 0,
      createdAt: Date.now(), lastUsedAt: Date.now()
    });
    return ema;
  }

  /** 她也会写绘马；用户在绘马架看到时算一次「她主动表达」 */
  async herEma(rt: FortuneRuntime): Promise<Ema> {
    const d = rt.card.data;
    const prompt = [
      `你现在是 ${d.name}。性格：${d.personality}`,
      `【你们的关系】${rt.relationship.level}，亲密度 ${Math.round(rt.relationship.bond)}/100`,
      `【你当前的情绪】${rt.emotion.current}`,
      rt.events.length > 0 ? `【最近发生的事】${rt.events[rt.events.length - 1].title}` : '',
      `【任务】你在神社的绘马架上写了一块绘马，替对方许了个愿。`,
      `要求：一句话，不超过 18 个字，符合你的性格（话少、不太会直说）。`,
      `直接输出绘马上的文字，不要引号，不要解释。`
    ].filter(x => x.length > 0).join('\n');
    let text = '';
    try {
      text = (await this.registry.chat().complete([
        { role: 'system', content: prompt }, { role: 'user', content: '请输出绘马上的文字。' }
      ])).trim().replace(/^["「『]|["」』]$/g, '');
    } catch (e) {
      text = '希望旅人明天不要淋雨。';
    }
    const ema: Ema = {
      id: `ema_her_${Date.now()}`, characterId: rt.characterId, text,
      author: 'character', createdAt: Date.now(), seenByUser: false, mentioned: false
    };
    await this.repo.saveEma(ema);
    return ema;
  }

  // ---------- 御守 ----------

  equippedModifiers(s: ShrineState): Omamori['modifiers'] {
    if (!s.equipped) return {};
    const found = OMAMORI_TABLE.find(o => o.id === s.equipped);
    return found ? found.modifiers : {};
  }

  async equip(rt: FortuneRuntime, id: OmamoriId | null): Promise<void> {
    rt.shrine.equipped = id;
    await this.repo.saveShrine(rt.shrine);
  }

  // ---------- 工具 ----------

  dateKey(d: Date = new Date()): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  static shiftDate(key: string, delta: number): string {
    const [y, m, d] = key.split('-').map(Number);
    const dt = new Date(y, m - 1, d + delta);
    return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`;
  }

  /** 字符串 → [0,1) 的确定性散列（FNV-1a 变体） */
  static hash01(s: string): number {
    let h = 2166136261;
    for (let i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return ((h >>> 0) % 1000000) / 1000000;
  }

  static pickRank(v: number): FortuneRank {
    for (let i = 0; i < BASE_CUMULATIVE.length; i++) {
      if (v < BASE_CUMULATIVE[i]) return RANKS[i];
    }
    return '凶';
  }

  private static pickN(pool: string[], salt: string, n: number): string[] {
    const out: string[] = [];
    let i = 0;
    while (out.length < n && i < 32) {
      const x = pool[Math.floor(FortuneService.hash01(`${salt}|${i}`) * pool.length) % pool.length];
      if (out.indexOf(x) < 0) out.push(x);
      i++;
    }
    return out;
  }
}

// ==================== 运行时 & 持久化端口 ====================

export interface FortuneRuntime {
  characterId: string;
  card: CharacterCard;
  relationship: RelationshipState;
  emotion: EmotionState;
  memories: Memory[];
  events: StoryEvent[];
  shrine: ShrineState;
  completedStoryCount: number;
}

export interface FortuneRepoPort {
  getFortune(characterId: string, date: string): Promise<Fortune | null>;
  saveFortune(f: Fortune): Promise<void>;
  saveShrine(s: ShrineState): Promise<void>;
  saveEma(e: Ema): Promise<void>;
  saveMemory(m: Memory): Promise<void>;
  saveEvent(e: StoryEvent): Promise<void>;
  saveRelationship(r: RelationshipState): Promise<void>;
}
