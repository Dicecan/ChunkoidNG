<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { site } from '../data/features'
import { t, locale, setLocale, LOCALES } from '../i18n'

const route = useRoute()
const open = ref(false)
const langOpen = ref(false)
const langWrap = ref(null)

const nav = [
  { to: '/', key: 'nav.home' },
  { to: '/features', key: 'nav.features' },
  { to: '/download', key: 'nav.download' },
  { to: '/docs', key: 'nav.docs' },
  { to: '/changelog', key: 'nav.changelog' },
  { to: '/faq', key: 'nav.faq' },
  { to: '/about', key: 'nav.about' }
]

watch(() => route.fullPath, () => {
  open.value = false
  langOpen.value = false
})

function choose(code) {
  setLocale(code)
  langOpen.value = false
}

function onDocClick(e) {
  if (langWrap.value && !langWrap.value.contains(e.target)) langOpen.value = false
}
onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <header class="hdr">
    <div class="container hdr-inner">
      <RouterLink to="/" class="brand">
        <img src="/logo.svg" alt="" width="26" height="26" />
        <span class="brand-name">{{ site.name }}</span>
        <span class="brand-ver">{{ site.version }}</span>
      </RouterLink>

      <div class="hdr-right">
        <nav class="nav" :class="{ open }">
          <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="nav-link">
            {{ t(item.key) }}
          </RouterLink>
          <a class="nav-link nav-ext" :href="site.repo" target="_blank" rel="noopener">
            {{ t('nav.github') }} ↗
          </a>
        </nav>

        <div ref="langWrap" class="lang" :class="{ open: langOpen }">
          <button
            class="lang-btn"
            type="button"
            :aria-label="t('nav.language')"
            :aria-expanded="langOpen"
            @click="langOpen = !langOpen"
          >
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor"
                 stroke-width="1.8" stroke-linecap="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18" />
            </svg>
            <span>{{ LOCALES.find((l) => l.code === locale)?.short }}</span>
          </button>
          <ul class="lang-menu">
            <li v-for="l in LOCALES" :key="l.code">
              <button
                type="button"
                class="lang-item"
                :class="{ active: l.code === locale }"
                :lang="l.htmlLang"
                @click="choose(l.code)"
              >
                <span>{{ l.label }}</span>
                <span v-if="l.code === locale" class="lang-check">✓</span>
              </button>
            </li>
          </ul>
        </div>

        <button
          class="nav-toggle"
          type="button"
          :aria-expanded="open"
          :aria-label="t('nav.toggleMenu')"
          @click="open = !open"
        >
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.hdr {
  position: sticky;
  top: 0;
  z-index: 50;
  background: color-mix(in srgb, var(--bg) 88%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.hdr-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 56px;
}

.hdr-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  font-weight: 600;
  font-size: 0.98rem;
  text-decoration: none;
  white-space: nowrap;

  padding: 10px 0;
  margin: -10px 0;
  min-height: 44px;
  align-items: center;
}
.brand:hover { text-decoration: none; }
.brand img { border-radius: 6px; flex: none; }

.brand-ver {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--accent-text);
  background: var(--accent-soft);
  padding: 2px 7px;
  border-radius: 5px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  font-size: 0.89rem;
  color: var(--text-soft);
  padding: 8px 11px;
  border-radius: 8px;
  text-decoration: none;
  white-space: nowrap;
}
.nav-link:hover { color: var(--text); background: var(--bg-soft); text-decoration: none; }
.nav-link.router-link-exact-active {
  color: var(--accent-text);
  background: var(--accent-soft);
  font-weight: 500;
}
.nav-ext { color: var(--text-faint); }

.lang { position: relative; }

.lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 44px;
  height: 44px;
  padding: 0 11px;
  font-family: inherit;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-soft);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 9px;
  cursor: pointer;
}
.lang-btn:hover { color: var(--text); border-color: var(--border-strong); }

.lang-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 168px;
  margin: 0;
  padding: 5px;
  list-style: none;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow);
  display: none;
}
.lang.open .lang-menu { display: block; }

.lang-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 44px;
  padding: 9px 12px;
  font-family: inherit;
  font-size: 0.9rem;
  color: var(--text-soft);
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
}
.lang-item:hover { background: var(--bg-soft); color: var(--text); }
.lang-item.active { color: var(--accent-text); font-weight: 500; }
.lang-check { font-size: 0.8rem; }

.nav-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
  width: 44px;
  height: 44px;
  padding: 0;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 9px;
  cursor: pointer;
}
.nav-toggle span {
  display: block;
  width: 17px;
  height: 1.5px;
  background: var(--text-soft);
  border-radius: 2px;
  transition: transform 0.2s, opacity 0.2s;
}

.nav-toggle[aria-expanded='true'] span:nth-child(1) { transform: translateY(5.5px) rotate(45deg); }
.nav-toggle[aria-expanded='true'] span:nth-child(2) { opacity: 0; }
.nav-toggle[aria-expanded='true'] span:nth-child(3) { transform: translateY(-5.5px) rotate(-45deg); }

@media (max-width: 980px) {
  .nav-toggle { display: flex; }

  .nav {
    position: absolute;
    top: 56px;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 2px;
    padding: 8px;
    background: var(--bg);
    border-bottom: 1px solid var(--border);
    box-shadow: var(--shadow);
    display: none;

    max-height: calc(100vh - 56px);
    overflow-y: auto;
    overscroll-behavior: contain;
  }

  .nav.open { display: flex; }

  .nav-link {
    min-height: 48px;
    padding: 12px 14px;
    border-radius: 9px;
    font-size: 0.95rem;
  }
}

@media (max-width: 420px) {
  .brand-ver { display: none; }

  .lang-menu {
    position: fixed;
    left: var(--gutter);
    right: var(--gutter);
    top: 64px;
    min-width: 0;
  }
}

@media (max-width: 360px) {
  .brand-name { display: none; }
}
</style>