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

  // Verify signature if secret is configured
  if (secret) {
    if (!signature) {
      logger.warn('GitHub webhook rejected: missing X-Hub-Signature-256 header')
      return res.status(401).send('Missing signature')
    }

    const payload = JSON.stringify(req.body)
    const expectedSig = 'sha256=' + crypto.createHmac('sha256', secret).update(payload).digest('hex')

    if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expectedSig))) {
      logger.warn('GitHub webhook rejected: invalid signature')
      return res.status(401).send('Invalid signature')
    }
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
