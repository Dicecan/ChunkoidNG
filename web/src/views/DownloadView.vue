<script setup>
import { ref, computed, onMounted } from 'vue'
import { site, release as fallbackRelease } from '../data/features'
import { t, localized, useI18n } from '../i18n'
import {
  fetchReleasesData,
  triggerManualSync,
  DEFAULT_MIRRORS,
  getStoredMirrorId,
  setStoredMirrorId,
  getStoredCustomPrefix,
  setStoredCustomPrefix,
  resolveAssetUrl
} from '../api/releases'

const { locale } = useI18n()
const isZh = computed(() => locale.value === 'zh-CN')

const releasesData = ref(null)
const loading = ref(true)
const syncing = ref(false)
const syncMsg = ref('')

const selectedMirrorId = ref(getStoredMirrorId())
const customPrefix = ref(getStoredCustomPrefix())
const showMirrorConfig = ref(false)
const expandedNotes = ref({})
const copiedSha = ref('')
const copiedLink = ref('')

const mirrors = computed(() => {
  const list = releasesData.value?.mirrors || DEFAULT_MIRRORS
  return list.slice().sort((a, b) => (b.priority || 0) - (a.priority || 0))
})

// Top recommended mirrors for Chinese users: GitHub official and Local cache
const recommendedMirrors = computed(() => {
  return mirrors.value.filter((m) => !m.isThirdParty)
})

// Third-party accelerated lines placed below
const thirdPartyMirrors = computed(() => {
  return mirrors.value.filter((m) => m.isThirdParty)
})

// Effective mirror ID:
// If NOT Chinese, strictly force GitHub official ('official')
const effectiveMirrorId = computed(() => {
  if (!isZh.value) return 'official'
  return selectedMirrorId.value
})

const activeMirror = computed(() => {
  if (!isZh.value) {
    return mirrors.value.find((m) => m.id === 'official') || DEFAULT_MIRRORS[0]
  }
  if (selectedMirrorId.value === 'custom') {
    return {
      id: 'custom',
      name: t('download.customMirrorTitle'),
      description: customPrefix.value || t('download.mirrorCustomPlaceholder'),
      type: 'prefix',
      prefix: customPrefix.value
    }
  }
  return mirrors.value.find((m) => m.id === selectedMirrorId.value) || mirrors.value[0] || DEFAULT_MIRRORS[0]
})

const latestRelease = computed(() => {
  if (releasesData.value?.latest) {
    return releasesData.value.latest
  }
  return null
})

const latestApk = computed(() => {
  return latestRelease.value?.primaryApk || null
})

const latestDownloadUrl = computed(() => {
  if (latestApk.value) {
    return resolveAssetUrl(latestApk.value, effectiveMirrorId.value, mirrors.value, customPrefix.value)
  }
  return fallbackRelease.apkUrl || ''
})

const historyReleases = computed(() => {
  const all = releasesData.value?.releases || []
  if (all.length <= 1) return []
  return all.slice(1)
})

const requirements = computed(() => [
  { k: t('download.reqSystem'), v: site.minAndroid },
  { k: t('download.reqTarget'), v: site.targetAndroid },
  { k: t('download.reqArch'), v: site.arch },
  { k: t('download.reqStorage'), v: t('download.reqStorageValue') },
  { k: t('download.reqNetwork'), v: t('download.reqNetworkValue') }
])

const steps = computed(() => [
  t('download.step1'),
  t('download.step2'),
  t('download.step3'),
  t('download.step4')
])

onMounted(async () => {
  try {
    releasesData.value = await fetchReleasesData()
  } finally {
    loading.value = false
  }
})

function onMirrorChange(id) {
  selectedMirrorId.value = id
  setStoredMirrorId(id)
}

function saveCustomConfig() {
  setStoredCustomPrefix(customPrefix.value)
  if (customPrefix.value) {
    onMirrorChange('custom')
  }
}

async function handleManualSync() {
  if (syncing.value) return
  syncing.value = true
  syncMsg.value = t('download.syncing')
  try {
    const res = await triggerManualSync()
    syncMsg.value = t('download.syncSuccess')
    setTimeout(async () => {
      releasesData.value = await fetchReleasesData()
      syncing.value = false
      setTimeout(() => (syncMsg.value = ''), 4000)
    }, 1500)
  } catch (e) {
    syncMsg.value = t('download.syncFailed')
    syncing.value = false
    setTimeout(() => (syncMsg.value = ''), 4000)
  }
}

function copyText(text, type) {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    if (type === 'sha') {
      copiedSha.value = text
      setTimeout(() => (copiedSha.value = ''), 2500)
    } else {
      copiedLink.value = text
      setTimeout(() => (copiedLink.value = ''), 2500)
    }
  })
}

function toggleNotes(id) {
  expandedNotes.value[id] = !expandedNotes.value[id]
}

function getReleaseDownloadUrl(rel) {
  const apk = rel.primaryApk
  if (!apk) return rel.htmlUrl
  return resolveAssetUrl(apk, effectiveMirrorId.value, mirrors.value, customPrefix.value)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString(undefined, { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch (_) {
    return dateStr
  }
}
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('download.eyebrow') }}</div>
        <h1>{{ t('download.title') }}</h1>
        <p>{{ t('download.lead', { version: latestRelease?.tagName || site.version }) }}</p>
      </div>

      <!-- Chinese users: Full mirror selector with GitHub + Local on top, third-party below -->
      <section v-if="isZh" class="card mirror-panel">
        <div class="mirror-panel-head">
          <div class="mirror-title-wrap">
            <span class="mirror-icon">⚡</span>
            <div>
              <div class="mirror-title">{{ t('download.mirrorSelect') }}</div>
              <div class="small muted">{{ t('download.mirrorDesc') }}</div>
            </div>
          </div>

          <div class="mirror-tools">
            <button
              type="button"
              class="btn btn-sm btn-ghost"
              @click="showMirrorConfig = !showMirrorConfig"
            >
              🛠️ {{ t('download.mirrorConfig') }}
            </button>
            <button
              type="button"
              class="btn btn-sm"
              :class="{ 'btn-loading': syncing }"
              :disabled="syncing"
              @click="handleManualSync"
            >
              🔄 {{ syncing ? t('download.syncing') : t('download.syncNow') }}
            </button>
          </div>
        </div>

        <!-- Group 1: 优先推荐 (GitHub 官方直链 + 本地高速缓存) -->
        <div class="mirror-group">
          <div class="group-label">
            <span>{{ t('download.recommendedMirrorsTitle') }}</span>
            <span class="badge-rec">推荐</span>
          </div>
          <div class="mirror-chips">
            <button
              v-for="m in recommendedMirrors"
              :key="m.id"
              type="button"
              class="chip chip-rec"
              :class="{ active: selectedMirrorId === m.id }"
              @click="onMirrorChange(m.id)"
            >
              <span class="chip-dot"></span>
              <span class="chip-name">{{ m.name }}</span>
            </button>
          </div>
        </div>

        <!-- Group 2: 第三方加速线路 (放在下面) -->
        <div class="mirror-group" style="margin-top: 10px;">
          <div class="group-label muted">
            <span>{{ t('download.thirdPartyMirrorsTitle') }}</span>
          </div>
          <div class="mirror-chips">
            <button
              v-for="m in thirdPartyMirrors"
              :key="m.id"
              type="button"
              class="chip"
              :class="{ active: selectedMirrorId === m.id }"
              @click="onMirrorChange(m.id)"
            >
              <span class="chip-dot"></span>
              <span class="chip-name">{{ m.name }}</span>
            </button>
            <button
              type="button"
              class="chip"
              :class="{ active: selectedMirrorId === 'custom' }"
              @click="onMirrorChange('custom')"
            >
              <span class="chip-dot"></span>
              <span class="chip-name">{{ t('download.customMirrorTitle') }}</span>
            </button>
          </div>
        </div>

        <div class="active-mirror-desc">
          <span class="muted">当前线路：</span>
          <strong>{{ activeMirror.name }}</strong>
          <span class="small muted" style="margin-left: 8px">{{ activeMirror.description }}</span>
        </div>

        <!-- Collapsible Mirror Config Drawer (No Cloudflare) -->
        <div v-if="showMirrorConfig" class="mirror-config-drawer">
          <div class="form-row">
            <label class="form-label" for="custom-prefix">{{ t('download.mirrorCustom') }}</label>
            <div class="form-input-group">
              <input
                id="custom-prefix"
                v-model="customPrefix"
                type="url"
                class="input"
                :placeholder="t('download.mirrorCustomPlaceholder')"
              />
              <button type="button" class="btn btn-primary" @click="saveCustomConfig">
                {{ t('download.mirrorSave') }}
              </button>
            </div>
          </div>
          <div class="small muted" style="margin-top: 6px">
            {{ t('download.customTip') }}
          </div>
        </div>

        <div v-if="syncMsg" class="sync-notification">
          {{ syncMsg }}
        </div>
      </section>

      <!-- Non-Chinese users: Clean notice, direct GitHub release -->
      <section v-else class="card mirror-panel-simple">
        <div class="small muted">
          🌐 {{ t('download.directGithubNote') }}
        </div>
      </section>

      <!-- Main Download Card + Requirements Grid -->
      <div class="dl-grid" style="margin-top: 20px">
        <div class="card dl-main">
          <div class="dl-head">
            <div>
              <div class="dl-ver">
                {{ latestRelease?.name || site.version }}
              </div>
              <div class="small muted">
                <span v-if="latestRelease?.tagName">{{ latestRelease.tagName }} · </span>
                <span v-if="latestRelease?.publishedAt">{{ formatDate(latestRelease.publishedAt) }} · </span>
                <span>{{ latestApk?.sizeFormatted || localized(fallbackRelease.size) }}</span>
              </div>
            </div>
            <span class="tag" :class="{ 'tag-warn': latestRelease?.prerelease }">
              {{ latestRelease?.prerelease ? t('download.prerelease') : t('download.stable') }}
            </span>
          </div>

          <!-- Download Action Buttons -->
          <div class="dl-actions">
            <a
              v-if="latestDownloadUrl"
              class="btn btn-primary btn-lg"
              :href="latestDownloadUrl"
              download
            >
              🚀 {{ t('download.downloadApk') }}
            </a>
            <span v-else class="btn btn-primary btn-lg disabled">
              {{ t('download.notAvailable') }}
            </span>

            <button
              v-if="latestDownloadUrl"
              type="button"
              class="btn"
              @click="copyText(latestDownloadUrl, 'link')"
            >
              📋 {{ copiedLink === latestDownloadUrl ? t('download.linkCopied') : t('download.copyLink') }}
            </button>
          </div>

          <!-- SHA256 Checksum Display -->
          <div v-if="latestRelease?.sha256" class="sha-box">
            <div class="sha-head">
              <span class="small muted">SHA-256:</span>
              <button
                type="button"
                class="btn-copy-sha"
                @click="copyText(latestRelease.sha256, 'sha')"
              >
                {{ copiedSha === latestRelease.sha256 ? t('download.sha256Copied') : t('download.sha256Copy') }}
              </button>
            </div>
            <code class="sha-text">{{ latestRelease.sha256 }}</code>
          </div>

          <p class="small muted" style="margin: 14px 0 0">
            {{ latestRelease?.body ? latestRelease.body.slice(0, 160) + '...' : localized(fallbackRelease.note) }}
          </p>

          <div class="dl-alt">
            <a :href="latestRelease?.htmlUrl || site.repo + '/releases'" target="_blank" rel="noopener" class="btn">
              {{ t('download.viewReleases') }} ↗
            </a>
            <a :href="site.repo" target="_blank" rel="noopener" class="btn">
              {{ t('download.viewSource') }} ↗
            </a>
          </div>
        </div>

        <div class="card">
          <h3 style="margin-top: 0">{{ t('download.requirements') }}</h3>
          <dl class="req-list">
            <div v-for="r in requirements" :key="r.k">
              <dt>{{ r.k }}</dt>
              <dd>{{ r.v }}</dd>
            </div>
          </dl>
        </div>
      </div>

      <!-- Historical Releases Archive (Only shown if past releases exist) -->
      <section v-if="historyReleases.length > 0" class="section" style="margin-top: 40px">
        <div class="section-head-wrap">
          <div>
            <h2>{{ t('download.historyTitle') }}</h2>
            <p class="small muted">{{ t('download.historyLead') }}</p>
          </div>
          <span class="small tag">{{ historyReleases.length }}</span>
        </div>

        <div class="history-list">
          <div v-for="rel in historyReleases" :key="rel.id || rel.tagName" class="card history-card">
            <div class="history-row">
              <div class="history-meta">
                <div class="history-ver">
                  <span class="ver-name">{{ rel.name || rel.tagName }}</span>
                  <span class="tag tag-sm" :class="{ 'tag-warn': rel.prerelease }">
                    {{ rel.prerelease ? t('download.prerelease') : t('download.stable') }}
                  </span>
                </div>
                <div class="small muted">
                  <span>{{ rel.tagName }}</span>
                  <span v-if="rel.publishedAt"> · {{ formatDate(rel.publishedAt) }}</span>
                  <span v-if="rel.primaryApk"> · {{ rel.primaryApk.sizeFormatted }}</span>
                </div>
              </div>

              <div class="history-actions">
                <a
                  class="btn btn-sm btn-primary"
                  :href="getReleaseDownloadUrl(rel)"
                  download
                >
                  🚀 {{ t('download.downloadApk') }}
                </a>
                <button
                  v-if="rel.sha256"
                  type="button"
                  class="btn btn-sm"
                  @click="copyText(rel.sha256, 'sha')"
                >
                  {{ copiedSha === rel.sha256 ? t('download.sha256Copied') : 'SHA-256' }}
                </button>
                <button
                  v-if="rel.body"
                  type="button"
                  class="btn btn-sm btn-ghost"
                  @click="toggleNotes(rel.id || rel.tagName)"
                >
                  {{ expandedNotes[rel.id || rel.tagName] ? t('download.hideNotes') : t('download.showNotes') }}
                </button>
              </div>
            </div>

            <div v-if="expandedNotes[rel.id || rel.tagName]" class="history-notes">
              <pre class="notes-pre">{{ rel.body }}</pre>
            </div>
          </div>
        </div>
      </section>

      <!-- Steps & Explanation -->
      <section class="section" style="margin-top: 40px">
        <h2>{{ t('download.installSteps') }}</h2>
        <ol class="steps">
          <li v-for="(s, i) in steps" :key="i">{{ s }}</li>
        </ol>
      </section>

      <section class="section">
        <h2>{{ t('download.packageTitle') }}</h2>
        <div class="card">
          <p style="margin: 0; font-size: 0.9rem">{{ t('download.packageDesc') }}</p>
        </div>
      </section>

      <div class="note">
        <div class="note-title">{{ t('download.safetyTitle') }}</div>
        <ul style="font-size: 0.89rem; margin: 0">
          <li>{{ t('download.safety1') }}</li>
          <li>{{ t('download.safety2') }}</li>
          <li>{{ t('download.safety3') }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mirror-panel {
  background: var(--bg-soft);
  border: 1px solid var(--border-strong);
  padding: 16px 20px;
}

.mirror-panel-simple {
  background: var(--bg-soft);
  border: 1px solid var(--border);
  padding: 12px 18px;
}

.mirror-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.mirror-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mirror-icon {
  font-size: 1.4rem;
  background: var(--accent-soft);
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mirror-title {
  font-weight: 600;
  font-size: 1.05rem;
  color: var(--text);
}

.mirror-tools {
  display: flex;
  gap: 8px;
}

.mirror-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.group-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text);
}

.badge-rec {
  background: var(--accent-soft);
  color: var(--accent-text);
  font-size: 0.72rem;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.mirror-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--text-soft);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.86rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip:hover {
  border-color: var(--accent);
  color: var(--accent-text);
}

.chip.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #ffffff;
  font-weight: 600;
}

.chip-rec {
  border-color: var(--border-strong);
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.8;
}

.active-mirror-desc {
  font-size: 0.85rem;
  color: var(--text-soft);
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--border);
}

.mirror-config-drawer {
  margin-top: 14px;
  padding: 12px 14px;
  background: var(--bg);
  border-radius: var(--radius);
  border: 1px solid var(--border);
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text);
}

.form-input-group {
  display: flex;
  gap: 8px;
}

.input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-soft);
  color: var(--text);
  font-size: 0.88rem;
}

.sync-notification {
  margin-top: 10px;
  padding: 6px 12px;
  background: var(--accent-soft);
  color: var(--accent-text);
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
}

.dl-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
}

@media (min-width: 820px) {
  .dl-grid {
    grid-template-columns: 1.6fr 1fr;
  }
}

.dl-main {
  display: flex;
  flex-direction: column;
}

.dl-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.dl-ver {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 4px;
}

.dl-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.btn-lg {
  padding: 10px 22px;
  font-size: 1rem;
}

.btn-sm {
  padding: 5px 10px;
  font-size: 0.82rem;
}

.btn-ghost {
  background: transparent;
  border: 1px dashed var(--border-strong);
}

.sha-box {
  margin-top: 14px;
  padding: 10px 12px;
  background: var(--bg-sunken);
  border-radius: 6px;
  border: 1px solid var(--border);
}

.sha-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.btn-copy-sha {
  background: none;
  border: none;
  color: var(--accent-text);
  font-size: 0.82rem;
  cursor: pointer;
  padding: 0;
}
.btn-copy-sha:hover {
  text-decoration: underline;
}

.sha-text {
  font-size: 0.8rem;
  word-break: break-all;
  display: block;
}

.dl-alt {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 18px;
}

.section-head-wrap {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-card {
  padding: 14px 18px;
}

.history-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.history-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.history-ver {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ver-name {
  font-weight: 600;
  font-size: 1rem;
}

.tag-sm {
  font-size: 0.72rem;
  padding: 2px 7px;
}

.tag-warn {
  background: #fdf3e7;
  color: #b5811f;
  border: 1px solid #fae1c3;
}

.history-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.history-notes {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}

.notes-pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 0.86rem;
  color: var(--text-soft);
  line-height: 1.6;
}

.req-list {
  margin: 0;
}
.req-list > div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 7px 0;
  font-size: 0.87rem;
  border-bottom: 1px solid var(--border);
}
.req-list > div:last-child {
  border-bottom: 0;
}
.req-list dt {
  color: var(--text-faint);
  white-space: nowrap;
}
.req-list dd {
  margin: 0;
  text-align: right;
}

.steps {
  max-width: 62ch;
}

@media (max-width: 640px) {
  .mirror-panel-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .mirror-tools {
    width: 100%;
    justify-content: space-between;
  }
  .form-input-group {
    flex-direction: column;
  }
  .dl-grid {
    gap: 14px;
  }
  .dl-head {
    padding-bottom: 14px;
    margin-bottom: 14px;
  }
  .dl-ver {
    font-size: 1.08rem;
  }
  .dl-actions,
  .dl-alt {
    flex-direction: column;
  }
  .dl-actions .btn,
  .dl-alt .btn {
    width: 100%;
  }
  .dl-alt {
    padding-top: 14px;
    gap: 9px;
  }
  .req-list > div {
    font-size: 0.85rem;
    padding: 8px 0;
    gap: 10px;
  }
  .req-list dt {
    white-space: normal;
  }
  .history-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .history-actions {
    width: 100%;
    justify-content: flex-start;
  }
  .steps {
    padding-left: 1.15em;
  }
}
</style>
