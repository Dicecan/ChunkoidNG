import { Router } from 'express'
import { syncReleases } from '../syncer.js'
import { getLocalReleaseData } from '../storage.js'
import { getConfig } from '../config.js'
import { verifyAdmin } from '../middleware/auth.js'

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

// POST /api/sync - Trigger sync manually (admin authenticated; never trust Host header)
syncRouter.post('/', verifyAdmin, async (req, res) => {
  const cfg = getConfig()
  const localBaseUrl = cfg.server?.publicUrl || `http://localhost:${cfg.server.port}`

  res.json({
    code: 0,
    message: 'Sync task started in background'
  })

  syncReleases({ reason: 'api-trigger', localBaseUrl }).catch((err) => {
    console.error('Manual sync error:', err.message)
  })
})
