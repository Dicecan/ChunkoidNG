function timestamp() {
  return new Date().toISOString()
}

export const logger = {
  info(...args) {
    console.log(`[${timestamp()}] [INFO]`, ...args)
  },
  warn(...args) {
    console.warn(`[${timestamp()}] [WARN]`, ...args)
  },
  error(...args) {
    console.error(`[${timestamp()}] [ERROR]`, ...args)
  },
  debug(...args) {
    if (process.env.DEBUG) {
      console.log(`[${timestamp()}] [DEBUG]`, ...args)
    }
  }
}
