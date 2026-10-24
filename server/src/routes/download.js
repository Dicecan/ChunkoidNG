import { Router } from 'express'
import path from 'node:path'
import fs from 'node:fs'
import { getDownloadPath, hasLocalDownload } from '../storage.js'
import { getMirrors } from '../mirrors.js'
import { getLocalReleaseData } from '../storage.js'
import { logger } from '../utils/logger.js'

export const downloadRouter = Router()

downloadRouter.get('/:tag/:filename', (req, res) => {
  const { tag, filename } = req.params

  // Validate filename to prevent path traversal
  const safeFilename = path.basename(filename)
  if (safeFilename !== filename) {
    return res.status(400).send('Invalid filename')
  }

  const filePath = getDownloadPath(tag, safeFilename)

  if (hasLocalDownload(tag, safeFilename)) {
    logger.info(`Serving local download: ${tag}/${safeFilename} to ${req.ip}`)
    res.setHeader('Content-Type', 'application/vnd.android.package-archive')
    res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(safeFilename)}"`)
    return res.sendFile(filePath)
  }

  // If local file is not found, fallback to fast mirror redirection
  logger.warn(`Local download not found for ${tag}/${safeFilename}, resolving redirect mirror`)
  const releaseData = getLocalReleaseData()
  const release = (releaseData.releases || []).find((r) => r.tagName === tag)
  const asset = release?.assets?.find((a) => a.name === safeFilename)

  if (asset && asset.browserDownloadUrl) {
    const mirrors = getMirrors().filter((m) => m.enabled && m.type !== 'local')
    const preferredMirror = mirrors[0]

    let redirectUrl = asset.browserDownloadUrl
    if (preferredMirror && preferredMirror.type === 'prefix' && preferredMirror.prefix) {
      redirectUrl = `${preferredMirror.prefix.replace(/\/+$/, '')}/${asset.browserDownloadUrl}`
    }

    logger.info(`Redirecting download request to: ${redirectUrl}`)
    return res.redirect(302, redirectUrl)
  }

  res.status(404).send('Asset not found')
})
