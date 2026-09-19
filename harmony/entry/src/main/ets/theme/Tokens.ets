/**
 * EchoFlow 设计 token · 「星轨 Star-Rail」
 * 与 design/02-设计系统与UI规格.md 的 token 表一一对应。
 * 颜色走 resources/base/element/color.json（$r('app.color.ef_*')），
 * 本文件只承载尺寸 / 字阶 / 圆角 / 动效等无法资源化的常量。
 */

// ---------------- 间距（4 的倍数） ----------------
export class Sp {
  static readonly s1: number = 4;
  static readonly s2: number = 8;
  static readonly s3: number = 12;
  static readonly s4: number = 16;
  static readonly s5: number = 20;
  static readonly s6: number = 24;
  static readonly s8: number = 32;
  /** 页面左右安全边距 */
  static readonly page: number = 16;
  /** 区块之间的间距 */
  static readonly section: number = 22;
}

// ---------------- 字阶 ----------------
export class Fs {
  static readonly cap: number = 11;
  static readonly foot: number = 12;
  static readonly body: number = 14;
  static readonly title: number = 16;
  static readonly h3: number = 20;
  static readonly h2: number = 24;
  static readonly h1: number = 30;
}

export class Fw {
  static readonly regular: FontWeight = FontWeight.Regular;
  static readonly medium: FontWeight = FontWeight.Medium;
  static readonly semi: FontWeight = FontWeight.Medium;
  static readonly bold: FontWeight = FontWeight.Bold;
}

/** 数字统一等宽，避免亲密度/时长/token 计数跳动。用法：Text().fontFeature('"tnum" 1') */
export const TNUM: string = '"tnum" 1';

// ---------------- 圆角 ----------------
export class Rd {
  static readonly xs: number = 8;
  static readonly sm: number = 12;
  static readonly md: number = 16;
  static readonly lg: number = 20;
  static readonly xl: number = 28;
  static readonly tab: number = 24;
  static readonly pill: number = 999;
}

// ---------------- 动效 ----------------
export class Mo {
  /** 与 CSS cubic-bezier(.22,.61,.36,1) 等价的弹性曲线 */
  static readonly curve: Curve = Curve.FastOutSlowIn;
  static readonly dur: number = 260;
  static readonly durSlow: number = 420;
  static readonly pulse: number = 3000;
  static readonly breathe: number = 2200;
}

// ---------------- 语义色分组（供组件按语义取色，禁止硬编码） ----------------
export enum Tone { Primary, Bond, Emotion, Memory, Voice, Growth, Neutral }

export class ToneColor {
  /** 主色值 */
  static fg(tone: Tone): Resource {
    switch (tone) {
      case Tone.Primary: return $r('app.color.ef_primary');
      case Tone.Bond:    return $r('app.color.ef_bond');
      case Tone.Emotion: return $r('app.color.ef_emotion');
      case Tone.Memory:  return $r('app.color.ef_memory');
      case Tone.Voice:   return $r('app.color.ef_voice');
      case Tone.Growth:  return $r('app.color.ef_growth');
      default:           return $r('app.color.ef_text_1');
    }
  }
  /** 14% 透明底 */
  static bg(tone: Tone): Resource {
    switch (tone) {
      case Tone.Primary: return $r('app.color.ef_primary_16');
      case Tone.Bond:    return $r('app.color.ef_bond_14');
      case Tone.Emotion: return $r('app.color.ef_emotion_14');
      case Tone.Memory:  return $r('app.color.ef_memory_14');
      case Tone.Voice:   return $r('app.color.ef_voice_14');
      default:           return $r('app.color.ef_glass');
    }
  }
  /** 40% 透明描边 */
  static stroke(tone: Tone): Resource {
    switch (tone) {
      case Tone.Primary: return $r('app.color.ef_primary_42');
      case Tone.Bond:    return $r('app.color.ef_bond_14');
      case Tone.Emotion: return $r('app.color.ef_emotion_14');
      case Tone.Memory:  return $r('app.color.ef_memory_14');
      case Tone.Voice:   return $r('app.color.ef_voice_14');
      default:           return $r('app.color.ef_hairline');
    }
  }
  /** 情绪 → 色调（情绪不用玫瑰单色，按情绪细分更直观） */
  static ofEmotion(emotion: string): Tone {
    switch (emotion) {
      case '开心': case '期待': return Tone.Bond;
      case '悲伤': case '孤独': case '疲惫': return Tone.Memory;
      case '生气': case '紧张': case '惊讶': return Tone.Emotion;
      case '害羞': return Tone.Emotion;
      case '平静': default: return Tone.Neutral;
    }
  }
}

// ---------------- 情绪枚举（与 core/StateAnalyzer 的判定集合一致） ----------------
export class Emotions {
  static readonly all: string[] = ['平静', '开心', '悲伤', '期待', '孤独', '害羞', '紧张', '惊讶', '生气', '疲惫'];
  /** 关系等级，可由角色卡 extensions.echoflow.relation_levels 覆盖 */
  static readonly defaultLevels: string[] = ['陌生', '初识', '熟悉', '信赖', '亲密', '特殊羁绊'];
  static readonly defaultThresholds: number[] = [0, 20, 40, 60, 80, 95];

  /** 情绪对回复的约束，注入 Prompt 与 TTS */
  static directive(emotion: string): string {
    switch (emotion) {
      case '平静': return '语气平缓，句子短，不用感叹号。';
      case '开心': return '语气轻快，可以用一点语气词和短促的句子。';
      case '悲伤': return '语气低沉，允许停顿和省略号，不要说教。';
      case '期待': return '语气里带一点按捺不住的雀跃，会主动追问。';
      case '孤独': return '语气克制，会隐晦地表达想被陪着，但不直接要求。';
      case '害羞': return '句子变短，会用回避性的动作描写掩饰。';
      case '紧张': return '语速感偏快，句子碎片化，容易说半句。';
      case '惊讶': return '先给一个短反应，再补完整句。';
      case '生气': return '语气冷，句子短，允许刺一下但保留关心。';
      case '疲惫': return '回复更短，标点变少，允许说「不太想说话」。';
      default: return '保持角色一贯的语气。';
    }
  }
}
