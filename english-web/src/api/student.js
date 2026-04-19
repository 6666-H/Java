import request from './request'

export function getStudentHome() {
  return request.get('/student/home')
}

export function getStudentWordbook(tab = 'all') {
  return request.get('/student/wordbook', { params: { tab } })
}

export function getStudentStats() {
  return request.get('/student/stats')
}

export function getStudentNotifications() {
  return request.get('/student/notifications')
}

export function markStudentNotificationsRead() {
  return request.put('/student/notifications')
}

export function getStudentProfile() {
  return request.get('/student/profile')
}

export function updateStudentProfile(payload) {
  return request.put('/student/profile', payload)
}

export function getStudentLastStudy() {
  return request.get('/student/last-study')
}

export function saveStudentLastStudy(payload) {
  return request.put('/student/last-study', payload)
}

export function updateStudentPassword(oldPassword, newPassword) {
  return request.put('/student/password', { oldPassword, newPassword })
}

export function getStudentUnitWords(unitId) {
  return request.get(`/student/units/${unitId}/words`)
}

export function getStudentUnitJourney(unitId, params = {}) {
  return request.get(`/student/units/${unitId}/journey`, { params })
}

export function getStudentStudySession(unitId, stage) {
  return request.get(`/student/units/${unitId}/session/${stage}`)
}

export function saveStudentStudySession(unitId, stage, payload) {
  return request.put(`/student/units/${unitId}/session/${stage}`, payload)
}

export function clearStudentStudySession(unitId, stage) {
  return request.delete(`/student/units/${unitId}/session/${stage}`)
}

export function getStudentStageResults(limit = 10) {
  return request.get('/student/results', { params: { limit } })
}

export function saveStudentStageResult(payload) {
  return request.put('/student/results', payload)
}
