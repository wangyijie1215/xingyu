import { Sp, Fs, Fw, Rd, TNUM, Tone } from '../theme/Tokens';
import { EfChip } from '../components/Ef';
import { Fortune, FortuneRank } from '../core/FortuneService';

/**
 * 签文页（神社 · 每日运势）
 *
 * 视觉核心是一张**和纸签文**：
 *   等级字用三色渐变裁切（62px / 900 字重）——它是全页面唯一的大字，
 *   六项运势用分段条而不是连续进度条（神社的签文本来就是分格写的），
 *   幸运四件套用两列网格，宜忌用绿/红双色卡但对「凶」不作红色处理。
 *
 * 页面下半部分是**角色的解读**——签文本身只是数据，她怎么念这张签才是重点。
 */

const SHRINE_RED = '#E5544B';
const SHRINE_GOLD = '#E8C87A';
const WASHI = '#F4ECDC';
const WASHI_2 = '#E7DCC6';
const WASHI_INK = '#3A3128';
const WASHI_INK_2 = '#6B5E4C';

/** 等级 → 渐变三色。注意「凶」用灰蓝而非红色：陪伴类产品不该制造焦虑 */
const RANK_GRADIENT: Record<string, string[]> = {
  '大吉': ['#C99A22', '#E8C87A', '#A87A16'],
  '吉': ['#C4682B', '#E8A263'],
  '中吉': ['#6B4BC0', '#A98BFF'],
  '小吉': ['#2E8F78', '#7FE0C4'],
  '末吉': ['#57527A', '#9C97BD'],
  '凶': ['#4A5A7A', '#8FA6C9']
};

export interface FortuneVM {
  fortune: Fortune;
  portrait: string;
  characterName: string;
  relationshipLevel: string;
  emotion: string;
  /** 抽签后的连锁反应提示 */
  appliedChips: string[];
  appliedNote: string;
}

@Component
export struct FortunePage {
  @Prop vm: FortuneVM;

  build() {
    Scroll() {
      Column() {
        this.TopBar()
        Column() {
          this.Slip()
          this.Reading()
          this.Applied()
          this.Actions()
        }
        .width('100%').padding({ left: Sp.page, right: Sp.page, bottom: 32 })
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')
    }
    .width('100%').height('100%')
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
    .backgroundColor($r('app.color.ef_night_800'))
  }

  @Builder
  TopBar() {
    Row() {
      Text('‹').fontSize(22).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
        .onClick(() => { /* router.back() */ })
      Column() {
        Text('银月神社').fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).letterSpacing(2)
        Text('今日签文').fontSize(Fs.h2).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
      }.alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 4 })
      Text('↗').fontSize(16).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
    }
    .width('100%').padding({ left: 12, right: 12, top: 12, bottom: 10 })
  }

  // ==================== 和纸签文 ====================
  @Builder
  Slip() {
    Column() {
      // ---- 签头 ----
      Row() {
        Text(`第 ${FortunePage.pad(this.vm.fortune.slipNo)} 番`)
          .fontSize(Fs.cap).fontColor(WASHI_INK_2).letterSpacing(1).fontFeature(TNUM)
        Blank()
        Text(FortunePage.dateLabel(this.vm.fortune.date))
          .fontSize(Fs.cap).fontColor(WASHI_INK_2).fontFeature(TNUM)
      }
      .width('100%')
      .padding({ bottom: 10 })
      .border({ width: { bottom: 1 }, color: 'rgba(58,49,40,0.18)' })

      // ---- 等级 ----
      Column() {
        Text('御 籤').fontSize(Fs.cap).fontColor(WASHI_INK_2).letterSpacing(12)
        Text(this.vm.fortune.rank)
          .fontSize(62).fontWeight(FontWeight.Bolder).letterSpacing(3)
          .linearGradient({
            angle: 160,
            colors: FortunePage.gradientOf(this.vm.fortune.rank)
          })
        Text(this.vm.fortune.summary)
          .fontSize(Fs.foot).fontColor(WASHI_INK_2).margin({ top: 4 })
      }
      .width('100%').padding({ top: 8, bottom: 12 }).alignItems(HorizontalAlign.Center)

      // ---- 六项运势 ----
      Column() {
        ForEach(this.vm.fortune.dimensions, (dim) => {
          Row() {
            Text(dim.key).fontSize(Fs.foot).fontWeight(Fw.bold).fontColor(WASHI_INK)
              .width(62).textAlign(TextAlign.Start)
            // 分段条：5 格，已达成格用朱→金渐变
            Row() {
              ForEach([0, 1, 2, 3, 4], (i: number) => {
                Row().layoutWeight(1).height(6).borderRadius(3)
                  .backgroundColor(i < Math.round(dim.value / 20) ? SHRINE_GOLD : 'rgba(58,49,40,0.14)')
                  .margin({ right: 3 })
              })
            }.layoutWeight(1)
            Text(`${dim.value}`).fontSize(Fs.cap).fontColor(WASHI_INK_2)
              .width(44).textAlign(TextAlign.End).fontFeature(TNUM)
          }
          .width('100%').padding({ top: 9, bottom: 9 })
          .border({ width: { bottom: 1 }, color: 'rgba(58,49,40,0.16)', style: BorderStyle.Dashed })
        })
      }.width('100%')

      // ---- 幸运四件套 ----
      Row() {
        this.Meta('幸运物', this.vm.fortune.luck.item)
        this.Meta('幸运色', this.vm.fortune.luck.colorName, this.vm.fortune.luck.colorHex)
      }.width('100%').margin({ top: 14 })
      Row() {
        this.Meta('幸运方位', this.vm.fortune.luck.direction)
        this.Meta('幸运数字', `${this.vm.fortune.luck.number}`)
      }.width('100%').margin({ top: 8 })

      // ---- 宜 / 忌 ----
      Row() {
        this.YiJi('宜', this.vm.fortune.good, true)
        this.YiJi('忌', this.vm.fortune.bad, false)
      }.width('100%').margin({ top: 10 })

      Text('银 月 神 社 · 御 籤 · 第 ' + FortunePage.pad(this.vm.fortune.slipNo) + ' 番')
        .fontSize(10).fontColor(WASHI_INK_2).letterSpacing(10)
        .width('100%').textAlign(TextAlign.Center)
        .padding({ top: 10 }).margin({ top: 16 })
        .border({ width: { top: 1 }, color: 'rgba(58,49,40,0.14)' })
    }
    .width('100%')
    .padding({ left: 18, right: 18, top: 20, bottom: 18 })
    .alignItems(HorizontalAlign.Start)
    .linearGradient({ angle: 158, colors: [[WASHI, 0.0], [WASHI_2, 0.62], ['#DCCFB4', 1.0]] })
    .borderRadius({ topLeft: 6, topRight: 6, bottomLeft: 10, bottomRight: 10 })
    .shadow({ radius: 26, color: 'rgba(0,0,0,0.6)', offsetX: 0, offsetY: 22 })
  }

  @Builder
  Meta(k: string, v: string, dot: string = '') {
    Column() {
      Text(k).fontSize(10).fontColor(WASHI_INK_2).letterSpacing(1)
      Row() {
        if (dot.length > 0) {
          Row().width(10).height(10).borderRadius(3).backgroundColor(dot)
            .border({ width: 1, color: 'rgba(0,0,0,0.18)' }).margin({ right: 5 })
        }
        Text(v).fontSize(13).fontWeight(Fw.bold).fontColor(WASHI_INK)
      }.margin({ top: 2 })
    }
    .alignItems(HorizontalAlign.Start)
    .layoutWeight(1)
    .padding({ left: 10, right: 10, top: 8, bottom: 8 })
    .backgroundColor('rgba(58,49,40,0.06)')
    .borderRadius(10)
    .border({ width: 1, color: 'rgba(58,49,40,0.10)' })
    .margin({ right: 4 })
  }

  @Builder
  YiJi(label: string, items: string[], good: boolean) {
    Column() {
      Text(label).fontSize(10).letterSpacing(4)
        .fontColor(good ? '#1F6B58' : '#8E3227')
      ForEach(items, (t: string) => {
        Text(t).fontSize(Fs.foot).fontColor(good ? '#1F6B58' : '#8E3227').margin({ top: 3 })
      })
    }
    .alignItems(HorizontalAlign.Start)
    .layoutWeight(1)
    .padding({ left: 11, right: 11, top: 9, bottom: 9 })
    .backgroundColor(good ? 'rgba(46,143,120,0.10)' : 'rgba(200,74,60,0.09)')
    .borderRadius(10)
    .border({ width: 1, color: good ? 'rgba(46,143,120,0.26)' : 'rgba(200,74,60,0.24)' })
    .margin({ right: 4 })
  }

  // ==================== 角色解读 ====================
  @Builder
  Reading() {
    Column() {
      Row() {
        Image(this.vm.portrait).width(34).height(34).borderRadius(11).objectFit(ImageFit.Cover)
        Column() {
          Text(`${this.vm.characterName} 的解读`)
            .fontSize(Fs.body).fontWeight(Fw.bold).fontColor(SHRINE_GOLD)
          Text(`关系「${this.vm.relationshipLevel}」· 情绪 ${this.vm.emotion}`)
            .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ top: 2 })
        }.alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 10 })
        EfChip({ label: '羁绊 +1', tone: Tone.Bond })
      }.width('100%')

      Text(this.vm.fortune.reading)
        .fontSize(Fs.body).lineHeight(24).fontColor('#FFFFFF')
        .margin({ top: 11 }).width('100%')

      Text(`今日缘签 · 你与她 ${this.vm.fortune.bondScore} · ${this.vm.fortune.bondRank}`)
        .fontSize(Fs.foot).fontColor(SHRINE_GOLD).margin({ top: 11 })
    }
    .width('100%')
    .padding(14)
    .alignItems(HorizontalAlign.Start)
    .borderRadius(Rd.lg)
    .linearGradient({ angle: 150, colors: [['rgba(155,123,255,0.16)', 0.0], ['rgba(232,200,122,0.09)', 1.0]] })
    .border({ width: 1, color: 'rgba(232,200,122,0.28)' })
    .margin({ top: 14 })
  }

  // ==================== 连锁反应 ====================
  @Builder
  Applied() {
    Column() {
      Row() {
        ForEach(this.vm.appliedChips, (c: string, i: number) => {
          EfChip({
            label: c,
            tone: i === 0 ? Tone.Memory : (i === 1 ? Tone.Primary : Tone.Bond)
          }).margin({ right: 6, bottom: 6 })
        })
      }.width('100%')
      Text(this.vm.appliedNote)
        .fontSize(Fs.foot).lineHeight(20).fontColor($r('app.color.ef_text_2'))
        .margin({ top: 6 }).width('100%')
    }
    .width('100%').padding(14).alignItems(HorizontalAlign.Start)
    .backgroundColor($r('app.color.ef_glass'))
    .borderRadius(Rd.lg)
    .border({ width: 1, color: $r('app.color.ef_hairline') })
    .margin({ top: Sp.section })
  }

  @Builder
  Actions() {
    Row() {
      Text('挂上绘马')
        .fontSize(Fs.body).fontWeight(Fw.bold).fontColor('#FFFFFF')
        .layoutWeight(1).textAlign(TextAlign.Center).padding({ top: 12, bottom: 12 })
        .linearGradient({ angle: 135, colors: [[SHRINE_RED, 0.0], ['#B08D3C', 1.0]] })
        .borderRadius(Rd.pill)
        .margin({ right: 8 })
      Text('收下御守')
        .fontSize(Fs.body).fontWeight(Fw.bold).fontColor('#FFFFFF')
        .layoutWeight(1).textAlign(TextAlign.Center).padding({ top: 12, bottom: 12 })
        .backgroundColor($r('app.color.ef_glass'))
        .borderRadius(Rd.pill)
        .border({ width: 1, color: $r('app.color.ef_hairline') })
    }
    .width('100%').margin({ top: Sp.section })
  }

  // ==================== 工具 ====================

  private static pad(n: number): string {
    const s = `${n}`;
    const cn = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
    // 签号用汉字，和纸上的数字本来就是写的
    return s.split('').map(c => cn[Number(c)]).join(' ');
  }

  private static dateLabel(date: string): string {
    const [y, m, d] = date.split('-');
    return `${y}.${m}.${d}`;
  }

  private static gradientOf(rank: FortuneRank | string): Array<[string, number]> {
    const c = RANK_GRADIENT[rank] ?? RANK_GRADIENT['中吉'];
    const out: Array<[string, number]> = [];
    for (let i = 0; i < c.length; i++) {
      out.push([c[i], c.length === 1 ? 0 : i / (c.length - 1)]);
    }
    return out;
  }
}
