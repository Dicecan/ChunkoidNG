import { createRouter, createWebHashHistory } from 'vue-router'
import { t } from '../i18n'

import Home from '../views/HomeView.vue'
import Features from '../views/FeaturesView.vue'
import FeatureDetail from '../views/FeatureDetailView.vue'
import Download from '../views/DownloadView.vue'
import Docs from '../views/DocsView.vue'
import DocPage from '../views/DocPageView.vue'
import Changelog from '../views/ChangelogView.vue'
import Faq from '../views/FaqView.vue'
import About from '../views/AboutView.vue'
import NotFound from '../views/NotFoundView.vue'

const routes = [
  { path: '/', name: 'home', component: Home, meta: { titleKey: 'meta.homeTitle' } },
  { path: '/features', name: 'features', component: Features, meta: { titleKey: 'meta.featuresTitle' } },
  { path: '/features/:id', name: 'feature-detail', component: FeatureDetail, meta: { titleKey: 'meta.featureDetailTitle' } },
  { path: '/download', name: 'download', component: Download, meta: { titleKey: 'meta.downloadTitle' } },
  { path: '/docs', name: 'docs', component: Docs, meta: { titleKey: 'meta.docsTitle' } },
  { path: '/docs/:slug', name: 'doc-page', component: DocPage, meta: { titleKey: 'meta.docsTitle' } },
  { path: '/changelog', name: 'changelog', component: Changelog, meta: { titleKey: 'meta.changelogTitle' } },
  { path: '/faq', name: 'faq', component: Faq, meta: { titleKey: 'meta.faqTitle' } },
  { path: '/about', name: 'about', component: About, meta: { titleKey: 'meta.aboutTitle' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFound, meta: { titleKey: 'meta.notFoundTitle' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    if (saved) return saved
    if (to.hash) return { el: to.hash, behavior: 'smooth', top: 80 }
    return { top: 0 }
  }
})

export function setTitle(title) {
  const base = t('meta.siteName')
  document.title = title ? `${title} · ${base}` : base
}

export function applyRouteTitle(route) {
  const key = route.meta?.titleKey
  setTitle(key ? t(key) : '')
}

router.afterEach((to) => applyRouteTitle(to))

export default router
