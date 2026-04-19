import request from './request'

export function getAdminDashboard() {
  return request.get('/admin/dashboard')
}

export function getTenantOverview() {
  return request.get('/admin/tenants/overview')
}

export function getTenantList() {
  return request.get('/admin/tenants')
}

export function getBookOverview(params) {
  return request.get('/admin/books/overview', { params })
}

export function getAuthorizationOverview(params) {
  return request.get('/admin/tenant-book-auth/overview', { params })
}

export function getTenantBookIds(tenantId) {
  return request.get('/admin/tenant-book-auth/list', { params: { tenantId } })
}

export function replaceTenantBookIds(payload) {
  return request.put('/admin/tenant-book-auth', payload)
}

export function getSystemSettings() {
  return request.get('/admin/settings')
}

export function saveSystemSettings(payload) {
  return request.put('/admin/settings', payload)
}

export function uploadSystemLogo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/settings/logo', formData)
}
