<script setup>
import { t } from '../i18n'
</script>

<template>
  <div class="page">
    <div class="container nf">
      <div class="nf-code">404</div>
      <h1>{{ t('meta.notFoundTitle') }}</h1>
      <p>{{ t('common.notFoundDesc') }}</p>
      <div class="nf-actions">
        <RouterLink to="/" class="btn btn-primary">{{ t('common.backToHome') }}</RouterLink>
        <RouterLink to="/docs" class="btn">{{ t('common.viewDocs') }}</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.nf { text-align: center; padding: 70px 20px; }

.nf-code {
  font-family: var(--mono);
  font-size: 3.4rem;
  font-weight: 700;
  color: var(--border-strong);
  line-height: 1;
  margin-bottom: 10px;
}

.nf p { margin-bottom: 26px; }
.nf-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }

@media (max-width: 640px) {
  .nf { padding: 52px 16px; }
  .nf-code { font-size: 2.8rem; }
  .nf-actions { flex-direction: column; }
  .nf-actions .btn { width: 100%; }
}
</style>