export const changelog = [
  {
    version: 'Beta 0.7',
    code: 7,
    status: 'upcoming',
    highlight: {
      'zh-CN': '全新 MAID™ 女仆安全与性能治理底座、温和截断策略与女仆巡检看板（开发中 · 未发布）',
      en: 'MAID™ governance architecture, gentle truncation policy and inspection dashboard (In Development / Unreleased)',
      ja: 'MAID™ メイド統合セキュリティ基盤、安全切詰ポリシー、巡回ダッシュボード（開発中・未リリース）'
    },
    date: {
      'zh-CN': '未发布（开发中）',
      en: 'Unreleased (In Dev)',
      ja: '未リリース（開発中）'
    },
    items: [
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
