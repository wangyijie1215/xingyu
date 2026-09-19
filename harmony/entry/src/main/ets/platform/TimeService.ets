import { i18n } from '@kit.LocalizationKit';
import { connection } from '@kit.NetworkKit';

/**
 * 时间感知（需求 §23）
 * 角色绝对不能把时间搞错：启动时从 HarmonyOS 取真实系统时间与时区，
 * 并把 Current Date / Time / Timezone 明确写入 Prompt。
 */
export class TimeService {
  private festivalTable: Record<string, string> = {
    '01-01': '元旦', '02-14': '情人节', '03-08': '妇女节', '05-01': '劳动节',
    '06-01': '儿童节', '10-01': '国庆节', '12-25': '圣诞节',
    '09-10': '教师节', '09-17': '纪念日'
  };

  now(): Date {
    return new Date();
  }

  tz(): string {
    try {
      return i18n.getSystemTimeZone();
    } catch (e) {
      return 'Asia/Shanghai';
    }
  }

  /** yyyy年MM月dd日 */
  dateStr(d: Date = new Date()): string {
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
  }

  /** 周四 */
  weekdayStr(d: Date = new Date()): string {
    return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];
  }

  /** HH:mm */
  timeStr(d: Date = new Date()): string {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  describe(d: Date = new Date()): string {
    return `${this.dateStr(d)} ${this.timeStr(d)}（${this.weekdayStr(d)}，${this.tz()}）`;
  }

  /** 节日 / 纪念日；命中时主动消息会拿到额外权重 */
  festival(d: Date = new Date()): string {
    const key = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return this.festivalTable[key] ?? '';
  }

  /** 相对时间：刚刚 / N 分钟前 / N 小时前 / N 天前 */
  relative(ts: number): string {
    const diff = Date.now() - ts;
    const min = Math.floor(diff / 60000);
    if (min < 1) return '刚刚';
    if (min < 60) return `${min} 分钟前`;
    const hour = Math.floor(min / 60);
    if (hour < 24) return `${hour} 小时前`;
    return `${Math.floor(hour / 24)} 天前`;
  }

  /** 是否在 Wi-Fi 上（主动消息「仅 Wi-Fi」限流需要） */
  isWifi(): boolean {
    try {
      const net = connection.getDefaultNetSync();
      if (!net || net.netId === 0) return false;
      const caps = connection.getNetCapabilitiesSync(net);
      const bearers = caps.bearerTypes ?? [];
      return bearers.includes(connection.NetBearType.BEARER_WIFI);
    } catch (e) {
      return true;   // 取不到网络信息时不阻塞主动消息
    }
  }

  /** 距上次互动过去了多久（小时），主动性最重要的触发因子 */
  idleHours(lastInteractionAt: number): number {
    return Math.max(0, (Date.now() - lastInteractionAt) / 3600000);
  }
}

/**
 * 天气 / 环境（需求 §24）
 * 只在用户授权后获取；提示词里明确要求「仅在与对话相关时提及，不要每轮主动播报」。
 */
export interface WeatherData {
  text: string;
  temp: string;
  city: string;
  updatedAt: number;
}

export class WeatherService {
  private data: WeatherData | null = null;
  private allowed: boolean = false;

  setPermission(granted: boolean): void { this.allowed = granted; }

  /** 由后台任务调用，不阻塞 UI；失败时静默保留上一次数据 */
  async refresh(): Promise<void> {
    if (!this.allowed) return;
    try {
      // 由具体的天气 Provider（可插拔）实现，此处仅保留契约
      this.data = await WeatherProviderImpl.fetch();
    } catch (e) {
      console.warn('[Weather] 获取失败，沿用上一次数据：', e);
    }
  }

  summary(): string {
    if (!this.allowed || !this.data) return '';
    return `${this.data.city} ${this.data.text} ${this.data.temp}`;
  }

  changedSince(lastText: string): boolean {
    return this.data !== null && this.data.text !== lastText;
  }

  current(): WeatherData | null { return this.data; }
}

/** 可插拔的天气 Provider 契约（与 Provider 体系保持一致） */
export interface WeatherProvider {
  fetch(): Promise<WeatherData>;
}

export class WeatherProviderImpl {
  static impl: WeatherProvider | null = null;
  static async fetch(): Promise<WeatherData> {
    if (!WeatherProviderImpl.impl) throw new Error('未配置天气 Provider');
    return WeatherProviderImpl.impl.fetch();
  }
}
