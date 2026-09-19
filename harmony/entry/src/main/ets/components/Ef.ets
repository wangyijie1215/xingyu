import { Sp, Fs, Fw, Rd, Mo, TNUM, Tone, ToneColor } from '../theme/Tokens';

/** 语义 Chip：6 种色调，全部走 Token，禁止页面里写死颜色 */
@Component
export struct EfChip {
  @Prop label: string = '';
  @Prop tone: Tone = Tone.Neutral;
  @Prop mini: boolean = false;

  build() {
    Text(this.label)
      .fontSize(this.mini ? 10 : Fs.cap)
      .fontWeight(Fw.semi)
      .fontColor(ToneColor.fg(this.tone))
      .padding({ left: this.mini ? 7 : 10, right: this.mini ? 7 : 10, top: this.mini ? 2 : 4, bottom: this.mini ? 2 : 4 })
      .backgroundColor(ToneColor.bg(this.tone))
      .borderRadius(Rd.pill)
      .border({ width: 1, color: ToneColor.stroke(this.tone) })
  }
}

/** 玻璃卡面：--glass + hairline + 圆角。raised 用更亮的面 + 阴影 */
@Component
export struct EfCard {
  @BuilderParam content: () => void;
  @Prop raised: boolean = false;
  @Prop pad: number = 14;

  build() {
    Column() { this.content() }
      .width('100%')
      .alignItems(HorizontalAlign.Start)
      .padding(this.pad)
      .backgroundColor(this.raised ? $r('app.color.ef_night_600') : $r('app.color.ef_glass'))
      .borderRadius(Rd.lg)
      .border({ width: 1, color: $r('app.color.ef_hairline') })
      .shadow(this.raised
        ? { radius: 10, color: 'rgba(0,0,0,0.35)', offsetX: 0, offsetY: 2 }
        : { radius: 0, color: 'transparent', offsetX: 0, offsetY: 0 })
  }
}

/** 区块标题 + 右侧跳转 */
@Component
export struct SectionHead {
  @Prop title: string = '';
  @Prop action: string = '';
  onAction?: () => void;

  build() {
    Row() {
      Text(this.title).fontSize(Fs.title).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
      Blank()
      if (this.action.length > 0) {
        Text(this.action).fontSize(Fs.foot).fontColor($r('app.color.ef_primary'))
          .onClick(() => { if (this.onAction) this.onAction(); })
      }
    }
    .width('100%')
    .margin({ bottom: 10 })
    .alignItems(VerticalAlign.Bottom)
  }
}

/** 亲密度环（§7）：62px，紫→橙渐变描边，中心「72 / 100」 */
@Component
export struct BondRing {
  @Prop bond: number = 0;
  @Prop size: number = 62;

  build() {
    Stack() {
      Progress({ value: this.bond, total: 100, type: ProgressType.Ring })
        .width(this.size)
        .height(this.size)
        .style({ strokeWidth: 5 })
        .color({ angle: 135, colors: [[$r('app.color.ef_primary'), 0.0], [$r('app.color.ef_bond'), 1.0]] })
        .backgroundColor($r('app.color.ef_track'))
      Column() {
        Text(`${Math.round(this.bond)}`)
          .fontSize(17).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
          .fontFeature(TNUM)
        Text('/100').fontSize(9).fontColor($r('app.color.ef_text_3')).margin({ top: 2 })
      }
      .justifyContent(FlexAlign.Center)
    }
    .width(this.size).height(this.size)
  }
}

/** 通用进度条：语义色渐变 */
@Component
export struct EfMeter {
  @Prop ratio: number = 0;         // 0 ~ 1
  @Prop from: Resource = $r('app.color.ef_primary');
  @Prop to: Resource = $r('app.color.ef_primary_light');
  @Prop height: number = 5;

  build() {
    Stack({ alignContent: Alignment.Start }) {
      Row().width('100%').height(this.height)
        .backgroundColor($r('app.color.ef_track')).borderRadius(Rd.pill)
      Row().width(`${Math.max(0, Math.min(1, this.ratio)) * 100}%`).height(this.height)
        .linearGradient({ angle: 90, colors: [[this.from, 0.0], [this.to, 1.0]] })
        .borderRadius(Rd.pill)
        .animation({ duration: Mo.dur, curve: Mo.curve })
    }
    .width('100%').height(this.height)
  }
}

/** 列表行：图标 + 主文案 + 副文案 + 箭头 */
@Component
export struct EfListRow {
  @Prop icon: string = '';
  @Prop tone: Tone = Tone.Primary;
  @Prop title: string = '';
  @Prop desc: string = '';
  @Prop divider: boolean = true;
  onTap?: () => void;

  build() {
    Column() {
      Row() {
        Text(this.icon).fontSize(15)
          .width(34).height(34).textAlign(TextAlign.Center)
          .backgroundColor(ToneColor.bg(this.tone)).borderRadius(Rd.sm)
        Column() {
          Text(this.title).fontSize(Fs.body).fontWeight(Fw.semi).fontColor($r('app.color.ef_text_1'))
          if (this.desc.length > 0) {
            Text(this.desc).fontSize(Fs.cap).fontColor($r('app.color.ef_text_3')).margin({ top: 2 })
          }
        }.alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 12 })
        Text('›').fontSize(15).fontColor($r('app.color.ef_text_3'))
      }.width('100%').padding({ top: 13, bottom: 13 })
      if (this.divider) { Divider().color($r('app.color.ef_hairline')) }
    }
    .onClick(() => { if (this.onTap) this.onTap(); })
  }
}

/** 消息操作菜单用的网格按钮（§12） */
@Component
export struct SheetMenuItem {
  @Prop icon: string = '';
  @Prop label: string = '';
  @Prop danger: boolean = false;
  onTap?: () => void;

  build() {
    Column() {
      Text(this.icon).fontSize(18).fontColor(this.danger ? $r('app.color.ef_emotion') : $r('app.color.ef_text_1'))
      Text(this.label).fontSize(Fs.cap)
        .fontColor(this.danger ? $r('app.color.ef_emotion') : $r('app.color.ef_text_1'))
        .margin({ top: 6 })
    }
    .justifyContent(FlexAlign.Center)
    .padding({ top: 12, bottom: 12 })
    .borderRadius(Rd.md)
    .onClick(() => { if (this.onTap) this.onTap(); })
  }
}
