# 星语 · EchoFlow

> 一个**全本地运行**的 AI 角色陪伴 App。
> 角色有记忆、关系、情绪、日程、声音和形象——而且这一切都不出你的设备。

```
对话模型  → 本地 Ollama（或任意 OpenAI 兼容接口）
语音合成  → 本地 edge-tts 服务 / 阿里云 CosyVoice / 手机自带
角色立绘  → 本地 ComfyUI（Pony Diffusion V6 XL）
语音识别  → 暂未接入
```

**为什么强调"全本地"**：对一个陪你说话的 App 来说，"你的对话没有发到任何人的服务器上"比任何功能都重要。默认配置下它就是这样。

---

## 它是什么

不是"ChatGPT 套个角色卡壳"，而是让角色**有自己的生活**：

| 能力 | 说明 |
|---|---|
| **记忆** | 从对话里自动提取长期记忆，下次聊天她记得 |
| **关系** | 亲密度随互动增长，从「陌生」到更深的关系 |
| **情绪** | 她有自己的情绪状态，会影响回复的语气 |
| **日程** | 她此刻在做什么（打烊、在路口站着……），忙的时候回复更短 |
| **主动** | 会给你发消息、写日记、留信、时间胶囊 |
| **声音** | 6 个角色各有音色，能真的打电话 |
| **形象** | 桌宠用角色原图悬浮在桌面 |

### 十八个内置应用

App 里有一个**模拟手机**——微信风的聊天、朋友圈、角色库、神社、日程、电话、相册、音乐、天气、备忘、计算器、文档、漂流瓶、YIJIE 助手、桌宠、世界书、生图、语音设置。

其中比较有意思的几个：

- **神社**：每日一签，她会为你解签（口吻符合她的性格）
- **漂流瓶**：捞到 43 条原创的祝福/治愈文案；约 1/4 概率捞到**角色扔的瓶子**
- **YIJIE 助手**：一个有人格但不扮演的干活 AI，能写文档、翻译、做计划
- **世界书**：切换世界观，天气和场景会跟着变

---

## 快速开始

### 1. 装 APK

```
dist/EchoFlow-v3.6-*.apk
```

需要 Android 8.0+（minSdk 26）。

### 2. 配模型

**推荐：本地 Ollama**（不需要 key，不消耗额度）

```bash
# 电脑上
OLLAMA_HOST=0.0.0.0:11434 ollama serve
```

App 里：首页 `⋯` → 设置 → 模型来源选「本地 Ollama」
- 模拟器填 `10.0.2.2:11434`
- 真机填电脑局域网 IP，例如 `192.168.1.100:11434`

点「检测并拉取模型列表」，选一个即可。

**也可以**用任意 OpenAI 兼容的云端接口。

### 3. （可选）开语音

```bash
pip install edge-tts
cd tools
python tts-server.py
```

App 里：手机 → 语音 → 渠道选「本地 TTS 服务」→ 检测连接。

音色有 **322 个**，中文有晓晓（温柔）和晓伊（活泼），日文有七海（二次元感更强）。

> **注意**：服务默认监听 `0.0.0.0`，真机连接时防火墙要放行 5001。

也可以用阿里云百炼的 CosyVoice（音质更好，2 元/万字符，国内直连），
在同一个页面切渠道并填 API Key。

### 4. （可选）开本地生图

```bash
cd ComfyUI
python main.py --listen 0.0.0.0
```

App 里：手机 → 生图 → 更换渠道 → 本地 ComfyUI。

> **`--listen 0.0.0.0` 不能省**。ComfyUI 默认只监听 `127.0.0.1`，手机连不上。

---

## 从源码构建

```bash
cd src-android/EchoFlow-Android

# 签名配置（可选，只构建 debug 包可以跳过）
cp keystore.properties.example keystore.properties
# 编辑填入你自己的密钥库

gradle assembleDebug      # 或 assembleRelease
```

**环境要求**：
- JDK 17
- Android SDK（platform 34, build-tools 34.0.0）
- Gradle 8.7

**生成自己的密钥库**：

```bash
keytool -genkeypair -v -keystore echoflow-release.jks \
  -alias echoflow -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <你的密码> -keypass <你的密码>
```

> 换密钥库会导致已安装的版本无法覆盖升级——Android 要求同包名的签名一致。

---

## 项目结构

```
src-android/EchoFlow-Android/     Android 工程（Java，98 个类）
  app/src/main/java/com/echoflow/chat/
    EfActivity.java        所有页面的基类（dp / 状态栏 / 通用组件）
    Http.java              统一网络层
    ApiClient.java         LLM 调用（含流式）
    PhoneStore.java        聊天记录存储（JSONL 增量写）
    TtsProvider.java       语音合成（多渠道）
    ImageProvider.java     生图（多渠道）
    PromptBuilder.java     角色 System Prompt 组装
    StateAnalyzer.java     从对话提取记忆/关系/情绪
    ...

design/                          设计与技术文档（21 篇）
tools/                           本地服务与调试脚本
  tts-server.py          edge-tts HTTP 服务
dist/                            构建产物
shots-android/                   模拟器验证截图
```

---

## 设计系统

「星轨 · 夜色」——原创视觉语言，**不复制任何现有产品的 UI、Logo 或素材**。

| | |
|---|---|
| 背景 | 夜色四级递进 `#08060F` → `#2C2350` |
| 主色 | `#9B7BFF` |
| 语义色 | 羁绊 `#FFA657` · 情绪 `#FF9BB0` · 记忆 `#7FE0C4` · 语音 `#79D3F5` |
| 正文 | `#C3BEDB`（对比度约 8:1） |
| 卡片 | 三层叠加：渐变描边 + 实底 + 顶部高光 |

完整规格见 `design/02-设计系统与UI规格.md`。

---

## 文档索引

按开发顺序，记录了每个功能的**决策理由**和**踩过的坑**：

| 文档 | 内容 |
|---|---|
| `01-现状盘点.md` | 解包原始 APK 得到的完整能力清单 |
| `02-设计系统与UI规格.md` | 设计 token / 组件规范 / 页面规格 |
| `03~09-*` | 生命感核心、模拟手机、世界书、生图、音乐等 |
| `17-文档与漂流瓶与美术.md` | **含一次源码事故的完整复盘** |
| `18-桌宠三修与多渠生图.md` | ComfyUI 接入踩的四个坑 |
| `19-语音通话TTS.md` | TTS 四渠道 / 音频验证方法 |
| `20-CosyVoice云端语音.md` | CosyVoice 接入 / 模拟器输入法坑 |
| `21-工程重构.md` | 网络层 / 基类 / 存储格式三项治理 |

---

## 已知问题

- **语音识别未接入**：打电话时你说话仍是打字输入（本机有 whisper 可用）
- **没有流式语音**：整句合成完再播，长句有 1~2 秒等待
- **情绪未联动语音**：CosyVoice 支持 `emotion` 参数，但还没接
- **桌宠**：不避让桌面图标；重启后不自动恢复
- **美术升级**：只覆盖了主要页面，部分次要页面仍是旧样式

---

## 许可

代码供学习参考。内置角色形象、文案均为本项目原创。

第三方依赖：
- [edge-tts](https://github.com/rany2/edge-tts)（微软在线语音，需联网）
- [Pony Diffusion V6 XL](https://civitai.com/models/257749)（生图模型，自行下载）
