import { CharacterCard, RelationshipState, EmotionState, Memory, StoryEvent } from './Models';
import { ProviderRegistry } from '../provider/LlmProvider';
import { TimeService } from '../platform/TimeService';

/**
 * 信笺 Letters —— 对话之外的另一种说话方式
 *
 * 为什么需要它：聊天受「即时性」约束——句子短、有来有回、不能停顿太久。
 * 信解除了这个约束。信可以很长，可以说一半就停，可以在最后加一句 P.S.。
 * 对于「话少、不太会直说」的角色，信是她唯一能长篇表达的地方，
 * 因此**信里的她比聊天里的她更坦白**——这是这个功能全部的价值所在。
 */

export interface Letter {
  id: string;
  characterId: string;
  direction: 'to_her' | 'from_her';
  /** 称呼：致 旅人 / 致 爱丽丝 */
  to: string;
  greet: string;
  body: string;
  sign: string;
  dateLabel: string;
  /** 信末的 P.S. —— 常常是最动人的一句 */
  ps: string;
  createdAt: number;
  read: boolean;
  /** 生成这封信时引用的记忆 / 事件，用于「这封信是怎么写出来的」 */
  basis: { memoryIds: string[]; eventIds: string[]; weather: string; emotion: string; bond: number };
  /** 可保存为回忆卡 */
  savedAsCard: boolean;
}

export interface LetterContext {
  card: CharacterCard;
  relationship: RelationshipState;
  emotion: EmotionState;
  memories: Memory[];
  recentEvents: StoryEvent[];
  weather: string;
  /** 用户写的信的内容（to_her 时用于生成她的回信） */
  userLetter?: string;
}

export class LetterService {
  constructor(
    private readonly registry: ProviderRegistry,
    private readonly time: TimeService,
    private readonly repo: LetterRepoPort
  ) {}

  /** 用户寄出一封信 */
  async sendFromUser(ctx: LetterContext, body: string): Promise<Letter> {
    const letter: Letter = {
      id: `ltr_u_${Date.now()}`,
      characterId: ctx.card.data.name,
      direction: 'to_her',
      to: ctx.card.data.name,
      greet: '',
      body,
      sign: '你',
      dateLabel: this.time.dateStr(),
      ps: '',
      createdAt: Date.now(),
      read: true,
      basis: { memoryIds: [], eventIds: [], weather: ctx.weather, emotion: ctx.emotion.current, bond: ctx.relationship.bond },
      savedAsCard: false
    };
    await this.repo.saveLetter(letter);
    return letter;
  }

  /**
   * 她回信。
   * 与聊天回复的关键差异：
   *  - 长度：3~6 段（聊天通常 1~3 句）
   *  - 节奏：允许停顿、允许「写到这里就写不下去了」
   *  - 坦白度：她可以说出平时不说的东西（因为不必面对对方）
   *  - 必须有落款 + 一句 P.S.
   */
  async replyFromHer(ctx: LetterContext): Promise<Letter> {
    const d = ctx.card.data;
    const memRefs = ctx.memories.slice(0, 5);
    const evRefs = ctx.recentEvents.slice(0, 3);

    const prompt = [
      `你现在是 ${d.name}。角色性格：${d.personality}`,
      `【场景设定】${d.scenario}`,
      `【你们的关系】${ctx.relationship.level}，亲密度 ${Math.round(ctx.relationship.bond)}/100`,
      `【你当前的情绪】${ctx.emotion.current}`,
      `【时间与天气】${this.time.describe()}　${ctx.weather}`,
      memRefs.length > 0 ? `【你记得的事】\n${memRefs.map(m => `- ${m.content}`).join('\n')}` : '',
      evRefs.length > 0 ? `【你们经历过的事】\n${evRefs.map(e => `- ${e.date} ${e.title}：${e.body}`).join('\n')}` : '',
      ctx.userLetter ? `【你刚收到的信】\n${ctx.userLetter}` : '',
      ``,
      `【任务】写一封回信。`,
      `严格输出以下五行（每行以字段名开头，不要多余解释、不要 markdown）：`,
      `GREET: 开头的一句短问候（不要写「亲爱的」，不要写「见字如面」以外的套话）`,
      `BODY: 信的正文。3~5 段，段与段之间用 \\n\\n 分隔。`,
      `SIGN: 落款（只有名字）`,
      `PS: 一句附言，以「P.S.」开头`,
      ``,
      `要求：`,
      `- 用写字的语气，不是说话的语气：句子可以更长，可以停顿，可以用省略号`,
      `- 必须至少具体引用一件【你记得的事】或【你们经历过的事】，不要泛泛而谈`,
      `- 这是你比较少有的能好好说话的机会，因此可以说出平时不会当面说的话`,
      `- 允许写不下去：「……就写到这吧。」是好的结尾`,
      `- 段落要短，人写信不会写一堵墙`,
      `- 不要提到「AI」「模型」「用户」「系统」`
    ].filter(x => x.length > 0).join('\n');

    let raw = '';
    try {
      raw = await this.registry.chat().complete([
        { role: 'system', content: prompt },
        { role: 'user', content: '请写这封回信。' }
      ]);
    } catch (e) {
      raw = '';
    }

    const parsed = LetterService.parse(raw, d.name);
    const letter: Letter = {
      id: `ltr_h_${Date.now()}`,
      characterId: d.name,
      direction: 'from_her',
      to: '旅人',
      greet: parsed.greet,
      body: parsed.body,
      sign: parsed.sign,
      dateLabel: `${this.time.dateStr()} ${this.time.timeStr()}`,
      ps: parsed.ps,
      createdAt: Date.now(),
      read: false,
      basis: {
        memoryIds: memRefs.map(m => m.id),
        eventIds: evRefs.map(e => e.id),
        weather: ctx.weather,
        emotion: ctx.emotion.current,
        bond: ctx.relationship.bond
      },
      savedAsCard: false
    };
    await this.repo.saveLetter(letter);

    // 信是一次重要的互动：写入记忆 + 关系成长
    await this.repo.saveMemory({
      id: `mem_ltr_${letter.id}`,
      characterId: d.name,
      tier: 'relation',
      content: `她给你写了一封信（${letter.dateLabel}），信里提到：「${LetterService.excerpt(letter.body, 30)}」`,
      importance: 0.8,
      grade: 'meaningful',
      keywords: ['信', '来信'],
      source: { chatId: '', messageId: '', ts: Date.now(), excerpt: letter.greet || '信笺' },
      locked: false, disabled: false, hitCount: 0,
      createdAt: Date.now(), lastUsedAt: Date.now()
    });
    return letter;
  }

  static parse(raw: string, name: string): { greet: string; body: string; sign: string; ps: string } {
    const pick = (key: string): string => {
      const re = new RegExp(`${key}\\s*[:：]\\s*([\\s\\S]*?)(?=\\n\\s*(?:GREET|BODY|SIGN|PS)\\s*[:：]|$)`, 'i');
      const m = raw.match(re);
      return m ? m[1].trim() : '';
    };
    const greet = pick('GREET');
    const body = pick('BODY').replace(/\\n\\n/g, '\n\n');
    const sign = pick('SIGN') || name;
    let ps = pick('PS');
    if (ps && !/^p\.?s\.?/i.test(ps)) ps = `P.S. ${ps}`;

    if (!body) {
      return {
        greet: '见字如面。',
        body: '今天没发生什么特别的事。\n\n只是坐下来的时候，忽然想给你写点什么。\n\n……就写到这吧。',
        sign: name,
        ps: 'P.S. 明天见。'
      };
    }
    return { greet, body, sign, ps };
  }

  static excerpt(s: string, n: number): string {
    const clean = s.replace(/\n+/g, ' ');
    return clean.length > n ? clean.substring(0, n) + '…' : clean;
  }
}

export interface LetterRepoPort {
  saveLetter(l: Letter): Promise<void>;
  saveMemory(m: Memory): Promise<void>;
  listLetters(characterId: string): Promise<Letter[]>;
}

/**
 * 时间胶囊 Time Capsule —— 写给未来的话
 *
 * 它是「时间感知」的延长线：不是知道今天是几号，而是**知道未来某一天会发生什么**。
 * 到期时以「特别消息」送达：一条主动消息 + 一次记忆写入 + 一条事件。
 * 封存期间内容不参与任何检索——连她也不会引用它，否则就不叫胶囊了。
 */

export type CapsuleAuthor = 'user_to_her' | 'her_to_user' | 'user_to_self';

export interface TimeCapsule {
  id: string;
  characterId: string;
  author: CapsuleAuthor;
  /** 开启日 yyyy-MM-dd */
  openAt: string;
  title: string;
  /** 封存内容。非到期日绝不参与记忆检索与 Prompt 组装 */
  content: string;
  sealedAt: number;
  opened: boolean;
  openedAt?: number;
  /** 到期时的送达方式 */
  deliver: 'morning_first' | 'proactive' | 'event_only';
  /** 是否允许她主动提起「我也有一个胶囊要给你」 */
  teaser: boolean;
}

export interface CapsuleView extends TimeCapsule {
  daysLeft: number;
  label: string;
  /** 已到期但尚未送达 */
  due: boolean;
}

export class CapsuleService {
  constructor(
    private readonly registry: ProviderRegistry,
    private readonly time: TimeService,
    private readonly repo: CapsuleRepoPort
  ) {}

  create(author: CapsuleAuthor, characterId: string, title: string, content: string, openAt: string): TimeCapsule {
    return {
      id: `cap_${Date.now()}`,
      characterId,
      author,
      openAt,
      title,
      content,
      sealedAt: Date.now(),
      opened: false,
      deliver: author === 'her_to_user' ? 'proactive' : 'morning_first',
      teaser: author === 'her_to_user'
    };
  }

  views(list: TimeCapsule[]): CapsuleView[] {
    const today = new Date();
    const base = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
    return list.map(c => {
      const parts = c.openAt.split('-').map(Number);
      const target = new Date(parts[0], parts[1] - 1, parts[2]).getTime();
      const daysLeft = Math.round((target - base) / 86400000);
      return {
        ...c,
        daysLeft,
        label: daysLeft > 0 ? `${daysLeft} 天` : daysLeft === 0 ? '今天' : '已到期',
        due: !c.opened && daysLeft <= 0
      };
    }).sort((a, b) => a.daysLeft - b.daysLeft);
  }

  /** 由每日后台任务调用：把到期的胶囊变成一条特别消息 */
  async deliverDue(list: TimeCapsule[], rt: CapsuleRuntime): Promise<Letter[]> {
    const out: Letter[] = [];
    for (const c of this.views(list)) {
      if (!c.due) continue;

      const content = c.author === 'her_to_user' ? c.content : c.content;
      const letter: Letter = {
        id: `ltr_cap_${c.id}`,
        characterId: c.characterId,
        direction: c.author === 'her_to_user' ? 'from_her' : 'to_her',
        to: c.author === 'her_to_user' ? '旅人' : c.characterId,
        greet: c.author === 'her_to_user' ? '这封信，是几天前的我写的。' : '这是你留给那一天的话。',
        body: content,
        sign: c.author === 'her_to_user' ? c.characterId : '那天的你',
        dateLabel: c.openAt,
        ps: '',
        createdAt: Date.now(),
        read: false,
        basis: { memoryIds: [], eventIds: [], weather: '', emotion: rt.emotion.current, bond: rt.relationship.bond },
        savedAsCard: false
      };
      out.push(letter);
      await this.repo.saveLetter(letter);

      // 到期 = 一次记忆写入 + 一条事件
      await this.repo.saveMemory({
        id: `mem_cap_${c.id}`,
        characterId: c.characterId,
        tier: 'relation',
        content: c.author === 'her_to_user'
          ? `你打开了她在 ${c.openAt} 留给你的时间胶囊：「${LetterService.excerpt(content, 40)}」`
          : `你在 ${c.openAt} 写给未来的话被打开了：「${LetterService.excerpt(content, 40)}」`,
        importance: 0.85,
        grade: 'important_fact',
        keywords: ['时间胶囊', '未来'],
        source: { chatId: '', messageId: '', ts: Date.now(), excerpt: `时间胶囊 · 开启日 ${c.openAt}` },
        locked: false, disabled: false, hitCount: 0,
        createdAt: Date.now(), lastUsedAt: Date.now()
      });
      await this.repo.saveEvent({
        id: `evt_cap_${c.id}`,
        characterId: c.characterId,
        date: c.openAt,
        title: `时间胶囊到期：${c.title}`,
        body: LetterService.excerpt(content, 80),
        milestone: true, bondDelta: 3, images: [], sourceMessageIds: [], note: ''
      });
      await this.repo.markOpened(c.id);
    }
    return out;
  }

  /**
   * 她也会写胶囊。生成内容时她可以「想对未来的人说点什么」——
   * 这比日常回复更容易写出有分量的话，因为它自带时间维度。
   */
  async herCapsule(rt: CapsuleRuntime, openAt: string): Promise<TimeCapsule> {
    const d = rt.card.data;
    const prompt = [
      `你现在是 ${d.name}。性格：${d.personality}`,
      `【你们的关系】${rt.relationship.level}，亲密度 ${Math.round(rt.relationship.bond)}/100`,
      `【你当前的情绪】${rt.emotion.current}`,
      rt.events.length > 0 ? `【最近发生的事】${rt.events[rt.events.length - 1].title}` : '',
      `【任务】你要写一个时间胶囊，给对方在未来（${openAt}）打开。`,
      `写 2~4 句。可以是那时候想问对方的话、那时候希望对方知道的事、或者你现在不敢直接说的一句。`,
      `要符合你的性格：话少、不太会直说，但写在只会被未来打开的地方，可以稍微坦白一点。`,
      `直接输出内容，不要引号、不要标题、不要解释。`
    ].filter(x => x.length > 0).join('\n');

    let content = '';
    try {
      content = (await this.registry.chat().complete([
        { role: 'system', content: prompt }, { role: 'user', content: '请输出胶囊内容。' }
      ])).trim();
    } catch (e) {
      content = '到那天，你还在这条街上吗？\n如果不在也没关系，我只是想问问。';
    }

    const cap = this.create('her_to_user', d.name, '爱丽丝写给你的胶囊', content, openAt);
    await this.repo.saveCapsule(cap);
    return cap;
  }
}

export interface CapsuleRepoPort {
  saveCapsule(c: TimeCapsule): Promise<void>;
  markOpened(id: string): Promise<void>;
  saveLetter(l: Letter): Promise<void>;
  saveMemory(m: Memory): Promise<void>;
  saveEvent(e: StoryEvent): Promise<void>;
}

export interface CapsuleRuntime {
  card: CharacterCard;
  relationship: RelationshipState;
  emotion: EmotionState;
  events: StoryEvent[];
}
