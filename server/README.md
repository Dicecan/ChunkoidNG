# Chunkoid NG 官方网站后端服务 (Sync Server)

面向 Chunkoid NG 官方网站的全自动版本同步与分发中枢。具备 **GitHub Releases 自动监听同步、APK 离线缓存、SHA-256 校验自动计算、旧版本归档流转，以及中国大陆多线路高速加速下载** 等核心能力。

---

## 🌟 核心特性

1. **GitHub Releases 自动同步**
   - **双重触发机制**：
     - **实时 Webhook**：接收 GitHub `release` 事件（`published` / `created` / `released`），通过 `X-Hub-Signature-256` 签名校验，秒级自动同步新版本与 Release Notes。
     - **定时被动轮询**：每隔 30 分钟（可配置）自动轮询 GitHub REST API，即便在内网或无公网 IP 场景下也能稳定获取新版本。
     - **手动即时同步**：随时通过 `POST /api/sync` 触发刷新。
2. **APK 自动缓存与高速直连**
   - 当检测到带有 `.apk` 产物的新 Release 时，自动流式下载至本地存储（`storage/downloads/<tag>/`）。
   - 采用流式 `crypto` 实时计算 APK 真实 SHA-256 校验码，防止篡改。
   - 提供自动防溢出清理机制，仅在磁盘保留最新的 $N$ 个历史版本 APK。
3. **中国大陆快速下载线路中继**
   - 内置多条国内免翻墙高速直连线路（本地高速节点、GHFast、GHProxy、Moeyy 镜像、GitHub 官方源）。
   - 支持通过 API 或前端界面快捷添加自定义反代前缀（如自建 Cloudflare Worker、国内 CDN 或服务器反代）。
   - 用户在官网下载时，一键切换线路即可满速直连下载最新版或历史版本。
4. **历史版本归档展示**
   - 自动按发布时间倒序排列所有历史版本。
   - 记录预发布（Pre-release）与正式版状态、文件大小、SHA-256 码与完整更新日志。
5. **前后端一体化极简部署**
   - 后端启动时自动检测 `web/dist`，直接托管前端静态页面与 SPA 路由回退，也可搭配 Nginx 做动静分离。

---

## 🚀 快速启动

### 1. 安装依赖

```bash
cd server
npm install
```

### 2. 配置说明

可直接修改 `server/config.json` 或在 `server/.env` 中配置环境变量：

```json
{
  "server": {
    "port": 3000,
    "host": "0.0.0.0",
    "serveStatic": true,
    "corsOrigin": "*"
  },
  "github": {
    "owner": "Dicecan",
    "repo": "ChunkoidNG",
    "token": "",
    "webhookSecret": "",
    "syncIntervalMinutes": 30,
    "autoDownloadApk": true,
    "maxDownloadHistoryCount": 5
  },
  "mirrors": [
    {
      "id": "local",
      "name": "本地高速节点 (服务器缓存)",
      "type": "local",
      "description": "从本站服务器直链下载，国内多线直连，速度最快最稳定",
      "enabled": true,
      "priority": 100
    },
    {
      "id": "ghfast",
      "name": "中国加速线路 1 (GHFast)",
      "type": "prefix",
      "prefix": "https://ghfast.top/",
      "description": "国内优质 GitHub Release 加速节点，免代理高速下载",
      "enabled": true,
      "priority": 90
    },
    {
      "id": "ghproxy",
      "name": "中国加速线路 2 (GHProxy)",
      "type": "prefix",
      "prefix": "https://ghproxy.net/",
      "description": "公用 GitHub 反代加速通道",
      "enabled": true,
      "priority": 80
    },
    {
      "id": "moeyy",
      "name": "中国加速线路 3 (Moeyy 镜像)",
      "type": "prefix",
      "prefix": "https://github.moeyy.xyz/",
      "description": "高可用第三方 GitHub 下载中继",
      "enabled": true,
      "priority": 70
    },
    {
      "id": "official",
      "name": "GitHub 官方源 (海外/直连)",
      "type": "direct",
      "prefix": "",
      "description": "GitHub 官方原链，适合海外或具备国际互联网访问环境的用户",
      "enabled": true,
      "priority": 10
    }
  ],
  "admin": {
    "token": "chunkoid-admin-change-me"
  }
}
```

### 3. 运行服务

```bash
# 生产模式启动
npm start

# 开发热重载模式
npm run dev

# 手动执行一次同步命令
npm run sync
```

---

## 📡 API 接口一览

| 接口 | 方法 | 权限 | 说明 |
| :--- | :--- | :--- | :--- |
| `/api/releases` | `GET` | 公开 | 获取所有版本归档列表、最新版信息、加速线路与同步状态 |
| `/api/releases/latest` | `GET` | 公开 | 获取最新版本详情与各线路直链 |
| `/api/releases/:tag` | `GET` | 公开 | 获取指定 tag（如 `v0.5.0-canary`）的版本详情 |
| `/api/download/:tag/:file` | `GET` | 公开 | 本地高速直链下载；若本地未缓存则自动 302 重定向至中国大陆加速线路 |
| `/api/mirrors` | `GET` | 公开 | 查询当前所有已启用的加速线路列表 |
| `/api/admin/mirrors/quick-add`| `POST`| 管理员 | 快捷新增或修改加速镜像节点 |
| `/api/admin/mirrors/toggle` | `POST`| 管理员 | 快捷启用/禁用某条镜像线路 |
| `/api/sync` | `POST` | 公开/管理员 | 手动触发后台同步 GitHub 最新 Releases |
| `/api/sync/status` | `GET` | 公开 | 查看后台同步状态、最近同步时间与版本总数 |
| `/api/webhook/github` | `POST`| GitHub | GitHub Webhook 接收端点（自动校验 HMAC 签名） |
| `/api/health` | `GET` | 公开 | 服务健康状态检查 |

---

## 🔗 配置 GitHub 自动触发 Webhook

在 GitHub 仓库发布 Release 时实现秒级自动同步：
1. 打开 GitHub 仓库设置：`Settings -> Webhooks -> Add webhook`。
2. **Payload URL**：`https://你的域名/api/webhook/github`
3. **Content type**：`application/json`
4. **Secret**：填入 `config.json` 中配置的 `webhookSecret`。
5. **Which events would you like to trigger this webhook?**：勾选 `Let me select individual events`，并勾选 **Releases**。
6. 点击 **Add webhook** 保存即可。
