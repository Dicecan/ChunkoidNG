<script setup>
import { ref, computed } from 'vue'
import { site, friendlyLinks, community } from '../data/features'
import { t, localized } from '../i18n'

const thanks = [
  { name: 'Dozener (DozenesStudio)', roleKey: 'aboutRoleOriginal' },
  { name: 'DICECAN (EncoreTeam)', roleKey: 'aboutRoleMaintainer' },
  { name: 'Ryan Steven', roleKey: 'aboutRoleContributor' }
]

const roles = computed(() => ({
  aboutRoleOriginal: { 'zh-CN': '原作者与奠基人', en: 'Original author & founder', ja: '原作者・創始者' },
  aboutRoleMaintainer: { 'zh-CN': '当前维护与重构团队', en: 'Current maintainer & rewrite team', ja: '現在のメンテナンス・リファクタリングチーム' },
  aboutRoleContributor: { 'zh-CN': '核心贡献者', en: 'Core contributor', ja: '主要コントリビューター' }
}))

const deps = [
  { name: 'The Hive - Chunker', desc: { 'zh-CN': '核心转换引擎', en: 'Core conversion engine', ja: '中核となる変換エンジン' }, license: 'MIT', url: 'https://github.com/HiveGamesOSS/Chunker' },
  { name: 'HTMonkeyG - XOR-MC-Archive-Decrypt', desc: { 'zh-CN': '网易存档异或解密算法参考', en: 'Reference for NetEase XOR decryption', ja: 'NetEase セーブの XOR 復号アルゴリズムの参考' }, license: 'GPL-3.0', url: 'https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt' },
  { name: 'PowerNukkit - NBT-Manipulator', desc: { 'zh-CN': 'NBT 读写库', en: 'NBT read/write library', ja: 'NBT 読み書きライブラリ' }, license: 'MIT', url: 'https://github.com/PowerNukkit/NBT-Manipulator' },
  { name: 'iq80 - leveldb', desc: { 'zh-CN': '纯 Java LevelDB 实现', en: 'Pure-Java LevelDB implementation', ja: '純 Java の LevelDB 実装' }, license: 'Apache-2.0', url: 'https://github.com/dain/leveldb' },
  { name: 'HiveGamesOSS - leveldb-mcpe-java', desc: { 'zh-CN': '基岩版 LevelDB 支持', en: 'Bedrock LevelDB support', ja: '統合版 LevelDB のサポート' }, license: 'Apache-2.0 / BSD', url: 'https://github.com/HiveGamesOSS/leveldb-mcpe-java' },
  { name: 'Dicecan - NetEaseDecryptorSDK', desc: { 'zh-CN': '网易解密 SDK', en: 'NetEase decryption SDK', ja: 'NetEase 復号 SDK' }, license: 'GPL-3.0', url: 'https://github.com/Dicecan/NetEaseDecryptorSDK' }
]

const people = computed(() => thanks.map((p) => ({ name: p.name, role: localized(roles.value[p.roleKey]) })))
const dependencyList = computed(() => deps.map((d) => ({ ...d, descText: localized(d.desc) })))
const friendlyLinksList = computed(() =>
  friendlyLinks.map((l) => ({
    ...l,
    descText: localized(l.desc),
    tagText: localized(l.tag)
  }))
)

const stack = computed(() => [
  { k: t('about.stackLang'), v: 'Kotlin 2.0+' },
  { k: t('about.stackUi'), v: 'Jetpack Compose (Material Design 3)' },
  { k: t('about.stackAsync'), v: 'Kotlinx Coroutines & Flow' },
  { k: t('about.stackSandbox'), v: site.runtime },
  { k: t('about.stackEngine'), v: site.engine },
  { k: t('about.stackMin'), v: site.minAndroid }
])

const toastMsg = ref('')
let toastTimer = null

function showToast(msg) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMsg.value = ''
  }, 2500)
}

function copyQQ() {
  const qq = community.qqGroup
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(qq).then(() => {
      showToast(t('about.copiedQQ', { qq }))
    }).catch(() => {
      fallbackCopy(qq)
    })
  } else {
    fallbackCopy(qq)
  }
}

function fallbackCopy(text) {
  const input = document.createElement('input')
  input.value = text
  document.body.appendChild(input)
  input.select()
  try {
    document.execCommand('copy')
    showToast(t('about.copiedQQ', { qq: text }))
  } catch (e) {
    showToast(text)
  }
  document.body.removeChild(input)
}
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('about.eyebrow') }}</div>
        <h1>{{ t('about.title') }}</h1>
        <p>{{ t('about.lead', { name: site.name, maintainer: site.maintainer, author: site.author }) }}</p>
      </div>

      <!-- 友情链接与合作项目（顶部显著展示） -->
      <section class="section">
        <div class="section-badge-header">
          <h2>{{ t('about.friendlyLinks') }}</h2>
          <span class="badge badge-accent">Featured</span>
        </div>
        <p class="muted" style="margin-top: -6px; margin-bottom: 16px">{{ t('about.friendlyLinksDesc') }}</p>

        <div class="links-grid">
          <div
            v-for="link in friendlyLinksList"
            :key="link.url"
            class="link-card"
            :class="{ 'featured-card': link.featured }"
          >
            <div class="link-header">
              <div class="link-title-wrap">
                <strong class="link-title">{{ link.name }}</strong>
                <span v-if="link.featured" class="tag-star">★ 原作者新作</span>
              </div>
              <span class="badge" v-if="link.tagText">{{ link.tagText }}</span>
            </div>
            <p class="link-desc">{{ link.descText }}</p>
            <div class="link-action">
              <a
                :href="link.url"
                target="_blank"
                rel="noopener"
                class="btn btn-sm"
                :class="link.featured ? 'btn-primary' : ''"
              >
                {{ t('about.visitSite') }} ↗
              </a>
            </div>
          </div>
        </div>
      </section>

      <!-- 官方社群与支持通道 (来自 about.html) -->
      <section class="section">
        <h2>{{ t('about.communityTitle') }}</h2>
        <p class="muted" style="margin-top: -6px; margin-bottom: 16px">{{ t('about.communityDesc') }}</p>

        <div class="grid grid-2">
          <!-- QQ 交流群 -->
          <div class="card community-card">
            <div class="card-head-row">
              <div class="card-icon-box">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12c0 1.54.36 2.98.97 4.29L1 23l6.71-1.97C9.02 21.64 10.46 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2zm0 18c-1.39 0-2.69-.35-3.83-.97l-.27-.15-4.04 1.18 1.18-3.95-.17-.28C4.3 14.69 4 13.38 4 12c0-4.41 3.59-8 8-8s8 3.59 8 8-3.59 8-8 8z"/>
                </svg>
              </div>
              <div>
                <h3 style="margin: 0">{{ t('about.qqTitle') }}</h3>
                <div class="small muted">{{ t('about.qqDesc') }}</div>
              </div>
            </div>
            <div class="qq-number-display">{{ community.qqGroup }}</div>
            <p class="small muted" style="margin: 0 0 14px">{{ t('about.qqNotice') }}</p>
            <div style="margin-top: auto">
              <button type="button" class="btn btn-sm btn-primary" @click="copyQQ">
                {{ t('about.copyQQ') }}
              </button>
            </div>
          </div>

          <!-- 爱发电支持 -->
          <div class="card community-card">
            <div class="card-head-row">
              <div class="card-icon-box heart-icon">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
              </div>
              <div>
                <h3 style="margin: 0">{{ t('about.donateTitle') }}</h3>
                <div class="small muted">Afdian / Sponsor</div>
              </div>
            </div>
            <p class="small muted" style="margin: 10px 0 16px">{{ t('about.donateDesc') }}</p>
            <div class="btn-group" style="margin-top: auto; display: flex; gap: 8px; flex-wrap: wrap">
              <a :href="community.donate[0].url" target="_blank" rel="noopener" class="btn btn-sm">
                {{ t('about.sponsorMaintainer') }} ↗
              </a>
              <a :href="community.donate[1].url" target="_blank" rel="noopener" class="btn btn-sm">
                {{ t('about.sponsorAuthor') }} ↗
              </a>
            </div>
          </div>

          <!-- 视频主页与流媒体 -->
          <div class="card community-card">
            <div class="card-head-row">
              <div class="card-icon-box">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
                  <path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/>
                </svg>
              </div>
              <div>
                <h3 style="margin: 0">{{ t('about.mediaTitle') }}</h3>
                <div class="small muted">Bilibili & Douyin</div>
              </div>
            </div>
            <p class="small muted" style="margin: 10px 0 16px">{{ t('about.mediaDesc') }}</p>
            <div class="btn-group" style="margin-top: auto; display: flex; gap: 8px; flex-wrap: wrap">
              <a :href="community.bilibili[0].url" target="_blank" rel="noopener" class="btn btn-sm">
                {{ t('about.bilibiliNoches') }} ↗
              </a>
              <a :href="community.bilibili[1].url" target="_blank" rel="noopener" class="btn btn-sm">
                {{ t('about.bilibiliAuthor') }} ↗
              </a>
              <a :href="community.douyin[0].url" target="_blank" rel="noopener" class="btn btn-sm">
                {{ t('about.douyinAuthor') }} ↗
              </a>
            </div>
          </div>

          <!-- 开源 GitHub 仓库 -->
          <div class="card community-card">
            <div class="card-head-row">
              <div class="card-icon-box">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
                </svg>
              </div>
              <div>
                <h3 style="margin: 0">{{ t('about.githubTitle') }}</h3>
                <div class="small muted">GPL-3.0 Open Source</div>
              </div>
            </div>
            <p class="small muted" style="margin: 10px 0 16px">{{ t('about.githubDesc') }}</p>
            <div style="margin-top: auto">
              <a :href="site.repo" target="_blank" rel="noopener" class="btn btn-sm">
                {{ t('about.visitGithub') }} ↗
              </a>
            </div>
          </div>
        </div>
      </section>

      <!-- 项目背景 -->
      <section class="section">
        <h2>{{ t('about.background') }}</h2>
        <div class="card">
          <p style="margin-top: 0">{{ t('about.backgroundDesc') }}</p>
          <ul style="margin-bottom: 0">
            <li>{{ t('about.point1') }}</li>
            <li>{{ t('about.point2') }}</li>
            <li>{{ t('about.point3', { version: site.version }) }}</li>
          </ul>
        </div>
      </section>

      <!-- 技术栈 -->
      <section class="section">
        <h2>{{ t('about.stack') }}</h2>
        <div class="card">
          <table class="table">
            <tbody>
              <tr v-for="row in stack" :key="row.k">
                <th style="width: 170px">{{ row.k }}</th>
                <td>{{ row.v }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 致谢 -->
      <section class="section">
        <h2>{{ t('about.thanks') }}</h2>
        <div class="grid grid-3">
          <div v-for="p in people" :key="p.name" class="card">
            <div class="person-name">{{ p.name }}</div>
            <div class="small muted">{{ p.role }}</div>
          </div>
        </div>
      </section>

      <!-- 第三方开源组件 -->
      <section class="section">
        <h2>{{ t('about.thirdParty') }}</h2>
        <div class="card">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t('about.thComponent') }}</th>
                <th>{{ t('about.thUsage') }}</th>
                <th style="width: 140px">{{ t('about.thLicense') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dependencyList" :key="d.name">
                <td>
                  <a :href="d.url" target="_blank" rel="noopener">{{ d.name }} ↗</a>
                </td>
                <td>{{ d.descText }}</td>
                <td>{{ d.license }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 开源协议 -->
      <section class="section">
        <h2>{{ t('about.licenseTitle') }}</h2>
        <div class="card">
          <p style="margin-top: 0">{{ t('about.licenseDesc') }}</p>
          <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 14px">
            <a :href="site.repo" target="_blank" rel="noopener" class="btn">{{ t('download.viewSource') }} ↗</a>
            <a :href="site.repo + '/blob/main/LICENSE'" target="_blank" rel="noopener" class="btn">
              {{ t('about.readLicense') }} ↗
            </a>
          </div>
        </div>
      </section>
    </div>

    <!-- 轻量 Toast 提示 -->
    <transition name="toast-fade">
      <div v-if="toastMsg" class="toast-box">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span>{{ toastMsg }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.person-name { font-weight: 600; font-size: 0.93rem; margin-bottom: 3px; }

.section-badge-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.section-badge-header h2 { margin: 0; }

.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.link-card {
  padding: 18px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  transition: transform 0.18s ease, border-color 0.18s ease;
}
.link-card:hover {
  border-color: var(--border-hover, #42b883);
  transform: translateY(-2px);
}

.featured-card {
  border-color: var(--accent);
  background: linear-gradient(145deg, rgba(47, 125, 99, 0.08), var(--bg-soft));
  box-shadow: 0 4px 16px rgba(47, 125, 99, 0.12);
}

.link-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.link-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.link-title {
  font-size: 1.1rem;
  color: var(--text);
}

.tag-star {
  font-size: 0.72rem;
  font-weight: 600;
  color: #e5a420;
  background: rgba(229, 164, 32, 0.12);
  padding: 2px 6px;
  border-radius: 4px;
}

.link-desc {
  font-size: 0.88rem;
  color: var(--text-soft);
  margin: 0 0 16px 0;
  line-height: 1.55;
  flex: 1;
}

.link-action {
  margin-top: auto;
}

/* Community cards */
.community-card {
  display: flex;
  flex-direction: column;
}

.card-head-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.card-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--accent-soft, rgba(47, 125, 99, 0.15));
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.heart-icon {
  color: #e25555;
  background: rgba(226, 85, 85, 0.12);
}

.qq-number-display {
  display: inline-block;
  font-family: monospace;
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--accent);
  background: var(--bg);
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px dashed var(--border);
  margin: 8px 0 12px 0;
  align-self: flex-start;
}

/* Toast */
.toast-box {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: #1e2621;
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  z-index: 9999;
  border: 1px solid rgba(255, 255, 255, 0.12);
}
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}

@media (max-width: 640px) {
  .person-name { font-size: 0.95rem; }
  .table a { min-height: 40px; display: inline-flex; align-items: center; padding: 3px 0; }
  .qq-number-display { font-size: 1.15rem; }
}
</style>
