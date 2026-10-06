

export const docPages = [
  {
    slug: 'getting-started',
    'zh-CN': {
      title: '快速开始',
      summary: '首次安装后需要了解的运行环境、权限与基本流程。',
      sections: [
        { heading: '安装与首次启动', paragraphs: ['Chunkoid NG 当前处于 Canary 开发预览阶段，请从下载页或项目仓库获取安装包。', '首次启动时，应用需要复制内置的 OpenJDK 17 运行环境（RootFS）到应用私有目录，此过程只需一次，请保持应用处于前台。'] },
        { heading: '权限说明', list: ['通知权限：用于展示转换进度与完成提醒（Android 13 及以上需要手动授予）。', '前台服务：保证长时间转换过程不被系统回收。', '电池优化白名单：可选，授予后长时间转换更稳定，部分厂商系统需要额外设置。', '存储访问：通过系统文件选择器（SAF）授权，应用只会访问你主动选择的目录，不会遍历整机存储。'] },
        { heading: '推荐工作流', list: ['在「功能」页进入「存档转换」。', '选择世界文件夹或 .zip / .mcworld 压缩包。', '等待应用读取世界信息，确认识别的平台与版本正确。', '选择目标版本，按需配置维度裁剪与世界设置。', '开始转换，保持应用在后台运行，等待完成通知。', '导出到目标目录，并在游戏内验证存档可正常读取。'] }
      ]
    },
    en: {
      title: 'Getting Started',
      summary: 'Runtime, permissions and the basic workflow to know after installing.',
      sections: [
        { heading: 'Installation and first launch', paragraphs: ['Chunkoid NG is currently in the Canary development preview stage. Get the installer from the download page or the repository.', 'On first launch the app needs to copy its bundled OpenJDK 17 runtime (RootFS) into the app private directory. This happens only once — keep the app in the foreground while it completes.'] },
        { heading: 'Permissions', list: ['Notifications: used to show conversion progress and completion alerts (must be granted manually on Android 13 and above).', 'Foreground service: keeps long conversions from being reclaimed by the system.', 'Battery optimization allowlist: optional. Granting it makes long conversions more stable; some vendor ROMs require extra steps.', 'Storage access: granted through the system file picker (SAF). The app only accesses directories you explicitly choose and never scans your entire storage.'] },
        { heading: 'Recommended workflow', list: ['Open "World Conversion" from the Features page.', 'Select a world folder or a .zip / .mcworld archive.', 'Wait for the app to read the world metadata and confirm the detected platform and version are correct.', 'Choose the target version and configure dimension pruning and world settings as needed.', 'Start the conversion, keep the app running in the background, and wait for the completion notification.', 'Export to your target directory and verify in-game that the world loads correctly.'] }
      ]
    },
    ja: {
      title: 'はじめに',
      summary: 'インストール後に知っておきたい実行環境、権限、基本的な流れ。',
      sections: [
        { heading: 'インストールと初回起動', paragraphs: ['Chunkoid NG は現在 Canary の開発プレビュー段階です。インストーラはダウンロードページまたはリポジトリから入手してください。', '初回起動時、アプリは内蔵の OpenJDK 17 実行環境（RootFS）をアプリ専用ディレクトリへコピーします。この処理は一度だけなので、完了するまでアプリを前面に保ってください。'] },
        { heading: '権限について', list: ['通知：変換の進捗と完了を知らせるために使用します（Android 13 以降では手動での許可が必要です）。', 'フォアグラウンドサービス：長時間の変換がシステムに回収されないようにします。', '電池最適化の除外：任意です。許可すると長時間の変換が安定します。メーカー製 ROM では追加設定が必要な場合があります。', 'ストレージアクセス：システムのファイル選択（SAF）で許可します。アプリはあなたが選んだフォルダにのみアクセスし、端末全体を走査することはありません。'] },
        { heading: 'おすすめの流れ', list: ['「機能」ページから「ワールド変換」を開きます。', 'ワールドフォルダ、または .zip / .mcworld を選択します。', 'アプリがワールド情報を読み取るまで待ち、判定されたプラットフォームとバージョンが正しいか確認します。', '変換先のバージョンを選び、必要に応じてディメンション整理とワールド設定を行います。', '変換を開始し、アプリをバックグラウンドで動かしたまま完了通知を待ちます。', '目的のフォルダへ書き出し、ゲーム内で正常に読み込めることを確認します。'] }
      ]
    }
  },
  {
    slug: 'world-conversion',
    'zh-CN': {
      title: '存档转换',
      summary: 'Java 与基岩版世界双向转换的完整说明与注意事项。',
      sections: [
        { heading: '支持的范围', list: ['方向：Java 版 → 基岩版，基岩版 → Java 版。', '版本：约 1.8.8 至 1.21+，可在目标版本列表中自由选择。', '输入形式：世界文件夹、.zip 压缩包、.mcworld 文件。'] },
        { heading: '版本选择建议', paragraphs: ['应用会在读取世界后自动推荐一个「等价目标版本」，这是按游戏世代推荐的双端对应基线，通常是最稳妥的选择。', '若目标版本低于源版本（降级），应用会给出风险提示。涉及 1.18 的世界高度扩展或 1.13 的方块扁平化时，降级可能造成不可逆的数据损失。'] },
        { heading: '转换过程', paragraphs: ['转换由内置的 Chunker CLI 在 OpenJDK 17 沙箱中执行，全程离线。', '进度按引擎输出实时解析，同时提供原始日志视图，便于排查异常。', '产物会先写入临时目录并校验（需包含 level.dat 或 db/ 目录）后才正式落盘，失败时会自动清理临时文件。'] },
        { heading: '注意事项', list: ['务必在转换前备份原始存档。', '大型存档转换耗时较长，建议连接电源并使用前台服务模式。', '转换完成后请在游戏内实际进入一次，确认区块、实体与红石机械表现正常。'] }
      ]
    },
    en: {
      title: 'World Conversion',
      summary: 'Full documentation and caveats for two-way Java ↔ Bedrock world conversion.',
      sections: [
        { heading: 'Supported range', list: ['Directions: Java → Bedrock and Bedrock → Java.', 'Versions: roughly 1.8.8 through 1.21+, freely selectable from the target version list.', 'Inputs: world folders, .zip archives and .mcworld files.'] },
        { heading: 'Choosing a target version', paragraphs: ['After reading a world, the app recommends an "equivalent target version" — the paired baseline for that game epoch, and usually the safest choice.', 'If the target version is lower than the source (a downgrade), the app shows a risk warning. When the 1.18 world height expansion or the 1.13 block flattening is involved, downgrading can cause irreversible data loss.'] },
        { heading: 'How conversion runs', paragraphs: ['Conversion is performed by the bundled Chunker CLI inside the OpenJDK 17 sandbox, fully offline.', 'Progress is parsed live from engine output, and a raw log view is available for troubleshooting.', 'Output is first written to a temporary directory and validated (it must contain level.dat or a db/ directory) before being committed; temporary files are cleaned up automatically on failure.'] },
        { heading: 'Caveats', list: ['Always back up the original world before converting.', 'Large worlds take a long time; connecting a charger and using foreground service mode is recommended.', 'After conversion, actually load the world once in-game to confirm chunks, entities and redstone contraptions behave correctly.'] }
      ]
    },
    ja: {
      title: 'ワールド変換',
      summary: 'Java 版と統合版の双方向変換についての詳細と注意事項。',
      sections: [
        { heading: '対応範囲', list: ['方向：Java 版 → 統合版、統合版 → Java 版。', 'バージョン：約 1.8.8 〜 1.21+。変換先の一覧から自由に選択できます。', '入力形式：ワールドフォルダ、.zip、.mcworld ファイル。'] },
        { heading: '変換先バージョンの選び方', paragraphs: ['ワールドを読み取ると、アプリが「対応する基準バージョン」を自動で提案します。これはゲーム世代ごとの対応関係に基づくもので、通常は最も安全な選択です。', '変換先が元のバージョンより低い場合（ダウングレード）、アプリがリスクを警告します。1.18 のワールド高さ拡張や 1.13 のブロック平坦化が関わる場合、ダウングレードは元に戻せないデータ損失を招くことがあります。'] },
        { heading: '変換の流れ', paragraphs: ['変換は内蔵の Chunker CLI が OpenJDK 17 サンドボックス上で実行し、すべてオフラインで完結します。', '進捗はエンジンの出力からリアルタイムに解析され、生のログ表示も確認できるため、異常の切り分けに役立ちます。', '出力はまず一時ディレクトリに書き込まれ、検証（level.dat または db/ ディレクトリの存在を確認）を経てから確定します。失敗時は一時ファイルが自動で削除されます。'] },
        { heading: '注意事項', list: ['変換前に必ず元のワールドをバックアップしてください。', '大きなワールドの変換は時間がかかるため、電源に接続し、フォアグラウンドサービスを有効にすることをおすすめします。', '変換後はゲーム内で一度実際に入り、チャンク・エンティティ・レッドストーン装置が正常か確認してください。'] }
      ]
    }
  },
  {
    slug: 'netease',
    'zh-CN': {
      title: '网易存档解密',
      summary: '处理网易版加密存档，以及对普通存档进行被动加密。',
      sections: [
        { heading: '解密流程', list: ['进入「网易存档解密」。', '选择网易版存档所在的世界文件夹。', '应用会读取 db/CURRENT 与 MANIFEST 文件并自动推导密钥。', '解密完成后，可一键流转到世界转换器继续处理。'] },
        { heading: '关于密钥', paragraphs: ['应用会自动从存档元数据推导密钥，并使用候选密钥进行反向验证，验证通过才会继续写入。若验证失败，会明确报错而不会输出损坏的存档。', '如你已知自定义密钥，也可在界面中手动填写。'] },
        { heading: '被动加密', paragraphs: ['被动加密可将普通存档转换为网易版可识别的加密存档格式。加密后请务必先在游戏内确认存档可正常读取。'] }
      ]
    },
    en: {
      title: 'NetEase Decryption',
      summary: 'Handling encrypted NetEase saves, and passive encryption of regular worlds.',
      sections: [
        { heading: 'Decryption steps', list: ['Open "NetEase Decryption & Encryption".', 'Select the world folder containing the NetEase save.', 'The app reads db/CURRENT and the MANIFEST files and derives the key automatically.', 'Once decrypted, you can hand the result straight to the world converter.'] },
        { heading: 'About the key', paragraphs: ['The app derives the key from the save metadata and validates it by reverse-checking candidate keys. It only proceeds once validation passes. If validation fails it reports an explicit error instead of writing corrupted output.', 'If you already know a custom key, you can enter it manually in the UI.'] },
        { heading: 'Passive encryption', paragraphs: ['Passive encryption converts a regular world into the encrypted save format recognized by NetEase builds. After encrypting, always confirm in-game that the world loads correctly.'] }
      ]
    },
    ja: {
      title: 'NetEase セーブデータの復号',
      summary: 'NetEase 版の暗号化セーブの扱いと、通常ワールドの暗号化について。',
      sections: [
        { heading: '復号の手順', list: ['「NetEase セーブデータの復号と暗号化」を開きます。', 'NetEase 版のセーブがあるワールドフォルダを選択します。', 'アプリが db/CURRENT と MANIFEST ファイルを読み取り、鍵を自動で導出します。', '復号が完了したら、そのままワールド変換へ引き継げます。'] },
        { heading: '鍵について', paragraphs: ['アプリはセーブのメタデータから鍵を導出し、候補となる鍵で逆検証します。検証に通った場合のみ処理を続行します。検証に失敗した場合は、壊れたデータを出力せず明確にエラーを報告します。', '独自の鍵をご存知の場合は、画面から手動で入力することもできます。'] },
        { heading: 'パッシブ暗号化', paragraphs: ['パッシブ暗号化を使うと、通常のワールドを NetEase 版が認識できる暗号化セーブ形式に変換できます。暗号化後は必ずゲーム内で正常に読み込めることを確認してください。'] }
      ]
    }
  },
  {
    slug: 'pruner',
    'zh-CN': {
      title: '维度与区块裁剪',
      summary: '压缩存档体积、剔除冗余区块的预设与风险说明。',
      sections: [
        { heading: '预设说明', list: ['全维度保留：保留主世界、下界与末地，不做裁剪，适合需要完整世界的场景。', '仅主世界：剔除下界与末地，常用于只保留建筑作品的场景。', '极速轻量化：仅保留主世界核心内容，体积压缩最明显。', '自定义维度：逐项勾选需要保留的维度。'] },
        { heading: '保留原始 NBT', paragraphs: ['开启该选项后，裁剪过程会保留原始 NBT 结构，适用于希望尽量维持存档结构完整性的场景。'] },
        { heading: '风险提示', paragraphs: ['裁剪是不可逆操作，被剔除的区块无法恢复。操作前请务必备份，并在游戏内确认出生点与重要建筑区域未被误删。'] }
      ]
    },
    en: {
      title: 'Dimension & Chunk Pruning',
      summary: 'Presets and risks for shrinking world size and stripping redundant chunks.',
      sections: [
        { heading: 'Presets', list: ['Keep all dimensions: retains the overworld, Nether and End with no pruning — for cases where a complete world is needed.', 'Overworld only: strips the Nether and End, commonly used when keeping only builds.', 'Aggressive lightweight: keeps only the core of the overworld, giving the largest size reduction.', 'Custom: tick the dimensions you want to keep individually.'] },
        { heading: 'Keep original NBT', paragraphs: ['When enabled, pruning preserves the original NBT structure, suited to cases where you want to keep the save structure as intact as possible.'] },
        { heading: 'Risk warning', paragraphs: ['Pruning is irreversible — stripped chunks cannot be recovered. Always back up first, and check in-game that the spawn area and important builds were not removed.'] }
      ]
    },
    ja: {
      title: 'ディメンションとチャンクの整理',
      summary: 'ワールド容量の削減と不要チャンクの除去に関するプリセットとリスク。',
      sections: [
        { heading: 'プリセット', list: ['全ディメンション保持：オーバーワールド・ネザー・エンドをそのまま保持し、整理を行いません。完全なワールドが必要な場合に適しています。', 'オーバーワールドのみ：ネザーとエンドを除去します。建築作品だけを残したい場合によく使われます。', '高速軽量化：オーバーワールドの核心部分のみを残し、最も容量を削減できます。', 'カスタム：保持したいディメンションを個別に選択します。'] },
        { heading: '元の NBT を保持', paragraphs: ['この項目を有効にすると、整理の際に元の NBT 構造が保持されます。セーブの構造をできるだけ保ちたい場合に適しています。'] },
        { heading: 'リスクについて', paragraphs: ['整理は元に戻せない操作で、除去されたチャンクは復元できません。作業前に必ずバックアップし、スポーン地点や重要な建築物が消えていないかゲーム内で確認してください。'] }
      ]
    }
  },
  {
    slug: 'nbt-editor',
    'zh-CN': {
      title: 'NBT / LevelDB 编辑器',
      summary: '浏览与修改存档数据的操作方式。',
      sections: [
        { heading: '可打开的内容', list: ['单文件 NBT：.dat、.nbt、level.dat', '区域文件：.mca、.mcr', 'LevelDB 数据库：世界目录下的 db/ 文件夹', '压缩包：.zip、.mcworld（可直接解析，无需手动解压）'] },
        { heading: '语义标签', paragraphs: ['编辑器内置 Minecraft 语义描述引擎，会自动为常见字段（如 Health、GameType、InhabitedTime 等）标注用途说明与合理的取值范围，降低误改风险。'] },
        { heading: '大数组处理', paragraphs: ['对于生物群系网格、高度图、调色板等超大数组，编辑器采用分页加载，并支持十六进制与十进制切换查看，避免一次性渲染导致卡顿。'] },
        { heading: '修改建议', list: ['修改前备份存档。', '优先使用界面上标注了语义说明的字段。', '修改后先在小号存档或副本中验证。'] }
      ]
    },
    en: {
      title: 'NBT / LevelDB Editor',
      summary: 'How to browse and modify save data.',
      sections: [
        { heading: 'What it can open', list: ['Single-file NBT: .dat, .nbt, level.dat', 'Region files: .mca, .mcr', 'LevelDB databases: the db/ folder inside a world directory', 'Archives: .zip, .mcworld (parsed directly, no manual extraction needed)'] },
        { heading: 'Semantic labels', paragraphs: ['The editor includes a Minecraft semantic descriptor that annotates common fields (such as Health, GameType and InhabitedTime) with their purpose and a sensible value range, reducing the risk of mistakes.'] },
        { heading: 'Large arrays', paragraphs: ['For very large arrays such as biome grids, heightmaps and palettes, the editor loads data page by page and supports switching between hexadecimal and decimal views, avoiding stalls from rendering everything at once.'] },
        { heading: 'Editing advice', list: ['Back up the world before making changes.', 'Prefer fields that carry a semantic description in the UI.', 'Verify changes on a throwaway copy of the world first.'] }
      ]
    },
    ja: {
      title: 'NBT / LevelDB エディタ',
      summary: 'セーブデータを閲覧・編集する手順。',
      sections: [
        { heading: '開ける形式', list: ['単一ファイル NBT：.dat、.nbt、level.dat', 'リージョンファイル：.mca、.mcr', 'LevelDB データベース：ワールドディレクトリ内の db/ フォルダ', 'アーカイブ：.zip、.mcworld（手動で解凍せずそのまま解析できます）'] },
        { heading: '意味ラベル', paragraphs: ['エディタには Minecraft の意味記述エンジンが内蔵されており、よく使うフィールド（Health、GameType、InhabitedTime など）に用途と適切な値の範囲を表示します。誤った変更のリスクを下げられます。'] },
        { heading: '巨大な配列の扱い', paragraphs: ['バイオーム格子、ハイトマップ、パレットなどの非常に大きな配列は、ページ単位で読み込みます。16 進と 10 進の表示を切り替えられるため、一度にすべてを描画することによる固まりを避けられます。'] },
        { heading: '編集のすすめ', list: ['変更前にワールドをバックアップしてください。', 'UI に意味の説明が付いているフィールドを優先して使ってください。', '変更後は、使い捨てのコピーで先に確認してください。'] }
      ]
    }
  },
  {
    slug: 'pack-converter',
    'zh-CN': {
      title: '材质包转换',
      summary: 'Java 与基岩版资源包双向转换的说明。',
      sections: [
        { heading: '转换内容', list: ['方块与物品贴图的命名映射。', 'Blockstates 与自定义方块 / 实体模型结构。', '语言文件（Java JSON ↔ 基岩 .lang）。', '音效配置（sounds.json ↔ sound_definitions.json）。', '包描述文件（pack.mcmeta ↔ manifest.json），可生成新的 UUID。'] },
        { heading: '动画贴图', paragraphs: ['Java 版的 .png.mcmeta 动画贴图会被合成为基岩版的垂直帧带纹理。转换内置了帧数与尺寸的熔断保护，避免极端高分辨率动画导致内存溢出。'] },
        { heading: '兼容性说明', paragraphs: ['双端的模型与渲染机制存在差异，复杂的自定义模型、着色器或特殊的贴图引用方式可能无法完全等价转换，转换后请在游戏内确认效果。'] }
      ]
    },
    en: {
      title: 'Resource Pack Conversion',
      summary: 'Two-way conversion of Java and Bedrock resource packs.',
      sections: [
        { heading: 'What gets converted', list: ['Block and item texture name mapping.', 'Blockstates and custom block / entity model structures.', 'Language files (Java JSON ↔ Bedrock .lang).', 'Sound configuration (sounds.json ↔ sound_definitions.json).', 'Pack descriptors (pack.mcmeta ↔ manifest.json), with a new UUID generated.'] },
        { heading: 'Animated textures', paragraphs: ['Java .png.mcmeta animated textures are composited into Bedrock vertical frame strips. Built-in circuit breakers on frame count and size prevent out-of-memory failures from extreme high-resolution animations.'] },
        { heading: 'Compatibility', paragraphs: ['The two platforms differ in model and rendering mechanics, so complex custom models, shaders or unusual texture reference styles may not convert to a fully equivalent result. Verify the outcome in-game after converting.'] }
      ]
    },
    ja: {
      title: 'リソースパックの変換',
      summary: 'Java 版と統合版のリソースパックの双方向変換について。',
      sections: [
        { heading: '変換される内容', list: ['ブロックとアイテムのテクスチャ名の対応付け。', 'Blockstates とカスタムブロック / エンティティのモデル構造。', '言語ファイル（Java JSON ↔ 統合版 .lang）。', 'サウンド設定（sounds.json ↔ sound_definitions.json）。', 'パッケージ記述ファイル（pack.mcmeta ↔ manifest.json）。新しい UUID を生成できます。'] },
        { heading: 'アニメーションテクスチャ', paragraphs: ['Java 版の .png.mcmeta アニメーションテクスチャは、統合版の縦フレーム帯テクスチャへ合成されます。フレーム数とサイズの上限チェックを内蔵し、極端な高解像度アニメによるメモリ不足を防ぎます。'] },
        { heading: '互換性について', paragraphs: ['両プラットフォームはモデルと描画の仕組みが異なるため、複雑なカスタムモデル、シェーダー、特殊なテクスチャ参照は完全に同等な形へ変換できない場合があります。変換後はゲーム内で結果を確認してください。'] }
      ]
    }
  },
  {
    slug: 'midi',
    'zh-CN': {
      title: 'MIDI 红石音乐',
      summary: '把 MIDI 变成可播放的红石音乐世界或机械结构。',
      sections: [
        { heading: '处理流程', list: ['选择 .mid / .midi 文件。', '应用解析音轨并与乐器、音高进行映射。', '选择产出形式并进行量化处理。', '生成结果后导出到目标位置。'] },
        { heading: '产出形式', list: ['超平坦音乐世界：直接写入 LevelDB 区块数据，生成进入即可收听的世界。', '红石机械结构：导出 .mcstructure 或 .schem 结构文件。', 'NBS 文件：用于在其他音符盒编辑器中继续编辑。', '数据包指令：生成可被数据包调度的播放指令。'] },
        { heading: '使用建议', paragraphs: ['红石音乐依赖游戏刻与区块加载，建议在超平坦世界或空置维度中播放，以保证节拍稳定。大型 MIDI 生成的区块数量可观，转换耗时较长。'] }
      ]
    },
    en: {
      title: 'MIDI Redstone Music',
      summary: 'Turn MIDI into playable redstone music worlds or contraption structures.',
      sections: [
        { heading: 'Pipeline', list: ['Select a .mid / .midi file.', 'The app parses the tracks and maps them to instruments and pitches.', 'Choose the output format and apply quantization.', 'Export the generated result to your target location.'] },
        { heading: 'Output formats', list: ['Superflat music world: writes directly into LevelDB chunk data, producing a world you can enter and immediately listen to.', 'Redstone contraption structure: exports .mcstructure or .schem files.', 'NBS file: for continuing to edit in other note block editors.', 'Datapack commands: generates playback commands schedulable by a datapack.'] },
        { heading: 'Recommendations', paragraphs: ['Redstone music depends on game ticks and chunk loading, so playing it in a superflat world or an empty dimension keeps the tempo stable. Large MIDI files generate a considerable number of chunks, making conversion slower.'] }
      ]
    },
    ja: {
      title: 'MIDI レッドストーン音楽',
      summary: 'MIDI を再生可能なレッドストーン音楽ワールドや装置の構造物に変換します。',
      sections: [
        { heading: '処理の流れ', list: ['.mid / .midi ファイルを選択します。', 'アプリがトラックを解析し、楽器と音高に対応付けます。', '出力形式を選び、量子化を行います。', '生成された結果を目的の場所へ書き出します。'] },
        { heading: '出力形式', list: ['超平坦な音楽ワールド：LevelDB のチャンクデータへ直接書き込み、入ってすぐに聴けるワールドを生成します。', 'レッドストーン装置の構造物：.mcstructure または .schem を書き出します。', 'NBS ファイル：他の音符ブロックエディタで編集を続けるために使用します。', 'データパックのコマンド：データパックからスケジュール実行できる再生コマンドを生成します。'] },
        { heading: 'おすすめ', paragraphs: ['レッドストーン音楽はゲームティックとチャンクロードに依存するため、テンポを安定させるには超平坦ワールドや空のディメンションでの再生をおすすめします。大きな MIDI は生成されるチャンクが多くなり、変換に時間がかかります。'] }
      ]
    }
  },
  {
    slug: 'faq-tech',
    'zh-CN': {
      title: '常见问题',
      summary: '运行环境、兼容性与故障排查。',
      sections: [
        { heading: '设备要求', list: ['系统版本：Android 8.0 (API 27) 及以上。', '架构：当前版本的沙箱运行环境面向 arm64-v8a。', '存储：应用内置转换引擎与运行环境，安装包体积较大，请预留充足空间。'] },
        { heading: '为什么安装包这么大', paragraphs: ['安装包内包含了完整的转换引擎（Chunker CLI）与 OpenJDK 17 运行环境，以保证在手机端离线完成转换，无需电脑或联网。'] },
        { heading: '转换卡住或长时间无响应', list: ['确认电量充足，并将应用加入电池优化白名单。', '保持应用在前台或后台运行，不要强制停止。', '大型存档请耐心等待，可在日志视图中确认引擎仍在输出。', '若确认失败，请通过「崩溃日志」功能导出日志以便排查。'] },
        { heading: '转换后存档无法打开', list: ['确认目标版本与你的游戏版本匹配。', '确认导出的目录结构正确（Java 版为包含 level.dat 的文件夹，基岩版为 .mcworld 或世界文件夹）。', '尝试重新转换，并选择更接近源版本的等价目标版本。'] }
      ]
    },
    en: {
      title: 'Troubleshooting',
      summary: 'Runtime environment, compatibility and troubleshooting.',
      sections: [
        { heading: 'Device requirements', list: ['OS version: Android 8.0 (API 27) or higher.', 'Architecture: the sandbox runtime for the current version targets arm64-v8a.', 'Storage: the engine and runtime are bundled, so the installer is large — allow plenty of free space.'] },
        { heading: 'Why is the installer so large', paragraphs: ['The installer bundles the complete conversion engine (Chunker CLI) and an OpenJDK 17 runtime so that conversion can be done offline on your phone, with no computer or network.'] },
        { heading: 'Conversion seems stuck or unresponsive', list: ['Make sure the battery is sufficient and add the app to the battery optimization allowlist.', 'Keep the app running in the foreground or background, and do not force stop it.', 'Large worlds take time — check the log view to confirm the engine is still producing output.', 'If it has genuinely failed, export the log via the crash log feature to help diagnose.'] },
        { heading: 'The converted world will not open', list: ['Confirm the target version matches your game version.', 'Confirm the exported directory structure is correct (Java: a folder containing level.dat; Bedrock: a .mcworld file or world folder).', 'Try converting again and choose an equivalent target version closer to the source version.'] }
      ]
    },
    ja: {
      title: '困ったときは',
      summary: '動作環境、互換性、トラブルシューティング。',
      sections: [
        { heading: '動作要件', list: ['OS バージョン：Android 8.0 (API 27) 以上。', 'アーキテクチャ：現行バージョンのサンドボックス実行環境は arm64-v8a 向けです。', 'ストレージ：変換エンジンと実行環境を同梱しているためインストーラが大きく、十分な空き容量が必要です。'] },
        { heading: 'インストーラが大きい理由', paragraphs: ['インストーラには完全な変換エンジン（Chunker CLI）と OpenJDK 17 実行環境が含まれています。これにより、PC やネットワークなしでスマートフォン単体でのオフライン変換が可能になります。'] },
        { heading: '変換が止まった / 反応がない', list: ['バッテリー残量を確認し、アプリを電池最適化の除外に追加してください。', 'アプリを前面または背面で動かしたままにし、強制停止しないでください。', '大きなワールドは時間がかかります。ログ表示でエンジンが出力を続けているか確認してください。', '明らかに失敗している場合は、「クラッシュログ」機能からログを書き出して原因調査にご利用ください。'] },
        { heading: '変換後のワールドが開けない', list: ['変換先のバージョンがゲームのバージョンと一致しているか確認してください。', '書き出したディレクトリ構造が正しいか確認してください（Java 版：level.dat を含むフォルダ、統合版：.mcworld またはワールドフォルダ）。', 'もう一度変換し、元のバージョンに近い対応バージョンを選んでみてください。'] }
      ]
    }
  }
]

export function findDoc(slug) {
  return docPages.find((d) => d.slug === slug)
}
