<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { docPages, findDoc } from '../data/docs'
import { t, localized, locale } from '../i18n'
import { setTitle } from '../router'

const route = useRoute()
const doc = computed(() => findDoc(route.params.slug))
const content = computed(() => (doc.value ? localized(doc.value) : null))

const index = computed(() => docPages.findIndex((d) => d.slug === route.params.slug))
const prev = computed(() => {
  const i = index.value
  return i > 0 ? { slug: docPages[i - 1].slug, title: localized(docPages[i - 1]).title } : null
})
const next = computed(() => {
  const i = index.value
  return i >= 0 && i < docPages.length - 1
    ? { slug: docPages[i + 1].slug, title: localized(docPages[i + 1]).title }
    : null
})
const toc = computed(() => docPages.map((d) => ({ slug: d.slug, title: localized(d).title })))

watch(
  [() => doc.value, locale],
  () => setTitle(content.value ? content.value.title : t('meta.docsTitle')),
  { immediate: true }
)
</script>

<template>
  <div class="page">
    <div class="container">
      <template v-if="doc">
        <nav class="crumbs small">
          <RouterLink to="/">{{ t('nav.home') }}</RouterLink>
          <span>/</span>
          <RouterLink to="/docs">{{ t('nav.docs') }}</RouterLink>
          <span>/</span>
          <span class="muted">{{ content.title }}</span>
        </nav>

        <div class="doc-layout">
          <aside class="doc-nav">
            <div class="doc-nav-title">{{ t('common.toc') }}</div>
            <RouterLink v-for="d in toc" :key="d.slug" :to="'/docs/' + d.slug" class="doc-nav-link">
              {{ d.title }}
            </RouterLink>
          </aside>

          <article class="prose doc-body">
            <h1>{{ content.title }}</h1>
            <p class="doc-summary">{{ content.summary }}</p>

            <section v-for="(s, i) in content.sections" :key="i">
              <h3>{{ s.heading }}</h3>
              <p v-for="(p, j) in s.paragraphs || []" :key="'p' + j">{{ p }}</p>
              <ul v-if="s.list">
                <li v-for="(li, j) in s.list" :key="'l' + j">{{ li }}</li>
              </ul>
            </section>

            <div class="doc-pager">
              <RouterLink v-if="prev" :to="'/docs/' + prev.slug" class="pager-item">
                <span class="pager-dir">← {{ t('common.prev') }}</span>
                <span class="pager-title">{{ prev.title }}</span>
              </RouterLink>
              <span v-else></span>
              <RouterLink v-if="next" :to="'/docs/' + next.slug" class="pager-item pager-right">
                <span class="pager-dir">{{ t('common.next') }} →</span>
                <span class="pager-title">{{ next.title }}</span>
              </RouterLink>
              <span v-else></span>
            </div>
          </article>
        </div>
      </template>

      <template v-else>
        <div class="page-head">
          <h1>{{ t('common.notFoundDoc') }}</h1>
          <p>{{ t('common.notFoundDocDesc') }}</p>
        </div>
        <RouterLink to="/docs" class="btn">{{ t('nav.docs') }}</RouterLink>
      </template>
    </div>
  </div>
</template>

<style scoped>
.crumbs { display: flex; gap: 8px; margin-bottom: 18px; color: var(--text-faint); }

.doc-layout { display: grid; gap: 34px; grid-template-columns: 1fr; align-items: start; }

@media (min-width: 900px) {
  .doc-layout { grid-template-columns: 200px minmax(0, 1fr); gap: 48px; }
}

.doc-nav { display: none; position: sticky; top: 78px; font-size: 0.87rem; }
@media (min-width: 900px) { .doc-nav { display: block; } }

.doc-nav-title {
  font-size: 0.76rem;
  font-weight: 600;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: var(--text-faint);
  margin-bottom: 10px;
}

.doc-nav-link {
  display: block;
  padding: 5px 10px;
  border-left: 2px solid var(--border);
  color: var(--text-soft);
  margin-bottom: 2px;
}
.doc-nav-link:hover { color: var(--text); border-left-color: var(--border-strong); text-decoration: none; }
.doc-nav-link.router-link-active {
  color: var(--accent-text);
  border-left-color: var(--accent);
  font-weight: 500;
}

.doc-body h1 { font-size: 1.55rem; }

.doc-summary {
  font-size: 0.95rem;
  color: var(--text-faint);
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 26px;
}

.doc-pager {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 44px;
  padding-top: 22px;
  border-top: 1px solid var(--border);
}

.pager-item { display: flex; flex-direction: column; gap: 2px; max-width: 46%; }
.pager-right { text-align: right; margin-left: auto; }
.pager-dir { font-size: 0.78rem; color: var(--text-faint); }
.pager-title { font-size: 0.9rem; font-weight: 500; }

@media (max-width: 640px) {
  .crumbs { font-size: 0.82rem; margin-bottom: 14px; }
  .doc-body h1 { font-size: 1.4rem; }
  .doc-summary { font-size: 0.92rem; padding-bottom: 15px; margin-bottom: 22px; }
  .doc-pager { gap: 12px; margin-top: 34px; padding-top: 18px; }
  .pager-item { max-width: 48%; }
  .pager-title { font-size: 0.85rem; }
}

@media (max-width: 640px) {
  .crumbs a { display: inline-flex; align-items: center; min-height: 40px; padding: 0 2px; margin: -8px 0; }
}
</style>