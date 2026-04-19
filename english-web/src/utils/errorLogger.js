/**
 * 错误日志服务 - 统一错误处理和上报
 */

// 错误级别
export const ErrorLevel = {
  INFO: 'info',
  WARNING: 'warning',
  ERROR: 'error',
  CRITICAL: 'critical',
}

// 错误类型
export const ErrorType = {
  NETWORK: 'NETWORK_ERROR',
  API: 'API_ERROR',
  VALIDATION: 'VALIDATION_ERROR',
  AUTH: 'AUTH_ERROR',
  UNKNOWN: 'UNKNOWN_ERROR',
}

/**
 * 错误日志类
 */
class ErrorLogger {
  constructor() {
    this.queue = []
    this.maxQueueSize = 50
    this.batchSize = 10
    this.flushTimer = null
    this.autoFlushInterval = 30000 // 30 秒
    this.startAutoFlush()
  }

  /**
   * 记录错误
   * @param {Object} options
   * @param {string} options.message - 错误消息
   * @param {ErrorType} options.type - 错误类型
   * @param {ErrorLevel} options.level - 错误级别
   * @param {string} options.url - 请求 URL
   * @param {Object} options.extra - 额外信息
   */
  log({ message, type = ErrorType.UNKNOWN, level = ErrorLevel.ERROR, url = '', extra = {} }) {
    const error = {
      id: this.generateId(),
      timestamp: new Date().toISOString(),
      message,
      type,
      level,
      url,
      extra,
      userAgent: navigator?.userAgent || '',
      pathname: window?.location?.pathname || '',
    }

    // 控制台输出
    this.logToConsole(error)

    // 加入队列
    this.queue.push(error)
    if (this.queue.length >= this.maxQueueSize) {
      this.flush()
    }

    // 严重错误立即上报
    if (level === ErrorLevel.CRITICAL) {
      this.flush()
    }

    return error
  }

  /**
   * 记录网络错误
   */
  logNetworkError(url, error) {
    return this.log({
      message: `网络请求失败：${url}`,
      type: ErrorType.NETWORK,
      level: ErrorLevel.ERROR,
      url,
      extra: {
        errorMessage: error?.message,
        errorStack: error?.stack,
      },
    })
  }

  /**
   * 记录 API 错误
   */
  logApiError(url, statusCode, response) {
    return this.log({
      message: `API 错误：${statusCode}`,
      type: ErrorType.API,
      level: statusCode >= 500 ? ErrorLevel.CRITICAL : ErrorLevel.ERROR,
      url,
      extra: {
        statusCode,
        response,
      },
    })
  }

  /**
   * 记录认证错误
   */
  logAuthError(message) {
    return this.log({
      message: message || '认证失败',
      type: ErrorType.AUTH,
      level: ErrorLevel.WARNING,
      extra: {
        pathname: window?.location?.pathname,
      },
    })
  }

  /**
   * 记录验证错误
   */
  logValidationError(field, message) {
    return this.log({
      message: `验证失败：${field} - ${message}`,
      type: ErrorType.VALIDATION,
      level: ErrorLevel.WARNING,
      extra: { field },
    })
  }

  /**
   * 上报错误到服务器
   */
  async flush() {
    if (this.queue.length === 0) return

    const errorsToSend = this.queue.splice(0, this.batchSize)

    try {
      // 这里可以发送到错误收集服务
      // await fetch('/api/error-log', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify({ errors: errorsToSend }),
      // })
      console.log('[ErrorLogger] Flushed errors:', errorsToSend.length)
    } catch (e) {
      // 上报失败，重新加入队列
      this.queue.unshift(...errorsToSend)
      console.error('[ErrorLogger] Flush failed:', e)
    }
  }

  /**
   * 获取错误统计
   */
  getStats() {
    const now = new Date()
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)

    const recentErrors = this.queue.filter(
      (e) => new Date(e.timestamp) > oneHourAgo
    )

    return {
      total: this.queue.length,
      lastHour: recentErrors.length,
      byType: this.groupByType(recentErrors),
      byLevel: this.groupByLevel(recentErrors),
    }
  }

  /**
   * 清空错误队列
   */
  clear() {
    this.queue = []
    if (this.flushTimer) {
      clearTimeout(this.flushTimer)
    }
  }

  // ========== 私有方法 ==========

  startAutoFlush() {
    this.flushTimer = setInterval(() => this.flush(), this.autoFlushInterval)
  }

  logToConsole(error) {
    const prefix = `[ErrorLogger][${error.level.toUpperCase()}]`
    const color = this.getLevelColor(error.level)

    console.log(
      `%c${prefix} ${error.message}`,
      `color: ${color}; font-weight: bold;`
    )

    if (error.extra && Object.keys(error.extra).length > 0) {
      console.log('%cExtra:', 'color: #888;', error.extra)
    }
  }

  getLevelColor(level) {
    const colors = {
      [ErrorLevel.INFO]: '#1890ff',
      [ErrorLevel.WARNING]: '#faad14',
      [ErrorLevel.ERROR]: '#f5222d',
      [ErrorLevel.CRITICAL]: '#722ed1',
    }
    return colors[level] || '#666'
  }

  groupByType(errors) {
    return errors.reduce((acc, e) => {
      acc[e.type] = (acc[e.type] || 0) + 1
      return acc
    }, {})
  }

  groupByLevel(errors) {
    return errors.reduce((acc, e) => {
      acc[e.level] = (acc[e.level] || 0) + 1
      return acc
    }, {})
  }

  generateId() {
    return `err_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }
}

// 单例
export const errorLogger = new ErrorLogger()

// 全局错误捕获
if (typeof window !== 'undefined') {
  window.addEventListener('error', (event) => {
    errorLogger.log({
      message: event.message || '全局错误',
      type: ErrorType.UNKNOWN,
      level: ErrorLevel.CRITICAL,
      extra: {
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        stack: event.error?.stack,
      },
    })
  })

  window.addEventListener('unhandledrejection', (event) => {
    errorLogger.log({
      message: `未处理的 Promise 拒绝：${event.reason}`,
      type: ErrorType.UNKNOWN,
      level: ErrorLevel.ERROR,
      extra: {
        reason: event.reason?.message || event.reason,
      },
    })
  })
}

export default errorLogger
