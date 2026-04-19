import request from './request'

/** 书本列表，支持按版本、年级筛选 */
export function listBooks(opts = {}) {
  return request.get('/books', { params: opts })
}

/** 版本列表（如人教版、外研版） */
export function listVersions() {
  return request.get('/books/versions')
}

/** 年级列表，可选按版本筛选 */
export function listGrades(versionName) {
  return request.get('/books/grades', { params: versionName ? { versionName } : {} })
}

/** 单元列表（按书本） */
export function listUnits(bookId) {
  return request.get('/units', { params: { bookId } })
}

/** 单元进度列表：总词数、已掌握、错词、完成度、完成时间 */
export function getUnitProgress(bookId) {
  return request.get('/units/progress', { params: { bookId } })
}

/** 书本统计：完成率、掌握数、错词数、平均正确率 */
export function getBookStats(bookId) {
  return request.get('/books/stats', { params: { bookId } })
}

export function getUnitProgressDetail(unitId) {
  return request.get(`/units/${unitId}/progress`)
}
