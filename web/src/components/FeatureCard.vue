<script setup>
import { computed } from 'vue'
import { localized } from '../i18n'

const props = defineProps({
  feature: { type: Object, required: true },
  compact: { type: Boolean, default: false }
})

const content = computed(() => localized(props.feature))
</script>

<template>
  <RouterLink :to="'/features/' + feature.id" class="fcard" :class="{ compact }">
    <div class="fcard-top">
      <span class="fcard-title">{{ content.title }}</span>
      <span class="badge">{{ content.badge }}</span>
    </div>
    <p class="fcard-sum">{{ content.summary }}</p>
    <div class="fcard-tags">
      <span v-for="tg in content.tags" :key="tg" class="tag">{{ tg }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.fcard {
  display: block;
  position: relative;
  padding: 18px 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg);
  color: inherit;
  text-decoration: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.fcard:hover { border-color: var(--border-strong); box-shadow: var(--shadow); text-decoration: none; }
.fcard:active { background: var(--bg-soft); }

.fcard::before {
  content: '';
  position: absolute;
  left: 0;
  top: 18px;
  bottom: 18px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: v-bind('feature.accent');
  opacity: 0.85;
}

.fcard-top { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; margin-bottom: 7px; }
.fcard-title { font-weight: 600; font-size: 1rem; color: var(--text); }
.fcard-sum { margin: 0 0 12px; font-size: 0.89rem; color: var(--text-soft); line-height: 1.7; }
.fcard-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.compact .fcard-sum { margin-bottom: 10px; }

@media (max-width: 640px) {
  .fcard { padding: 16px 18px; }
  .fcard-title { font-size: 0.97rem; }
}
</style>
