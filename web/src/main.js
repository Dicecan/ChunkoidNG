import { createApp, watch } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/base.css'
import { locale, applyHtmlLang } from './i18n'
import { applyRouteTitle } from './router'

applyHtmlLang()

watch(locale, () => {
  applyHtmlLang()
  applyRouteTitle(router.currentRoute.value)
})

createApp(App).use(router).mount('#app')
