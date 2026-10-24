import { site, release as fallbackRelease } from '../data/features.js'
import { changelog } from '../data/changelog.js'

const MIRROR_STORAGE_KEY = 'chunkoid-selected-mirror'
const CUSTOM_PREFIX_KEY = 'chunkoid-custom-prefix'

export const DEFAULT_MIRRORS = [
  {
    id: 'official',
    name: 'GitHub 官方直链 (推荐)',
    type: 'direct',
    prefix: '',
    description: 'GitHub 官方发布源，官方直连无中转',
    enabled: true,
    priority: 100,
    isThirdParty: false
  },
  {
    id: 'local',
    name: '本地高速节点 (服务器缓存)',
    type: 'local',
    description: '本站服务器缓存直连，稳定高速',
    enabled: true,
    priority: 90,
    isThirdParty: false
  },
  {
    id: 'ghfast',
    name: '第三方加速 1 (GHFast)',
    type: 'prefix',
    prefix: 'https://ghfast.top/',
    description: '第三方 GitHub Release 国内中继线路',
    enabled: true,
    priority: 80,
    isThirdParty: true
  },
  {
    id: 'ghproxy',
    name: '第三方加速 2 (GHProxy)',
    type: 'prefix',
    prefix: 'https://ghproxy.net/',
    description: '公共第三方反代中继线路',
    enabled: true,
    priority: 70,
    isThirdParty: true
  },
  {
    id: 'moeyy',
    name: '第三方加速 3 (Moeyy 镜像)',
    type: 'prefix',
    prefix: 'https://github.moeyy.xyz/',
    description: '第三方备用下载镜像通道',
    enabled: true,
    priority: 60,
    isThirdParty: true
  }
]

export function getStoredMirrorId() {
  try {
    return localStorage.getItem(MIRROR_STORAGE_KEY) || 'official'
  } catch (_) {
    return 'official'
  }
}

export function setStoredMirrorId(id) {
  try {
    localStorage.setItem(MIRROR_STORAGE_KEY, id)
  } catch (_) {}
}

export function getStoredCustomPrefix() {
  try {
    return localStorage.getItem(CUSTOM_PREFIX_KEY) || ''
  } catch (_) {
    return ''
  }
}

export function setStoredCustomPrefix(prefix) {
  try {
    localStorage.setItem(CUSTOM_PREFIX_KEY, prefix.trim())
  } catch (_) {}
}

export async function fetchReleasesData() {
  try {
    const res = await fetch('/api/releases')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const json = await res.json()
    if (json.code === 0 && json.data) {
      return json.data
    }
  } catch (err) {
    console.warn('Backend /api/releases unreachable, using static fallback:', err.message)
  }

  // Graceful fallback when backend is offline
  return getFallbackData()
}

export async function triggerManualSync() {
  try {
    const res = await fetch('/api/sync', { method: 'POST' })
    return await res.json()
  } catch (err) {
    return { code: 500, message: err.message }
  }
}

export function resolveAssetUrl(asset, mirrorId, mirrorsList = DEFAULT_MIRRORS, customPrefix = '', tagName = '') {
  if (!asset) return ''
  const rawUrl = asset.browserDownloadUrl || asset.browser_download_url || asset.url || ''
  if (!rawUrl) return ''

  if (mirrorId === 'custom' && customPrefix) {
    const cleanPrefix = customPrefix.replace(/\/+$/, '')
    return `${cleanPrefix}/${rawUrl}`
  }

  // If local mirror, ALWAYS use relative path against current browser origin
  if (mirrorId === 'local') {
    const tag = tagName || asset.tagName || 'Beta-0.6'
    return `/api/download/${encodeURIComponent(tag)}/${encodeURIComponent(asset.name)}`
  }

  // Look for matched mirror in asset.mirrors first
  if (asset.mirrors && Array.isArray(asset.mirrors)) {
    const found = asset.mirrors.find((m) => m.id === mirrorId)
    if (found && found.url) {
      // Strip any hardcoded host if it was accidentally saved as localhost
      if (found.url.includes('/api/download/')) {
        return found.url.substring(found.url.indexOf('/api/download/'))
      }
      return found.url
    }
  }

  // Resolve using mirrors list
  const mirror = (mirrorsList || DEFAULT_MIRRORS).find((m) => m.id === mirrorId)
  if (!mirror) return rawUrl

  if (mirror.type === 'local') {
    const tag = tagName || asset.tagName || 'Beta-0.6'
    return `/api/download/${encodeURIComponent(tag)}/${encodeURIComponent(asset.name)}`
  }

  if (mirror.type === 'prefix' && mirror.prefix) {
    const cleanPrefix = mirror.prefix.replace(/\/+$/, '')
    return `${cleanPrefix}/${rawUrl}`
  }

  return rawUrl
}

function getFallbackData() {
  const versions = changelog.map((item, idx) => {
    const tag = `Beta-${item.version.replace(/[^0-9.]/g, '') || item.code}`
    const apkName = `chunkoid-ng-${tag.toLowerCase()}-arm64.apk`
    const rawUrl = `${site.repo}/releases/download/${tag}/${apkName}`

    const primaryApk = {
      name: apkName,
      sizeFormatted: '约 56 MB',
      browserDownloadUrl: rawUrl,
      isApk: true,
      cachedLocally: false,
      mirrors: DEFAULT_MIRRORS.map((m) => ({
        id: m.id,
        name: m.name,
        type: m.type,
        url: m.type === 'local' ? `/api/download/${tag}/${apkName}` : (m.prefix ? `${m.prefix.replace(/\/+$/, '')}/${rawUrl}` : rawUrl)
      }))
    }

    return {
      id: item.code,
      tagName: tag,
      name: `Chunkoid NG ${item.version}`,
      body: Object.values(item.highlight || {})[0] || '',
      prerelease: item.status !== 'current',
      publishedAt: new Date().toISOString(),
      htmlUrl: `${site.repo}/releases`,
      isLatest: idx === 0,
      sha256: fallbackRelease.sha256 || '',
      primaryApk,
      assets: [primaryApk]
    }
  })

  return {
    lastSync: null,
    syncStatus: 'offline_fallback',
    total: versions.length,
    latest: versions[0] || null,
    mirrors: DEFAULT_MIRRORS,
    releases: versions
  }
}
