/**
 * EchoFlow 数据模型
 * Character 与 Tavern Character Card V2 双向兼容，扩展字段全部落在 extensions.echoflow，
 * 因此导出回 SillyTavern 时结构不被破坏。
 */

// ==================== 角色卡 ====================

export interface EfVoice {
  provider: string;            // TTS Provider id
  voiceId: string;             // 外部 TTS 的 Voice ID
  speed: number;               // 0.5 ~ 2.0
  pitch: number;               // -12 ~ +12
  emotionMap: Record<string, string>;  // 「开心」→ 'cheerful'
}

export interface EfAppearanceLock {
  prompt: string;              // 外貌锁定的基础 prompt
  seed: number;
  refImage: string;            // 参考图路径
}

export interface EfVisual {
  theme: string;               // 角色主题色键：violet / cyan / amber / rose
  chatBg: string;              // 聊天背景：纯色 / 渐变 key / 图片路径
  chatBgBlur: boolean;
  bubbleStyle: string;
  fontScale: number;
  avatarSide: 'left' | 'right';
}

export interface EfExtension {
  id: string;
  voice?: EfVoice;
  appearanceLock?: EfAppearanceLock;
  visual?: EfVisual;
  skillIds?: string[];
  relationLevels?: string[];
  relationThresholds?: number[];
  worldIds?: string[];
  schedule?: Record<string, string>;   // "08:10": "起床" —— 日常日程，影响主动消息
}

export interface CharacterData {
  name: string;
  description: string;
  personality: string;
  scenario: string;
  firstMes: string;
  mesExample: string;
  creatorNotes: string;
  systemPrompt: string;
  postHistoryInstructions: string;
  alternateGreetings: string[];
  tags: string[];
  creator: string;
  characterVersion: string;
  extensions: { echoflow?: EfExtension };
}

export interface CharacterCard {
  spec: 'chara_card_v2';
  specVersion: '2.0';
  data: CharacterData;
  /** 本地附加：版本历史（§43），聊天记录引用创建时的快照 */
  versions?: string[];
  createdAt?: number;
}

// ==================== 记忆（§3 / §4 / §5） ====================

export type MemoryTier = 'short' | 'mid' | 'long' | 'relation';

export type MemoryGrade =
  | 'core_identity' | 'strong_preference' | 'important_fact'
  | 'meaningful' | 'general' | 'trivial';

export interface MemorySource {
  chatId: string;
  messageId: string;
  ts: number;
  excerpt: string;
}

export interface Memory {
  id: string;
  characterId: string;
  tier: MemoryTier;
  content: string;
  importance: number;          // 0 ~ 1
  grade: MemoryGrade;
  keywords: string[];
  source: MemorySource;
  locked: boolean;
  disabled: boolean;
  hitCount: number;
  createdAt: number;
  lastUsedAt: number;
}

export const GRADE_LABEL: Record<MemoryGrade, string> = {
  core_identity: '核心身份',
  strong_preference: '强烈偏好',
  important_fact: '重要事实',
  meaningful: '有意义',
  general: '一般',
  trivial: '琐碎'
};

export const GRADE_WEIGHT: Record<MemoryGrade, number> = {
  core_identity: 1.0, strong_preference: 0.9, important_fact: 0.82,
  meaningful: 0.62, general: 0.4, trivial: 0.15
};

// ==================== 关系（§6 / §7） ====================

export interface RelationshipState {
  characterId: string;
  bond: number;                // 0 ~ 100
  level: string;
  levelIndex: number;
  sources: Record<string, number>;   // chat / event / gift / moments / memory
  lastDelta: number;
  metAt: number;
  updatedAt: number;
}

// ==================== 情绪（§8） ====================

export interface EmotionPoint {
  emotion: string;
  from: number;
  to: number;
}

export interface EmotionState {
  characterId: string;
  current: string;
  prev: string;
  valence: number;             // -1 ~ 1
  arousal: number;             // 0 ~ 1
  since: number;
  history: EmotionPoint[];
}

// ==================== 事件 / 回忆卡（§9 / §16 / §40） ====================

export interface MemoryCard {
  title: string;
  subtitle: string;
  story: string;
  dialogue: string[];
  image: string;
  exported: boolean;
}

export interface StoryEvent {
  id: string;
  characterId: string;
  date: string;                // yyyy-MM-dd
  title: string;
  body: string;
  milestone: boolean;
  bondDelta: number;
  images: string[];
  sourceMessageIds: string[];
  note: string;
  card?: MemoryCard;
}

// ==================== 会话 / 消息（§12 ~ §15） ====================

export interface MessageVariant {
  content: string;
  createdAt: number;
  providerId: string;
  model: string;
}

export interface Message {
  id: string;
  chatId: string;
  role: 'user' | 'assistant' | 'system';
  variants: MessageVariant[];   // swipe 多答案，永不覆盖
  activeVariant: number;
  kind: 'text' | 'voice' | 'image' | 'event' | 'call';
  voice?: { url: string; duration: number; transcript: string };
  image?: { url: string; caption: string; generated: boolean };
  locked: boolean;
  favorite: boolean;
  createdAt: number;
}

export interface BranchInfo {
  id: string;
  parent: string | null;
  parentMessageId: string | null;
  label: string;
}

export interface ChatSession {
  chatId: string;
  characterId: string;
  characterVersion: string;
  cardSnapshot: CharacterCard;  // 保证旧聊天不被新版本改写
  personaSnapshot: string;
  worldIds: string[];
  branch: BranchInfo;
  createdAt: number;
  lastMessageAt: number;
}

// ==================== 世界观 / Lorebook（§11） ====================

export interface LoreEntry {
  keys: string[];
  content: string;
  insertionOrder: number;
  position: 'before_char' | 'after_char';
  caseSensitive: boolean;
  constant: boolean;
  priority: number;
}

export interface Lorebook {
  id: string;
  name: string;
  entries: LoreEntry[];
}

// ==================== 主动消息（§2 / §49 / §54） ====================

export interface ProactiveMessage {
  id: string;
  characterId: string;
  createdAt: number;
  trigger: string;             // 'weather_rain+memory_rain+idle_3d'
  text: string;
  voice?: string;
  read: boolean;
  deliveredVia: 'inapp' | 'notification' | 'form';
}

export interface ProactivePolicy {
  enabled: boolean;
  perCharacterDisabled: string[];
  dailyLimit: number;
  dndStart: string;            // '23:30'
  dndEnd: string;              // '07:30'
  wifiOnly: boolean;
  /** 0 = 克制，1 = 黏人；映射为触发阈值 */
  clinginess: number;
}

// ==================== Provider / 模型路由（§34 / §35） ====================

export type ProviderCapability = 'stream' | 'tools' | 'vision' | 'json' | 'tts' | 'stt' | 'image';

export interface ProviderConfig {
  id: string;
  label: string;
  kind: 'openai-compatible' | 'grok' | 'local' | 'custom' | 'tts' | 'stt' | 'image';
  baseUrl: string;
  model: string;
  /** 密钥不落盘明文，只保留 SecureStore 的引用键 */
  secretRef: string;
  headers: Record<string, string>;
  params: Record<string, string | number>;
  capabilities: ProviderCapability[];
}

export interface ModelRoute {
  chat: string;                // ProviderConfig.id
  summarize: string;
  analyze: string;
  image: string;
  tts: string;
  stt: string;
}

// ==================== 分析器输出（§53 链路末端） ====================

export interface AnalyzedMemory {
  content: string;
  importance: number;
  grade: MemoryGrade;
  keywords: string[];
}

export interface AnalyzedEvent {
  title: string;
  body: string;
  milestone: boolean;
  bondDelta: number;
}

export interface ConsistencyReport {
  characterOk: boolean;
  worldOk: boolean;
  timelineOk: boolean;
  memoryOk: boolean;
  issues: string[];
}

export interface AnalysisResult {
  memories: AnalyzedMemory[];
  relationDelta: number;
  emotion: { name: string; confidence: number };
  event: AnalyzedEvent | null;
  consistency: ConsistencyReport;
}
