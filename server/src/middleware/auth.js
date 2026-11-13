import crypto from 'node:crypto'
import { getConfig } from '../config.js'

const DEFAULT_INSECURE_TOKEN = 'chunkoid-admin-change-me'

export function isConstantTimeEqual(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string') return false
  const digestA = crypto.createHash('sha256').update(a).digest()
  const digestB = crypto.createHash('sha256').update(b).digest()
  return crypto.timingSafeEqual(digestA, digestB)
}

export function verifyAdmin(req, res, next) {
  const authHeader = req.headers['authorization'] || ''
  const match = typeof authHeader === 'string' ? authHeader.match(/^Bearer\s+(.+)$/i) : null
  const token = match ? match[1].trim() : ''
  const cfg = getConfig()
  const configuredToken = cfg.admin?.token

  if (!configuredToken || configuredToken === DEFAULT_INSECURE_TOKEN) {
    return res.status(503).json({
      code: 503,
      message: 'Admin endpoint disabled: configure a strong ADMIN_TOKEN'
    })
  }

  if (!token || !isConstantTimeEqual(token, configuredToken)) {
    return res.status(401).json({ code: 401, message: 'Unauthorized: invalid admin token' })
  }
  next()
}
