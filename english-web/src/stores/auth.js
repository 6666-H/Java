import { defineStore } from 'pinia'
import { login as apiLogin } from '../api/auth'

const ROLE_KEYS = ['SUPER_ADMIN', 'ORG_ADMIN', 'STUDENT']

function getTokenKey(role) {
  return `token_${role}`
}

function getStateKey(role) {
  return `auth_state_${role}`
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    tokenByRole: {},
    userId: null,
    tenantId: null,
    username: '',
    role: '',
    tenantInfo: null,
    mustChangePwd: false,
    nickname: '',
    avatar: ''
  }),
  getters: {
    isLoggedIn: (s) => ROLE_KEYS.some(r => !!s.tokenByRole?.[r]),
    hasTokenForRole: (s) => (role) => !!s.tokenByRole?.[role],
    tokenForRole: (s) => (role) => s.tokenByRole?.[role] || '',
    currentToken: (s) => {
      const path = typeof window !== 'undefined' ? window.location.pathname : ''
      if (path.startsWith('/admin')) return s.tokenByRole?.SUPER_ADMIN || ''
      if (path.startsWith('/org')) return s.tokenByRole?.ORG_ADMIN || ''
      if (path.startsWith('/student')) return s.tokenByRole?.STUDENT || ''
      return s.tokenByRole?.SUPER_ADMIN || s.tokenByRole?.ORG_ADMIN || s.tokenByRole?.STUDENT || ''
    },
  },
  actions: {
    async login(username, password, tenantId, role) {
      const res = await apiLogin(username, password, tenantId, role)
      const r = res.role
      this.tokenByRole = { ...this.tokenByRole, [r]: res.token }
      this.userId = res.userId
      this.tenantId = res.tenantId
      this.username = res.username
      this.role = r
      this.mustChangePwd = !!res.mustChangePwd
      this.nickname = res.nickname || ''
      this.avatar = res.avatar || ''
      localStorage.setItem(getTokenKey(r), res.token)
      localStorage.setItem(getStateKey(r), JSON.stringify({
        userId: res.userId,
        tenantId: res.tenantId,
        username: res.username,
        role: r,
        mustChangePwd: !!res.mustChangePwd,
        nickname: res.nickname || '',
        avatar: res.avatar || ''
      }))
      return res
    },
    logout(role) {
      if (role) {
        this.tokenByRole = { ...this.tokenByRole, [role]: '' }
        delete this.tokenByRole[role]
        localStorage.removeItem(getTokenKey(role))
        localStorage.removeItem(getStateKey(role))
        if (this.role === role) {
          this.role = ''
          this.userId = null
          this.tenantId = null
          this.username = ''
          this.mustChangePwd = false
          this.nickname = ''
          this.avatar = ''
        }
      } else {
        this.tokenByRole = {}
        this.userId = null
        this.tenantId = null
        this.username = ''
        this.role = ''
        this.tenantInfo = null
        this.mustChangePwd = false
        this.nickname = ''
        this.avatar = ''
        ROLE_KEYS.forEach(r => {
          localStorage.removeItem(getTokenKey(r))
          localStorage.removeItem(getStateKey(r))
        })
      }
    },
    clearTokenForRole(role) {
      this.tokenByRole = { ...this.tokenByRole, [role]: '' }
      delete this.tokenByRole[role]
      localStorage.removeItem(getTokenKey(role))
      localStorage.removeItem(getStateKey(role))
    },
    initFromStorage() {
      const tokens = {}
      ROLE_KEYS.forEach(r => {
        const t = localStorage.getItem(getTokenKey(r))
        if (t) tokens[r] = t
      })
      // 兼容旧版：迁移 token → tokenByRole（无法确定角色时放入 role 从 auth_state 读取）
      const oldToken = localStorage.getItem('token')
      if (oldToken && Object.keys(tokens).length === 0) {
        try {
          const stateStr = localStorage.getItem('auth_state')
          const role = stateStr ? (JSON.parse(stateStr).role || '') : ''
          if (ROLE_KEYS.includes(role)) {
            tokens[role] = oldToken
            localStorage.setItem(getTokenKey(role), oldToken)
          }
          localStorage.removeItem('token')
          localStorage.removeItem('auth_state')
        } catch (_) {}
      }
      this.tokenByRole = tokens
      const firstRole = ROLE_KEYS.find(r => tokens[r])
      if (!firstRole) return
      try {
        const t = tokens[firstRole]
        const b64 = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
        const payload = JSON.parse(atob(b64))
        this.userId = payload.sub != null ? Number(payload.sub) : null
        this.tenantId = payload.tenantId != null ? String(payload.tenantId) : null
        this.username = payload.username ?? ''
        this.role = payload.role ?? firstRole
        const stateStr = localStorage.getItem(getStateKey(firstRole))
        if (stateStr) {
          const state = JSON.parse(stateStr)
          if (!this.username) this.username = state.username
          if (!this.role) this.role = state.role
          this.mustChangePwd = !!state.mustChangePwd
          this.nickname = state.nickname || ''
          this.avatar = state.avatar || ''
        }
      } catch (_) {}
    },
  },
})
