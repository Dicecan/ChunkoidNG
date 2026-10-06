<script setup>
import { computed } from 'vue'
import { features, site, release } from '../data/features'
import { t, localized } from '../i18n'
import FeatureCard from '../components/FeatureCard.vue'
import HeroParallax from '../components/HeroParallax.vue'

const facts = computed(() => [
  { k: t('home.factVersion'), v: site.version },
  { k: t('home.factSystem'), v: site.minAndroid },
  { k: t('home.factArch'), v: site.arch },
  { k: t('home.factLicense'), v: site.license }
])

const highlights = computed(() => [
  { title: t('home.why1Title'), desc: t('home.why1Desc') },
  { title: t('home.why2Title'), desc: t('home.why2Desc') },
  { title: t('home.why3Title'), desc: t('home.why3Desc') },
  { title: t('home.why4Title'), desc: t('home.why4Desc') }
])

const stack = computed(() => [
  { k: t('home.stackEngine'), v: site.engine },
  { k: t('home.stackRuntime'), v: site.runtime },
  { k: t('home.stackTarget'), v: site.targetAndroid },
  { k: t('home.stackLang'), v: t('home.stackLangValue') },
  { k: t('home.factLicense'), v: site.license }
])
</script>

<template>
  <div class="page">
    <section class="hero">
      <HeroParallax class="hero-bg" />
      <div class="hero-scrim" aria-hidden="true"></div>

      <div class="container hero-inner">
        <div class="hero-top">
          <div class="hero-copy">
            <div class="hero-eyebrow">{{ t('home.eyebrow') }}</div>
            <h1 class="hero-title">{{ t('home.titleLine1') }}<br />{{ t('home.titleLine2') }}</h1>
            <p class="hero-lead">{{ t('home.lead') }}</p>

            <div class="hero-actions">
              <RouterLink to="/download" class="btn btn-primary">{{ t('common.getApp') }}</RouterLink>
              <RouterLink to="/features" class="btn btn-on-glass">{{ t('common.browseFeatures') }}</RouterLink>
            </div>
          </div>
        </div>

        <div class="hero-facts glass">
          <dl class="fact-list">
            <div v-for="f in facts" :key="f.k">
              <dt>{{ f.k }}</dt>
              <dd>{{ f.v }}</dd>
            </div>
          </dl>
        </div>
      </div>
    </section>

    <section class="container section section-top">
      <h2>{{ t('home.coreFeatures') }}</h2>
      <div class="grid grid-2">
        <FeatureCard v-for="f in features" :key="f.id" :feature="f" />
      </div>
      <p class="small muted" style="margin-top: 14px">
        <RouterLink to="/features">{{ t('home.viewAll') }} →</RouterLink>
      </p>
    </section>

    <section class="container section">
      <h2>{{ t('home.whyTitle') }}</h2>
      <div class="grid grid-2">
        <div v-for="h in highlights" :key="h.title" class="card">
          <h3 style="margin-bottom: 6px">{{ h.title }}</h3>
          <p style="margin: 0; font-size: 0.89rem">{{ h.desc }}</p>
        </div>
      </div>
    </section>

    <section class="container section">
      <h2>{{ t('home.stackTitle') }}</h2>
      <div class="card">
        <table class="table">
          <tbody>
            <tr v-for="row in stack" :key="row.k">
              <th style="width: 180px">{{ row.k }}</th>
              <td>{{ row.v }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="note" style="margin-top: 16px">
        <div class="note-title">{{ t('home.aboutVersion') }}</div>
        <p style="margin: 0; font-size: 0.89rem">
          {{ t('home.aboutVersionDesc', { version: site.version }) }}
          <RouterLink to="/docs/getting-started">{{ t('common.readDocs') }}</RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>

.hero {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--border);
  background: #0a0f0d;
}

.hero-bg { position: absolute; inset: 0; z-index: 0; }

.hero-scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(90deg,
      rgba(8, 14, 12, 0.10) 0%,
      rgba(8, 14, 12, 0.34) 46%,
      rgba(8, 14, 12, 0.62) 100%),
    linear-gradient(180deg,
      rgba(8, 14, 12, 0.42) 0%,
      rgba(8, 14, 12, 0.18) 40%,
      rgba(8, 14, 12, 0.5) 78%,
      var(--bg) 100%);
}

.hero-inner {
  position: relative;
  z-index: 2;
  padding-top: 76px;
  padding-bottom: 40px;
}

.hero-top { display: flex; justify-content: flex-end; }

.hero-copy {
  max-width: 640px;
  text-align: right;
  color: #f2f6f4;
}

.hero-eyebrow {
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #b6e2cd;
  margin-bottom: 12px;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.55);
}

.hero-title {
  font-size: clamp(1.9rem, 5vw, 2.7rem);
  line-height: 1.22;
  letter-spacing: -0.02em;
  margin-bottom: 16px;
  color: #ffffff;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.6), 0 1px 3px rgba(0, 0, 0, 0.5);
}

.hero-lead {
  font-size: 1rem;
  margin: 0 0 24px auto;
  max-width: 46ch;
  color: rgba(242, 246, 244, 0.9);
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.6), 0 1px 2px rgba(0, 0, 0, 0.45);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.btn-on-glass {
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px) saturate(150%);
  -webkit-backdrop-filter: blur(10px) saturate(150%);
  border-color: rgba(255, 255, 255, 0.32);
  color: #ffffff;
}
.btn-on-glass:hover {
  background: rgba(255, 255, 255, 0.22);
  border-color: rgba(255, 255, 255, 0.5);
}

.glass {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(18px) saturate(150%);
  -webkit-backdrop-filter: blur(18px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-lg);
  box-shadow: 0 16px 40px -18px rgba(0, 0, 0, 0.5);
}

.hero-facts { margin-top: 34px; padding: 16px 22px; }

.fact-list { display: grid; gap: 4px 40px; grid-template-columns: 1fr; margin: 0; }
@media (min-width: 620px) { .fact-list { grid-template-columns: repeat(2, 1fr); } }
@media (min-width: 1000px) { .fact-list { grid-template-columns: repeat(4, 1fr); } }

.fact-list > div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 5px 0;
  font-size: 0.87rem;
}
.fact-list dt { color: rgba(242, 246, 244, 0.62); white-space: nowrap; }
.fact-list dd { margin: 0; color: #ffffff; text-align: right; }

@media (max-width: 900px) {
  .hero {
    display: flex;
    flex-direction: column;
    background: var(--bg);
  }

  .hero-bg {
    position: relative;
    inset: auto;
    width: 100%;
    aspect-ratio: 3 / 2;
    max-height: 78vw;
    flex: none;
    border-bottom: 1px solid var(--border);
  }

  .hero-scrim {
    background:
      linear-gradient(180deg,
        rgba(8, 14, 12, 0.16) 0%,
        rgba(8, 14, 12, 0.04) 30%,
        rgba(8, 14, 12, 0.10) 66%,
        rgba(8, 14, 12, 0.42) 100%);
  }

  .hero-inner { padding-top: 20px; padding-bottom: 24px; }
  .hero-top { justify-content: flex-start; }
  .hero-copy { text-align: left; max-width: none; color: var(--text); }

  .hero-eyebrow {
    color: var(--accent);
    text-shadow: none;
    font-size: 0.76rem;
    margin-bottom: 8px;
  }

  .hero-title {
    color: var(--text);
    font-size: clamp(1.5rem, 6vw, 1.85rem);
    text-shadow: none;
    margin-bottom: 10px;
  }

  .hero-lead {
    color: var(--text-soft);
    text-shadow: none;
    font-size: 0.94rem;
    line-height: 1.65;
    margin: 0 0 16px;
    max-width: none;
  }

  .hero-actions { justify-content: flex-start; gap: 9px; }
  .hero-actions .btn { min-height: 44px; }

  .btn-on-glass {
    background: var(--bg);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    border-color: var(--border-strong);
    color: var(--text);
  }
  .btn-on-glass:hover { background: var(--bg-soft); border-color: var(--accent); }

  .hero-facts {
    margin-top: 16px;
    padding: 13px 16px;
    background: var(--bg-soft);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: none;
  }

  .fact-list { grid-template-columns: repeat(2, 1fr); gap: 3px 16px; }
  .fact-list > div { font-size: 0.8rem; padding: 4px 0; gap: 8px; }
  .fact-list dt { color: var(--text-faint); white-space: normal; }
  .fact-list dd { color: var(--text); }
}

@media (max-width: 900px) and (max-height: 760px) {
  .hero-bg { aspect-ratio: 2 / 1; max-height: 42vw; }
  .hero-inner { padding-top: 12px; padding-bottom: 14px; }
  .hero-eyebrow { margin-bottom: 5px; font-size: 0.72rem; }
  .hero-title { font-size: 1.32rem; margin-bottom: 7px; }
  .hero-lead { font-size: 0.88rem; line-height: 1.55; margin-bottom: 11px; }
  .hero-actions .btn { min-height: 44px; padding: 8px 14px; font-size: 0.9rem; }
  .hero-facts { margin-top: 11px; padding: 9px 12px; }
  .fact-list > div { font-size: 0.75rem; padding: 3px 0; }
}

@media (prefers-reduced-transparency: reduce) {
  .glass { backdrop-filter: none; -webkit-backdrop-filter: none; background: rgba(12, 20, 17, 0.9); }
  .btn-on-glass { backdrop-filter: none; -webkit-backdrop-filter: none; background: rgba(255,255,255,0.24); }
}
</style>
