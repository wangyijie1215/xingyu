import { Sp, Fs, Fw, Rd, TNUM, Tone, ToneColor } from '../theme/Tokens';
import { EfCard, EfChip, EfMeter, SectionHead } from '../components/Ef';
import { Memory, GRADE_LABEL, MemoryGrade, MemoryTier } from '../core/Models';

/**
 * 记忆管理器（需求 §4 / §5）
 * 用户必须能：看得见、搜得到、改得动、知道「为什么 AI 会记住这件事」。
 * 重要程度可视化是防记忆库膨胀的关键——低价值记忆必须看起来就不重要。
 */

export type MemorySegment = 'about_you' | 'relation' | 'world';

export interface MemoryVM {
  characterName: string;
  segment: MemorySegment;
  counts: Record<MemorySegment, number>;
  items: Memory[];
  autoExtractOn: boolean;
  pendingCandidates: number;
}

@Component
export struct MemoryManager {
  @Prop vm: MemoryVM;

  build() {
    Column() {
      this.TopBar()
      this.SegBar()
      this.SearchBar()
      List({ space: 9 }) {
        ForEach(this.vm.items, (m: Memory) => {
          ListItem() { this.MemoryCard(m) }
        }, (m: Memory) => m.id)
      }
      .layoutWeight(1)
      .width('100%')
      .padding({ left: Sp.page, right: Sp.page, top: 12, bottom: 12 })
      .scrollBar(BarState.Off)
      .edgeEffect(EdgeEffect.Spring)

      Text(this.vm.autoExtractOn
        ? `自动提取已开启 · 本轮对话新增 ${this.vm.pendingCandidates} 条候选记忆`
        : '自动提取已关闭 · 记忆只能手动添加')
        .fontSize(Fs.foot).fontColor($r('app.color.ef_text_3'))
        .width('100%').padding({ left: Sp.page, right: Sp.page, bottom: 16 })
    }
    .width('100%').height('100%')
    .backgroundColor($r('app.color.ef_night_800'))
  }

  @Builder
  TopBar() {
    Row() {
      Text('‹').fontSize(22).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
      Column() {
        Text(this.vm.characterName).fontSize(Fs.cap).fontColor($r('app.color.ef_text_3'))
          .letterSpacing(1)
        Text('记忆库').fontSize(Fs.h2).fontWeight(Fw.bold).fontColor($r('app.color.ef_text_1'))
      }.alignItems(HorizontalAlign.Start).layoutWeight(1).margin({ left: 4 })
      Text('＋').fontSize(20).fontColor($r('app.color.ef_text_1'))
        .width(36).height(36).textAlign(TextAlign.Center)
    }
    .width('100%').padding({ left: 12, right: 12, top: 12, bottom: 10 })
  }

  @Builder
  SegBar() {
    Row() {
      this.SegItem('about_you', `关于你 ${this.vm.counts.about_you}`)
      this.SegItem('relation', `关系记忆 ${this.vm.counts.relation}`)
      this.SegItem('world', `世界观 ${this.vm.counts.world}`)
    }
    .width('100%').padding(4)
    .backgroundColor($r('app.color.ef_night_600'))
    .borderRadius(Rd.pill)
    .border({ width: 1, color: $r('app.color.ef_hairline') })
    .margin({ left: Sp.page, right: Sp.page })
  }

  @Builder
  SegItem(seg: MemorySegment, label: string) {
    Text(label)
      .fontSize(Fs.foot).fontWeight(Fw.bold)
      .fontColor(this.vm.segment === seg ? $r('app.color.ef_text_1') : $r('app.color.ef_text_2'))
      .textAlign(TextAlign.Center)
      .layoutWeight(1).padding({ top: 8, bottom: 8 })
      .backgroundColor(this.vm.segment === seg ? $r('app.color.ef_glass_2') : Color.Transparent)
      .borderRadius(Rd.pill)
      .onClick(() => { /* 切换分段并重新分页加载 */ })
  }

  @Builder
  SearchBar() {
    Row() {
      Text('🔍').fontSize(13)
      Text('搜索记忆、来源消息…').fontSize(Fs.body).fontColor($r('app.color.ef_text_3')).margin({ left: 8 })
    }
    .width('100%').height(40).padding({ left: 14, right: 14 })
    .backgroundColor($r('app.color.ef_night_600'))
    .borderRadius(Rd.pill)
    .border({ width: 1, color: $r('app.color.ef_hairline') })
    .margin({ left: Sp.page, right: Sp.page, top: 12 })
  }

  @Builder
  MemoryCard(m: Memory) {
    Column() {
      Text(m.content)
        .fontSize(Fs.body).lineHeight(22)
        .fontColor(m.disabled ? $r('app.color.ef_text_3') : $r('app.color.ef_text_1'))

      // 重要程度：条长 = 重要度，颜色 = 语义分组
      Row() {
        Text(`${this.GradeLabel(m)}${m.locked ? ' · 已锁定' : ''}`)
          .fontSize(Fs.cap).fontWeight(Fw.semi)
          .fontColor(this.ToneOf(m))
        EfMeter({
          ratio: m.importance,
          from: this.ToneOf(m),
          to: $r('app.color.ef_bond_light')
        }).layoutWeight(1).margin({ left: 8 })
      }
      .width('100%').margin({ top: 9 })

      // 来源追溯：为什么 AI 会记住这件事？
      Row() {
        Text(`来源：${m.source.excerpt}`)
          .fontSize(Fs.cap).fontColor($r('app.color.ef_text_3'))
          .layoutWeight(1).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        Text('为什么 AI 会记住这件事？')
          .fontSize(Fs.cap).fontColor($r('app.color.ef_primary'))
          .onClick(() => { /* 打开来源消息并高亮 */ })
        Text(m.locked ? '🔓' : '🔒').fontSize(11).margin({ left: 12 })
          .onClick(() => { /* 切换锁定 */ })
        Text('✎').fontSize(11).margin({ left: 10 })
          .onClick(() => { /* 编辑 */ })
      }
      .width('100%').margin({ top: 8 })
    }
    .width('100%')
    .padding(13)
    .backgroundColor($r('app.color.ef_glass'))
    .borderRadius(Rd.md)
    .border({
      width: 1,
      color: m.locked ? 'rgba(255,166,87,0.35)' : $r('app.color.ef_hairline')
    })
    .opacity(m.disabled ? 0.45 : 1.0)
  }

  private GradeLabel(m: Memory): string {
    return `重要程度 ${GRADE_LABEL[m.grade as MemoryGrade] ?? '一般'}`;
  }

  private ToneOf(m: Memory): Resource {
    if (m.grade === 'trivial') return $r('app.color.ef_text_3');
    if (m.locked) return $r('app.color.ef_memory');
    if (m.importance >= 0.8) return $r('app.color.ef_bond');
    if (m.importance >= 0.5) return $r('app.color.ef_primary');
    return $r('app.color.ef_text_3');
  }
}

/**
 * 记忆检索面板：输入关键词 → 命中记忆 + 来源消息片段（需求 §48 全局搜索在记忆维度的投影）
 */
export function filterMemories(all: Memory[], keyword: string, segment: MemorySegment): Memory[] {
  const k = keyword.trim().toLowerCase();
  return all.filter(m => {
    const seg = m.tier === 'relation' ? 'relation' : 'about_you';
    if (segment !== 'world' && seg !== segment) return false;
    if (k.length === 0) return true;
    return m.content.toLowerCase().includes(k)
      || m.keywords.some(x => x.toLowerCase().includes(k))
      || m.source.excerpt.toLowerCase().includes(k);
  }).sort((a, b) => b.importance - a.importance);
}
