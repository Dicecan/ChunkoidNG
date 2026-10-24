import { logger } from './utils/logger.js'

const GITHUB_API_BASE = 'https://api.github.com'

function getHeaders(token) {
  const headers = {
    'Accept': 'application/vnd.github.v3+json',
    'User-Agent': 'ChunkoidNG-SyncServer/1.0.0'
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

export async function fetchReleases({ owner, repo, token, page = 1, perPage = 30 }) {
  const url = `${GITHUB_API_BASE}/repos/${owner}/${repo}/releases?page=${page}&per_page=${perPage}`
  logger.info(`Fetching releases from GitHub: ${owner}/${repo} (page ${page})`)

  const res = await fetch(url, { headers: getHeaders(token) })
  if (!res.ok) {
    const errorText = await res.text()
    if (res.status === 403 && res.headers.get('x-ratelimit-remaining') === '0') {
      const resetTime = res.headers.get('x-ratelimit-reset')
      const resetDate = resetTime ? new Date(parseInt(resetTime, 10) * 1000).toLocaleTimeString() : 'soon'
      throw new Error(`GitHub API rate limit exceeded. Resets at ${resetDate}. Consider providing GITHUB_TOKEN in config.`)
    }
    throw new Error(`GitHub API HTTP ${res.status}: ${errorText}`)
  }

  const data = await res.json()
  return data
}

export async function fetchLatestRelease({ owner, repo, token }) {
  const url = `${GITHUB_API_BASE}/repos/${owner}/${repo}/releases/latest`
  logger.info(`Fetching latest release from GitHub: ${owner}/${repo}`)

  const res = await fetch(url, { headers: getHeaders(token) })
  if (!res.ok) {
    if (res.status === 404) {
      logger.warn(`No latest release found on ${owner}/${repo} (possibly only pre-releases exist)`)
      return null
    }
    const errorText = await res.text()
    throw new Error(`GitHub API HTTP ${res.status}: ${errorText}`)
  }

  return await res.json()
}
