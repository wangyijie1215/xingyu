import { asset } from '@kit.AssetStoreKit';
import { util } from '@kit.ArkTS';

/**
 * 安全存储（需求 §34 / §46）
 * API Key 走 HarmonyOS 系统级 Asset Store（HUKS 支持），
 * 明文永不落盘、永不进备份、永不出现在导出包中。
 *
 * Android 侧的等价物是 Keystore + EncryptedSharedPreferences，
 * v3.0 的 SecureStore 语义完全一致，迁移时只需替换实现。
 */
export class SecureStore {
  private static readonly ALIAS_PREFIX = 'echoflow_provider_';
  /** 进程内缓存，避免每次请求都读一次密钥库 */
  private static cache: Map<string, string> = new Map();

  static async put(refKey: string, secret: string): Promise<void> {
    const alias = SecureStore.ALIAS_PREFIX + refKey;
    await SecureStore.remove(refKey);
    const attr: asset.AssetMap = new Map();
    attr.set(asset.Tag.ALIAS, SecureStore.str2buf(alias));
    attr.set(asset.Tag.ACCESSIBILITY, asset.Accessibility.DEVICE_FIRST_UNLOCKED);
    attr.set(asset.Tag.SECRET, SecureStore.str2buf(secret));
    attr.set(asset.Tag.DATA_LABEL_NORMAL_1, SecureStore.str2buf('echoflow-api-key'));
    await asset.add(attr);
    SecureStore.cache.set(refKey, secret);
  }

  static async get(refKey: string): Promise<string> {
    const hit = SecureStore.cache.get(refKey);
    if (hit !== undefined) return hit;
    const query: asset.AssetMap = new Map();
    query.set(asset.Tag.ALIAS, SecureStore.str2buf(SecureStore.ALIAS_PREFIX + refKey));
    query.set(asset.Tag.RETURN_TYPE, asset.ReturnType.ALL);
    try {
      const res = await asset.query(query);
      const secret = SecureStore.buf2str(res[0].get(asset.Tag.SECRET) as Uint8Array);
      SecureStore.cache.set(refKey, secret);
      return secret;
    } catch (e) {
      // 密钥不存在是正常情况（新装 / 已删除 Provider）
      return '';
    }
  }

  static async remove(refKey: string): Promise<void> {
    SecureStore.cache.delete(refKey);
    const query: asset.AssetMap = new Map();
    query.set(asset.Tag.ALIAS, SecureStore.str2buf(SecureStore.ALIAS_PREFIX + refKey));
    try {
      await asset.remove(query);
    } catch (e) { /* 不存在即视为已删除 */ }
  }

  /** 备份/导出前必须调用：确保任何序列化路径都拿不到密钥 */
  static isSecretField(fieldName: string): boolean {
    const f = fieldName.toLowerCase();
    return f.includes('key') || f.includes('token') || f.includes('secret') || f.includes('password');
  }

  private static str2buf(s: string): Uint8Array {
    return new util.TextEncoder().encodeInto(s);
  }

  private static buf2str(b: Uint8Array): string {
    return new util.TextDecoder().decodeToString(b);
  }
}
