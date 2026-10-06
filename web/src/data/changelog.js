

export const changelog = [
  {
    version: 'CANARY 0.5',
    code: 5,
    status: 'current',
    highlight: {
      'zh-CN': '材质包双向转换、MIDI 红石音乐工坊、崩溃诊断系统',
      en: 'Two-way resource pack conversion, MIDI redstone music workshop, crash diagnostics',
      ja: 'リソースパック双方向変換、MIDI レッドストーン音楽工房、クラッシュ診断'
    },
    date: {
      'zh-CN': '开发预览',
      en: 'Development preview',
      ja: '開発プレビュー'
    },
    items: [
      { type: 'feat', text: { 'zh-CN': '新增材质包双版本双向深度转换工坊，支持 Blockstates 与模型重构、动画帧安全展开与熔断。', en: 'Added a deep two-way resource pack conversion workshop with blockstate and model reconstruction, plus safe animation frame expansion and circuit breaking.', ja: 'リソースパックの双方向ディープ変換工房を追加。Blockstates とモデルの再構築、アニメーションフレームの安全な展開と上限チェックに対応。' } },
      { type: 'feat', text: { 'zh-CN': '新增 MIDI 转红石音乐与超平坦工坊，支持音符盒多音轨解析、蛇形回路走线、LevelDB 区块直写。', en: 'Added a MIDI to redstone music and superflat workshop with multi-track note block parsing, serpentine routing and direct LevelDB chunk writing.', ja: 'MIDI からレッドストーン音楽・超平坦工房を追加。音符ブロックのマルチトラック解析、蛇行配線、LevelDB チャンクへの直接書き込みに対応。' } },
      { type: 'feat', text: { 'zh-CN': '新增独立崩溃诊断与安全导出系统，采用独立进程展示，具备 Binder 溢出防护与 4KB 安全截断。', en: 'Added a standalone crash diagnostics and safe export system running in an isolated process, with Binder overflow protection and 4KB safe truncation.', ja: '独立したクラッシュ診断と安全な書き出し機能を追加。分離プロセスで表示し、Binder オーバーフロー対策と 4KB の安全な切り詰めを実装。' } },
      { type: 'feat', text: { 'zh-CN': '新增后台通知权限与省电策略申请体系，支持厂商多级省电设置跳转。', en: 'Added a background notification permission and power-saving request system with multi-level vendor power setting navigation.', ja: 'バックグラウンド通知権限と省電力設定のリクエスト体系を追加。メーカーごとの多段階な省電力設定への遷移に対応。' } },
      { type: 'feat', text: { 'zh-CN': '新增网易版存档被动加密能力。', en: 'Added passive encryption for NetEase saves.', ja: 'NetEase 版セーブのパッシブ暗号化に対応。' } },
      { type: 'fix', text: { 'zh-CN': '修复 LevelDB 读取触发缓存驱逐断言导致的崩溃，并新增回归测试。', en: 'Fixed a crash caused by a cache eviction assertion during LevelDB reads, with a regression test added.', ja: 'LevelDB 読み取り時にキャッシュ退避のアサーションで発生するクラッシュを修正し、回帰テストを追加。' } },
      { type: 'fix', text: { 'zh-CN': '修复小端 NBT 写入 UTF 字符串使用字符数而非字节长度导致的 level.dat 损坏问题。', en: 'Fixed level.dat corruption caused by little-endian NBT writing UTF strings by character count instead of byte length.', ja: 'リトルエンディアン NBT で UTF 文字列をバイト長ではなく文字数で書き込み、level.dat が壊れる問題を修正。' } },
      { type: 'fix', text: { 'zh-CN': '加固 MIDI 解析与材质包解压安全性，补充路径穿越防护与资源配额限制。', en: 'Hardened MIDI parsing and resource pack extraction, adding path traversal protection and resource quotas.', ja: 'MIDI 解析とリソースパック展開の安全性を強化し、パストラバーサル対策とリソース上限を追加。' } }
    ]
  },
  {
    version: 'CANARY 0.4',
    code: 4,
    status: 'past',
    highlight: {
      'zh-CN': '专业 NBT / LevelDB 编辑器、语义标签、2D 区块栅格',
      en: 'Professional NBT / LevelDB editor, semantic labels, 2D chunk grid',
      ja: '本格的な NBT / LevelDB エディタ、意味ラベル、2D チャンクグリッド'
    },
    date: { 'zh-CN': '历史版本', en: 'Previous release', ja: '過去のバージョン' },
    items: [
      { type: 'feat', text: { 'zh-CN': '新增专业 NBT / LevelDB 可视化编辑器（全格式解析、结构树增删改查、大数组分页编辑）。', en: 'Added a professional NBT / LevelDB visual editor with full format parsing, structure tree CRUD and paged editing of large arrays.', ja: '本格的な NBT / LevelDB ビジュアルエディタを追加（全形式の解析、構造ツリーの追加・編集・削除、巨大配列のページ編集）。' } },
      { type: 'feat', text: { 'zh-CN': '新增 Minecraft 智能语义分析与作用标签，降低新手修改门槛。', en: 'Added Minecraft semantic analysis and purpose labels, lowering the barrier for newcomers.', ja: 'Minecraft の意味解析と用途ラベルを追加し、初心者でも扱いやすくしました。' } },
      { type: 'feat', text: { 'zh-CN': '新增 2D 区块栅格可视化与精准瘦身，支持框选区块与停滞分析。', en: 'Added 2D chunk grid visualization with precise pruning, box selection and stagnation analysis.', ja: '2D チャンクグリッド表示と精密な整理を追加。範囲選択と滞留分析に対応。' } },
      { type: 'feat', text: { 'zh-CN': '新增独立维度裁剪与存档瘦身工作台，提供多档预设与自定义维度开关。', en: 'Added a standalone dimension pruning and world slimming workbench with multiple presets and per-dimension toggles.', ja: 'ディメンション整理とワールド軽量化の専用ワークベンチを追加。複数のプリセットとディメンション個別の切り替えに対応。' } }
    ]
  },
  {
    version: 'CANARY 0.3',
    code: 3,
    status: 'past',
    highlight: {
      'zh-CN': '版本映射体系、多语言架构、前台保活服务',
      en: 'Version mapping system, i18n architecture, foreground service',
      ja: 'バージョン対応表、多言語アーキテクチャ、フォアグラウンドサービス'
    },
    date: { 'zh-CN': '历史版本', en: 'Previous release', ja: '過去のバージョン' },
    items: [
      { type: 'feat', text: { 'zh-CN': '新增版本升降级基础映射体系与兼容风险评估，支持同代版本等价推荐。', en: 'Added a version upgrade/downgrade mapping system with compatibility risk assessment and equivalent-version recommendations.', ja: 'バージョンのアップ / ダウングレード対応表と互換性リスク評価を追加。同世代の対応バージョン提案に対応。' } },
      { type: 'feat', text: { 'zh-CN': '建立规范化多语言架构（简体中文 / English / 日本語），支持应用内动态切换。', en: 'Established a standardized multilingual architecture (Simplified Chinese / English / Japanese) with in-app switching.', ja: '標準化された多言語アーキテクチャ（簡体字中国語 / 英語 / 日本語）を構築し、アプリ内での動的切り替えに対応。' } },
      { type: 'feat', text: { 'zh-CN': '转换任务搭载前台保活通知服务，并提供转换历史管理与容灾补救导出。', en: 'Conversion jobs now run under a foreground service, with conversion history and recovery export.', ja: '変換処理をフォアグラウンドサービスで維持し、変換履歴の管理と復旧用エクスポートを追加。' } }
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
