import { Router } from 'express'
import { syncReleases } from '../syncer.js'
import { getLocalReleaseData } from '../storage.js'
import { getConfig } from '../config.js'

export const syncRouter = Router()

// GET /api/sync/status - Check sync status
syncRouter.get('/status', (req, res) => {
  const data = getLocalReleaseData()
  const cfg = getConfig()
  res.json({
    code: 0,
    data: {
      lastSync: data.lastSync,
      syncStatus: data.syncStatus,
      syncMessage: data.syncMessage,
      repo: `${cfg.github.owner}/${cfg.github.repo}`,
      totalReleases: (data.releases || []).length,
      latestVersion: data.latest?.tagName || null,
      syncIntervalMinutes: cfg.github.syncIntervalMinutes
    }
  })
})

// POST /api/sync - Trigger sync manually
syncRouter.post('/', async (req, res) => {
  const host = req.get('host')
  const protocol = req.protocol
  const localBaseUrl = `${protocol}://${host}`

  res.json({
    code: 0,
    message: 'Sync task started in background'
  })

  syncReleases({ reason: 'api-trigger', localBaseUrl }).catch((err) => {
    console.error('Manual sync error:', err.message)
  })
})
