import { Router } from 'express'
import { getLocalReleaseData } from '../storage.js'
import { getMirrors } from '../mirrors.js'

export const releasesRouter = Router()

// GET /api/releases - All releases (with version list)
releasesRouter.get('/', (req, res) => {
  const data = getLocalReleaseData()
  const mirrors = getMirrors().filter((m) => m.enabled)

  const releases = (data.releases || []).map((rel) => ({
    id: rel.id,
    tagName: rel.tagName,
    name: rel.name,
    body: rel.body,
    prerelease: rel.prerelease,
    publishedAt: rel.publishedAt,
    htmlUrl: rel.htmlUrl,
    isLatest: rel.isLatest,
    sha256: rel.sha256,
    primaryApk: rel.primaryApk,
    assets: rel.assets
  }))

  res.json({
    code: 0,
    data: {
      lastSync: data.lastSync,
      syncStatus: data.syncStatus,
      total: releases.length,
      latest: data.latest,
      mirrors,
      releases
    }
  })
})

// GET /api/releases/latest - Only latest release
releasesRouter.get('/latest', (req, res) => {
  const data = getLocalReleaseData()
  if (!data.latest && (!data.releases || data.releases.length === 0)) {
    return res.status(404).json({
      code: 404,
      message: 'No release found yet. Synchronizing with GitHub...'
    })
  }

  const latest = data.latest || data.releases[0]
  const mirrors = getMirrors().filter((m) => m.enabled)

  res.json({
    code: 0,
    data: {
      ...latest,
      lastSync: data.lastSync,
      mirrors
    }
  })
})

// GET /api/releases/:tag - Specific release by tag
releasesRouter.get('/:tag', (req, res) => {
  const { tag } = req.params
  const data = getLocalReleaseData()
  const release = (data.releases || []).find((r) => r.tagName === tag || r.tagName === `v${tag}`)

  if (!release) {
    return res.status(404).json({
      code: 404,
      message: `Release with tag "${tag}" not found`
    })
  }

  res.json({
    code: 0,
    data: release
  })
})
