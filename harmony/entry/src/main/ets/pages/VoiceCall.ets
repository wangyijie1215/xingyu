import { Sp, Fs, Fw, Rd, TNUM, Tone, ToneColor, Mo } from '../theme/Tokens';
import { EfCard } from '../components/Ef';

/**
 * 实时语音通话（需求 §19）
 * 界面刻意做到最简：只有「她」「状态」「时长」「字幕」和三个控制。
 * 通话转录与文字聊天关联，通话结束后可生成总结并写入事件簿。
 */

export interface TranscriptLine {
  who: string;        // '爱丽丝' | '你'
  isCharacter: boolean;
  text: string;
}

export interface CallVM {
  characterName: string;
  portrait: string;
  state: 'connecting' | 'listening' | 'thinking' | 'speaking';
  seconds: number;
  muted: boolean;
  captionOn: boolean;
  transcript: TranscriptLine[];
}

@Component
export struct VoiceCall {
  @Prop vm: CallVM;
  @State wavePhase: number = 0;

  build() {
    Stack() {
      // 模糊立绘背景：blur 26 + brightness .5，营造「她在对面」的空间感
      Image(this.vm.portrait)
        .width('100%').height('100%')
        .objectFit(ImageFit.Cover)
        .blur(26)
        .opacity(0.5)
        .scale({ x: 1.15, y: 1.15 })
      Column()
        .width('100%').height('100%')
        .linearGradient({ angle: 180, colors: [['rgba(7,7,13,0.5)', 0.0], ['rgba(7,7,13,0.92)', 1.0]] })

      Column() {
        Text('实时语音 · 通话转录已开启')
          .fontSize(Fs.cap).fontColor($r('app.color.ef_text_2')).letterSpacing(1)

        // 头像 + 双层脉冲环：这是「她在呼吸」的核心视觉
        Stack() {
          Circle().width(152).height(152).fill(Color.Transparent)
            .stroke($r('app.color.ef_primary')).strokeWidth(1).opacity(0.6)
            .scale({ x: 1 + 0.55 * this.wavePhase, y: 1 + 0.55 * this.wavePhase })
            .animation({ duration: Mo.pulse, curve: Curve.EaseOut, iterations: -1 })
          Image(this.vm.portrait)
            .width(152).height(152).borderRadius(76)
            .border({ width: 2, color: 'rgba(255,255,255,0.18)' })
        }
        .width(152).height(152).margin({ top: 18 })

        Text(this.vm.characterName)
          .fontSize(Fs.h2).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
          .margin({ top: 20 })

        Text(this.StateText())
          .fontSize(Fs.foot).fontColor(ToneColor.fg(Tone.Memory))
          .padding({ left: 12, right: 12, top: 4, bottom: 4 })
          .backgroundColor(ToneColor.bg(Tone.Memory))
          .borderRadius(Rd.pill)
          .margin({ top: 10 })

        // 波形：只有在她说话时才明显起伏
        Row() {
          ForEach([0, 1, 2, 3, 4, 5, 6, 7, 8], (i: number) => {
            Row()
              .width(4).height(this.vm.state === 'speaking' ? 34 : 9)
              .borderRadius(3)
              .linearGradient({ angle: 180, colors: [[$r('app.color.ef_primary_light'), 0.0], [$r('app.color.ef_voice'), 1.0]] })
              .margin({ right: 3 })
              .animation({
                duration: 1100,
                curve: Curve.EaseInOut,
                iterations: -1,
                delay: i * 100,
                playMode: PlayMode.Alternate
              })
          })
        }
        .height(44).margin({ top: 34 }).alignItems(VerticalAlign.Center)

        Text(this.Clock())
          .fontSize(Fs.h1).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
          .fontFeature(TNUM).letterSpacing(1).margin({ top: 14 })

        // 实时字幕
        if (this.vm.captionOn && this.vm.transcript.length > 0) {
          EfCard({ pad: 14 }) {
            Column() {
              Text('实时字幕').fontSize(Fs.foot).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
              ForEach(this.vm.transcript.slice(-2), (t: TranscriptLine) => {
                Row() {
                  Text(`${t.who}：`)
                    .fontSize(Fs.foot)
                    .fontColor(t.isCharacter ? $r('app.color.ef_primary') : $r('app.color.ef_voice'))
                  Text(t.text).fontSize(Fs.foot).fontColor($r('app.color.ef_text_2')).layoutWeight(1)
                }
                .alignItems(VerticalAlign.Top).margin({ top: 6 })
              })
            }.alignItems(HorizontalAlign.Start)
          }
          .margin({ left: Sp.page, right: Sp.page, top: 22 })
        }

        Blank()

        Row() {
          this.Ctrl(this.vm.muted ? '🔇' : '🎙', this.vm.muted, () => { /* toggle mute */ })
          this.Ctrl('💬', this.vm.captionOn, () => { /* toggle caption */ })
          this.Ctrl('✕', false, () => { /* 结束并生成通话总结 */ }, true)
        }
        .margin({ bottom: 12 })

        Text('静音 · 字幕 · 结束')
          .fontSize(Fs.foot).fontColor($r('app.color.ef_text_2')).margin({ bottom: 30 })
      }
      .width('100%').height('100%')
      .padding({ top: 40 })
    }
    .width('100%').height('100%')
    .backgroundColor($r('app.color.ef_night_800'))
    .onAppear(() => { this.wavePhase = 1; })
  }

  private StateText(): string {
    switch (this.vm.state) {
      case 'connecting': return '正在连接…';
      case 'thinking': return '正在想…';
      case 'speaking': return '她正在说…';
      default: return '正在聆听……';
    }
  }

  private Clock(): string {
    const m = Math.floor(this.vm.seconds / 60);
    const s = this.vm.seconds % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  @Builder
  Ctrl(glyph: string, active: boolean, onTap: () => void, danger: boolean = false) {
    Text(glyph).fontSize(20)
      .fontColor(danger ? $r('app.color.ef_emotion') : (active ? $r('app.color.ef_primary') : $r('app.color.ef_text_1')))
      .width(56).height(56).textAlign(TextAlign.Center)
      .backgroundColor(danger ? $r('app.color.ef_emotion_14')
        : (active ? $r('app.color.ef_primary_16') : $r('app.color.ef_glass')))
      .borderRadius(Rd.pill)
      .border({ width: 1, color: $r('app.color.ef_hairline') })
      .margin({ left: 8, right: 8 })
      .onClick(onTap)
  }
}
