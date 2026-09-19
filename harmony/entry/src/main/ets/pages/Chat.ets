import { Sp, Fs, Fw, Rd, Mo, TNUM, Tone, ToneColor } from '../theme/Tokens';
import { EfChip, EfMeter, SheetMenuItem } from '../components/Ef';
import { Message, MessageVariant } from '../core/Models';
import { ContextUsage } from '../core/ContextManager';

/**
 * 聊天（需求 §12 / §13 / §14 / §17 / §18 / §36 / §52）
 *
 * 三条硬约束：
 *   1) 流式渲染只更新**最后一条消息的文本节点**，不重建整列表（§52）
 *   2) 重新生成写入 variants[]，永不覆盖旧答案（§13）
 *   3) 羁绊变化只在真正变化时以一行轻提示出现，不在每条消息后播报（§7）
 */

export interface ChatHeaderVM {
  characterId: string;
  name: string;
  portrait: string;
  relationshipLevel: string;
  emotion: string;
  online: boolean;
}

export interface BranchOption {
  label: string;   // 【推开那扇门】
  branchId: string;
}

export interface ChatVM {
  header: ChatHeaderVM;
  messages: Message[];
  usage: ContextUsage;
  /** 仅当本轮数值真正变化时非空，例：羁绊 +2 · 关系「熟悉」· 情绪 期待 → 安心 */
  bondNote: string;
  /** 剧情分支，与自由输入并存，绝不替换输入框 */
  branches: BranchOption[];
  /** 正在流式输出的消息 id，空表示空闲 */
  streamingId: string;
  /** 自动总结是否已触发，用于把上下文条转为橙色 */
  compressing: boolean;
}

@Component
export struct Chat {
  @Prop vm: ChatVM;
  @State input: string = '';
  @State showMessageMenu: boolean = false;
  @State menuTargetIndex: number = -1;
  @State recording: boolean = false;
  /** 流式增量节流缓冲：16ms 合并一次，避免高频 setState 造成掉帧 */
  private pendingDelta: string = '';
  private flushTimer: number = -1;

  build() {
    Stack({ alignContent: Alignment.Bottom }) {
      Column() {
        this.TopBar()
        this.ContextBar()
        List({ space: 14 }) {
          ForEach(this.vm.messages, (m: Message, idx: number) => {
            ListItem() { this.MessageItem(m, idx) }
          }, (m: Message) => m.id)
        }
        .layoutWeight(1)
        .width('100%')
        .padding({ left: Sp.page, right: Sp.page, top: 4, bottom: 8 })
        .scrollBar(BarState.Off)
        .edgeEffect(EdgeEffect.Spring)
        .sticky(StickyStyle.None)
      }
      .width('100%').height('100%')

      this.Composer()

      if (this.showMessageMenu) { this.MessageMenuSheet() }
    }
    .width('100%').height('100%')
    .backgroundColor($r('app.color.ef_night_800'))
  }

  // ==================== 顶栏 ====================
  @Builder
  TopBar() {
    Row() {
      Text('‹').fontSize(22).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
        .onClick(() => { /* router.back() */ })
      Image(this.vm.header.portrait)
        .width(34).height(34).borderRadius(11).objectFit(ImageFit.Cover).margin({ left: 4 })
      Column() {
        Text(this.vm.header.name).fontSize(Fs.title).fontWeight(Fw.bold)
          .fontColor($r('app.color.ef_text_1'))
        Row() {
          if (this.vm.header.online) {
            Row().width(6).height(6).borderRadius(3)
              .backgroundColor($r('app.color.ef_memory')).margin({ right: 6 })
          }
          Text(`${this.vm.header.relationshipLevel} · ${this.vm.header.emotion} · ${this.vm.header.online ? '在线' : '离线'}`)
            .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3'))
            .maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        }.margin({ top: 2 })
      }
      .alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 8 })
      Text('📞').fontSize(16).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
        .onClick(() => { /* router.pushUrl({url:'pages/VoiceCall'}) */ })
      Text('⋯').fontSize(18).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
        .onClick(() => { this.showMessageMenu = true; })
    }
    .width('100%')
    .padding({ left: 12, right: 12, top: 12, bottom: 10 })
    .backgroundColor($r('app.color.ef_night_700'))
    .border({ width: { bottom: 1 }, color: $r('app.color.ef_hairline') })
  }

  // ==================== 上下文条（§36）：常驻可见，是「她不会突然忘记剧情」的承诺 ====================
  @Builder
  ContextBar() {
    Row() {
      Text('上下文').fontSize(Fs.cap).fontColor($r('app.color.ef_text_3'))
      EfMeter({
        ratio: this.vm.usage.ratio,
        from: this.vm.compressing ? $r('app.color.ef_bond') : $r('app.color.ef_memory'),
        to: this.vm.compressing ? $r('app.color.ef_emotion') : $r('app.color.ef_voice')
      })
      .layoutWeight(1).margin({ left: 8, right: 8 })
      Text(`${this.Format(this.vm.usage.used)} / ${this.Format(this.vm.usage.max)}`)
        .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).fontFeature(TNUM)
    }
    .width('100%')
    .padding({ left: Sp.page, right: Sp.page, bottom: 8 })
  }

  private Format(n: number): string {
    return n >= 1000 ? `${(n / 1000).toFixed(1)}K` : `${n}`;
  }

  // ==================== 消息 ====================
  @Builder
  MessageItem(m: Message, idx: number) {
    Column() {
      if (m.kind === 'text') { this.TextBubble(m) }
      else if (m.kind === 'voice') { this.VoiceBubble(m) }
      else if (m.kind === 'image') { this.ImageBubble(m) }

      // swipe 指示条：仅在多答案时出现，回复 2 / 5
      if (m.role === 'assistant' && m.variants.length > 1) {
        Row() {
          Text('‹').onClick(() => { /* 切到上一答案 */ })
          Text(`回复 ${m.activeVariant + 1} / ${m.variants.length}`)
            .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ left: 10, right: 10 })
            .fontFeature(TNUM)
          Text('›').onClick(() => { /* 切到下一答案 */ })
        }
        .width('100%').justifyContent(FlexAlign.Center).margin({ top: -4, bottom: 10 })
      }

      // 羁绊轻提示：整套系统里唯一允许暴露数值的位置
      if (idx === this.vm.messages.length - 1 && this.vm.bondNote.length > 0) {
        Text(this.vm.bondNote)
          .fontSize(Fs.cap).fontColor(ToneColor.fg(Tone.Bond))
          .width('100%').textAlign(TextAlign.Center).margin({ bottom: 12 })
      }

      // 剧情分支：与自由输入并存，只是快捷方式
      if (idx === this.vm.messages.length - 1 && this.vm.branches.length > 0) {
        Column() {
          ForEach(this.vm.branches, (b: BranchOption) => {
            Text(b.label)
              .fontSize(Fs.foot).fontWeight(Fw.semi)
              .fontColor(ToneColor.fg(Tone.Primary))
              .width('100%').padding(10)
              .backgroundColor($r('app.color.ef_primary_16'))
              .borderRadius(Rd.sm)
              .border({ width: 1, color: $r('app.color.ef_primary_42') })
              .margin({ bottom: 7 })
              .onClick(() => { /* 走分支 */ })
          })
        }
        .width('100%').padding({ left: 34, bottom: 12 })
      }
    }
    .width('100%')
  }

  @Builder
  TextBubble(m: Message) {
    Row() {
      if (m.role === 'assistant') {
        Image(this.vm.header.portrait).width(34).height(34).borderRadius(11).objectFit(ImageFit.Cover)
      } else { Blank().width(0) }
      Text(m.variants[m.activeVariant]?.content ?? '')
        .fontSize(Fs.body).lineHeight(23)
        .fontColor(m.role === 'user' ? '#FFFFFF' : $r('app.color.ef_text_1'))
        .padding({ left: 13, right: 13, top: 10, bottom: 10 })
        .constraintSize({ maxWidth: 262 })
        .backgroundColor(m.role === 'user'
          ? $r('app.color.ef_primary') : $r('app.color.ef_night_600'))
        .borderRadius(m.role === 'user'
          ? { topLeft: 18, topRight: 18, bottomLeft: 18, bottomRight: 6 }
          : { topLeft: 18, topRight: 18, bottomLeft: 6, bottomRight: 18 })
        .border(m.role === 'assistant' ? { width: 1, color: $r('app.color.ef_hairline') } : { width: 0, color: 'transparent' })
        .margin({ left: m.role === 'assistant' ? 8 : 0, right: m.role === 'user' ? 0 : 8 })
        .gesture(LongPressGesture().onAction(() => { this.showMessageMenu = true; }))
      if (m.role === 'user') { Blank() } else { Blank() }
    }
    .width('100%')
    .justifyContent(m.role === 'user' ? FlexAlign.End : FlexAlign.Start)
    .alignItems(VerticalAlign.Bottom)
  }

  /** 语音气泡（§17）：播放 + 波形 + 时长，天青色系 */
  @Builder
  VoiceBubble(m: Message) {
    Row() {
      Image(this.vm.header.portrait).width(34).height(34).borderRadius(11).objectFit(ImageFit.Cover)
      Row() {
        Text('▶').fontSize(10).fontColor($r('app.color.ef_voice'))
          .width(26).height(26).textAlign(TextAlign.Center)
          .backgroundColor($r('app.color.ef_voice_14')).borderRadius(Rd.pill)
        Row() {
          ForEach([8, 15, 21, 12, 18, 10, 16, 9, 14, 7], (h: number, i: number) => {
            Row().width(2.5).height(h).borderRadius(2)
              .backgroundColor($r('app.color.ef_voice'))
              .margin({ right: 2.5 })
          })
        }.margin({ left: 10, right: 10 }).height(22).alignItems(VerticalAlign.Center)
        Text(this.Dur(m.voice?.duration ?? 0))
          .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).fontFeature(TNUM)
      }
      .padding(8)
      .backgroundColor($r('app.color.ef_night_600'))
      .borderRadius({ topLeft: 18, topRight: 18, bottomLeft: 6, bottomRight: 18 })
      .border({ width: 1, color: $r('app.color.ef_hairline') })
      .margin({ left: 8 })
    }
    .width('100%').alignItems(VerticalAlign.Bottom)
  }

  /** 图片气泡（§20 / §21）：角色照片 + caption */
  @Builder
  ImageBubble(m: Message) {
    Row() {
      Image(this.vm.header.portrait).width(34).height(34).borderRadius(11).objectFit(ImageFit.Cover)
      Column() {
        Image(m.image?.url ?? '')
          .width(222).height(280).borderRadius(14).objectFit(ImageFit.Cover)
        Text(m.image?.caption ?? '')
          .fontSize(Fs.foot).fontColor($r('app.color.ef_text_2'))
          .padding({ left: 8, right: 8, top: 7, bottom: 5 }).lineHeight(19)
      }
      .padding(5)
      .backgroundColor($r('app.color.ef_night_600'))
      .borderRadius({ topLeft: 18, topRight: 18, bottomLeft: 6, bottomRight: 18 })
      .border({ width: 1, color: $r('app.color.ef_hairline') })
      .margin({ left: 8 })
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%').alignItems(VerticalAlign.Bottom)
  }

  private Dur(sec: number): string {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  // ==================== 输入区（§18）：语音与文字同一行，不切模式 ====================
  @Builder
  Composer() {
    Row() {
      Text('＋').fontSize(17).fontColor($r('app.color.ef_text_2'))
        .width(34).height(34).textAlign(TextAlign.Center)
        .backgroundColor($r('app.color.ef_glass')).borderRadius(Rd.pill)
        .onClick(() => { this.showMessageMenu = true; })

      TextInput({ placeholder: '说点什么…', text: this.input })
        .placeholderColor($r('app.color.ef_text_3'))
        .placeholderFont({ size: Fs.body })
        .fontSize(Fs.body)
        .fontColor($r('app.color.ef_text_1'))
        .backgroundColor(Color.Transparent)
        .layoutWeight(1)
        .height(34)
        .margin({ left: 8, right: 8 })
        .onChange((v: string) => { this.input = v; })

      Text('🎤').fontSize(15)
        .fontColor($r('app.color.ef_voice'))
        .width(34).height(34).textAlign(TextAlign.Center)
        .backgroundColor($r('app.color.ef_voice_14')).borderRadius(Rd.pill)
        .gesture(LongPressGesture()
          .onAction(() => { this.recording = true; /* 开始 STT */ })
          .onActionEnd(() => { this.recording = false; /* 结束 → 转文字 → 发送 */ }))

      Text(this.input.length > 0 ? '↑' : '⋯').fontSize(16)
        .fontColor('#FFFFFF')
        .width(34).height(34).textAlign(TextAlign.Center)
        .linearGradient({ angle: 135, colors: [[$r('app.color.ef_primary'), 0.0], [$r('app.color.ef_primary_deep'), 1.0]] })
        .borderRadius(Rd.pill).margin({ left: 8 })
        .onClick(() => { /* 发送 */ })
    }
    .width('100%')
    .padding({ left: 6, right: 6, top: 5, bottom: 5 })
    .backgroundColor($r('app.color.ef_night_500'))
    .borderRadius(Rd.pill)
    .border({ width: 1, color: $r('app.color.ef_hairline') })
    .margin({ left: 12, right: 12, bottom: 16 })
  }

  // ==================== 消息操作菜单（§12） ====================
  @Builder
  MessageMenuSheet() {
    Column() {
      Row().width(36).height(4).borderRadius(Rd.pill)
        .backgroundColor('rgba(255,255,255,0.22)').margin({ bottom: 12 })
      Text('消息操作').fontSize(Fs.foot).fontWeight(Fw.bold)
        .fontColor($r('app.color.ef_text_3')).margin({ bottom: 10 })

      Flex({ wrap: FlexWrap.Wrap }) {
        SheetMenuItem({ icon: '↻', label: '重新生成' })
        SheetMenuItem({ icon: '→', label: '继续生成' })
        SheetMenuItem({ icon: '✎', label: '编辑' })
        SheetMenuItem({ icon: '⧉', label: '复制' })
        SheetMenuItem({ icon: '★', label: '收藏' })
        SheetMenuItem({ icon: '🧠', label: '加入记忆' })
        SheetMenuItem({ icon: '📖', label: '创建事件' })
        SheetMenuItem({ icon: '⑂', label: '从这里分支' })
        SheetMenuItem({ icon: '▶', label: '转为语音' })
        SheetMenuItem({ icon: '⬇', label: '导出图片' })
        SheetMenuItem({ icon: '🗑', label: '删除', danger: true })
      }.width('100%')

      Text('关闭').fontSize(Fs.body).fontColor($r('app.color.ef_text_2'))
        .width('100%').textAlign(TextAlign.Center).padding({ top: 12, bottom: 4 })
        .onClick(() => { this.showMessageMenu = false; })
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 10, bottom: 26 })
    .backgroundColor($r('app.color.ef_night_700'))
    .borderRadius({ topLeft: Rd.xl, topRight: Rd.xl })
    .border({ width: { top: 1 }, color: $r('app.color.ef_hairline') })
  }

  // ==================== 流式渲染（§52） ====================

  /**
   * 由 LlmProvider 的 onDelta 调用。
   * 关键：16ms 合并一次增量，且只改**最后一条消息**的文本，
   * 因此长回复也不会因为整列表重建而卡顿。
   */
  onStreamDelta(delta: string): void {
    this.pendingDelta += delta;
    if (this.flushTimer !== -1) return;
    this.flushTimer = setTimeout(() => {
      this.flushTimer = -1;
      const last = this.vm.messages[this.vm.messages.length - 1];
      if (!last || last.role !== 'assistant') { this.pendingDelta = ''; return; }
      const v: MessageVariant = last.variants[last.activeVariant];
      v.content += this.pendingDelta;
      this.pendingDelta = '';
      // 只触发该 ListItem 的重绘（ForEach key = message.id，其他项不重建）
    }, 16);
  }

  aboutToDisappear(): void {
    if (this.flushTimer !== -1) { clearTimeout(this.flushTimer); this.flushTimer = -1; }
  }
}
