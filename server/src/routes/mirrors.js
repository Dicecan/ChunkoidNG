import { Router } from 'express'
import { getMirrors, updateMirrorsList } from '../mirrors.js'
import { getConfig, saveConfig } from '../config.js'

export const mirrorsRouter = Router()

function verifyAdmin(req, res, next) {
  const authHeader = req.headers['authorization'] || ''
  const token = authHeader.replace(/^Bearer\s+/i, '') || req.query.token
  const cfg = getConfig()

  if (!token || token !== cfg.admin?.token) {
    return res.status(401).json({ code: 401, message: 'Unauthorized: invalid admin token' })
  }
  next()
}

// GET /api/mirrors - List all mirrors
mirrorsRouter.get('/', (req, res) => {
  const mirrors = getMirrors()
  res.json({
    code: 0,
    data: mirrors
  })
})

// POST /api/admin/mirrors - Update entire mirrors array
mirrorsRouter.post('/', verifyAdmin, (req, res) => {
  const { mirrors } = req.body
  if (!Array.isArray(mirrors)) {
    return res.status(400).json({ code: 400, message: 'Invalid mirrors payload' })
  }

  try {
    const updated = updateMirrorsList(mirrors)
    res.json({ code: 0, message: 'Mirrors updated successfully', data: updated.mirrors })
  } catch (err) {
    res.status(500).json({ code: 500, message: err.message })
  }
})

// POST /api/admin/mirrors/quick-add - Quickly add or update a mirror
mirrorsRouter.post('/quick-add', verifyAdmin, (req, res) => {
  const { id, name, type, prefix, description, priority, enabled = true } = req.body
  if (!id || !name || !type) {
    return res.status(400).json({ code: 400, message: 'Missing required mirror fields (id, name, type)' })
  }

  const mirrors = getMirrors()
  const existingIdx = mirrors.findIndex((m) => m.id === id)

  const newMirror = {
    id,
    name,
    type,
    prefix: prefix || '',
    description: description || '',
    priority: priority !== undefined ? Number(priority) : 50,
    enabled: Boolean(enabled)
  }

  if (existingIdx >= 0) {
    mirrors[existingIdx] = { ...mirrors[existingIdx], ...newMirror }
  } else {
    mirrors.push(newMirror)
  }

  updateMirrorsList(mirrors)
  res.json({ code: 0, message: 'Mirror added/updated successfully', data: newMirror })
})

// POST /api/admin/mirrors/toggle - Quickly toggle a mirror on/off
mirrorsRouter.post('/toggle', verifyAdmin, (req, res) => {
  const { id, enabled } = req.body
  if (!id) {
    return res.status(400).json({ code: 400, message: 'Missing mirror id' })
  }

  const mirrors = getMirrors()
  const mirror = mirrors.find((m) => m.id === id)
  if (!mirror) {
    return res.status(404).json({ code: 404, message: 'Mirror not found' })
  }

  mirror.enabled = enabled !== undefined ? Boolean(enabled) : !mirror.enabled
  updateMirrorsList(mirrors)
  res.json({ code: 0, message: `Mirror "${id}" is now ${mirror.enabled ? 'enabled' : 'disabled'}`, data: mirror })
})
