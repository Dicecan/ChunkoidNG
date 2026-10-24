import { getConfig, saveConfig } from './config.js'

export function getMirrors() {
  const config = getConfig()
  return (config.mirrors || []).slice().sort((a, b) => (b.priority || 0) - (a.priority || 0))
}

export function buildAssetMirrors(asset, tag, localServerBaseUrl = '') {
  const mirrors = getMirrors()
  const rawUrl = asset.browser_download_url || asset.browserDownloadUrl || ''

  return mirrors
    .filter((m) => m.enabled)
    .map((m) => {
      let url = rawUrl
      if (m.type === 'local') {
        // Relative URL so that visitor browser always requests current server domain/host
        const safeTag = encodeURIComponent(tag)
        const safeFile = encodeURIComponent(asset.name)
        url = `/api/download/${safeTag}/${safeFile}`
      } else if (m.type === 'prefix' && m.prefix) {
        url = `${m.prefix.replace(/\/+$/, '')}/${rawUrl}`
      }

      return {
        id: m.id,
        name: m.name,
        type: m.type,
        url,
        description: m.description,
        isLocal: m.type === 'local'
      }
    })
}

export function updateMirrorsList(newMirrors) {
  if (!Array.isArray(newMirrors)) {
    throw new Error('Mirrors must be an array')
  }
  return saveConfig({ mirrors: newMirrors })
}
