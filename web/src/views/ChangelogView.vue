<script setup>
import { computed } from 'vue'
import { changelog, roadmap } from '../data/changelog'
import { site } from '../data/features'
import { t, localized } from '../i18n'

const typeLabel = (type) =>
  ({ feat: t('changelog.typeFeat'), fix: t('changelog.typeFix'), chore: t('changelog.typeChore') }[type] || type)

const releases = computed(() =>
  changelog.map((r) => ({
    version: r.version,
    status: r.status,
    highlight: localized(r.highlight),
    date: localized(r.date),
    items: r.items.map((it) => ({ type: it.type, text: localized(it.text) }))
  }))
)

const plans = computed(() =>
  roadmap.map((r) => ({ title: localized(r.title), desc: localized(r.desc) }))
)
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('changelog.eyebrow') }}</div>
        <h1>{{ t('changelog.title') }}</h1>
        <p>{{ t('changelog.lead', { version: site.version }) }}</p>
      </div>

      <div class="timeline">
        <section v-for="rel in releases" :key="rel.version" class="release">
          <div class="release-side">
            <div class="release-ver">{{ rel.version }}</div>
            <div class="small muted">{{ rel.date }}</div>
            <span v-if="rel.status === 'current'" class="badge" style="margin-top: 6px">
              {{ t('changelog.latest') }}
            </span>
          </div>

          <div class="release-body">
            <div class="release-hl">{{ rel.highlight }}</div>
            <ul class="changes">
              <li v-for="(item, i) in rel.items" :key="i">
                <span class="ctype" :data-type="item.type">{{ typeLabel(item.type) }}</span>
                <span>{{ item.text }}</span>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <section class="section" style="margin-top: 48px">
        <h2>{{ t('changelog.roadmap') }}</h2>
        <div class="grid grid-2">
          <div v-for="p in plans" :key="p.title" class="card">
            <h3 style="font-size: 0.95rem; margin-bottom: 5px">{{ p.title }}</h3>
            <p style="margin: 0; font-size: 0.87rem">{{ p.desc }}</p>
          </div>
        </div>
        <p class="small muted" style="margin-top: 14px">{{ t('changelog.roadmapNote') }}</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.timeline { display: flex; flex-direction: column; }

.release {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr;
  padding: 24px 0;
  border-bottom: 1px solid var(--border);
}
.release:last-child { border-bottom: 0; }

@media (min-width: 760px) {
  .release { grid-template-columns: 170px minmax(0, 1fr); gap: 32px; }
}

.release-ver { font-weight: 600; font-size: 0.98rem; color: var(--accent-text); }

.release-hl {
  font-size: 0.93rem;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 12px;
}

.changes { list-style: none; padding: 0; margin: 0; }

.changes li {
  display: flex;
  gap: 10px;
  align-items: baseline;
  font-size: 0.89rem;
  color: var(--text-soft);
  padding: 4px 0;
}

.ctype {
  flex: none;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 4px;
  background: var(--bg-sunken);
  color: var(--text-faint);
  min-width: 34px;
  text-align: center;
}
.ctype[data-type='feat'] { background: var(--accent-soft); color: var(--accent-text); }
.ctype[data-type='fix'] { background: rgba(176, 69, 60, 0.1); color: var(--danger); }

@media (max-width: 640px) {
  .release { padding: 20px 0; gap: 10px; }
  .release-side { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
  .release-side .badge { margin-top: 0 !important; }
  .release-hl { font-size: 0.91rem; margin-bottom: 10px; }
  .changes li { gap: 9px; font-size: 0.87rem; align-items: flex-start; padding: 5px 0; }
  .ctype { margin-top: 2px; }
}
</style>