import request from './request'

/** @param role 期望登录角色：STUDENT | ORG_ADMIN | SUPER_ADMIN，后端校验用户实际角色需与其一致 */
export function login(username, password, tenantId, role) {
  const body = { username, password }
  if (tenantId != null) body.tenantId = tenantId
  if (role) body.role = role
  return request.post('/auth/login', body)
}

export function updatePassword(oldPassword, newPassword) {
  return request.put('/auth/password', { oldPassword, newPassword })
}

export function logout() {
  return request.post('/auth/logout')
}

export function refreshToken() {
  return request.post('/auth/refresh')
}
