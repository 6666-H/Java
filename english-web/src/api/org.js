import request from './request'

export function getOrgTasks() {
  return request.get('/tenant/tasks')
}

export function getOrgBooks() {
  return request.get('/tenant/books')
}

export function createOrgTasks(payload) {
  return request.post('/tenant/tasks', payload)
}

export function deleteOrgTask(taskId) {
  return request.delete(`/tenant/tasks/${taskId}`)
}

export function updateOrgStudent(userId, payload) {
  return request.put(`/tenant/users/${userId}`, payload)
}

export function resetOrgStudentPassword(userId, newPassword) {
  return request.put(`/tenant/users/${userId}/password`, { newPassword })
}

export function getOrgReport(params = {}) {
  return request.get('/tenant/report', { params })
}

export function getOrgReportSummary(params = {}) {
  return request.get('/tenant/report/summary', { params })
}

export function getOrgStageResults(params = {}) {
  return request.get('/tenant/stage-results', { params })
}

export function getOrgSettings() {
  return request.get('/tenant/settings')
}

export function saveOrgSettings(payload) {
  return request.put('/tenant/settings', payload)
}

export function uploadOrgLogo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/tenant/settings/logo', formData)
}
