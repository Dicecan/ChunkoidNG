<script setup>
import { computed } from 'vue'
import { features, site, release, friendlyLinks } from '../data/features'
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

const featuredLink = computed(() => friendlyLinks.find((l) => l.featured) || friendlyLinks[0])
const linksList = computed(() =>
  friendlyLinks.map((l) => ({
    ...l,
    descText: localized(l.desc),
    tagText: localized(l.tag)
  }))
)
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

    <!-- 核心功能 -->
    <section class="container section section-top">
      <h2>{{ t('home.coreFeatures') }}</h2>
      <div class="grid grid-2">
        <FeatureCard v-for="f in features" :key="f.id" :feature="f" />
      </div>
      <p class="small muted" style="margin-top: 14px">
        <RouterLink to="/features">{{ t('home.viewAll') }} →</RouterLink>
      </p>
    </section>

    <!-- 原作者新作特别推荐 Banner (极显眼) -->
    <section class="container section">
      <div class="featured-link-banner">
        <div class="featured-link-content">
          <div class="featured-link-badge">★ {{ t('home.featuredLinkBadge') }}</div>
          <h3 class="featured-link-title">{{ t('home.featuredLinkTitle') }}</h3>
          <p class="featured-link-desc">{{ t('home.featuredLinkDesc') }}</p>
        </div>
        <div class="featured-link-action">
          <a :href="featuredLink.url" target="_blank" rel="noopener" class="btn btn-primary btn-glow">
            {{ t('home.featuredLinkBtn') }} ↗
          </a>
        </div>
      </div>
    </section>

    <!-- 为什么选择 -->
    <section class="container section">
      <h2>{{ t('home.whyTitle') }}</h2>
      <div class="grid grid-2">
        <div v-for="h in highlights" :key="h.title" class="card">
          <h3 style="margin-bottom: 6px">{{ h.title }}</h3>
          <p style="margin: 0; font-size: 0.89rem">{{ h.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 技术栈与架构 -->
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

    <!-- 友情链接与生态合作 -->
    <section class="container section">
      <h2>{{ t('home.friendlyLinks') }}</h2>
      <p class="muted" style="margin-top: -6px; margin-bottom: 16px">{{ t('home.friendlyLinksLead') }}</p>
      <div class="home-links-grid">
        <a
          v-for="link in linksList"
          :key="link.url"
          :href="link.url"
          target="_blank"
          rel="noopener"
          class="home-link-card"
          :class="{ 'home-link-featured': link.featured }"
        >
          <div class="home-link-head">
            <span class="home-link-name">{{ link.name }} ↗</span>
            <span class="badge" v-if="link.tagText">{{ link.tagText }}</span>
          </div>
          <p class="home-link-desc">{{ link.descText }}</p>
        </a>
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
  font-size: 2.85rem;
  line-height: 1.15;
  margin: 0 0 18px;
  letter-spacing: -0.02em;
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.65);
}

.hero-lead {
  font-size: 1.1rem;
  line-height: 1.6;
  margin: 0 0 28px;
  color: #dce7e1;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.65);
}

.hero-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.hero-facts {
  margin-top: 56px;
  border-radius: 14px;
  padding: 16px 20px;
}

.fact-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin: 0;
}
.fact-list dt { font-size: 0.76rem; color: #a4beaf; margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.04em; }
.fact-list dd { margin: 0; font-size: 0.95rem; font-weight: 600; color: #f2f6f4; }

/* Featured Link Banner */
.featured-link-banner {
  background: linear-gradient(135deg, rgba(47, 125, 99, 0.16) 0%, rgba(229, 164, 32, 0.09) 100%), var(--bg-soft);
  border: 1px solid var(--accent);
  padding: 24px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(47, 125, 99, 0.12);
}

.featured-link-badge {
  display: inline-block;
  font-size: 0.74rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #e5a420;
  background: rgba(229, 164, 32, 0.14);
  padding: 3px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.featured-link-title {
  margin: 0 0 6px 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text);
}

.featured-link-desc {
  margin: 0;
  font-size: 0.92rem;
  color: var(--text-soft);
  max-width: 60ch;
  line-height: 1.5;
}

.btn-glow {
  box-shadow: 0 2px 14px rgba(47, 125, 99, 0.4);
}

/* Home links grid */
.home-links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.home-link-card {
  padding: 16px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 10px;
  display: block;
  text-decoration: none;
  transition: transform 0.18s ease, border-color 0.18s ease;
}
.home-link-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
}
.home-link-featured {
  border-color: rgba(47, 125, 99, 0.6);
  background: linear-gradient(145deg, rgba(47, 125, 99, 0.08), var(--bg-soft));
}

.home-link-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.home-link-name {
  font-size: 1.02rem;
  font-weight: 600;
  color: var(--text);
}

.home-link-desc {
  font-size: 0.86rem;
  color: var(--text-soft);
  margin: 0;
  line-height: 1.45;
}

@media (max-width: 760px) {
  .hero-inner { padding-top: 56px; }
  .hero-copy { text-align: left; }
  .hero-actions { justify-content: flex-start; }
  .hero-title { font-size: 2.2rem; }
  .fact-list { grid-template-columns: repeat(2, 1fr); gap: 14px 18px; }
  .featured-link-banner { flex-direction: column; align-items: flex-start; }
}

@media (max-width: 640px) {
  .hero-title { font-size: 1.85rem; }
  .hero-lead { font-size: 0.98rem; }
  .fact-list { grid-template-columns: 1fr; }
}
</style>
