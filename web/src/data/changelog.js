export const changelog = [
  {
    version: 'Beta 0.7',
    code: 7,
    status: 'past',
    highlight: {
      'zh-CN': '生物与实体全量双向迁移引擎、全新 MAID™ 女仆安全与性能治理底座',
      en: 'Smart entity migration pipeline, MAID™ governance architecture and inspection dashboard',
      ja: '生物・エンティティ全量双方向変換エンジン、MAID™ メイド統合セキュリティ基盤'
    },
    date: {
      'zh-CN': '公开测试版',
      en: 'Public Beta',
      ja: '公開ベータ'
    },
    items: [
      { type: 'feat', text: { 'zh-CN': '首创全量生物与实体智能迁移引擎（JE ↔ BE 双向及同平台跨版本），打破上游 Chunker 仅 4 种装饰实体的限制，彻底攻克空壳实体、交易丢失与血量崩溃痛点。', en: 'Introduced smart bi-directional entity migration pipeline (JE ↔ BE and same-platform cross-version), overcoming upstream Chunker limitations and fixing empty shells, trade loss, and health crashes.', ja: '生物・エンティティの全量双方向変換エンジン（JE ↔ BE および同プラットフォーム世代間）を新設。空洞エンティティや取引消失、体力型クラッシュを根底から解消。' } },
      { type: 'feat', text: { 'zh-CN': '全量解析与双向换算村民交易表（Offers.Recipes），完整保留经验修补等附魔书、绿宝石交换比、补货次数、职业等级与价格乘数。', en: 'Full bi-directional villager trade offer mapping (Offers.Recipes), preserving enchanted books (e.g. Mending), emerald trades, tiers, uses, and price multipliers.', ja: '村人の取引表（Offers.Recipes）を双方向で完全換算。修繕などのエンチャント本、エメラルドレート、レベル、利用回数を完全に維持。' } },
      { type: 'feat', text: { 'zh-CN': '重构跨区块实体拓扑挂接算法，修复猪与栅栏跨区块拴绳断裂消失、船/矿车/马匹等载具与乘客跨区块解绑脱节问题。', en: 'Rebuilt cross-chunk entity topology linking, fixing severed cross-chunk leashes and detached vehicle-passenger pairs (boats, minecarts, leashed animals).', ja: 'チャンク間エンティティ参照トポロジーを再構築。チャンクを跨ぐ首ひも消失や、ボート・トロッコ・馬と乗員の分離問題を解決。' } },
      { type: 'feat', text: { 'zh-CN': '新增同平台跨版本实体流水线：支持 Java 1.17 前后实体 MCA 分离与合并、1.20.5 装备格式字典化，以及基岩版 LevelDB actorprefix/digp 迁移。', en: 'Added same-platform cross-version entity pipelines: handling Java 1.17+ entity MCA separation, 1.20.5 equipment format adaptation, and Bedrock LevelDB actor migration.', ja: '同プラットフォーム間バージョン移行パイプラインを追加：Java 1.17+ エンティティMCA分離、1.20.5 装備辞書変換、統合版 LevelDB actor 移行に対応。' } },
      { type: 'feat', text: { 'zh-CN': '全新集成 MAID™ (Managed Architecture for Integrity & Defense) 女仆统一安全与性能治理框架，收敛全模块防御与配额。', en: 'Introduced the MAID™ (Managed Architecture for Integrity & Defense) unified security and resource governance framework.', ja: '統合セキュリティ・性能ガバナンス基盤 MAID™（メイド™）を導入し、全モジュールの防護とクォータを集約。' } },
      { type: 'feat', text: { 'zh-CN': '首创资源超标「温和截断（Gentle Truncation）」策略与 100 条定长无锁环形审计体系，超限材质帧与超大 MIDI 音符自动安全修剪不崩溃。', en: 'Pioneered "Gentle Truncation" and lock-free ring-buffer audit logging, safely clamping oversized frames and MIDI notes without crashing.', ja: '「安全切詰（Gentle Truncation）」とリングバッファ監査ログを導入。上限超過のアニメフレームや巨大 MIDI を安全にトリミング。' } },
      { type: 'feat', text: { 'zh-CN': '新增设置「女仆™ 巡检看板」弹窗，支持设备体征总览、生效配额查看、实时拦截流与一键复制诊断报告。', en: 'Added "MAID™ Inspection Dashboard" dialog in Settings with device metrics, active quotas, live audit events and report export.', ja: '設定に「メイド™ 巡回ダッシュボード」を追加。端末スペック、有効制限、リアルタイム監査ログの閲覧とコピーに対応。' } },
      { type: 'fix', text: { 'zh-CN': '优化偏好设置卡片排版，解决双操作按钮导致的标题与说明单字纵向换行挤压问题。', en: 'Optimized Settings card layout, fixing title text vertical stacking caused by dual button widths.', ja: '設定画面のカードレイアウトを改善し、ボタン配置によるタイトルの縦長崩れを解消。' } }
    ]
  },
  {
    version: 'Beta 0.6',
    code: 6,
    status: 'current',
    highlight: {
      'zh-CN': '自适应硬件调优、首次启动欢迎向导、独立崩溃诊断与机型稳定性优化',
      en: 'Adaptive hardware tuning, welcome wizard, crash diagnostics and stability',
      ja: 'ハードウェア適応チューニング、初回ウィザード、クラッシュ診断と安定性向上'
    },
    date: {
      'zh-CN': '公开测试版',
      en: 'Public Beta',
      ja: '公開ベータ'
    },
    items: [
      { type: 'feat', text: { 'zh-CN': '新增自适应硬件调优体系（低内存/均衡/激进/自定义），根据设备 RAM 与核心数动态调整沙箱堆内存与 I/O 缓冲区。', en: 'Added adaptive hardware tuning (Low Memory / Balanced / Aggressive / Custom) based on device RAM and CPU cores.', ja: '端末の RAM と CPU コア数に応じてサンドボックスのメモリと I/O を最適化する適応チューニングを追加。' } },
      { type: 'feat', text: { 'zh-CN': '新增首次启动欢迎向导与权限申请引导，简化新用户初始化流程。', en: 'Added first-launch welcome wizard and permission guide for smoother setup.', ja: '初回起動ウィザードと権限案内を追加し、初期設定をスムーズにしました。' } },
      { type: 'feat', text: { 'zh-CN': '材质包双版本双向深度转换工坊，支持 Blockstates 与模型重构、动画帧安全展开与熔断。', en: 'Two-way resource pack conversion workshop with blockstates/model reconstruction and safe animation frame expansion.', ja: 'リソースパックの双方向ディープ変換工房を追加。モデル再構築とアニメーションフレームの安全な展開に対応。' } },
      { type: 'feat', text: { 'zh-CN': 'MIDI 转红石音乐与超平坦工坊，支持音符盒多音轨解析、蛇形回路走线、LevelDB 区块直写。', en: 'MIDI to redstone music and superflat workshop with multi-track parsing, serpentine routing and direct LevelDB chunk writing.', ja: 'MIDI からレッドストーン音楽・超平坦工房を追加。音符ブロックのマルチトラック解析、蛇行配線、LevelDB チャンクへの直接書き込みに対応。' } },
      { type: 'feat', text: { 'zh-CN': '独立崩溃诊断与安全导出系统，采用独立进程展示，具备 Binder 溢出防护与 4KB 安全截断。', en: 'Standalone crash diagnostics and safe export running in an isolated process with Binder overflow protection.', ja: '分離プロセスで動作するクラッシュ診断と安全な書き出し機能を追加。' } },
      { type: 'feat', text: { 'zh-CN': '网易版存档被动加密能力与 LevelDB 异或流式解密。', en: 'Passive encryption and streaming XOR decryption for NetEase saves.', ja: 'NetEase 版セーブのパッシブ暗号化と LevelDB XOR ストリーミング復号に対応。' } },
      { type: 'fix', text: { 'zh-CN': '加固 LevelDB 与 NBT 核心引擎，修复缓存驱逐断言与小端字符数导致的损坏问题。', en: 'Hardened LevelDB and NBT engines, fixing cache eviction assertion and endian string issues.', ja: 'LevelDB と NBT エンジンを強化し、キャッシュ退避アサーションなどの不具合を修正。' } }
    ]
  }
]

export const roadmap = [
  {
    title: { 'zh-CN': '正式版签名与分发', en: 'Official signing and distribution', ja: '正式版の署名と配布' },
    desc: { 'zh-CN': '替换开发签名，建立正式发布与校验流程。', en: 'Replace the development signing key and establish an official release and verification process.', ja: '開発用署名を置き換え、正式リリースと検証の流れを整備します。' }
  },
  {
    title: { 'zh-CN': '更多语言支持', en: 'More languages', ja: '対応言語の拡充' },
    desc: { 'zh-CN': '在现有中 / 英 / 日基础上扩展更多界面语言。', en: 'Extend beyond the current Chinese / English / Japanese support.', ja: '現在の中国語 / 英語 / 日本語に加えて、対応言語をさらに増やします。' }
  },
  {
    title: { 'zh-CN': '转换引擎版本跟进', en: 'Keep pace with the engine', ja: '変換エンジンの追随' },
    desc: { 'zh-CN': '持续跟进上游 Chunker 引擎，扩展支持的游戏版本。', en: 'Follow the upstream Chunker engine to expand supported game versions.', ja: '上流の Chunker エンジンを追跡し、対応するゲームバージョンを拡大します。' }
  },
  {
    title: { 'zh-CN': '性能与体积优化', en: 'Performance and size', ja: '性能とサイズの最適化' },
    desc: { 'zh-CN': '优化运行环境体积与大型存档的内存占用。', en: 'Optimize the runtime footprint and memory use on large worlds.', ja: '実行環境のサイズと、大きなワールドでのメモリ使用量を最適化します。' }
  }
]
