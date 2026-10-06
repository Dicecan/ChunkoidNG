<script setup>
import { computed } from 'vue'
import { docPages } from '../data/docs'
import { site } from '../data/features'
import { t, localized } from '../i18n'

const docs = computed(() => docPages.map((d) => ({ slug: d.slug, ...localized(d) })))
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('docs.eyebrow') }}</div>
        <h1>{{ t('docs.title') }}</h1>
        <p>{{ t('docs.lead') }}</p>
      </div>

      <div class="doc-list">
        <RouterLink v-for="(doc, i) in docs" :key="doc.slug" :to="'/docs/' + doc.slug" class="doc-item">
          <div class="doc-idx">{{ String(i + 1).padStart(2, '0') }}</div>
          <div>
            <div class="doc-title">{{ doc.title }}</div>
            <div class="doc-sum">{{ doc.summary }}</div>
          </div>
          <div class="doc-arrow">→</div>
        </RouterLink>
      </div>

      <div class="note" style="margin-top: 32px">
        <div class="note-title">{{ t('docs.faqTitle') }}</div>
        <p style="margin: 0; font-size: 0.89rem">
          {{ t('docs.faqDesc') }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.doc-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.doc-item {
  display: grid;
  grid-template-columns: 40px 1fr 20px;
  gap: 14px;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  color: inherit;
  text-decoration: none;
  transition: background 0.12s;
}
.doc-item:last-child { border-bottom: 0; }
.doc-item:hover { background: var(--bg-soft); text-decoration: none; }

.doc-idx { font-family: var(--mono); font-size: 0.85rem; color: var(--text-faint); }
.doc-title { font-weight: 600; font-size: 0.97rem; margin-bottom: 2px; }
.doc-sum { font-size: 0.86rem; color: var(--text-soft); }
.doc-arrow { color: var(--text-faint); text-align: right; }
.doc-item:hover .doc-arrow { color: var(--accent-text); }

@media (max-width: 640px) {
  .doc-item { grid-template-columns: 30px 1fr 16px; gap: 11px; padding: 14px 16px; min-height: 64px; }
  .doc-idx { font-size: 0.8rem; }
  .doc-title { font-size: 0.95rem; }
  .doc-sum { font-size: 0.84rem; line-height: 1.6; }
}
</style>