# CosyVoice 云端语音

> 对应 `dist/EchoFlow-v3.5-CosyVoice.apk`（8.02 MB）

---

## 一、为什么加云端

v3.4 的语音只有本地 edge-tts 和手机自带两条路。这两个都有硬伤：

| 渠道 | 问题 |
|---|---|
| 本地 edge-tts | 必须电脑开着，人在外面就用不了 |
| 手机自带 TTS | 机械音，而且所有角色都是同一个声音 |

所以加了阿里云百炼的 **CosyVoice**——国内直连、不用代理、按量计费。

---

## 二、实测数据

| | 本地 edge-tts | CosyVoice |
|---|---|---|
| 码率 | 48 kbps | **128 kbps** |
| 要开电脑 | 是 | 否 |
| 音色数 | 322（含日文） | 10（全中文） |
| 价格 | 免费 | 2 元/万字符 |

**同一句话**（"今天又下雨了，你那边呢？"）：
- edge-tts：12 KB
- CosyVoice：32 KB

**换算成体感**：角色一句话约 30 字，一次通话 20 轮约 600 字 ≈ **0.12 元**。
一万字符够打 300 多次电话。

---

## 三、接的时候踩的坑

### 坑 1：端点不在文档写的地方

CosyVoice 用这个端点：

```
POST https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer
```

而 MiniMax 系列用的是另一个：

```
POST https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
```

我在错误的端点上试了两次，返回的是 `url error, please check url！` ——
**这个报错很容易被误读成"参数写错了"，其实是端点不对。**

### 坑 2：它返回的不是音频，是一个 URL

别的渠道都是直接返回音频字节。CosyVoice 的响应长这样：

```json
{"output":{"audio":{"data":"",
                    "expires_at":1789875292,
                    "id":"audio_4a0261e2-...",
                    "url":"http://dashscope-result-bj.oss-....mp3?Expires=..."}}}
```

所以是**两步**：先拿 url，再去下载。URL 24 小时有效。

代码里对 `data` 字段也做了兜底（有些模型走 base64），
但现在走的确实是 url 这条路。

### 坑 3：模拟器的输入法会把英文转成中文

这个浪费了我最多时间。在模拟器里用 `adb shell input text` 填 URL 和模型名，
**Google 输入法会把 ASCII 自动"纠正"成中文**：

| 我输入 | 实际存进去的 |
|---|---|
| `https://dashscope...` | `HTTPS；／／大神scope。阿里云产生。com／...` |
| `qwen-turbo` | `请问－` |

**解决**：把输入法换成不联想的那个。

```bash
adb shell ime disable com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME
adb shell ime set com.google.android.tts/.../VoiceInputMethodService
```

之后 `input text` 就老老实实打 ASCII 了。

### 坑 4：保存按钮被键盘挡住

填完最后一个字段后键盘还开着，盖住了「保存」按钮。
`uiautomator dump` 里根本找不到「保存」这个节点，我一度以为按钮坏了。

**表现是"点了保存但没落盘"** —— 实际上那几次点击根本没打在按钮上。
判断方法：看 `shared_prefs/` 下有没有文件生成。

```bash
adb shell input keyevent 111          # 收键盘
adb shell input swipe 540 1600 540 900 300   # 上滑让按钮露出来
```

---

## 四、音色分配

10 个 CosyVoice 音色（全部实测可用），按角色性格分：

| 角色 | 音色 | 理由 |
|---|---|---|
| 月铃 | 龙婧 · 清冷 | 神秘疏离 |
| 艾莲 | 龙小夏 · 沉稳 | 成熟治愈 |
| 洛可 | 龙嫇 · 少女 | 话密 |
| 白夜 | 龙婧 · 清冷 | 安静 |
| 凛 | 龙悦 · 活泼 | 傲娇 |
| 爱丽丝 | 龙婉 · 温柔 | 温柔寡言 |

选音色时**会立刻试听**——挑音色看文字描述没用。

---

## 五、验证记录

| 项 | 结果 |
|---|---|
| 编译 | ✅ 0 错误 |
| release 签名 | ✅ 8.02 MB |
| release 启动 | ✅ 无崩溃 |
| key 落盘 | ✅ Keystore 加密，128 字节密文 |
| 地址/模型落盘 | ✅ `dashscope.../compatible-mode/v1` + `qwen-turbo` |
| 10 个音色 | ✅ 逐个 API 测试通过 |
| 试听合成 | ✅ 33109 字节 |
| 音频有效性 | ✅ ffprobe：**2.06 秒 / 128 kbps** |
| 通话首句 | ✅ 15973 字节 |
| 按角色分音色 | ✅ 六角色各不同 |
| 降级兜底 | ✅ 无 key 时自动退回系统 TTS |

---

## 六、关于那把 key

你给的那把 key 能同时做两件事：

1. **文本对话**（`qwen-turbo`，已配进设置页）
2. **语音合成**（CosyVoice，走独立的端点）

所以一把 key 就够。它在 App 里是 **Keystore 加密存储**的，
明文不落盘（`echoflow_secure.xml` 里只有密文）。

**但要说清楚**：你现在把 key 发在了聊天记录里，建议用完去
[百炼控制台](https://bailian.console.aliyun.com) **轮换一次**。
不是因为这里不安全，是因为聊天记录本身会留存。

---

## 七、没做到的

- **没有流式合成**。现在是"整句合成完再播"，
  长句会有 1~2 秒等待。DashScope 支持 SSE 流式（`X-DashScope-SSE: enable`），
  可以边收边播，但需要重写播放器。
- **没有音色克隆**。百炼支持复刻音色（MiniMax 要 9.9 元/个），
  这能让每个角色有**专属**声音而不是从 10 个里挑。这是下一步最值得做的。
- **情绪没有联动**。CosyVoice 支持 `emotion` 参数
  （happy/sad/angry/whisper 等），你的 `EmotionState.mood` 完全可以映射过去，
  但现在还没接。
- **`lofi_telephone` 音效没用上**。这个正好能做"电话音质"效果，
  配合通话界面很贴，但需要走 MiniMax 渠道（CosyVoice 没有这个参数）。
