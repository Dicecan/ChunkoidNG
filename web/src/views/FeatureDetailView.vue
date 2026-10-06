<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { features, findFeature } from '../data/features'
import { t, localized, locale } from '../i18n'
import FeatureCard from '../components/FeatureCard.vue'
import { setTitle } from '../router'

const route = useRoute()
const feature = computed(() => findFeature(route.params.id))
const content = computed(() => (feature.value ? localized(feature.value) : null))

const others = computed(() => features.filter((f) => f.id !== route.params.id).slice(0, 3))

watch(
  [() => feature.value, locale],
  () => {
    setTitle(content.value ? content.value.title : t('meta.featureDetailTitle'))
  },
  { immediate: true }
)
</script>

<template>
  <div class="page">
    <div class="container">
      <template v-if="feature">
        <nav class="crumbs small">
          <RouterLink to="/">{{ t('nav.home') }}</RouterLink>
          <span>/</span>
          <RouterLink to="/features">{{ t('nav.features') }}</RouterLink>
          <span>/</span>
          <span class="muted">{{ content.title }}</span>
        </nav>

        <div class="page-head">
          <div class="badge" style="margin-bottom: 10px">{{ content.badge }}</div>
          <h1>{{ content.title }}</h1>
          <p>{{ content.summary }}</p>
        </div>

        <div class="detail-grid">
          <article class="prose">
            <h2>{{ t('features.aboutFeature') }}</h2>
            <ul>
              <li v-for="(item, i) in content.detail" :key="i">{{ item }}</li>
            </ul>

            <div v-if="content.notes && content.notes.length" class="note">
              <div class="note-title">{{ t('features.notes') }}</div>
              <ul style="margin: 0">
                <li v-for="(n, i) in content.notes" :key="i">{{ n }}</li>
              </ul>
            </div>

            <h2>{{ t('common.relatedDocs') }}</h2>
            <p class="small">
              <RouterLink to="/docs">{{ t('common.readDocs') }}</RouterLink>
              {{ t('common.relatedDocsDesc') }}
            </p>
          </article>

          <aside class="side">
            <div class="card">
              <h3 style="font-size: 0.92rem">{{ t('common.tags') }}</h3>
              <div class="side-tags">
                <span v-for="tg in content.tags" :key="tg" class="tag">{{ tg }}</span>
              </div>
            </div>
            <div class="card">
              <h3 style="font-size: 0.92rem">{{ t('common.otherFeatures') }}</h3>
              <RouterLink v-for="o in others" :key="o.id" :to="'/features/' + o.id" class="side-link">
                {{ localized(o).title }}
              </RouterLink>
            </div>
            <div class="card">
              <h3 style="font-size: 0.92rem">{{ t('common.getStarted') }}</h3>
              <RouterLink to="/download" class="btn btn-primary" style="width: 100%; justify-content: center">
                {{ t('common.getApp') }}
              </RouterLink>
            </div>
          </aside>
        </div>
      </template>

      <template v-else>
        <div class="page-head">
          <h1>{{ t('common.notFoundFeature') }}</h1>
          <p>{{ t('common.notFoundFeatureDesc') }}</p>
        </div>
        <RouterLink to="/features" class="btn">{{ t('common.backToFeatures') }}</RouterLink>

        <div class="grid grid-2" style="margin-top: 32px">
          <FeatureCard v-for="f in features" :key="f.id" :feature="f" />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.crumbs { display: flex; gap: 8px; margin-bottom: 18px; color: var(--text-faint); }

.detail-grid { display: grid; gap: 32px; grid-template-columns: 1fr; align-items: start; }

@media (min-width: 900px) {
  .detail-grid { grid-template-columns: minmax(0, 1fr) 260px; gap: 44px; }
}

.side { display: flex; flex-direction: column; gap: 14px; }
.side-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.side-link {
  display: block;
  font-size: 0.89rem;
  padding: 5px 0;
  border-bottom: 1px solid var(--border);
  color: var(--text-soft);
}
.side-link:last-child { border-bottom: 0; }
.side-link:hover { color: var(--accent-text); }

.card h3 { margin-top: 0; margin-bottom: 10px; }

@media (max-width: 640px) {
  .crumbs { font-size: 0.82rem; margin-bottom: 14px; }
  .detail-grid { gap: 24px; }
  .side-link { min-height: 44px; display: flex; align-items: center; padding: 8px 0; }
}

@media (max-width: 640px) {
  .crumbs a { display: inline-flex; align-items: center; min-height: 40px; padding: 0 2px; margin: -8px 0; }
}
</style>