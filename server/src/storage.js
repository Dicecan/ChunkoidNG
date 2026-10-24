import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { pipeline } from 'node:stream/promises'
import { fileURLToPath } from 'node:url'
import { logger } from './utils/logger.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const STORAGE_DIR = path.resolve(__dirname, '../storage')
const DATA_DIR = path.join(STORAGE_DIR, 'data')
const DOWNLOADS_DIR = path.join(STORAGE_DIR, 'downloads')
const RELEASES_FILE = path.join(DATA_DIR, 'releases.json')

// Ensure storage directories exist
function ensureDirs() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true })
  }
  if (!fs.existsSync(DOWNLOADS_DIR)) {
    fs.mkdirSync(DOWNLOADS_DIR, { recursive: true })
  }
}

ensureDirs()

export function getLocalReleaseData() {
  ensureDirs()
  if (!fs.existsSync(RELEASES_FILE)) {
    return {
      lastSync: null,
      syncStatus: 'idle',
      syncMessage: '',
      latest: null,
      releases: []
    }
  }
  try {
    const raw = fs.readFileSync(RELEASES_FILE, 'utf-8')
    return JSON.parse(raw)
  } catch (err) {
    logger.error('Failed to read releases.json:', err.message)
    return {
      lastSync: null,
      syncStatus: 'error',
      syncMessage: err.message,
      latest: null,
      releases: []
    }
  }
}

export function saveLocalReleaseData(data) {
  ensureDirs()
  const tmpFile = `${RELEASES_FILE}.tmp`
  fs.writeFileSync(tmpFile, JSON.stringify(data, null, 2), 'utf-8')
  fs.renameSync(tmpFile, RELEASES_FILE)
}

export function getDownloadPath(tag, filename) {
  const safeTag = tag.replace(/[^a-zA-Z0-9._-]/g, '_')
  const tagDir = path.join(DOWNLOADS_DIR, safeTag)
  if (!fs.existsSync(tagDir)) {
    fs.mkdirSync(tagDir, { recursive: true })
  }
  return path.join(tagDir, filename)
}

export function hasLocalDownload(tag, filename) {
  const safeTag = tag.replace(/[^a-zA-Z0-9._-]/g, '_')
  const filePath = path.join(DOWNLOADS_DIR, safeTag, filename)
  return fs.existsSync(filePath) && fs.statSync(filePath).size > 0
}

export function getLocalDownloadStats(tag, filename) {
  const safeTag = tag.replace(/[^a-zA-Z0-9._-]/g, '_')
  const filePath = path.join(DOWNLOADS_DIR, safeTag, filename)
  if (!fs.existsSync(filePath)) return null
  const stat = fs.statSync(filePath)
  return {
    path: filePath,
    size: stat.size,
    mtime: stat.mtime
  }
}

export async function calculateFileSha256(filePath) {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash('sha256')
    const stream = fs.createReadStream(filePath)
    stream.on('data', (chunk) => hash.update(chunk))
    stream.on('end', () => resolve(hash.digest('hex')))
    stream.on('error', (err) => reject(err))
  })
}

export async function downloadFileStream(url, destPath) {
  const res = await fetch(url, {
    headers: {
      'User-Agent': 'ChunkoidNG-AssetDownloader/1.0.0'
    },
    redirect: 'follow'
  })

  if (!res.ok) {
    throw new Error(`Failed to download asset: HTTP ${res.status} ${res.statusText}`)
  }

  const tmpPath = `${destPath}.downloading`
  const fileStream = fs.createWriteStream(tmpPath)

  // Use pipeline for robust stream piping
  await pipeline(res.body, fileStream)

  // Rename atomically on completion
  fs.renameSync(tmpPath, destPath)
  logger.info(`Asset downloaded successfully: ${destPath}`)
}

export function pruneOldDownloads(keepTags) {
  try {
    ensureDirs()
    const safeKeepSet = new Set(keepTags.map((t) => t.replace(/[^a-zA-Z0-9._-]/g, '_')))
    const tagDirs = fs.readdirSync(DOWNLOADS_DIR)

    for (const dirName of tagDirs) {
      const fullDir = path.join(DOWNLOADS_DIR, dirName)
      if (fs.statSync(fullDir).isDirectory() && !safeKeepSet.has(dirName)) {
        logger.info(`Pruning old downloaded version cache: ${dirName}`)
        fs.rmSync(fullDir, { recursive: true, force: true })
      }
    }
  } catch (err) {
    logger.warn('Error during pruning old downloads:', err.message)
  }
}
