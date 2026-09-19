import { Sp, Fs, Fw, Rd, TNUM, Tone, ToneColor, Emotions } from '../theme/Tokens';
import { EfChip, EfCard, SectionHead, BondRing, EfMeter } from '../components/Ef';
import { CharacterCard, Memory, RelationshipState, EmotionState, StoryEvent, GRADE_LABEL, MemoryGrade } from '../core/Models';

/**
 * 角色主页（需求 §1 / §7 / §26 / §40）
 * 这是整个 App 的业务枢纽，也是「生命感」的门面：
 *   先看到她（立绘） → 再看到你们（相识/关系/情绪） → 再看到一起经历过的事（事件/记忆）
 * 而不是一张属性表。
 */

export interface CharacterHomeVM {
  card: CharacterCard;
  portrait: string;                 // 立绘
  bond: number;
  metDays: number;
  relationship: RelationshipState;
  emotion: EmotionState;
  recentEvent: StoryEvent | null;
  recentMemory: Memory | null;
  memoryCount: number;
  eventCount: number;
  voiceLine: string;                // 清冷少女音 · 语速 0.95x · 音调 +2
  chatBackground: string;
  latestPost: string;
}

@Component
export struct CharacterHome {
  @Prop vm: CharacterHomeVM;
  @State wishVoicePlaying: boolean = false;

  private name(): string { return this.vm.card.data.name; }
  private tags(): string[] { return this.vm.card.data.tags; }

  build() {
    Scroll() {
      Column() {
        this.Hero()
        this.Body()
      }
      .width('100%')
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
    .backgroundColor($r('app.color.ef_night_800'))
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
  }

  // ==================== 立绘 Hero ====================
  @Builder
  Hero() {
    Stack({ alignContent: Alignment.Top }) {
      Image(this.vm.portrait)
        .width('100%').height(340)
        .objectFit(ImageFit.Cover)
        .objectRepeat(ImageRepeat.NoRepeat)
        .alt($r('app.media.ef_placeholder'))

      // 底部三段式遮罩，让立绘无缝过渡到页面底色
      Column()
        .width('100%').height(340)
        .linearGradient({
          angle: 180,
          colors: [
            ['rgba(7,7,13,0.55)', 0.0],
            ['rgba(7,7,13,0.0)', 0.32],
            ['rgba(11,11,20,0.90)', 0.88],
            ['#0B0B14', 1.0]
          ]
        })

      Row() {
        this.NavBtn('‹', () => { /* router.back() */ })
        Blank()
        this.NavBtn('↗', () => { /* 分享角色卡 */ })
        this.NavBtn('⋯', () => { /* 更多 */ })
      }
      .width('100%').padding({ left: Sp.page, right: Sp.page, top: 12 })
    }
    .width('100%').height(340)
  }

  /** 压在立绘上的按钮必须自带深底 + 模糊，否则在明亮画面上不可读 */
  @Builder
  NavBtn(glyph: string, onTap: () => void) {
    Text(glyph)
      .fontSize(16).fontColor($r('app.color.ef_text_1'))
      .width(36).height(36).textAlign(TextAlign.Center)
      .backgroundColor('rgba(10,10,18,0.52)')
      .borderRadius(Rd.pill)
      .border({ width: 1, color: 'rgba(255,255,255,0.16)' })
      .backdropBlur(12)
      .margin({ left: 4, right: 4 })
      .onClick(onTap)
  }

  // ==================== 主体 ====================
  @Builder
  Body() {
    Column() {
      // ---- 身份行：头像 + 名字 + 版本 + 一句话身份 ----
      Row() {
        Image(this.vm.portrait)
          .width(76).height(76).borderRadius(24)
          .border({ width: 2, color: 'rgba(255,255,255,0.16)' })
          .shadow({ radius: 18, color: 'rgba(155,123,255,0.45)', offsetX: 0, offsetY: 4 })
        Column() {
          Row() {
            Text(this.name()).fontSize(Fs.h3).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
            Text(`v${this.vm.card.data.characterVersion}`)
              .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3'))
              .padding({ left: 7, right: 7, top: 2, bottom: 2 })
              .borderRadius(Rd.pill)
              .backgroundColor($r('app.color.ef_glass'))
              .margin({ left: 6 })
          }.alignItems(VerticalAlign.Center)
          Text(this.vm.card.data.scenario)
            .fontSize(Fs.foot).fontColor($r('app.color.ef_text_2')).margin({ top: 4 })
            .maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 12 })
        BondRing({ bond: this.vm.bond })
      }
      .width('100%').margin({ top: -46 })
      .alignItems(VerticalAlign.Bottom)

      // ---- 标签行 ----
      Flex({ wrap: FlexWrap.Wrap }) {
        ForEach(this.tags(), (t: string, i: number) => {
          EfChip({ label: `#${t}`, tone: i === 0 ? Tone.Primary : Tone.Neutral }).margin({ right: 6, bottom: 6 })
        })
        EfChip({ label: `作者：${this.vm.card.data.creator}`, tone: Tone.Neutral }).margin({ bottom: 6 })
      }.width('100%').margin({ top: 12 })

      // ---- 三项关键数值 ----
      Row() {
        this.Vital(`${this.vm.metDays}`, '天', '相识', Tone.Neutral)
        this.Vital(this.vm.relationship.level, '', '关系等级', Tone.Neutral)
        this.Vital(this.vm.emotion.current, '', '当前情绪', Tone.Emotion)
      }.width('100%').margin({ top: 14 })

      // ---- 快捷五宫格 ----
      Row() {
        this.Quick('💬', '聊天', true, () => { /* router.pushUrl({url:'pages/Chat'}) */ })
        this.Quick('🧠', '记忆', false, () => { /* → MemoryManager */ })
        this.Quick('📖', '事件', false, () => { /* → EventBook */ })
        this.Quick('✦', '朋友圈', false, () => { /* → Moments */ })
        this.Quick('⚙', '设定', false, () => { /* → RelationPanel */ })
      }.width('100%').margin({ top: 14 })

      // ---- 最近事件（§9） ----
      Column() {
        SectionHead({ title: '最近事件', action: `全部 ${this.vm.eventCount} 条` })
        if (this.vm.recentEvent) {
          EfCard({ raised: true }) {
            Row() {
              if (this.vm.recentEvent.images.length > 0) {
                Image(this.vm.recentEvent.images[0])
                  .width(72).height(72).borderRadius(14).objectFit(ImageFit.Cover)
              }
              Column() {
                Row() {
                  EfChip({ label: this.vm.recentEvent.date, tone: Tone.Bond })
                  if (this.vm.recentEvent.milestone) {
                    Text('里程碑').fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ left: 6 })
                  }
                }
                Text(this.vm.recentEvent.title)
                  .fontSize(Fs.body).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1')).margin({ top: 6 })
                Text(`第一次共用一把伞 · 羁绊 +${this.vm.recentEvent.bondDelta}`)
                  .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ top: 3 })
              }
              .alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 12 })
            }.width('100%')
          }
        }
      }.width('100%').margin({ top: Sp.section })

      // ---- 最近记忆（§4，含重要程度与来源追溯） ----
      Column() {
        SectionHead({ title: '最近记忆', action: `记忆库 ${this.vm.memoryCount}` })
        if (this.vm.recentMemory) {
          EfCard() {
            Text(`「${this.vm.recentMemory.content}」`)
              .fontSize(Fs.body).fontColor($r('app.color.ef_text_1')).lineHeight(22)
            Row() {
              Text(`重要程度 ${GRADE_LABEL[this.vm.recentMemory.grade]}`)
                .fontSize(Fs.cap).fontWeight(Fw.semi)
                .fontColor(ToneColor.fg(Tone.Bond))
              EfMeter({ ratio: this.vm.recentMemory.importance, from: $r('app.color.ef_bond'), to: $r('app.color.ef_bond_light') })
                .layoutWeight(1).margin({ left: 8 })
            }.width('100%').margin({ top: 9 })
            Text(`来源：${this.vm.recentMemory.source.excerpt}　·　为什么 AI 会记住这件事？`)
              .fontSize(Fs.cap).fontColor($r('app.color.ef_primary')).margin({ top: 8 })
          }
        }
      }.width('100%').margin({ top: Sp.section })

      // ---- 关系成长阶梯（§6） ----
      Column() {
        SectionHead({ title: '关系成长', action: '' })
        Row() {
          ForEach(Emotions.defaultLevels, (lv: string, i: number) => {
            Row()
              .layoutWeight(1).height(4).borderRadius(Rd.pill)
              .margin({ right: 4 })
              .linearGradient(i <= this.vm.relationship.levelIndex
                ? { angle: 90, colors: [[$r('app.color.ef_primary'), 0.0], [$r('app.color.ef_bond'), 1.0]] }
                : { angle: 90, colors: [[$r('app.color.ef_track'), 0.0], [$r('app.color.ef_track'), 1.0]] })
          })
        }.width('100%')
        Row() {
          ForEach(Emotions.defaultLevels, (lv: string, i: number) => {
            Text(lv)
              .fontSize(10).layoutWeight(1)
              .fontColor(i === this.vm.relationship.levelIndex
                ? $r('app.color.ef_bond') : $r('app.color.ef_text_3'))
          })
        }.width('100%').margin({ top: 6 })
        Text(`距「${this.NextLevel()}」还需 ${this.ToNext()} 点羁绊 · 聊天 / 共同事件 / 礼物 / 朋友圈互动均可成长`)
          .fontSize(Fs.foot).fontColor($r('app.color.ef_text_2')).margin({ top: 8 })
      }.width('100%').margin({ top: Sp.section })

      // ---- 角色声音（§17 / §30） ----
      Column() {
        SectionHead({ title: '角色声音', action: '' })
        EfCard() {
          Row() {
            Text(this.wishVoicePlaying ? '⏸' : '▶')
              .fontSize(10).fontColor($r('app.color.ef_voice'))
              .width(26).height(26).textAlign(TextAlign.Center)
              .backgroundColor($r('app.color.ef_voice_14'))
              .borderRadius(Rd.pill)
              .onClick(() => { this.wishVoicePlaying = !this.wishVoicePlaying; })
            Text(this.vm.voiceLine)
              .fontSize(Fs.foot).fontColor($r('app.color.ef_text_2'))
              .layoutWeight(1).margin({ left: 10 })
              .maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
            Text('试听').fontSize(Fs.foot).fontColor($r('app.color.ef_text_1'))
              .padding({ left: 14, right: 14, top: 6, bottom: 6 })
              .backgroundColor($r('app.color.ef_glass'))
              .borderRadius(Rd.pill)
          }.width('100%')
        }
      }.width('100%').margin({ top: Sp.section })

      // ---- 角色动态 ----
      Column() {
        SectionHead({ title: '角色动态', action: '更多' })
        EfCard() {
          Text(this.vm.latestPost)
            .fontSize(Fs.body).fontColor($r('app.color.ef_text_2')).lineHeight(22).maxLines(2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text('2 小时前 · ♡12 💬3')
            .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ top: 6 })
        }
      }.width('100%').margin({ top: Sp.section, bottom: 32 })
    }
    .width('100%')
    .padding({ left: Sp.page, right: Sp.page })
    .alignItems(HorizontalAlign.Start)
  }

  private NextLevel(): string {
    const lv = Emotions.defaultLevels;
    const i = this.vm.relationship.levelIndex;
    return i + 1 < lv.length ? lv[i + 1] : '已满';
  }

  private ToNext(): number {
    const th = Emotions.defaultThresholds;
    const i = this.vm.relationship.levelIndex;
    return i + 1 < th.length ? Math.max(0, th[i + 1] - this.vm.relationship.bond) : 0;
  }

  @Builder
  Vital(value: string, unit: string, label: string, tone: Tone) {
    Column() {
      Row() {
        Text(value)
          .fontSize(Fs.h3).fontWeight(Fw.bold).fontFeature(TNUM)
          .fontColor(tone === Tone.Neutral ? $r('app.color.ef_text_1') : ToneColor.fg(tone))
        if (unit.length > 0) {
          Text(unit).fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ left: 2 })
        }
      }.alignItems(VerticalAlign.Bottom)
      Text(label).fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ top: 2 })
    }
    .layoutWeight(1)
    .padding(10)
    .backgroundColor($r('app.color.ef_glass'))
    .borderRadius(Rd.md)
    .border({ width: 1, color: $r('app.color.ef_hairline') })
    .margin({ right: 4 })
  }

  @Builder
  Quick(icon: string, label: string, hot: boolean, onTap: () => void) {
    Column() {
      Text(icon).fontSize(17).fontColor(hot ? $r('app.color.ef_primary') : $r('app.color.ef_text_2'))
      Text(label).fontSize(Fs.cap).fontWeight(Fw.semi).margin({ top: 6 })
        .fontColor(hot ? $r('app.color.ef_primary') : $r('app.color.ef_text_2'))
    }
    .layoutWeight(1)
    .padding({ top: 10, bottom: 10 })
    .backgroundColor(hot ? $r('app.color.ef_primary_16') : $r('app.color.ef_glass'))
    .borderRadius(Rd.md)
    .border({ width: 1, color: hot ? $r('app.color.ef_primary_42') : $r('app.color.ef_hairline') })
    .margin({ right: 5 })
    .onClick(onTap)
  }
}
