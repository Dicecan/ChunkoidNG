import { Router } from 'express'
import path from 'node:path'
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

  let filePath = getDownloadPath(tag, safeFilename)
  let found = hasLocalDownload(tag, safeFilename)

  // Defensive check for duplicate .apk extension
  if (!found && !safeFilename.endsWith('.apk.apk')) {
    if (hasLocalDownload(tag, `${safeFilename}.apk`)) {
      filePath = getDownloadPath(tag, `${safeFilename}.apk`)
      found = true
    }
  }

  // Sanitize download display name so users never download a .apk.apk file
  const downloadDisplayName = safeFilename.replace(/\.apk\.apk$/i, '.apk')

  if (found) {
    logger.info(`Serving local download: ${tag}/${safeFilename} (as ${downloadDisplayName}) to ${req.ip}`)
    res.setHeader('Content-Type', 'application/vnd.android.package-archive')
    res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(downloadDisplayName)}"`)
    return res.sendFile(filePath)
  }

  // If local file is not found, fallback to fast mirror redirection
  logger.warn(`Local download not found for ${tag}/${safeFilename}, resolving redirect mirror`)
  const releaseData = getLocalReleaseData()
  const release = (releaseData.releases || []).find((r) => r.tagName === tag)
  const asset = release?.assets?.find((a) =>
    a.name === safeFilename ||
    a.name === `${safeFilename}.apk` ||
    a.name.replace(/\.apk\.apk$/i, '.apk') === safeFilename
  )

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
