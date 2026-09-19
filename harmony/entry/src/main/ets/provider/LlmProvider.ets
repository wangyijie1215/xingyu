import { http } from '@kit.NetworkKit';
import { ProviderConfig, ProviderCapability } from '../core/Models';
import { SecureStore } from '../platform/SecureStore';

/**
 * OpenAI-compatible LLM Provider
 * - SSE 流式（@ohos.net.http 的 dataReceive）
 * - 能力协商：supports() 决定 UI 是否出现对应功能（不支持则整块隐藏，不置灰）
 * - Grok / OpenAI / 本地自建 全部走同一条路径，只是 baseUrl 与 model 不同
 */

export interface ChatTurn {
  role: 'system' | 'user' | 'assistant' | 'tool';
  content: string;
  toolCallId?: string;
}

export interface ToolDef {
  type: 'function';
  function: { name: string; description: string; parameters: object };
}

export interface StreamHandlers {
  onDelta: (text: string) => void;
  onToolCall?: (name: string, args: string) => void;
  onDone: (full: string, finishReason: string) => void;
  onError: (e: Error) => void;
}

export class LlmProvider {
  readonly config: ProviderConfig;

  constructor(config: ProviderConfig) {
    this.config = config;
  }

  supports(cap: ProviderCapability): boolean {
    return this.config.capabilities.indexOf(cap) >= 0;
  }

  /** 主流式对话入口 */
  async stream(turns: ChatTurn[], handlers: StreamHandlers, tools?: ToolDef[]): Promise<void> {
    const key = await SecureStore.get(this.config.secretRef);
    const req = http.createHttp();

    const body: Record<string, Object> = {
      'model': this.config.model,
      'messages': turns as Object,
      'stream': true,
      'temperature': this.numParam('temperature', 0.85),
      'top_p': this.numParam('top_p', 0.95),
      'max_tokens': this.numParam('max_tokens', 2048)
    };
    // 只有 Provider 明确声明支持时才发送 tools —— 否则模型会报错或幻觉
    if (tools && tools.length > 0 && this.supports('tools')) {
      body['tools'] = tools as Object;
      body['tool_choice'] = 'auto';
    }

    let buffer = '';
    let full = '';
    let finishReason = 'stop';

    req.on('dataReceive', (chunk: ArrayBuffer) => {
      buffer += LlmProvider.decode(chunk);
      // SSE 以 \n\n 分隔事件
      let idx = buffer.indexOf('\n\n');
      while (idx >= 0) {
        const raw = buffer.substring(0, idx);
        buffer = buffer.substring(idx + 2);
        idx = buffer.indexOf('\n\n');

        const line = raw.split('\n').find(l => l.startsWith('data:'));
        if (!line) continue;
        const payload = line.substring(5).trim();
        if (payload === '[DONE]') { finishReason = finishReason || 'stop'; continue; }
        try {
          const json = JSON.parse(payload) as Record<string, Object>;
          const choices = json['choices'] as Array<Record<string, Object>>;
          if (!choices || choices.length === 0) continue;
          const delta = choices[0]['delta'] as Record<string, Object>;
          if (!delta) continue;
          const content = delta['content'];
          if (typeof content === 'string' && content.length > 0) {
            full += content;
            handlers.onDelta(content);
          }
          const tcs = delta['tool_calls'] as Array<Record<string, Object>>;
          if (tcs && handlers.onToolCall) {
            for (const tc of tcs) {
              const fn = tc['function'] as Record<string, Object>;
              if (fn) handlers.onToolCall(String(fn['name'] ?? ''), String(fn['arguments'] ?? ''));
            }
          }
          const fr = choices[0]['finish_reason'];
          if (typeof fr === 'string') finishReason = fr;
        } catch (e) {
          // 单个事件解析失败不应中断整条流
        }
      }
    });

    try {
      const resp = await req.request(this.endpoint(), {
        method: http.RequestMethod.POST,
        header: this.headers(key),
        extraData: JSON.stringify(body),
        connectTimeout: 30000,
        readTimeout: 300000,
        // 关键：拿到流式响应头即返回，不等整个 body
        expectDataType: http.HttpDataType.STRING
      });

      if (resp.responseCode !== 200) {
        handlers.onError(new Error(`HTTP ${resp.responseCode}：${JSON.stringify(resp.result)}`));
        return;
      }
      if (full.length === 0) {
        // 与 v3.0 一致的用户可读提示
        handlers.onError(new Error(
          '接口未返回流式数据，请检查 API 地址是否正确（应为根域名，不要带多余路径），或该模型是否支持 stream。'
        ));
        return;
      }
      handlers.onDone(full, finishReason);
    } catch (e) {
      handlers.onError(e as Error);
    } finally {
      req.destroy();
    }
  }

  /** 非流式 JSON 调用：用于记忆提取 / 关系情绪判定 / 剧情总结 */
  async complete(turns: ChatTurn[], jsonMode: boolean = false): Promise<string> {
    const key = await SecureStore.get(this.config.secretRef);
    const req = http.createHttp();
    const body: Record<string, Object> = {
      'model': this.config.model,
      'messages': turns as Object,
      'stream': false,
      'temperature': jsonMode ? 0.2 : 0.7
    };
    if (jsonMode && this.supports('json')) {
      body['response_format'] = { 'type': 'json_object' } as Object;
    }
    try {
      const resp = await req.request(this.endpoint(), {
        method: http.RequestMethod.POST,
        header: this.headers(key),
        extraData: JSON.stringify(body),
        connectTimeout: 20000,
        readTimeout: 120000,
        expectDataType: http.HttpDataType.STRING
      });
      if (resp.responseCode !== 200) throw new Error(`HTTP ${resp.responseCode}`);
      const json = JSON.parse(resp.result as string) as Record<string, Object>;
      const choices = json['choices'] as Array<Record<string, Object>>;
      const msg = choices[0]['message'] as Record<string, Object>;
      return String(msg['content'] ?? '');
    } finally {
      req.destroy();
    }
  }

  private endpoint(): string {
    const base = this.config.baseUrl.replace(/\/+$/, '');
    return base.endsWith('/chat/completions') ? base : `${base}/chat/completions`;
  }

  private headers(key: string): Record<string, string> {
    const h: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    };
    if (key) h['Authorization'] = `Bearer ${key}`;
    for (const k of Object.keys(this.config.headers)) h[k] = this.config.headers[k];
    return h;
  }

  private numParam(name: string, fallback: number): number {
    const v = this.config.params[name];
    return typeof v === 'number' ? v : fallback;
  }

  private static decode(chunk: ArrayBuffer): string {
    const bytes = new Uint8Array(chunk);
    let s = '';
    for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
    // UTF-8 → 字符串（避免中文被截断成乱码）
    return decodeURIComponent(escape(s));
  }
}

/**
 * Provider 注册表 + 模型路由（§34 / §35）
 * 不同任务走不同 Provider，从而节省 API 成本。
 */
export class ProviderRegistry {
  private providers: Map<string, LlmProvider> = new Map();
  private routeChat: string = '';
  private routeAnalyze: string = '';
  private routeSummarize: string = '';

  register(p: LlmProvider): void {
    this.providers.set(p.config.id, p);
  }

  setRoutes(chat: string, analyze: string, summarize: string): void {
    this.routeChat = chat;
    this.routeAnalyze = analyze;
    this.routeSummarize = summarize;
  }

  chat(): LlmProvider { return this.must(this.routeChat); }
  analyze(): LlmProvider { return this.must(this.routeAnalyze); }
  summarize(): LlmProvider { return this.must(this.routeSummarize); }

  get(id: string): LlmProvider | undefined { return this.providers.get(id); }

  private must(id: string): LlmProvider {
    const p = this.providers.get(id);
    if (!p) throw new Error(`Provider 未配置或已移除：${id}`);
    return p;
  }
}
