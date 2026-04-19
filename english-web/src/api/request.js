import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import { errorLogger, ErrorType, ErrorLevel } from '../utils/errorLogger'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

function getPathForToken() {
  if (typeof window === 'undefined') return ''
  const p = window.location.pathname || '/'
  if (p !== '/' && p !== '') return p
  const h = (window.location.hash || '').replace(/^#/, '').split('?')[0].trim() || ''
  return h ? (h.startsWith('/') ? h : '/' + h) : ''
}

function getTokenForRequest() {
  const path = getPathForToken()
  if (path.startsWith('/admin')) return localStorage.getItem('token_SUPER_ADMIN')
  if (path.startsWith('/org')) return localStorage.getItem('token_ORG_ADMIN')
  if (path.startsWith('/student')) return localStorage.getItem('token_STUDENT')
  return localStorage.getItem('token_SUPER_ADMIN') || localStorage.getItem('token_ORG_ADMIN') || localStorage.getItem('token_STUDENT')
}

request.interceptors.request.use((config) => {
  const token = getTokenForRequest()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (res) => {
    const { code, data, message } = res.data ?? {}
    if (code !== '0' && code !== 0) {
      // 记录 API 错误
      errorLogger.logApiError(res.config.url, code, { message })
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (err) => {
    // 记录网络错误
    errorLogger.logNetworkError(err.config?.url || 'unknown', err)

    if (err.response?.status === 401) {
      const path = getPathForToken()
      const store = useAuthStore()
      if (path.startsWith('/admin')) store.clearTokenForRole('SUPER_ADMIN')
      else if (path.startsWith('/org')) store.clearTokenForRole('ORG_ADMIN')
      else if (path.startsWith('/student')) store.clearTokenForRole('STUDENT')
      
      errorLogger.logAuthError('认证失败，请重新登录')
      
      const base = import.meta.env.PROD ? '' : '/#'
      window.location.href = window.location.origin + base + '/login'
    }
    
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export default request
