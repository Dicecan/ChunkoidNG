import { Router } from 'express'
import crypto from 'node:crypto'
import { getConfig } from '../config.js'
import { syncReleases } from '../syncer.js'
import { logger } from '../utils/logger.js'

export const webhookRouter = Router()

webhookRouter.post('/github', (req, res) => {
  const event = req.headers['x-github-event']
  const signature = req.headers['x-hub-signature-256']
  const cfg = getConfig()
  const secret = cfg.github.webhookSecret

  // Require configured secret and verify HMAC over exact raw request bytes
  if (!secret) {
    logger.warn('GitHub webhook rejected: webhookSecret is not configured')
    return res.status(503).send('Webhook secret not configured')
  }

  if (typeof signature !== 'string' || !signature) {
    logger.warn('GitHub webhook rejected: missing X-Hub-Signature-256 header')
    return res.status(401).send('Missing signature')
  }

  if (!Buffer.isBuffer(req.rawBody)) {
    logger.warn('GitHub webhook rejected: raw body unavailable')
    return res.status(400).send('Invalid body')
  }

  const expectedSig = 'sha256=' + crypto.createHmac('sha256', secret).update(req.rawBody).digest('hex')
  const sigBuf = Buffer.from(signature, 'utf-8')
  const expectedBuf = Buffer.from(expectedSig, 'utf-8')

  if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
    logger.warn('GitHub webhook rejected: invalid signature')
    return res.status(401).send('Invalid signature')
  }

  logger.info(`Received GitHub webhook event: "${event}"`)

  if (event === 'ping') {
    return res.json({ code: 0, message: 'pong' })
  }

  if (event === 'release') {
    const action = req.body?.action
    const releaseName = req.body?.release?.name || req.body?.release?.tag_name
    logger.info(`GitHub release event received: action="${action}", release="${releaseName}"`)

    if (['published', 'created', 'released', 'prereleased', 'edited'].includes(action)) {
      // Trigger sync in background
      setTimeout(() => {
        syncReleases({ reason: `webhook:${action}` }).catch((err) => {
          logger.error('Webhook-triggered sync error:', err.message)
        })
      }, 1000)

      return res.json({
        code: 0,
        message: `Sync scheduled for release action: ${action}`
      })
    }
  }

  res.json({ code: 0, message: `Ignored event: ${event}` })
})
