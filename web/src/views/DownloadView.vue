<script setup>
import { computed } from 'vue'
import { site, release } from '../data/features'
import { t, localized } from '../i18n'

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
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('download.eyebrow') }}</div>
        <h1>{{ t('download.title') }}</h1>
        <p>{{ t('download.lead', { version: site.version }) }}</p>
      </div>

      <div class="dl-grid">
        <div class="card dl-main">
          <div class="dl-head">
            <div>
              <div class="dl-ver">{{ site.version }}</div>
              <div class="small muted">
                {{ t('download.versionCode', { code: site.versionCode }) }} · {{ localized(release.size) }}
              </div>
            </div>
            <span class="tag">{{ t('download.devPreview') }}</span>
          </div>

          <div class="dl-actions">
            <a v-if="release.published" class="btn btn-primary" :href="release.apkUrl">
              {{ t('download.downloadApk') }}
            </a>
            <span v-else class="btn btn-primary disabled">{{ t('download.notAvailable') }}</span>
          </div>

          <p class="small muted" style="margin: 14px 0 0">{{ localized(release.note) }}</p>

          <div class="dl-alt">
            <a :href="site.repo + '/releases'" target="_blank" rel="noopener" class="btn">
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
.dl-grid { display: grid; gap: 16px; grid-template-columns: 1fr; }

@media (min-width: 820px) {
  .dl-grid { grid-template-columns: 1.6fr 1fr; }
}

.dl-main { display: flex; flex-direction: column; }

.dl-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.dl-ver { font-size: 1.15rem; font-weight: 600; margin-bottom: 3px; }
.dl-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.dl-alt { display: flex; gap: 10px; flex-wrap: wrap; margin-top: auto; padding-top: 18px; }

.req-list { margin: 0; }
.req-list > div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 7px 0;
  font-size: 0.87rem;
  border-bottom: 1px solid var(--border);
}
.req-list > div:last-child { border-bottom: 0; }
.req-list dt { color: var(--text-faint); white-space: nowrap; }
.req-list dd { margin: 0; text-align: right; }

.steps { max-width: 62ch; }

@media (max-width: 640px) {
  .dl-grid { gap: 14px; }
  .dl-head { padding-bottom: 14px; margin-bottom: 14px; }
  .dl-ver { font-size: 1.08rem; }
  .dl-actions, .dl-alt { flex-direction: column; }
  .dl-actions .btn, .dl-alt .btn { width: 100%; }
  .dl-alt { padding-top: 14px; gap: 9px; }
  .req-list > div { font-size: 0.85rem; padding: 8px 0; gap: 10px; }
  .req-list dt { white-space: normal; }
  .steps { padding-left: 1.15em; }
}
</style>