# 漫画镜头工坊 · 手机端

**最终形态：安卓 APK（原生壳 + 网页前端）。** 手机里直接打开 App，读手机文件夹里的漫画、标注**写回原文件夹**、去黑条、导演台台词核对、FleshOut 润色，还能**一键安装文件夹里的 apk**——零电脑依赖。

底部四个标签：**📚漫画 / 🎙导演台 / 🧹去黑条 / ✍FleshOut**。

> 同份前端 `web-lite/index.html` 是「桥接感知单文件」：在 APK 里走原生桥可写回；在普通浏览器里自动降级为「选图导出」也能用（见末尾「纯网页备用方案」）。

---

## 一、功能速览

| 标签 | 功能 | 说明 |
|---|---|---|
| 📚 漫画 | 浏览 / 标注 | APK 内：选手机项目文件夹读入全部图片；分镜框、对白、涂鸦、上色、撤销 → **写回原文件夹**（生成 `_edited.png` + `.json`）。网页降级：导出下载 |
| 🎙 导演台 | 台词核对 | 从文件夹读电脑端识别的 `json` / 粘贴台词本 → 逐行可编辑 + 已核对勾选 → 导出 / **写回** txt·json。⚠️ 配音 TTS 与 OpenCut 导入需电脑端 |
| 🧹 去黑条 | 去条带 | 选图 → 裁剪纯黑边 / 色调条带平滑（保守，零线条损伤）+ 笔刷圈选局部 → 前后对比 → 导出 / **写回** `_debanded.png` |
| ✍ FleshOut | 润色 | 本地：导入正文→大纲/角色表、格式化对白、导出；API：填你的 LLM 接口（OpenAI 兼容）做 AI 润色 |

---

## 二、出包：推到 GitHub 自动编译 APK（本机零安装）

本机没有 JDK / Android SDK，编译交给 **GitHub Actions 云端**完成。流程：把代码推上 GitHub → Actions 自动编译 → 下载 `app-debug.apk` → 手机安装。

### 路径 A（推荐，GUI 不用命令行）—— GitHub Desktop

1. 去 [desktop.github.com](https://desktop.github.com/) 装 GitHub Desktop，登录你的 GitHub 账号。
2. **File → Add local repository**，选本文件夹 `F:\workbuddy\comic-studio-mobile`，按提示 **Create a repository**（已 `git init` 过，直接 Add 即可）。
3. 左下写个 Summary（如「手机端 APK 版」），点 **Commit to main**。
4. 点 **Publish repository**：
   - Name 填 `comic-studio-mobile`（或你喜欢的）
   - 取消勾选「Keep this code private」→ 必须**公开**，否则私有仓库 Actions 要付费/受限
   - 点 **Publish**
5. 推送后打开仓库页面 → 顶部 **Actions** 标签 → 看到 `Build APK` 在跑（约 3–6 分钟）。
6. 跑完点该任务 → 右侧 **Artifacts** 里下载 `comic-studio-mobile-apk` → 解压得到 `app-debug.apk`。

### 路径 B（命令行，可选）

```bash
# 1) 在 github.com 新建一个空仓库（不要勾 README），得到地址
#    https://github.com/<你的用户名>/comic-studio-mobile.git

# 2) 在本文件夹执行（已 git init 并提交，只需连远程 + 推送）
git remote add origin https://github.com/<你的用户名>/comic-studio-mobile.git
git branch -M main
git push -u origin main

# 3) 之后默认分支 main 有推送就会自动触发 Actions 出包
```

> 推送后同样的：仓库 **Actions** 标签看进度，完成后在 Artifacts 下载 `comic-studio-mobile-apk`。

---

## 三、手机里要放什么文件（别传项目源码）

**不要**把电脑上的 Android 工程（`comic-studio-mobile/` 里的 `app/`、`web-lite/`、`build.gradle`、`.git` 等）拷到手机——那是「出包源码」，只在电脑上用来编译出 `app-debug.apk`，手机用不到。

手机上只需要两样东西：

1. **`app-debug.apk`**：从 GitHub Actions 的 Artifacts 下载，装到手机（这就是 App 本身）。
2. **你的漫画工作文件**：把漫画项目文件夹（图片 `png/jpg/webp`，以及电脑端识别生成的 `json`/`txt`）放到手机存储的 **`/内部存储/漫画镜头工坊/`** 目录下。

推荐目录结构：
```
/内部存储/漫画镜头工坊/
├── 我的漫画A/            ← 一个项目一个文件夹
│   ├── page001.png
│   ├── page002.png
│   └── project.json      ← 电脑端识别生成的台词/分镜（导演台可读）
├── 我的漫画B/
│   └── ...
└── (可选) 其他.apk        ← 在 App 内点一下即可安装
```

放法（任选）：
- **USB 数据线**：手机连电脑选「传输文件(MTP)」，把漫画文件夹拖进 `漫画镜头工坊`。
- 微信文件传输 / 网盘 / 任意方式先把文件弄进手机任意目录，再用 App 顶部的**路径栏**浏览到那个目录（App 有「所有文件访问」权限，能读任意位置）。

首次打开 App 会自动建好 `/存储卡/漫画镜头工坊` 并进入；在 App 里点图片标注，**写回**会生成 `<原名>_edited.png` + `<原名>.json` 同目录；去黑条写回 `_debanded.png`；导演台/FleshOut 也可写回 `json`/`txt`。

> 原始需求「拖文件夹进手机 + 装里面的 apk」：把任意 `.apk` 放进 `漫画镜头工坊`，在 App 里点它即调起系统安装器安装——无需电脑。

## 四、手机安装与首次授权（关键）

1. 把 `app-debug.apk` 传到手机，点它安装（若提示「允许安装未知应用」，按系统引导开一下权限）。
2. 首次打开会弹两个授权，**都要允许**：
   - **所有文件访问**：否则读不到文件夹（漫画/写回/装 apk 都依赖它）。
   - **安装未知应用**：装文件夹里 apk 时需要。
3. 授权完自动回到 App，默认进入 `/存储卡/漫画镜头工坊` 文件夹（没有会自动建）。把你的漫画文件夹放进去，或直接用 App 内浏览到任意路径。

---

## 五、纯网页备用方案（不装 App 也能用）

把 `web-lite/index.html` 传到手机，用浏览器（Chrome / Edge）打开即可用四个标签。**限制**：受浏览器沙箱约束，结果只能「导出下载」、**不能写回原文件夹**、**不能装 apk**；iPhone Safari 不支持选文件夹（改用选单张/多张图或安卓）。APK 模式无这些限制。

---

## 六、已知限制

1. **导演台**：配音生成（TTS）与 OpenCut 导入需电脑端，App 内只做台词核对与导出/写回。
2. **FleshOut · API 模式**：浏览器/WebView 跨域（CORS）可能拦截，需你的接口允许跨域或经代理；本地模式无需联网。
3. **去黑条为纯前端启发式**：裁剪纯黑边可靠；条带平滑为轻度保守处理（仅作用于偏暗低方差的均匀条带），重条带可能去不干净——可配合笔刷圈选局部加强。

---

## 七、工程结构

```
comic-studio-mobile/
├── app/                          # Android 原生壳（WebView）
│   └── src/main/
│       ├── assets/www/index.html ★ APK 内前端（= web-lite 部署版，含桥接写回）
│       ├── java/com/comicstudio/
│       │   ├── MainActivity.java  # 注入桥、授权、加载 index.html
│       │   ├── ComicBridge.java   # @JavascriptInterface：文件夹读写/写回/装 APK
│       │   └── ApkInstaller.java   # FileProvider 调系统安装器
│       └── AndroidManifest.xml     # 权限齐全（MANAGE_EXTERNAL_STORAGE / REQUEST_INSTALL_PACKAGES / FileProvider）
├── web-lite/index.html           # 源前端：桥接感知单文件（改这里，再部署进 assets/www）
├── .github/workflows/build.yml   # GitHub Actions 云端出包
├── build.gradle / settings.gradle / gradle.properties
└── README.md
```

**改前端流程**：编辑 `web-lite/index.html` → `node --check` 校验 JS → 复制为 `app/src/main/assets/www/index.html` → 推 GitHub 重新出包。
