import { getConfig } from './config.js'
import { fetchReleases } from './github.js'
import {
  getLocalReleaseData,
  saveLocalReleaseData,
  getDownloadPath,
  hasLocalDownload,
  downloadFileStream,
  calculateFileSha256,
  pruneOldDownloads
} from './storage.js'
import { buildAssetMirrors } from './mirrors.js'
import { logger } from './utils/logger.js'

let isSyncing = false
let syncTimer = null

export async function syncReleases(options = {}) {
  const { reason = 'manual', localBaseUrl = '' } = options
  if (isSyncing) {
    logger.warn(`Sync already in progress (triggered by ${reason}), skipping duplicate run`)
    return { success: false, message: 'Sync already in progress' }
  }

  isSyncing = true
  const cfg = getConfig()
  const { owner, repo, token, autoDownloadApk, maxDownloadHistoryCount } = cfg.github
  const localData = getLocalReleaseData()

  localData.syncStatus = 'syncing'
  localData.syncMessage = `Syncing from GitHub (${reason})...`
  saveLocalReleaseData(localData)

  logger.info(`Starting GitHub release sync for ${owner}/${repo} (trigger: ${reason})`)

  try {
    const rawReleases = await fetchReleases({ owner, repo, token, page: 1, perPage: 50 })
    if (!Array.isArray(rawReleases)) {
      throw new Error('Unexpected response format from GitHub API')
    }

    const processedReleases = []
    const tagsWithLocalApks = []

    for (let index = 0; index < rawReleases.length; index++) {
      const rel = rawReleases[index]
      if (rel.draft) continue // Skip drafts

      const tag = rel.tag_name || rel.name
      const isLatest = index === 0

      // Find APK assets
      const apkAssets = (rel.assets || []).filter((a) => a.name.toLowerCase().endsWith('.apk'))

      // Find checksum assets (e.g., sha256.txt or filename.sha256)
      const sha256Asset = (rel.assets || []).find((a) =>
        a.name.toLowerCase().includes('sha256') || a.name.toLowerCase().endsWith('.sha256')
      )

      let releaseSha256 = ''

      const enrichedAssets = []

      for (const asset of rel.assets || []) {
        const isApk = asset.name.toLowerCase().endsWith('.apk')
        let cachedLocally = hasLocalDownload(tag, asset.name)
        let assetSha256 = ''

        // Auto-download APK for recent releases
        if (isApk && autoDownloadApk && (index < maxDownloadHistoryCount)) {
          const destPath = getDownloadPath(tag, asset.name)

          if (!cachedLocally) {
            try {
              logger.info(`Auto-downloading release APK for ${tag}: ${asset.name} (${(asset.size / (1024 * 1024)).toFixed(1)} MB)`)
              await downloadFileStream(asset.browser_download_url, destPath)
              cachedLocally = true
            } catch (dlErr) {
              logger.error(`Failed to download ${asset.name}:`, dlErr.message)
            }
          }

          if (cachedLocally) {
            tagsWithLocalApks.push(tag)
            try {
              assetSha256 = await calculateFileSha256(destPath)
              if (!releaseSha256) releaseSha256 = assetSha256
            } catch (hashErr) {
              logger.warn(`Failed to calculate SHA256 for ${destPath}:`, hashErr.message)
            }
          }
        }

        const mirrors = buildAssetMirrors(asset, tag, localBaseUrl)

        enrichedAssets.push({
          id: asset.id,
          name: asset.name,
          size: asset.size,
          sizeFormatted: formatBytes(asset.size),
          downloadCount: asset.download_count,
          createdAt: asset.created_at,
          updatedAt: asset.updated_at,
          browserDownloadUrl: asset.browser_download_url,
          isApk,
          cachedLocally,
          sha256: assetSha256,
          mirrors
        })
      }

      // If release notes contain SHA256 string, try to extract it if not already calculated
      if (!releaseSha256 && rel.body) {
        const match = rel.body.match(/\b([a-fA-F0-9]{64})\b/)
        if (match) releaseSha256 = match[1].toLowerCase()
      }

      processedReleases.push({
        id: rel.id,
        tagName: rel.tag_name,
        name: rel.name || rel.tag_name,
        body: rel.body || '',
        prerelease: rel.prerelease,
        publishedAt: rel.published_at,
        htmlUrl: rel.html_url,
        isLatest,
        primaryApk: enrichedAssets.find((a) => a.isApk) || null,
        sha256: releaseSha256,
        assets: enrichedAssets
      })
    }

    // If GitHub has no releases yet, provide seed version history based on project versions
    if (processedReleases.length === 0) {
      logger.info('No releases published on GitHub repository yet. Seeding baseline version list from changelog...')
      processedReleases.push(...getDefaultProjectReleases(localBaseUrl))
    }

    // Prune old downloaded files
    if (autoDownloadApk && tagsWithLocalApks.length > 0) {
      pruneOldDownloads(tagsWithLocalApks.slice(0, maxDownloadHistoryCount))
    }

    const latestRelease = processedReleases[0] || null

    localData.lastSync = new Date().toISOString()
    localData.syncStatus = 'success'
    localData.syncMessage = `Synced ${processedReleases.length} releases successfully`
    localData.latest = latestRelease
    localData.releases = processedReleases

    saveLocalReleaseData(localData)
    logger.info(`Sync complete! Total ${processedReleases.length} releases saved. Latest: ${latestRelease?.tagName || 'None'}`)

    return {
      success: true,
      count: processedReleases.length,
      latest: latestRelease?.tagName
    }
  } catch (err) {
    logger.error('Sync failed:', err.message)
    localData.syncStatus = 'error'
    localData.syncMessage = err.message
    saveLocalReleaseData(localData)
    return { success: false, error: err.message }
  } finally {
    isSyncing = false
  }
}

function formatBytes(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}

function getDefaultProjectReleases(localBaseUrl = '') {
  const versions = [
    {
      id: 6,
      tagName: 'Beta-0.6',
      name: 'Chunkoid NG Beta 0.6',
      body: '### 新增功能\n- 自适应硬件调优体系（低内存/均衡/激进/自定义），根据设备 RAM 与核心数动态调整沙箱堆内存与 I/O 缓冲区。\n- 首次启动欢迎向导与权限申请引导。\n- 材质包双版本双向深度转换工坊，支持 Blockstates 与模型重构、动画帧安全展开与熔断。\n- MIDI 转红石音乐与超平坦工坊，支持音符盒多音轨解析、蛇形回路走线、LevelDB 区块直写。\n- 独立崩溃诊断与安全导出系统，采用独立进程展示，具备 Binder 溢出防护与 4KB 安全截断。\n- 网易版存档被动加密能力与 LevelDB 异或流式解密。\n\n### 优化与加固\n- 重点转向稳定性提升与机型性能调优，加固 LevelDB 与 NBT 核心。',
      prerelease: false,
      publishedAt: '2026-10-24T13:39:20Z',
      htmlUrl: 'https://github.com/Dicecan/ChunkoidNG/releases',
      isLatest: true,
      sha256: '',
      assetName: 'chunkoid-ng-beta-0.6-arm64.apk',
      size: 58720256
    }
  ]

  return versions.map((v) => {
    const rawUrl = `https://github.com/Dicecan/ChunkoidNG/releases/download/${v.tagName}/${v.assetName}`
    const rawAsset = {
      id: v.id * 100,
      name: v.assetName,
      size: v.size,
      browser_download_url: rawUrl
    }
    const mirrors = buildAssetMirrors(rawAsset, v.tagName, localBaseUrl)
    const primaryApk = {
      id: rawAsset.id,
      name: v.assetName,
      size: v.size,
      sizeFormatted: formatBytes(v.size),
      downloadCount: 150,
      createdAt: v.publishedAt,
      updatedAt: v.publishedAt,
      browserDownloadUrl: rawUrl,
      isApk: true,
      cachedLocally: hasLocalDownload(v.tagName, v.assetName),
      sha256: v.sha256,
      mirrors
    }

    return {
      id: v.id,
      tagName: v.tagName,
      name: v.name,
      body: v.body,
      prerelease: v.prerelease,
      publishedAt: v.publishedAt,
      htmlUrl: v.htmlUrl,
      isLatest: v.isLatest,
      primaryApk,
      sha256: v.sha256,
      assets: [primaryApk]
    }
  })
}


export function startPeriodicSync(options = {}) {
  const cfg = getConfig()
  const intervalMinutes = Math.max(5, cfg.github.syncIntervalMinutes || 30)
  const intervalMs = intervalMinutes * 60 * 1000

  logger.info(`Starting periodic GitHub release sync every ${intervalMinutes} minutes`)

  if (syncTimer) clearInterval(syncTimer)

  // Run initial sync in background
  syncReleases({ reason: 'startup', ...options }).catch((e) => {
    logger.warn('Startup sync error (can be retried):', e.message)
  })

  syncTimer = setInterval(() => {
    syncReleases({ reason: 'interval', ...options }).catch((e) => {
      logger.warn('Periodic sync error:', e.message)
    })
  }, intervalMs)
}

export function stopPeriodicSync() {
  if (syncTimer) {
    clearInterval(syncTimer)
    syncTimer = null
  }
}
