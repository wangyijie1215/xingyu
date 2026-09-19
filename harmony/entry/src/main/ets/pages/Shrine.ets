import { Sp, Fs, Fw, Rd, TNUM, Mo, Tone, ToneColor } from '../theme/Tokens';
import { EfChip, EfCard, SectionHead, EfMeter } from '../components/Ef';
import { Fortune, FortuneRank, OmamoriId, ShrineState, OMAMORI_TABLE, Ema } from '../core/FortuneService';
import { DiaryEntry } from '../core/Diary';

/**
 * 神社 · 每日运势（需求扩展）
 * 在深色「星轨」系统里开出一块暖色飞地：朱（鸟居）· 金（御守）· 和纸。
 * 页面结构：立绘背景 → 今日状态卡（朱印 / 连续参拜 / 今日签运）→ 陪你来参拜的角色
 *          → 御守栏 → 绘马预览 → 最近七日 → 她的日记
 */

const SHRINE_RED = '#E5544B';
const SHRINE_RED_DEEP = '#A8322C';
const SHRINE_GOLD = '#E8C87A';
const SHRINE_GOLD_DEEP = '#B08D3C';
const SHRINE_NIGHT = '#0E0A12';
const WASHI = '#F4ECDC';
const WASHI_2 = '#E7DCC6';
const WASHI_INK = '#3A3128';
const WASHI_INK_2 = '#6B5E4C';

export interface ShrineVM {
  characterName: string;
  portrait: string;              // 陪你来参拜的立绘（按情绪切换差分）
  shrineBg: string;
  shrineWaterBg: string;
  /** 今天是否已参拜 */
  visited: boolean;
  state: ShrineState;
  fortune: Fortune | null;
  /** 她今天在神社说的话（按签运变化） */
  attendantLine: string;
  emas: Ema[];
  omamoriLocked: OmamoriId[];
  /** 最近七日签运，用于柱状图 */
  weekHistory: Array<{ date: string; rank: FortuneRank }>;
  diary: DiaryEntry | null;
  /** 用于日记落款的当前状态文案 */
  emotionLabel: string;
  relationLabel: string;
}

@Component
export struct Shrine {
  @Prop vm: ShrineVM;
  @State petalPhase: number = 0;
  private petalTimer: number = -1;

  private readonly rankColor: Record<string, string[]> = {
    '大吉': ['#C99A22', '#E8C87A', '#A87A16'],
    '吉': ['#C4682B', '#E8A263'],
    '中吉': ['#6B4BC0', '#A98BFF'],
    '小吉': ['#2E8F78', '#7FE0C4'],
    '末吉': ['#57527A', '#9C97BD'],
    '凶': ['#4A5A7A', '#8FA6C9']
  };

  build() {
    Scroll() {
      Stack({ alignContent: Alignment.Top }) {
        // ---- 背景立绘：鸟居夜景 ----
        Column() {
          Image(this.vm.shrineBg)
            .width('100%').height(420)
            .objectFit(ImageFit.Cover)
          Column()
            .width('100%').height(420)
            .linearGradient({
              angle: 180,
              colors: [
                ['rgba(7,7,13,0.62)', 0.0],
                ['rgba(7,7,13,0.08)', 0.34],
                ['rgba(14,10,18,0.86)', 0.78],
                [SHRINE_NIGHT, 1.0]
              ]
            })
        }
        .width('100%').height(420).position({ x: 0, y: 0 })

        this.PetalLayer()

        Column() {
          this.Nav()
          Column() { this.TodayCard(); this.Attendant(); this.OmamoriRail(); this.EmaSection(); this.WeekSection(); this.DiarySection() }
            .width('100%').padding({ left: Sp.page, right: Sp.page, bottom: 110 })
            .alignItems(HorizontalAlign.Start)
        }
        .width('100%')
      }
      .width('100%')
      .backgroundColor(SHRINE_NIGHT)
    }
    .width('100%').height('100%')
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
    .backgroundColor(SHRINE_NIGHT)
    .onAppear(() => {
      this.petalTimer = setInterval(() => {
        this.petalPhase = (this.petalPhase + 1) % 1000;
      }, 60);
    })
    .onDisAppear(() => {
      if (this.petalTimer !== -1) { clearInterval(this.petalTimer); this.petalTimer = -1; }
    })
  }

  // ==================== 樱花飘落 ====================
  @Builder
  PetalLayer() {
    Stack() {
      ForEach([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11], (i: number) => {
        Text('❀')
          .fontSize(9 + (i % 4) * 3)
          .fontColor('#FFC4D8')
          .opacity(0.75)
          .position({
            x: `${(i * 37 + 11) % 96}%`,
            y: ((this.petalPhase * (4 + i % 3) + i * 40) % 460)
          })
          .rotate({ angle: (this.petalPhase * 3 + i * 30) % 360 })
      })
    }
    .width('100%').height(420)
    .hitTestBehavior(HitTestMode.None)
  }

  @Builder
  Nav() {
    Row() {
      this.NavBtn('‹', () => { /* router.back() */ })
      Blank()
      this.NavBtn('🎴', () => { /* 御守箱 */ })
      this.NavBtn('🪧', () => { /* 绘马架 */ })
    }
    .width('100%').padding({ left: 12, right: 12, top: 12 })
  }

  @Builder
  NavBtn(glyph: string, onTap: () => void) {
    Text(glyph).fontSize(16).fontColor('#FFFFFF')
      .width(36).height(36).textAlign(TextAlign.Center)
      .backgroundColor('rgba(10,10,18,0.52)')
      .borderRadius(Rd.pill)
      .border({ width: 1, color: 'rgba(255,255,255,0.16)' })
      .backdropBlur(12).margin({ left: 4, right: 4 })
      .onClick(onTap)
  }

  // ==================== 今日状态卡 ====================
  @Builder
  TodayCard() {
    Column() {
      Row() {
        Column() {
          Text('银月神社 · 第 ' + (this.vm.fortune ? this.vm.fortune.slipNo : '—') + ' 番')
            .fontSize(Fs.cap).fontColor(SHRINE_GOLD).letterSpacing(2)
          Text('今日运势').fontSize(Fs.h3).fontWeight(Fw.bold).fontColor('#FFFFFF').margin({ top: 4 })
          Text('2026年9月17日 周四 · 夜　|　小雨 18℃')
            .fontSize(Fs.foot).fontColor('rgba(255,255,255,0.66)').margin({ top: 6 })

          // 连续参拜
          Row() {
            ForEach([0, 1, 2, 3, 4, 5, 6], (i: number) => {
              Row().width(9).height(9).borderRadius(5)
                .backgroundColor(i < this.vm.state.streak
                  ? SHRINE_GOLD : 'rgba(255,255,255,0.12)')
                .margin({ right: 5 })
            })
            Text(`连续参拜 ${this.vm.state.streak} 天`)
              .fontSize(Fs.cap).fontWeight(Fw.bold).fontColor(SHRINE_GOLD).margin({ left: 6 })
          }.margin({ top: 12 })
        }
        .alignItems(HorizontalAlign.Start).layoutWeight(1)

        this.Seal(this.vm.visited)
      }
      .width('100%').alignItems(VerticalAlign.Top)

      Divider().color('rgba(232,200,122,0.2)').margin({ top: 14, bottom: 14 })

      if (this.vm.fortune) {
        Row() {
          Column() {
            Text('今日签运').fontSize(Fs.foot).fontColor('rgba(255,255,255,0.6)')
            Text(this.vm.fortune.rank)
              .fontSize(26).fontWeight(FontWeight.Bolder)
              .linearGradient({
                angle: 120,
                colors: [[this.rankColor[this.vm.fortune.rank][0], 0.0], [this.rankColor[this.vm.fortune.rank][1], 1.0]]
              })
              .margin({ top: 2 })
          }.alignItems(HorizontalAlign.Start).layoutWeight(1)
          Column() {
            Text('综合').fontSize(Fs.foot).fontColor('rgba(255,255,255,0.6)')
            Text(`${this.vm.fortune.score}`)
              .fontSize(26).fontWeight(Fw.bold).fontColor(SHRINE_GOLD).fontFeature(TNUM).margin({ top: 2 })
          }.alignItems(HorizontalAlign.End)
        }.width('100%')

        Row() {
          this.GoldButton('查看今日签文', () => { /* → Fortune */ })
          this.GhostButton('挂上绘马', () => { /* → Ema */ })
        }.width('100%').margin({ top: 14 })
      } else {
        this.GoldButton('去摇一支签', () => { /* → Omikuji */ })
      }
    }
    .width('100%').padding(14)
    .backgroundColor('rgba(232,200,122,0.07)')
    .borderRadius(Rd.lg)
    .border({ width: 1, color: 'rgba(232,200,122,0.42)' })
    .backdropBlur(14)
  }

  /** 朱印：已参拜的凭证 */
  @Builder
  Seal(visited: boolean) {
    Column() {
      Text(visited ? '已' : '未').fontSize(13).fontWeight(Fw.bold).fontColor(visited ? SHRINE_RED : '#6F6A8B')
      Text(visited ? '参拜' : '参拜').fontSize(13).fontWeight(Fw.bold).fontColor(visited ? SHRINE_RED : '#6F6A8B')
    }
    .justifyContent(FlexAlign.Center)
    .width(54).height(54)
    .borderRadius(8)
    .border({ width: 2.5, color: visited ? SHRINE_RED : '#6F6A8B', style: visited ? BorderStyle.Solid : BorderStyle.Dashed })
    .backgroundColor(visited ? 'rgba(229,84,75,0.08)' : Color.Transparent)
    .rotate({ angle: visited ? -6 : 0 })
  }

  @Builder
  GoldButton(label: string, onTap: () => void) {
    Text(label).fontSize(Fs.foot).fontWeight(Fw.bold).fontColor('#FFFFFF')
      .padding({ left: 16, right: 16, top: 9, bottom: 9 })
      .linearGradient({ angle: 135, colors: [[SHRINE_RED, 0.0], [SHRINE_GOLD_DEEP, 1.0]] })
      .borderRadius(Rd.pill)
      .shadow({ radius: 24, color: 'rgba(229,84,75,0.35)', offsetX: 0, offsetY: 8 })
      .onClick(onTap)
      .margin({ right: 8 })
  }

  @Builder
  GhostButton(label: string, onTap: () => void) {
    Text(label).fontSize(Fs.foot).fontWeight(Fw.bold).fontColor('#FFFFFF')
      .padding({ left: 16, right: 16, top: 9, bottom: 9 })
      .backgroundColor('rgba(255,255,255,0.07)')
      .borderRadius(Rd.pill)
      .border({ width: 1, color: 'rgba(255,255,255,0.14)' })
      .onClick(onTap)
  }

  // ==================== 陪你来参拜 ====================
  @Builder
  Attendant() {
    Column() {
      SectionHead({ title: '陪你来参拜', action: '她的反应随签运变化' })
      Stack({ alignContent: Alignment.Bottom }) {
        Image(this.vm.portrait)
          .width('100%').height(236)
          .objectFit(ImageFit.Cover)
        Column()
          .width('100%').height(236)
          .linearGradient({ angle: 180, colors: [['rgba(14,10,18,0)', 0.0], ['rgba(14,10,18,0.0)', 0.5], ['rgba(14,10,18,0.75)', 1.0]] })
        Row() {
          Text(this.vm.characterName)
            .fontSize(Fs.body).fontWeight(Fw.bold).fontColor(SHRINE_GOLD)
          Text(this.vm.attendantLine)
            .fontSize(Fs.body).fontColor('#FFFFFF').layoutWeight(1).margin({ left: 8 })
        }
        .padding(11)
        .backgroundColor('rgba(20,16,26,0.82)')
        .borderRadius({ topLeft: 16, topRight: 16, bottomLeft: 6, bottomRight: 16 })
        .border({ width: 1, color: 'rgba(255,255,255,0.08)' })
        .margin({ left: 16, right: 16, bottom: 8 })
        .backdropBlur(14)
      }
      .width('100%').height(236)
      .borderRadius(Rd.lg)
      .clip(true)
    }
    .width('100%').margin({ top: Sp.section })
  }

  // ==================== 御守 ====================
  @Builder
  OmamoriRail() {
    Column() {
      SectionHead({ title: '御守', action: `御守箱 ${this.vm.state.unlocked.length} / 8` })
      Scroll() {
        Row() {
          ForEach(OMAMORI_TABLE, (om) => {
            this.OmamoriCard(om.id, om.name, om.effect,
              this.vm.state.unlocked.indexOf(om.id) >= 0,
              this.vm.state.equipped === om.id,
              om.unlocked ? '初始持有' : om.unlockHint)
          })
        }
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .width('100%')
    }
    .width('100%').margin({ top: Sp.section })
  }

  @Builder
  OmamoriCard(id: OmamoriId, name: string, effect: string, unlocked: boolean, equipped: boolean, hint: string) {
    Column() {
      // 御守的「结」：金色圆扣 + 垂下的绳
      Column() {
        Row().width(22).height(22).borderRadius(11)
          .linearGradient({ angle: 140, colors: [[SHRINE_GOLD, 0.0], [SHRINE_GOLD_DEEP, 1.0]] })
          .shadow({ radius: 12, color: 'rgba(232,200,122,0.4)', offsetX: 0, offsetY: 0 })
        Row().width(2).height(10).backgroundColor(SHRINE_GOLD)
      }.margin({ bottom: 7 })

      Text(name).fontSize(Fs.foot).fontWeight(Fw.bold).fontColor(SHRINE_GOLD)
      Text(unlocked ? effect : hint)
        .fontSize(9).fontColor(unlocked ? 'rgba(255,255,255,0.45)' : '#6F6A8B')
        .margin({ top: 4 }).textAlign(TextAlign.Center).lineHeight(14)
    }
    .width(96).padding({ left: 8, right: 8, top: 10, bottom: 11 })
    .backgroundColor('rgba(42,33,64,0.72)')
    .borderRadius(14)
    .border({ width: 1, color: equipped ? SHRINE_GOLD : 'rgba(232,200,122,0.26)' })
    .opacity(unlocked ? 1.0 : 0.45)
    .margin({ right: 10 })
    .onClick(() => { /* 佩戴 / 卸下 */ })
  }

  // ==================== 绘马 ====================
  @Builder
  EmaSection() {
    Column() {
      SectionHead({ title: '绘马', action: '绘马架 ›' })
      Row() {
        ForEach(this.vm.emas.slice(0, 2), (e: Ema) => {
          this.EmaCard(e)
        })
      }
      .width('100%')
    }
    .width('100%').margin({ top: Sp.section })
  }

  /** 绘马：五角形木牌 + 木纹 + 顶部挂绳孔 */
  @Builder
  EmaCard(e: Ema) {
    Column() {
      Text(e.author === 'user' ? '你的绘马' : `${this.vm.characterName}的绘马`)
        .fontSize(10).fontWeight(Fw.bold).fontColor('#8A6A2E')
      Text(e.text)
        .fontSize(Fs.foot).fontColor('#453A28').lineHeight(19)
        .margin({ top: 6 }).maxLines(3).textOverflow({ overflow: TextOverflow.Ellipsis })
      Text(e.author === 'user' ? '已被她看见 · 她记下了' : '她写的')
        .fontSize(10).fontColor('#7A6540').margin({ top: 7 })
    }
    .alignItems(HorizontalAlign.Start)
    .width('48%').padding({ left: 11, right: 11, top: 14, bottom: 14 })
    .linearGradient({ angle: 168, colors: [['#E7D3A8', 0.0], ['#D6BE8C', 0.62], ['#C4A876', 1.0]] })
    .borderRadius({ topLeft: 10, topRight: 10, bottomLeft: 14, bottomRight: 14 })
    .border(e.author === 'user'
      ? { width: 2, color: SHRINE_RED } : { width: 0, color: 'transparent' })
    .shadow({ radius: 20, color: 'rgba(0,0,0,0.45)', offsetX: 0, offsetY: 8 })
    .margin({ right: '4%' })
  }

  // ==================== 最近七日 ====================
  @Builder
  WeekSection() {
    Column() {
      SectionHead({ title: '最近七日', action: '' })
      Column() {
        Row() {
          ForEach(this.vm.weekHistory, (h) => {
            Column() {
              Column()
                .width('100%')
                .height(Shrine.heightOf(h.rank))
                .borderRadius(5)
                .linearGradient({ angle: 180, colors: [[this.rankColor[h.rank][0], 0.0], [this.rankColor[h.rank][1], 1.0]] })
              Text(h.date.substring(5).replace('-', '/'))
                .fontSize(9).fontColor('#6F6A8B').margin({ top: 5 })
            }
            .layoutWeight(1)
            .justifyContent(FlexAlign.End)
          })
        }
        .width('100%').height(76).alignItems(VerticalAlign.Bottom)

        Text(`七日里 ${Shrine.rankTally(this.vm.weekHistory)}。她说：「有起有落才正常。」`)
          .fontSize(Fs.foot).fontColor('rgba(255,255,255,0.6)').margin({ top: 12 })
      }
      .width('100%').padding(14)
      .backgroundColor('rgba(255,255,255,0.05)')
      .borderRadius(Rd.lg)
      .border({ width: 1, color: 'rgba(232,200,122,0.18)' })
    }
    .width('100%').margin({ top: Sp.section })
  }

  private static heightOf(rank: FortuneRank): number {
    const h: Record<string, number> = { '大吉': 64, '吉': 56, '中吉': 44, '小吉': 34, '末吉': 26, '凶': 22 };
    return h[rank] ?? 30;
  }

  private static rankTally(list: Array<{ rank: FortuneRank }>): string {
    const m: Record<string, number> = {};
    for (const x of list) m[x.rank] = (m[x.rank] ?? 0) + 1;
    return Object.keys(m).map(k => `${k} ${m[k]} 次`).join(' · ');
  }

  // ==================== 她的日记 ====================
  @Builder
  DiarySection() {
    Column() {
      SectionHead({ title: '她的日记', action: '全部 ›' })
      if (this.vm.diary) {
        Column() {
          Row() {
            Image(this.vm.portrait).width(34).height(34).borderRadius(11).objectFit(ImageFit.Cover)
            Column() {
              Text('她的日记').fontSize(Fs.body).fontWeight(Fw.bold).fontColor('#FFFFFF')
              Text(`${this.vm.diary.date} · ${this.vm.emotionLabel}`)
                .fontSize(Fs.cap).fontColor('#6F6A8B').margin({ top: 2 })
            }.alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 10 })
            EfChip({ label: '她主动写的', tone: Tone.Bond })
          }.width('100%')

          Text(this.vm.diary.line)
            .fontSize(Fs.body).fontColor('#FFFFFF').lineHeight(27).margin({ top: 12 })
          Text(this.vm.diary.body)
            .fontSize(Fs.body).fontColor('rgba(255,255,255,0.72)').lineHeight(27).margin({ top: 8 })

          // ★ 她没说出口的那句话：日记的题眼，单独强调
          Text(`她没说出口的：「${this.vm.diary.unsaid}」`)
            .fontSize(Fs.foot).fontColor(SHRINE_GOLD).lineHeight(21)
            .padding(11).width('100%')
            .backgroundColor('rgba(232,200,122,0.08)')
            .borderRadius(Rd.sm)
            .border({ width: 1, color: 'rgba(232,200,122,0.24)' })
            .margin({ top: 12 })

          Divider().color('rgba(255,255,255,0.08)').margin({ top: 12, bottom: 9 })
          Text(`—— ${this.vm.characterName} · 关系「${this.vm.relationLabel}」`)
            .fontSize(Fs.cap).fontColor('#6F6A8B').width('100%').textAlign(TextAlign.End)
        }
        .width('100%').padding(16).alignItems(HorizontalAlign.Start)
        .borderRadius(Rd.lg)
        .backgroundColor('rgba(255,255,255,0.045)')
        .border({ width: 1, color: 'rgba(255,255,255,0.08)' })
      }
    }
    .width('100%').margin({ top: Sp.section })
  }
}
