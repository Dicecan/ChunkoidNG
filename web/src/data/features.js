

export const site = {
  name: 'Chunkoid NG',
  fullName: 'Chunkoid NG (Next-Generation)',
  version: 'CANARY 0.5',
  versionCode: 5,
  license: 'GPLv3',
  repo: 'https://github.com/Dicecan/ChunkoidNG',
  author: 'Dozener (DozenesStudio)',
  maintainer: 'DICECAN (EncoreTeam)',
  minAndroid: 'Android 8.0 (API 27)',
  targetAndroid: 'Android 15 (API 35)',
  arch: 'arm64-v8a',
  engine: 'Chunker CLI (The Hive, MIT)',
  runtime: 'OpenJDK 17 (aarch64 Linux RootFS)'
}

export const release = {
  published: false,
  apkUrl: '',
  sha256: '',
  note: {
    'zh-CN': 'CANARY 0.5 为开发预览版本，正式发布包发布后此处会更新下载地址与校验值。',
    en: 'CANARY 0.5 is a development preview build. The download URL and checksum will be published here once an official release is available.',
    ja: 'CANARY 0.5 は開発プレビュービルドです。正式リリースの公開後、ここにダウンロード先とチェックサムを掲載します。'
  },
  size: {
    'zh-CN': '约 56 MB',
    en: 'about 56 MB',
    ja: '約 56 MB'
  }
}

export const features = [
  {
    id: 'world_converter',
    accent: '#2f7d63',
    'zh-CN': {
      title: '存档转换',
      badge: '核心',
      summary: '基岩版 (BE) ↔ Java 版 (JE) 双向转换，支持 1.8.8+ 至 1.21+。',
      tags: ['双向转换', '无损', '跨版本'],
      detail: [
        '支持 Java 版与基岩版世界存档双向互转，可自由指定目标版本，经典版本（1.8.8 / 1.12.2 / 1.16.5 / 1.18.2 / 1.20.4 / 1.21.11 等）已标记为常用项。',
        '内置版本升降级映射体系：按游戏世代（epoch）推荐双端等价基线版本（如 Java 1.20.4 ↔ 基岩 1.20.80），并对跨代降级给出兼容风险预警。',
        '转换核心为 Chunker CLI（The Hive 开源项目），在应用内置的 OpenJDK 17 沙箱中运行，无需电脑、无需联网。',
        '转换任务搭载 Android 前台保活服务，长时间转换不易被杀进程；提供转换历史与容灾补救导出。',
        '输出先落盘到临时目录并校验产物完整性，通过后以重命名方式原子提交，避免产生半成品存档。'
      ],
      notes: [
        '跨版本降级（尤其涉及 1.18 世界高度扩展、1.13 方块扁平化）存在不可逆的数据损失风险，转换前请备份。',
        '转换结果建议在游戏内先验证，再用于正式存档。'
      ]
    },
    en: {
      title: 'World Conversion',
      badge: 'Core',
      summary: 'Two-way Bedrock (BE) ↔ Java (JE) conversion, supporting 1.8.8+ through 1.21+.',
      tags: ['Two-way', 'Lossless', 'Cross-version'],
      detail: [
        'Convert worlds in both directions between Java and Bedrock, freely choosing the target version. Classic versions (1.8.8 / 1.12.2 / 1.16.5 / 1.18.2 / 1.20.4 / 1.21.11 and more) are marked as popular choices.',
        'A built-in version mapping system recommends equivalent baseline versions per game epoch (for example Java 1.20.4 ↔ Bedrock 1.20.80) and warns about compatibility risks when downgrading across epochs.',
        'Conversion is powered by Chunker CLI (an open-source project by The Hive) running inside the bundled OpenJDK 17 sandbox — no computer and no network required.',
        'Jobs run under an Android foreground service so long conversions are less likely to be killed. Conversion history and recovery export are provided.',
        'Output is written to a temporary directory and verified for completeness, then committed atomically via rename, so a failed run never leaves a half-written world.'
      ],
      notes: [
        'Downgrading across versions (especially involving the 1.18 world height expansion or the 1.13 block flattening) carries a risk of irreversible data loss. Back up before converting.',
        'Verify the result in-game before using it as your main world.'
      ]
    },
    ja: {
      title: 'ワールド変換',
      badge: '中核',
      summary: '統合版 (BE) ↔ Java 版 (JE) の双方向変換。1.8.8+ から 1.21+ まで対応。',
      tags: ['双方向', 'ロスレス', 'バージョン間'],
      detail: [
        'Java 版と統合版のワールドを双方向に変換でき、変換先のバージョンを自由に指定できます。定番バージョン（1.8.8 / 1.12.2 / 1.16.5 / 1.18.2 / 1.20.4 / 1.21.11 など）はよく使う項目として表示されます。',
        'バージョン対応表を内蔵し、ゲーム世代（epoch）ごとに対応する基準バージョンを提案します（例：Java 1.20.4 ↔ 統合版 1.20.80）。世代をまたぐダウングレード時には互換性リスクを警告します。',
        '変換の中核は Chunker CLI（The Hive のオープンソースプロジェクト）で、アプリ内蔵の OpenJDK 17 サンドボックス上で動作します。PC もネットワークも不要です。',
        '変換処理は Android のフォアグラウンドサービスで維持されるため、長時間の変換でもプロセスが終了されにくくなっています。変換履歴と復旧用のエクスポートも用意されています。',
        '出力はまず一時ディレクトリへ書き込み、内容を検証したうえでリネームによりアトミックに確定するため、不完全なワールドが残りません。'
      ],
      notes: [
        'バージョンをまたぐダウングレード（特に 1.18 のワールド高さ拡張や 1.13 のブロック平坦化が関わる場合）は、元に戻せないデータ損失の恐れがあります。変換前に必ずバックアップしてください。',
        '変換結果は、本番のワールドとして使う前にゲーム内で確認することをおすすめします。'
      ]
    }
  },
  {
    id: 'netease_decryptor',
    accent: '#6b5bd2',
    'zh-CN': {
      title: '网易存档解密与加密',
      badge: '独家黑科技',
      summary: 'LevelDB 异或算法流式解密，并支持网易版被动加密。',
      tags: ['流式解密', '被动加密', '全自动'],
      detail: [
        '针对网易版 Minecraft 存档的加密格式，实现基于异或的流式解密，支持大存档低内存处理。',
        '通过 db/CURRENT 与 MANIFEST 文件推导密钥，并使用候选密钥反向验证，验证失败时明确报错而非静默输出损坏数据。',
        '支持校验 LevelDB 数据表尾部魔数，解密结果可确认完整性。',
        '同时提供反向的被动加密能力，可将普通存档转换为可被网易版识别的加密存档。',
        '解密完成后可一键流转到世界转换器，衔接后续处理流程。'
      ],
      notes: [
        '仅建议对自己拥有合法使用权的存档使用。',
        '被动加密后的存档请先在游戏内确认可正常读取。'
      ]
    },
    en: {
      title: 'NetEase Decryption & Encryption',
      badge: 'Exclusive',
      summary: 'Streaming XOR decryption for LevelDB saves, plus passive encryption for NetEase builds.',
      tags: ['Streaming', 'Passive encrypt', 'Automatic'],
      detail: [
        'Implements streaming XOR-based decryption for the encrypted save format used by NetEase Minecraft, handling large worlds with low memory usage.',
        'Derives the key from the db/CURRENT and MANIFEST files and validates it against candidate keys. If validation fails, it reports an explicit error instead of silently emitting corrupted data.',
        'Verifies the LevelDB table magic at the end of data files so decrypted output can be confirmed intact.',
        'Also provides the reverse capability — passive encryption — to turn a regular world into an encrypted save that NetEase builds can recognize.',
        'Once decrypted, the result can be handed straight to the world converter to continue the workflow.'
      ],
      notes: [
        'Only use this with worlds you are legally entitled to use.',
        'After passive encryption, confirm in-game that the world loads correctly.'
      ]
    },
    ja: {
      title: 'NetEase セーブデータの復号と暗号化',
      badge: '独自機能',
      summary: 'LevelDB の XOR ストリーミング復号に対応し、NetEase 版向けの暗号化も可能。',
      tags: ['ストリーミング復号', '暗号化', '自動'],
      detail: [
        'NetEase 版 Minecraft の暗号化されたセーブ形式に対し、XOR ベースのストリーミング復号を実装しています。大きなワールドも省メモリで処理できます。',
        'db/CURRENT と MANIFEST ファイルから鍵を導出し、候補となる鍵で逆検証します。検証に失敗した場合は、壊れたデータを黙って出力せず、明確にエラーを報告します。',
        'LevelDB のデータテーブル末尾のマジック値を検証するため、復号結果の完全性を確認できます。',
        '逆方向の「パッシブ暗号化」も提供しており、通常のワールドを NetEase 版が認識できる暗号化セーブに変換できます。',
        '復号が完了したら、そのままワールド変換へ引き継いで後続の処理を行えます。'
      ],
      notes: [
        'ご自身が正当な利用権を持つワールドにのみ使用してください。',
        '暗号化したワールドは、まずゲーム内で正常に読み込めることを確認してください。'
      ]
    }
  },
  {
    id: 'dimension_pruner',
    accent: '#c2762a',
    'zh-CN': {
      title: '维度与区块裁剪',
      badge: '智能瘦身',
      summary: '智能剔除未修改、无效的区块，精简地图体积。',
      tags: ['建筑师预设', '出生点保护', '防闪退'],
      detail: [
        '提供「全维度保留」「仅主世界」「极速轻量化」「自定义维度」四档预设，各维度均可独立开关。',
        '支持保留原始 NBT（-k）模式，在不破坏结构的前提下剥离冗余区块。',
        '面向建筑师与服务器玩家，可显著压缩存档体积、加快加载速度。',
        '裁剪任务同样运行在前台保活服务中，长时间处理更稳妥。'
      ],
      notes: [
        '裁剪会永久删除被剔除的区块数据，操作前务必备份存档。',
        '请在游戏内确认出生点、红石机械与重要建筑所在区块未被误删。'
      ]
    },
    en: {
      title: 'Dimension & Chunk Pruning',
      badge: 'Smart Prune',
      summary: 'Intelligently strip unmodified and invalid chunks to shrink world size.',
      tags: ['Builder presets', 'Spawn protection', 'Crash-safe'],
      detail: [
        'Four presets are provided — keep all dimensions, overworld only, aggressive lightweight, and custom — with each dimension toggled independently.',
        'Supports a keep-original-NBT (-k) mode that strips redundant chunks without breaking structure.',
        'Aimed at builders and server players, it can substantially reduce world size and speed up loading.',
        'Pruning jobs also run under the foreground service, making long operations more reliable.'
      ],
      notes: [
        'Pruning permanently deletes the stripped chunk data. Always back up the world first.',
        'Check in-game that the spawn area, redstone contraptions and important builds were not removed.'
      ]
    },
    ja: {
      title: 'ディメンションとチャンクの整理',
      badge: 'スマート整理',
      summary: '未変更・無効なチャンクを賢く除去し、ワールド容量を削減します。',
      tags: ['建築向けプリセット', 'スポーン保護', 'クラッシュ対策'],
      detail: [
        '「全ディメンション保持」「オーバーワールドのみ」「高速軽量化」「カスタム」の 4 種類のプリセットを用意し、各ディメンションを個別に切り替えられます。',
        '元の NBT を保持する（-k）モードに対応し、構造を壊さずに不要なチャンクだけを除去できます。',
        '建築勢やサーバー運営者向けで、ワールド容量の大幅な削減と読み込み速度の向上が期待できます。',
        '整理処理もフォアグラウンドサービス上で動作するため、長時間の処理でも安定します。'
      ],
      notes: [
        '整理すると除去されたチャンクデータは復元できません。作業前に必ずバックアップしてください。',
        'スポーン地点、レッドストーン装置、重要な建築物のチャンクが消えていないかゲーム内で確認してください。'
      ]
    }
  },
  {
    id: 'nbt_editor',
    accent: '#2f6fa8',
    'zh-CN': {
      title: 'NBT / LevelDB 编辑',
      badge: '专业工具',
      summary: '层级树形可视化查看，实时增删改查 NBT 与 LevelDB 键值。',
      tags: ['树形折叠', 'Hex 预览', '免解压'],
      detail: [
        '支持单文件 NBT（.dat / .nbt / level.dat）、MCA 区域文件（.mca / .mcr）以及 LevelDB 数据库（db/ 目录）的解析与结构树展开。',
        '内置 Minecraft 语义描述引擎，自动识别玩家属性（生命、饥饿、经验、背包）、实体与方块实体（箱子内容、告示牌）、世界核心规则，并给出双语的用途标签与合法数值范围引导。',
        '针对大型 ByteArray / IntArray / LongArray（生物群系网格、高度图、调色板）实现动态分页加载与十六进制 / 十进制展示，避免界面卡顿与内存溢出。',
        'LevelDB 记录按玩家、实体、方块实体、世界、区块等类别归类检索，并支持 2D 区块栅格可视化框选。',
        '采用纯 Java 的 LevelDB 引擎并补充原生 ZLib 压缩桥接，规避 JNI 内存映射导致的崩溃问题。'
      ],
      notes: [
        '直接编辑 NBT 可能导致存档无法被游戏读取，修改前请备份。',
        '建议在理解字段含义后再修改数值。'
      ]
    },
    en: {
      title: 'NBT / LevelDB Editor',
      badge: 'Pro Tool',
      summary: 'Tree view visualization with live create, read, update and delete of NBT and LevelDB entries.',
      tags: ['Tree folding', 'Hex preview', 'No unzip'],
      detail: [
        'Parses and expands single-file NBT (.dat / .nbt / level.dat), MCA region files (.mca / .mcr) and LevelDB databases (the db/ directory) into a navigable structure tree.',
        'A built-in Minecraft semantic descriptor identifies player attributes (health, hunger, experience, inventory), entities and block entities (chest contents, signs) and core world rules, surfacing bilingual purpose labels and valid value ranges.',
        'Large ByteArray / IntArray / LongArray values (biome grids, heightmaps, palettes) load through dynamic paging with hex and decimal views, avoiding UI stalls and out-of-memory crashes.',
        'LevelDB records are categorized for browsing by player, entity, block entity, world and chunk, with 2D chunk grid visualization and box selection.',
        'Uses a pure-Java LevelDB engine with a native ZLib compression bridge to avoid crashes caused by JNI memory mapping.'
      ],
      notes: [
        'Editing NBT directly can make a world unreadable by the game. Back up before making changes.',
        'Make sure you understand what a field does before changing its value.'
      ]
    },
    ja: {
      title: 'NBT / LevelDB エディタ',
      badge: 'プロ向け',
      summary: 'ツリー表示で可視化し、NBT と LevelDB のキーをその場で追加・編集・削除できます。',
      tags: ['ツリー折りたたみ', 'Hex 表示', '解凍不要'],
      detail: [
        '単一ファイルの NBT（.dat / .nbt / level.dat）、MCA リージョンファイル（.mca / .mcr）、LevelDB データベース（db/ ディレクトリ）を解析し、構造ツリーとして展開できます。',
        'Minecraft の意味記述エンジンを内蔵し、プレイヤー属性（体力・満腹度・経験値・インベントリ）、エンティティとブロックエンティティ（チェストの中身・看板）、ワールドの主要ルールを自動判別して、用途ラベルと有効な数値範囲を表示します。',
        '巨大な ByteArray / IntArray / LongArray（バイオーム格子・ハイトマップ・パレット）は動的なページ読み込みと 16 進 / 10 進表示に対応し、UI の固まりやメモリ不足を防ぎます。',
        'LevelDB のレコードはプレイヤー・エンティティ・ブロックエンティティ・ワールド・チャンクなどのカテゴリ別に閲覧でき、2D チャンクグリッドでの範囲選択にも対応します。',
        '純 Java の LevelDB エンジンを採用し、ネイティブ ZLib 圧縮ブリッジを追加することで、JNI のメモリマップに起因するクラッシュを回避しています。'
      ],
      notes: [
        'NBT を直接編集すると、ゲームがワールドを読み込めなくなる場合があります。変更前にバックアップしてください。',
        '各フィールドの意味を理解したうえで値を変更することをおすすめします。'
      ]
    }
  },
  {
    id: 'pack_converter',
    accent: '#b8477f',
    'zh-CN': {
      title: '材质包双向转换',
      badge: '资源工具',
      summary: '自动转换双端语言文件、音效配置及 manifest。',
      tags: ['贴图映射', 'UUID 生成', '自动修复'],
      detail: [
        '支持 Java 版与基岩版材质包全链路双向转换，并输出转换日志与进度。',
        '自动转换 Blockstates 与自定义方块 / 实体模型结构，适配双端贴图的命名与格式差异。',
        '自动将 Java 版 .png.mcmeta 动画贴图合成为基岩版垂直帧带纹理，并内置帧数与尺寸溢出熔断，避免极端高分辨率动画导致内存崩溃。',
        '语言文件（Java JSON ↔ 基岩 .lang）、音效配置与 manifest 自动转换，可生成新的 UUID。',
        '解压环节具备完整的路径穿越（Zip Slip）防护与条目数、体积配额限制。'
      ],
      notes: [
        '复杂的自定义模型或着色器可能无法完全等价转换。',
        '转换后请在游戏内启用材质包确认贴图与音效正常。'
      ]
    },
    en: {
      title: 'Resource Pack Conversion',
      badge: 'Resource Tool',
      summary: 'Automatically converts language files, sound configuration and manifests for both platforms.',
      tags: ['Texture mapping', 'UUID generation', 'Auto fix'],
      detail: [
        'Full two-way conversion of resource packs between Java and Bedrock, with conversion logs and progress output.',
        'Automatically converts blockstates and custom block / entity model structures, adapting to the naming and format differences between the two platforms.',
        'Automatically composites Java .png.mcmeta animated textures into Bedrock vertical frame strips, with built-in circuit breakers on frame count and size to prevent out-of-memory crashes from extreme high-resolution animations.',
        'Language files (Java JSON ↔ Bedrock .lang), sound configuration and manifests are converted automatically, and a new UUID can be generated.',
        'The extraction stage includes complete Zip Slip protection along with entry-count and size quotas.'
      ],
      notes: [
        'Complex custom models or shaders may not convert to a fully equivalent result.',
        'After converting, enable the pack in-game to confirm textures and sounds look right.'
      ]
    },
    ja: {
      title: 'リソースパックの双方向変換',
      badge: 'リソース',
      summary: '両プラットフォームの言語ファイル、サウンド設定、manifest を自動変換します。',
      tags: ['テクスチャ対応', 'UUID 生成', '自動修復'],
      detail: [
        'Java 版と統合版のリソースパックを全工程で双方向に変換し、変換ログと進捗を表示します。',
        'Blockstates やカスタムブロック / エンティティのモデル構造を自動変換し、両プラットフォームの命名・形式の違いを吸収します。',
        'Java 版の .png.mcmeta アニメーションテクスチャを統合版の縦フレーム帯テクスチャに自動合成します。フレーム数とサイズの上限チェックを内蔵し、極端な高解像度アニメによるメモリ不足を防ぎます。',
        '言語ファイル（Java JSON ↔ 統合版 .lang）、サウンド設定、manifest を自動変換し、新しい UUID を生成できます。',
        '展開処理には完全なパストラバーサル（Zip Slip）対策と、エントリ数・サイズの上限を実装しています。'
      ],
      notes: [
        '複雑なカスタムモデルやシェーダーは、完全に同等な形へ変換できない場合があります。',
        '変換後はゲーム内でリソースパックを有効にし、テクスチャとサウンドを確認してください。'
      ]
    }
  },
  {
    id: 'midi_converter',
    accent: '#b0453c',
    'zh-CN': {
      title: 'MIDI 转红石音乐',
      badge: '音乐工坊',
      summary: 'MIDI 转物理红石音乐机、超平坦即听世界及指令包。',
      tags: ['超平坦世界', '世界注入', 'NBS / 结构'],
      detail: [
        '精准解析 MIDI 音轨，将音高与乐器映射至对应基座方块与音符盒音阶（0-24），支持自动移调以适配红石音域。',
        '采用紧凑的蛇形（S 型）回路连线，规避传统单向布局的转弯断路隐患，并保持接近原曲的播放节拍。',
        '支持直接在底层 LevelDB 区块数据库写入超平坦音乐世界，生成可进入即可收听的世界存档。',
        '同时提供红石机械结构导出（.mcstructure / .schem）、NBS 文件与数据包指令等多种产出形式。',
        '兼容 1.21+ 的现代方块状态。'
      ],
      notes: [
        '大型 MIDI 会生成规模可观的区块，转换与导入耗时较长。',
        '红石音乐对游戏刻数与区块加载较敏感，建议在超平坦或空置维度中播放。'
      ]
    },
    en: {
      title: 'MIDI to Redstone Music',
      badge: 'Music Workshop',
      summary: 'Turn MIDI into physical redstone music machines, instant superflat worlds and command packs.',
      tags: ['Superflat world', 'World inject', 'NBS / structure'],
      detail: [
        'Parses MIDI tracks precisely, mapping pitch and instrument to the appropriate base block and note block pitch (0-24), with automatic transposition to fit the redstone range.',
        'Uses a compact serpentine (S-shaped) routing layout that avoids the broken-corner hazards of traditional one-way designs while keeping playback close to the original tempo.',
        'Can write a superflat music world directly into the underlying LevelDB chunk database, producing a world you can enter and immediately listen to.',
        'Also exports redstone contraption structures (.mcstructure / .schem), NBS files and datapack commands.',
        'Compatible with 1.21+ modern block states.'
      ],
      notes: [
        'Large MIDI files generate a considerable number of chunks, so conversion and import take longer.',
        'Redstone music is sensitive to game ticks and chunk loading; playing in a superflat world or an empty dimension is recommended.'
      ]
    },
    ja: {
      title: 'MIDI からレッドストーン音楽へ',
      badge: '音楽工房',
      summary: 'MIDI を物理的なレッドストーン音楽装置、超平坦ワールド、コマンドパックに変換します。',
      tags: ['超平坦ワールド', 'ワールド注入', 'NBS / 構造物'],
      detail: [
        'MIDI のトラックを正確に解析し、音高と楽器を対応する土台ブロックと音符ブロックの音階（0-24）に割り当てます。レッドストーンの音域に合わせた自動移調にも対応します。',
        'コンパクトな蛇行（S 字）配線を採用し、従来の片方向レイアウトで起きがちな曲がり角の断線を避けつつ、原曲に近いテンポを保ちます。',
        'LevelDB のチャンクデータベースへ直接書き込むことで超平坦な音楽ワールドを生成でき、入ってすぐに聴けるワールドになります。',
        'レッドストーン装置の構造物（.mcstructure / .schem）、NBS ファイル、データパックのコマンドなど、複数の出力形式に対応します。',
        '1.21+ の新しいブロック状態にも対応しています。'
      ],
      notes: [
        '大きな MIDI はかなりの数のチャンクを生成するため、変換と取り込みに時間がかかります。',
        'レッドストーン音楽はゲームティックとチャンクロードの影響を受けやすいため、超平坦ワールドや空のディメンションでの再生をおすすめします。'
      ]
    }
  },
  {
    id: 'sandbox_terminal',
    accent: '#4a5568',
    'zh-CN': {
      title: '沙箱终端控制台',
      badge: '极客模式',
      summary: '直通底层 Linux 沙箱与 OpenJDK 17，支持自定义 CLI 命令。',
      tags: ['OpenJDK 17', 'Shell', '手势缩放'],
      detail: [
        '内置精简的 Linux RootFS 沙箱与 OpenJDK 17 运行环境，可直接执行 Java 命令与 Shell 命令。',
        '终端界面支持手势缩放、日志复制与快速清屏，便于排查转换过程中的问题。',
        '作为转换引擎的底层支撑，也可用于手动调用 Chunker CLI 做高级操作。'
      ],
      notes: [
        '终端具备直接操作应用工作目录的能力，请谨慎执行破坏性命令。',
        'RootFS 可在设置中重置，用于修复沙箱环境损坏的情况。'
      ]
    },
    en: {
      title: 'Sandbox Terminal',
      badge: 'Geek Mode',
      summary: 'Direct access to the underlying Linux sandbox and OpenJDK 17, with custom CLI commands.',
      tags: ['OpenJDK 17', 'Shell', 'Pinch zoom'],
      detail: [
        'Ships a minimal Linux RootFS sandbox with an OpenJDK 17 runtime, letting you run Java and shell commands directly.',
        'The terminal supports pinch zoom, log copying and quick clearing, making it easier to diagnose problems during conversion.',
        'It underpins the conversion engine and can also be used to invoke Chunker CLI manually for advanced operations.'
      ],
      notes: [
        'The terminal can operate directly on the app working directory. Be careful with destructive commands.',
        'The RootFS can be reset in settings to repair a corrupted sandbox environment.'
      ]
    },
    ja: {
      title: 'サンドボックス端末',
      badge: 'ギーク向け',
      summary: '基盤の Linux サンドボックスと OpenJDK 17 に直接アクセスし、CLI コマンドを実行できます。',
      tags: ['OpenJDK 17', 'Shell', 'ピンチズーム'],
      detail: [
        '軽量な Linux RootFS サンドボックスと OpenJDK 17 実行環境を内蔵し、Java コマンドやシェルコマンドを直接実行できます。',
        '端末画面はピンチズーム、ログのコピー、素早いクリアに対応し、変換中の問題の切り分けに役立ちます。',
        '変換エンジンを支える基盤であり、Chunker CLI を手動で呼び出して高度な操作を行うこともできます。'
      ],
      notes: [
        '端末はアプリの作業ディレクトリを直接操作できます。破壊的なコマンドの実行には注意してください。',
        'RootFS は設定からリセットでき、サンドボックス環境が壊れた場合の修復に使えます。'
      ]
    }
  }
]

export function findFeature(id) {
  return features.find((f) => f.id === id)
}
