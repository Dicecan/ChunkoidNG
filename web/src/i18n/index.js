import { computed, ref } from 'vue'
import zhCN from '../locales/zh-CN.js'
import en from '../locales/en.js'
import ja from '../locales/ja.js'

export const LOCALES = [
  { code: 'zh-CN', label: '简体中文', short: '中', htmlLang: 'zh-CN' },
  { code: 'en', label: 'English', short: 'EN', htmlLang: 'en' },
  { code: 'ja', label: '日本語', short: '日', htmlLang: 'ja' }
]

const MESSAGES = { 'zh-CN': zhCN, en, ja }
const DEFAULT_LOCALE = 'zh-CN'
const STORAGE_KEY = 'chunkoid-ng-locale'

function normalize(code) {
  if (!code) return null
  const lower = String(code).toLowerCase()
  if (lower === 'zh' || lower.startsWith('zh-')) return 'zh-CN'
  if (lower === 'ja' || lower.startsWith('ja-')) return 'ja'
  if (lower === 'en' || lower.startsWith('en-')) return 'en'
  return null
}

function detectInitial() {
  try {
    const saved = normalize(localStorage.getItem(STORAGE_KEY))
    if (saved) return saved
  } catch (_) {

  }
  const nav = typeof navigator !== 'undefined' ? navigator.language : ''
  return normalize(nav) || DEFAULT_LOCALE
}

export const locale = ref(detectInitial())

export const messages = computed(() => MESSAGES[locale.value] || MESSAGES[DEFAULT_LOCALE])

function resolve(pack, path) {
  return path.split('.').reduce((acc, part) => (acc && acc[part] !== undefined ? acc[part] : undefined), pack)
}

export function t(path, params) {
  let value = resolve(messages.value, path)
  if (value === undefined) value = resolve(MESSAGES[DEFAULT_LOCALE], path)
  if (value === undefined) return path
  if (typeof value !== 'string') return value
  if (!params) return value
  return value.replace(/\{(\w+)\}/g, (m, key) => (params[key] !== undefined ? String(params[key]) : m))
}

export function localized(obj) {
  if (!obj) return obj
  if (obj[locale.value] !== undefined) return obj[locale.value]
  return obj[DEFAULT_LOCALE]
}

export function setLocale(code) {
  const next = normalize(code)
  if (!next || next === locale.value) return
  locale.value = next
  try {
    localStorage.setItem(STORAGE_KEY, next)
  } catch (_) {

  }
}

export function applyHtmlLang() {
  const meta = LOCALES.find((l) => l.code === locale.value) || LOCALES[0]
  if (typeof document !== 'undefined') document.documentElement.lang = meta.htmlLang
}

export function useI18n() {
  return { t, locale, messages, setLocale, localized, locales: LOCALES }
}
