import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { logger } from './utils/logger.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const CONFIG_PATH = path.resolve(__dirname, '../config.json')

let currentConfig = null

export function loadConfig() {
  try {
    const raw = fs.readFileSync(CONFIG_PATH, 'utf-8')
    currentConfig = JSON.parse(raw)
  } catch (err) {
    logger.warn('Failed to load config.json, using default fallback:', err.message)
    currentConfig = {
      server: { port: 3000, host: '0.0.0.0', serveStatic: true, corsOrigin: '*' },
      github: {
        owner: 'Dicecan',
        repo: 'ChunkoidNG',
        token: '',
        webhookSecret: '',
        syncIntervalMinutes: 30,
        autoDownloadApk: true,
        maxDownloadHistoryCount: 5
      },
      mirrors: [],
      admin: { token: 'chunkoid-admin-change-me' }
    }
  }

  // Environment variables override
  if (process.env.PORT) currentConfig.server.port = parseInt(process.env.PORT, 10)
  if (process.env.HOST) currentConfig.server.host = process.env.HOST
  if (process.env.GITHUB_OWNER) currentConfig.github.owner = process.env.GITHUB_OWNER
  if (process.env.GITHUB_REPO) currentConfig.github.repo = process.env.GITHUB_REPO
  if (process.env.GITHUB_TOKEN) currentConfig.github.token = process.env.GITHUB_TOKEN
  if (process.env.GITHUB_WEBHOOK_SECRET) currentConfig.github.webhookSecret = process.env.GITHUB_WEBHOOK_SECRET
  if (process.env.SYNC_INTERVAL_MINUTES) {
    currentConfig.github.syncIntervalMinutes = parseInt(process.env.SYNC_INTERVAL_MINUTES, 10)
  }
  if (process.env.AUTO_DOWNLOAD_APK !== undefined) {
    currentConfig.github.autoDownloadApk = process.env.AUTO_DOWNLOAD_APK === 'true'
  }
  if (process.env.MAX_DOWNLOAD_HISTORY_COUNT) {
    currentConfig.github.maxDownloadHistoryCount = parseInt(process.env.MAX_DOWNLOAD_HISTORY_COUNT, 10)
  }
  if (process.env.ADMIN_TOKEN) currentConfig.admin.token = process.env.ADMIN_TOKEN

  return currentConfig
}

export function getConfig() {
  if (!currentConfig) {
    return loadConfig()
  }
  return currentConfig
}

export function saveConfig(updates) {
  const cfg = getConfig()
  const merged = {
    ...cfg,
    ...updates,
    server: { ...cfg.server, ...(updates.server || {}) },
    github: { ...cfg.github, ...(updates.github || {}) },
    admin: { ...cfg.admin, ...(updates.admin || {}) }
  }
  if (updates.mirrors) {
    merged.mirrors = updates.mirrors
  }

  fs.writeFileSync(CONFIG_PATH, JSON.stringify(merged, null, 2), 'utf-8')
  currentConfig = merged
  logger.info('Config successfully updated and persisted to config.json')
  return currentConfig
}
