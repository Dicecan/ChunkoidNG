<script setup>
import { ref, computed } from 'vue'
import { site } from '../data/features'
import { t, localized, locale } from '../i18n'

const raw = [
  {
    'zh-CN': { q: 'Chunkoid NG 和旧版 Chunkoid 是什么关系？', a: 'Chunkoid NG 是 EncoreTeam（DICECAN）对原作者 Dozener 开发的安卓端 Minecraft 世界转换工具 Chunkoid 的现代化重构版本。原作者在进入大学之际将项目无偿交接给 EncoreTeam 继续维护，项目沿用 GPLv3 协议，并在所有代码库、文档与关于页面中永久保留原作者署名。' },
    en: { q: 'How does Chunkoid NG relate to the original Chunkoid?', a: 'Chunkoid NG is a modern rewrite by EncoreTeam (DICECAN) of Chunkoid, the Android Minecraft world conversion tool created by the original author Dozener. As the author was starting university, the project was handed over to EncoreTeam free of charge to keep it maintained. It stays under GPLv3 and permanently preserves the original author credit across all repositories, documentation and the about screen.' },
    ja: { q: 'Chunkoid NG と旧版 Chunkoid の関係は？', a: 'Chunkoid NG は、原作者 Dozener 氏が開発した Android 向け Minecraft ワールド変換ツール Chunkoid を、EncoreTeam（DICECAN）がモダナイズして作り直したものです。原作者の大学進学に伴い、維持管理のため無償で EncoreTeam へ引き継がれました。GPLv3 を維持し、すべてのリポジトリ・ドキュメント・情報画面において原作者のクレジットを永久に保持します。' }
  },
  {
    'zh-CN': { q: '转换需要联网吗？会消耗流量吗？', a: '不需要。转换引擎与 Java 运行环境都内置在应用里，整个转换过程完全在设备本地进行，不会上传你的存档到任何服务器。' },
    en: { q: 'Does conversion need a network connection or use data?', a: 'No. Both the conversion engine and the Java runtime are bundled in the app. The whole process runs locally on your device and never uploads your world to any server.' },
    ja: { q: '変換にネットワークは必要ですか？通信量を消費しますか？', a: '不要です。変換エンジンと Java 実行環境はどちらもアプリに内蔵されています。処理はすべて端末内で完結し、ワールドがサーバーへ送信されることはありません。' }
  },
  {
    'zh-CN': { q: '安装包为什么这么大？', a: '安装包内包含完整的区块转换引擎（Chunker CLI，约 30 MB）与 OpenJDK 17 运行环境（约 27 MB），因此体积较大。这是为了在手机端离线完成转换所必需的，无需电脑辅助。' },
    en: { q: 'Why is the installer so large?', a: 'It bundles the complete chunk conversion engine (Chunker CLI, about 30 MB) and an OpenJDK 17 runtime (about 27 MB). This is what makes fully offline conversion on a phone possible without a computer.' },
    ja: { q: 'インストーラが大きいのはなぜですか？', a: '完全なチャンク変換エンジン（Chunker CLI、約 30 MB）と OpenJDK 17 実行環境（約 27 MB）を同梱しているためです。PC なしでスマートフォン単体のオフライン変換を実現するために必要です。' }
  },
  {
    'zh-CN': { q: '支持哪些设备？', a: '需要 Android 8.0 (API 27) 及以上系统。当前版本的沙箱运行环境面向 arm64-v8a 架构构建。' },
    en: { q: 'Which devices are supported?', a: 'Android 8.0 (API 27) or higher. The sandbox runtime for the current version targets the arm64-v8a architecture.' },
    ja: { q: 'どの端末に対応していますか？', a: 'Android 8.0 (API 27) 以上が必要です。現行バージョンのサンドボックス実行環境は arm64-v8a 向けに構築されています。' }
  },
  {
    'zh-CN': { q: '转换会不会导致存档损坏？', a: '应用在写入前会先输出到临时目录并校验产物完整性，失败时自动清理，避免产生半成品存档。但跨版本转换（特别是降级）本身存在数据损失风险，任何操作前都请务必备份原始存档。' },
    en: { q: 'Can conversion corrupt my world?', a: 'Output is written to a temporary directory first and verified for completeness, with automatic cleanup on failure, so you never end up with a half-written world. That said, cross-version conversion (especially downgrading) inherently carries a risk of data loss, so always back up the original world first.' },
    ja: { q: '変換でワールドが壊れることはありますか？', a: '出力はまず一時ディレクトリへ書き込み、内容を検証したうえで確定します。失敗時は自動で削除されるため、不完全なワールドが残ることはありません。ただし、バージョンをまたぐ変換（特にダウングレード）にはデータ損失のリスクが伴うため、作業前に必ず元のワールドをバックアップしてください。' }
  },
  {
    'zh-CN': { q: '转换后游戏里打不开存档怎么办？', a: '首先确认目标版本与你的游戏版本匹配；其次确认导出的目录结构正确——Java 版应导出为包含 level.dat 的文件夹，基岩版可为 .mcworld 文件或世界文件夹。若仍失败，建议改用更接近源版本的等价目标版本重新转换。' },
    en: { q: 'The converted world will not open in-game. What should I do?', a: 'First confirm the target version matches your game version. Then confirm the exported directory structure is correct — Java worlds should be a folder containing level.dat, while Bedrock can be a .mcworld file or a world folder. If it still fails, try converting again with an equivalent target version closer to the source version.' },
    ja: { q: '変換後のワールドがゲームで開けません。どうすればよいですか？', a: 'まず変換先のバージョンがゲームのバージョンと一致しているか確認してください。次に、書き出したディレクトリ構造が正しいか確認してください（Java 版は level.dat を含むフォルダ、統合版は .mcworld またはワールドフォルダ）。それでも開けない場合は、元のバージョンに近い対応バージョンで再変換してみてください。' }
  },
  {
    'zh-CN': { q: '转换长时间没有完成，是卡住了吗？', a: '大型存档转换耗时较长属于正常现象。可以在转换页的日志视图中确认引擎是否仍在输出。建议将应用加入电池优化白名单，并保持应用在后台运行。若日志长时间无任何输出，可取消后重试。' },
    en: { q: 'Conversion has been running for a long time. Is it stuck?', a: 'Long conversion times are normal for large worlds. Check the log view on the conversion page to confirm the engine is still producing output. Adding the app to the battery optimization allowlist and keeping it running in the background is recommended. If the log shows no output for a long time, cancel and try again.' },
    ja: { q: '変換が長い時間終わりません。固まっているのでしょうか？', a: '大きなワールドでは変換に時間がかかるのは正常です。変換画面のログ表示で、エンジンが出力を続けているか確認してください。アプリを電池最適化の除外に追加し、バックグラウンドで動かしたままにすることをおすすめします。長時間まったく出力がない場合は、中断してやり直してください。' }
  },
  {
    'zh-CN': { q: '可以转换网易版存档吗？', a: '可以。应用内置网易版存档的解密能力，支持 LevelDB 异或流式解密与完整性校验，解密完成后可一键流转到世界转换器继续处理。同时也支持反向的被动加密。' },
    en: { q: 'Can it convert NetEase saves?', a: 'Yes. The app includes decryption for NetEase saves with streaming XOR decryption for LevelDB and integrity verification. Once decrypted, you can hand the result straight to the world converter. The reverse passive encryption is supported as well.' },
    ja: { q: 'NetEase 版のセーブも変換できますか？', a: 'はい。NetEase 版セーブの復号機能を内蔵しており、LevelDB の XOR ストリーミング復号と完全性の検証に対応しています。復号後はそのままワールド変換へ引き継げます。逆方向のパッシブ暗号化にも対応しています。' }
  },
  {
    'zh-CN': { q: '这个工具是免费的吗？', a: '是的，Chunkoid NG 基于 GPLv3 协议完全免费开源，代码与构建流程全部公开，你可以自行审阅、编译与分发。' },
    en: { q: 'Is this tool free?', a: 'Yes. Chunkoid NG is completely free and open source under GPLv3. The source and build process are fully public, so you can audit, compile and redistribute it yourself.' },
    ja: { q: 'このツールは無料ですか？', a: 'はい。Chunkoid NG は GPLv3 のもとで完全に無料のオープンソースです。ソースコードとビルド手順はすべて公開されており、ご自身で確認・ビルド・再配布できます。' }
  },
  {
    'zh-CN': { q: '遇到崩溃或异常怎么反馈？', a: '应用带有独立的崩溃诊断模块，会自动收集设备信息与调用堆栈。你可以在应用内导出崩溃日志，并附上问题描述提交到项目仓库的 Issues 中。' },
    en: { q: 'How do I report a crash or bug?', a: 'The app has a standalone crash diagnostics module that automatically collects device information and stack traces. Export the crash log from within the app and attach it, along with a description, to an issue on the repository.' },
    ja: { q: 'クラッシュや不具合はどう報告すればよいですか？', a: 'アプリには独立したクラッシュ診断モジュールがあり、端末情報とスタックトレースを自動で収集します。アプリからクラッシュログを書き出し、状況の説明とともにリポジトリの Issue へ添付してください。' }
  }
]

const faqs = computed(() => raw.map((item) => localized(item)))
const open = ref(-1)
function toggle(i) {
  open.value = open.value === i ? -1 : i
}
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('faq.eyebrow') }}</div>
        <h1>{{ t('faq.title') }}</h1>
        <p>{{ t('faq.lead') }}</p>
      </div>

      <div class="faq-list">
        <div v-for="(f, i) in faqs" :key="i" class="faq-item" :class="{ open: open === i }">
          <button class="faq-q" type="button" :aria-expanded="open === i" @click="toggle(i)">
            <span>{{ f.q }}</span>
            <span class="faq-icon" aria-hidden="true">{{ open === i ? '−' : '+' }}</span>
          </button>
          <div v-show="open === i" class="faq-a">{{ f.a }}</div>
        </div>
      </div>

      <div class="note" style="margin-top: 32px">
        <div class="note-title">{{ t('faq.notFoundTitle') }}</div>
        <p style="margin: 0; font-size: 0.89rem">
          {{ t('docs.faqDesc') }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.faq-list { border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.faq-item { border-bottom: 1px solid var(--border); }
.faq-item:last-child { border-bottom: 0; }
.faq-item.open { background: var(--bg-soft); }

.faq-q {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 15px 20px;
  background: transparent;
  border: 0;
  font-family: inherit;
  font-size: 0.94rem;
  font-weight: 500;
  color: var(--text);
  text-align: left;
  cursor: pointer;
}
.faq-q:hover { color: var(--accent-text); }

.faq-icon { flex: none; font-size: 1.15rem; color: var(--text-faint); line-height: 1; }

.faq-a {
  padding: 0 20px 18px;
  font-size: 0.89rem;
  color: var(--text-soft);
  max-width: 78ch;
}

@media (max-width: 640px) {
  .faq-q { padding: 15px 16px; min-height: 56px; font-size: 0.92rem; align-items: flex-start; gap: 12px; }
  .faq-icon { margin-top: 1px; }
  .faq-a { padding: 0 16px 16px; font-size: 0.88rem; }
}
</style>