# Chunkoid NG 官方网站

Chunkoid NG（安卓端 Minecraft 世界转换工具箱）的官方站点。使用 Vue 3 + Vite 构建，纯静态、无运行时依赖、无外部 CDN。

## 快速开始

```bash
npm install
npm run dev      # 开发服务器，默认 http://127.0.0.1:5173/
npm run build    # 构建到 dist/
npm run preview  # 本地预览构建产物
```

## 技术选型

| 项 | 选择 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 路由 | vue-router 4，**hash 模式**（静态托管无需服务端重写） |
| 构建 | Vite 6 |
| 样式 | 原生 CSS + CSS 自定义属性，自带浅色 / 深色主题 |
| 国际化 | 自研轻量 i18n（`src/i18n/`），无第三方依赖 |

产物只有 4 个文件（`index.html`、`logo.svg`、一个 JS、一个 CSS），可直接部署到任意静态托管。

## 多语言

站点支持 **简体中文（zh-CN）**、**English（en）**、**日本語（ja）** 三种语言。

- **自动检测**：首次访问按浏览器语言自动选择，无法匹配时回退到简体中文。
- **手动切换**：顶栏右侧地球图标按钮，选择后写入 `localStorage`（键名 `chunkoid-ng-locale`），刷新后保持。
- **切换即时生效**：无需刷新页面，同时同步 `<html lang>` 与页面标题。

### 新增一种语言

1. 在 `src/locales/` 下新建语言包（可复制 `en.js` 作为模板），补齐所有键。
2. 在 `src/i18n/index.js` 中 `import` 该语言包，并加入 `MESSAGES` 映射与 `LOCALES` 数组。
3. 在 `src/i18n/index.js` 的 `normalize()` 中为该语言增加浏览器语言码的匹配规则。

### 缺失翻译的回退行为

`t('a.b.c')` 按以下顺序取值：当前语言 → 简体中文 → 返回键名本身。

因此**漏翻会显示中文而不是空白**，键名会直接暴露在页面上，便于发现遗漏。

## 首页 2.5D 主视觉

首页顶部使用三层视差插画（`src/components/HeroParallax.vue`）。

- **图层与深度**：`layer1-bg`（背景，translateZ -55px，反向微移 -0.45x）、`layer2-char`（人物，+24px，+0.36x）、`layer3-grass`（前景花草，+85px，+1.32x）。三层的位移系数不同，形成视差纵深。
- **交互**：桌面端跟随鼠标位置，移动端在支持的设备上跟随陀螺仪倾角；无操作时缓慢自动摆动。
- **资源**：原图放在 `public/hero/`，已从 PNG 转为 WebP（3.8 MB → 586 KB），人物与前景图层保留透明通道。
- **无障碍**：遵循 `prefers-reduced-motion`（关闭自动摆动、忽略指针与陀螺仪输入，并支持在会话中实时切换）；背景层提供 `alt` 文本，人物与前景层标记为装饰性。

> ⚠️ 不要给 `layer-char` / `layer-grass` 添加 `filter`。在 `preserve-3d` 子树上任何 `filter` 都会强制生成扁平化包含块，从而破坏 WebKit 上的景深分层。

替换插画时，保持三张图的尺寸比例一致即可；若尺寸不同，同步更新组件内 `<img>` 的 `width` / `height` 属性以避免布局偏移。

## 内容维护

所有站点内容集中在 `src/data/`，与视图组件解耦。

### `features.js` — 功能数据与站点元信息

- `site`：站点元信息（版本号、协议、仓库地址、系统要求等），页头、页脚、首页与关于页共用。
- `features`：七大功能模块。每个模块的文案按 `{ 'zh-CN': {...}, en: {...}, ja: {...} }` 组织，公共字段（`id`、`accent`）在顶层。
- `release`：发布状态（见下方「发布新版本」）。

**新增功能模块**：在 `features` 数组追加一项，填好 `id`、`accent` 与三套语言文案即可。功能总览、首页卡片、详情页 `/features/:id`、页脚导航会自动同步，无需修改任何组件。

### `docs.js` — 文档

`docPages` 数组，每篇文档含 `slug`、标题、摘要与 `sections`（章节标题 + 段落或列表）。三套语言同样按语言分支组织。

**新增文档**：追加一项即可自动获得 `/docs/:slug` 子页面、侧边目录与上/下一篇导航。数组顺序即目录顺序。

### `changelog.js` — 更新日志与规划

- `changelog`：版本记录，含 `version`、`code`、`status`（`current` / `past`）、变更条目（`feat` / `fix` / `chore`）。
- `roadmap`：后续规划条目。

## 发布新版本时的检查清单

1. **更新版本号**：修改 `src/data/features.js` 中 `site.version` 与 `site.versionCode`。
2. **开启下载**：在 `release` 中填入 `published: true`、`apkUrl`（安装包直链）与 `sha256`（校验值）。未开启时下载页显示「尚未提供下载」的禁用按钮，避免伪造链接。
3. **追加更新日志**：在 `changelog` 数组**开头**插入新版本条目，并把上一版的 `status` 从 `current` 改为 `past`。
4. **同步功能变更**：若应用新增或调整了功能模块，同步更新 `features` 中对应条目的三套语言文案。

> `release.note` 也应随版本更新，说明当前版本的发布状态。

## 目录结构

```
src/
  i18n/index.js          多语言核心：locale 状态、t()、localized()、自动检测与持久化
  locales/
    zh-CN.js             简体中文界面文案
    en.js                English UI strings
    ja.js                日本語の UI 文言
  data/
    features.js          站点元信息、发布状态、七大功能模块
    docs.js              文档页面
    changelog.js         更新日志与后续规划
  router/index.js        路由表、滚动行为、标题管理
  components/
    SiteHeader.vue       顶栏：导航 + 语言切换器
    SiteFooter.vue       页脚
    FeatureCard.vue      功能卡片（多语言）
  views/                 各页面视图
  styles/base.css        设计令牌与全局样式
```

## 页面结构

| 路由 | 说明 |
| --- | --- |
| `/` | 首页 |
| `/features` | 功能总览 |
| `/features/:id` | 功能详情（7 个模块共用模板） |
| `/download` | 下载与安装说明 |
| `/docs` | 文档索引 |
| `/docs/:slug` | 文档子页面（8 篇共用模板，含侧边目录与上下篇导航） |
| `/changelog` | 更新日志与规划 |
| `/faq` | 常见问题 |
| `/about` | 关于、技术栈、致谢、开源协议 |
| `*` | 404 |

## 设计约定

- 强调色为低饱和绿 `#2f7d63`，功能模块各有独立主题色（`feature.accent`）。
- 所有可点击的元素在浅色 / 深色下均有足够的对比度。
- 移动端断点 980px（导航折叠）与 900px（多栏转单栏）。
- 不使用外部字体、图标库或统计脚本，保证离线可用与加载速度。

## 许可

站点内容与代码随 Chunkoid NG 项目一同以 **GPLv3** 协议开源。原作者 Dozener (DozenesStudio) 的署名永久保留。