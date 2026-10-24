import express from 'express'
import cors from 'cors'
import path from 'node:path'
import fs from 'node:fs'
import { fileURLToPath } from 'node:url'

import { getConfig } from './src/config.js'
import { startPeriodicSync } from './src/syncer.js'
import { releasesRouter } from './src/routes/releases.js'
import { downloadRouter } from './src/routes/download.js'
import { mirrorsRouter } from './src/routes/mirrors.js'
import { webhookRouter } from './src/routes/webhook.js'
import { syncRouter } from './src/routes/sync.js'
import { logger } from './src/utils/logger.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const cfg = getConfig()

const app = express()

// Middleware
app.use(cors({ origin: cfg.server.corsOrigin || '*' }))
app.use(express.json({ limit: '10mb' }))
app.use(express.urlencoded({ extended: true }))

// Request logger
app.use((req, res, next) => {
  logger.debug(`${req.method} ${req.url}`)
  next()
})

// API Routes
app.use('/api/releases', releasesRouter)
app.use('/api/download', downloadRouter)
app.use('/api/mirrors', mirrorsRouter)
app.use('/api/admin/mirrors', mirrorsRouter)
app.use('/api/webhook', webhookRouter)
app.use('/api/sync', syncRouter)

// Basic health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    time: new Date().toISOString(),
    service: 'Chunkoid NG Web Server'
  })
})

// Serve static frontend if web/dist exists
const webDistPath = path.resolve(__dirname, '../web/dist')
if (cfg.server.serveStatic && fs.existsSync(webDistPath)) {
  logger.info(`Serving static frontend from: ${webDistPath}`)
  app.use(express.static(webDistPath))

  // SPA fallback
  app.get('*', (req, res, next) => {
    if (req.url.startsWith('/api')) {
      return next()
    }
    res.sendFile(path.join(webDistPath, 'index.html'))
  })
}

// Start server
const { port, host } = cfg.server
const server = app.listen(port, host, () => {
  logger.info(`====================================================`)
  logger.info(` Chunkoid NG Backend Service is running!`)
  logger.info(` Local:    http://localhost:${port}`)
  logger.info(` Network:  http://${host}:${port}`)
  logger.info(` API docs: http://localhost:${port}/api/health`)
  logger.info(` GitHub:   ${cfg.github.owner}/${cfg.github.repo}`)
  logger.info(`====================================================`)

  // Start periodic syncer
  startPeriodicSync({ localBaseUrl: `http://localhost:${port}` })
})

// Graceful shutdown
process.on('SIGINT', () => {
  logger.info('Shutting down server gracefully...')
  server.close(() => {
    process.exit(0)
  })
})

process.on('SIGTERM', () => {
  logger.info('Shutting down server gracefully...')
  server.close(() => {
    process.exit(0)
  })
})
