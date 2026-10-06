<script setup>
import { site } from '../data/features'
import { t } from '../i18n'
const year = new Date().getFullYear()
</script>

<template>
  <footer class="ftr">
    <div class="container">
      <div class="ftr-grid">
        <div>
          <div class="ftr-brand">
            <img src="/logo.svg" alt="" width="24" height="24" />
            <span>{{ site.name }}</span>
          </div>
          <p class="ftr-desc">{{ t('home.lead') }}</p>
        </div>
        <div>
          <div class="ftr-title">{{ t('footer.nav') }}</div>
          <RouterLink to="/features">{{ t('footer.featuresOverview') }}</RouterLink>
          <RouterLink to="/download">{{ t('nav.download') }}</RouterLink>
          <RouterLink to="/docs">{{ t('footer.docs') }}</RouterLink>
          <RouterLink to="/changelog">{{ t('nav.changelog') }}</RouterLink>
        </div>
        <div>
          <div class="ftr-title">{{ t('footer.support') }}</div>
          <RouterLink to="/faq">{{ t('nav.faq') }}</RouterLink>
          <RouterLink to="/about">{{ t('footer.aboutThanks') }}</RouterLink>
          <a :href="site.repo" target="_blank" rel="noopener">{{ t('footer.repo') }} ↗</a>
          <a :href="site.repo + '/issues'" target="_blank" rel="noopener">{{ t('footer.issues') }} ↗</a>
        </div>
      </div>

      <div class="ftr-bottom">
        <span>{{ t('footer.copyright', { year, maintainer: site.maintainer, author: site.author }) }}</span>
        <span>{{ t('footer.license', { license: site.license }) }}</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.ftr {
  border-top: 1px solid var(--border);
  background: var(--bg-soft);
  padding: 36px 0 26px;
  margin-top: auto;
}

.ftr-grid { display: grid; gap: 26px; grid-template-columns: 1fr; }

@media (min-width: 700px) {
  .ftr-grid { grid-template-columns: 2fr 1fr 1fr; gap: 28px; }
}

.ftr-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 8px;
}
.ftr-brand img { border-radius: 6px; flex: none; }

.ftr-desc {
  font-size: 0.87rem;
  color: var(--text-faint);
  max-width: 46ch;
  margin: 0;
}

.ftr-title {
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-faint);
  margin-bottom: 8px;
}

.ftr-grid a {
  display: flex;
  align-items: center;
  font-size: 0.89rem;
  color: var(--text-soft);
  margin-bottom: 2px;

  min-height: 44px;
  padding: 4px 0;
}
.ftr-grid a:hover { color: var(--accent-text); }

.ftr-bottom {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;
  justify-content: space-between;
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
  font-size: 0.82rem;
  color: var(--text-faint);
}

@media (max-width: 640px) {
  .ftr { padding: 30px 0 22px; }
  .ftr-grid { gap: 22px; }

  .ftr-grid > div:nth-child(2),
  .ftr-grid > div:nth-child(3) { display: inline-block; }
  .ftr-bottom { flex-direction: column; gap: 4px; margin-top: 24px; }
}
</style>