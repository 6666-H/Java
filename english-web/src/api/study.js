import request from './request'

/**
 * 学习反馈（记忆科学方案）
 * @param wordId 单词 ID
 * @param feedbackType KNOW | DONT_KNOW | SPELLING_ERROR
 * @param mode flashcard | eng_ch | ch_eng | spell
 * @param errorType FLASHCARD | ENG_TO_CH | CH_TO_ENG | SPELLING_ERROR
 * @param userInput 用户输入（选题/拼写内容）
 * @param wrongCountSession 本 session 内该词答错次数，用于错误权重（错 2+ 需 4 次正确过关）
 */
export function submitFeedback(wordId, feedbackType, mode, errorType, userInput, wrongCountSession) {
  const body = { wordId, feedbackType }
  if (mode) body.mode = mode
  if (errorType) body.errorType = errorType
  if (userInput != null && userInput !== '') body.userInput = userInput
  if (typeof wrongCountSession === 'number') body.wrongCountSession = wrongCountSession
  return request.post('/study/submit', body)
}

/** 关卡式训练答题提交，使用产品记忆算法更新学习进度 */
export function submitJourneyAnswer(wordId, isCorrect, payload = {}) {
  return request.post('/study/submit', {
    wordId,
    isCorrect,
    ...payload
  })
}

/** 下一个单词（艾宾浩斯优先级） */
export function getNextWord(unitId) {
  return request.get('/word/next', { params: { unitId } })
}

/**
 * 智能复习列表（记忆科学方案）
 * 优先：弱词 > 到期复习 > 新词；支持按 mode 筛选
 */
export function getNextList(unitId, limit = 20, mode) {
  const params = { unitId, limit }
  if (mode) params.mode = mode
  return request.get('/study/next_list', { params })
}

/** 当前单词的选择题选项，由后端生成干扰项 */
export function getQuestionOptions(wordId, mode) {
  return request.get('/study/question_options', { params: { wordId, mode } })
}

/** 获取近一周错题（单词列表，用于首页左侧简要展示） */
export function getRecentErrors(days = 7, limit = 50) {
  return request.get('/study/recent_errors', { params: { days, limit } })
}

/** 当前仍需巩固的弱词列表 */
export function getWeakWords(unitId, limit = 20) {
  const params = { limit }
  if (unitId != null && unitId !== '') params.unitId = unitId
  return request.get('/study/weak_words', { params })
}

/** 有错题的日期列表，格式 yyyy-MM-dd，倒序 */
export function getErrorDates(days = 30) {
  return request.get('/study/error_dates', { params: { days } })
}

/** 按日期统计错题：{ date, totalCount, typeCounts }，日期倒序。支持 search 参数 */
export function getErrorStatsByDate(params) {
  const p = typeof params === 'object' ? params : { days: params ?? 90 }
  return request.get('/study/error_stats_by_date', { params: p })
}

/** 按日期统计错题数量（支持时间、书本、单元筛选）：{ date, count }[] */
export function getErrorStatsFiltered(params) {
  return request.get('/study/error_stats_filtered', { params })
}

/** 智能归纳列表（待完成）：后端分页+搜索。返回 { list, total, hasPending, firstDate } */
export function getSmartSummaryList(params) {
  return request.get('/study/smart_summary_list', { params })
}

/** 历史错题列表（已完成）：后端分页+搜索。返回 { list, total } */
export function getHistoryErrorList(params) {
  return request.get('/study/history_error_list', { params })
}

/** 某日错题明细：[{ wordId, word, meaning, errorType }, ...] */
export function getErrorsByDate(date) {
  return request.get('/study/errors_by_date', { params: { date } })
}

/** 某日错题按类型分组：[{ type, items }, ...]，后端已分组排序 */
export function getErrorsByDateGrouped(date) {
  return request.get('/study/errors_by_date_grouped', { params: { date } })
}

/** 某日错题对应的完整单词列表（用于重复训练） */
export function getWordsByErrorDate(date) {
  return request.get('/study/words_by_error_date', { params: { date } })
}

/** 本单元全部单词（用于复习本单元，前端会随机打乱顺序） */
export function getUnitWords(unitId) {
  return request.get('/study/unit_words', { params: { unitId } })
}

/** 学习热力图 { items, total, consecutiveDays, masteredCount } */
export function getStudyHeatmap() {
  return request.get('/study/heatmap')
}

/** 今日学习统计 */
export function getTodayStats() {
  return request.get('/stats/today')
}

/** 总览统计 */
export function getOverviewStats() {
  return request.get('/stats/overview')
}

/** 已掌握单词列表（分页）{ list, total } */
export function getMasteredWords(params) {
  return request.get('/study/mastered_words', { params })
}

/** 拼写校验 */
export function checkSpelling(wordId, userInput) {
  return request.post('/study/check_spelling', { wordId, userInput: userInput ?? '' })
}

/** 今日任务统计 */
export function getStudyStats(unitId) {
  return request.get('/study/stats', { params: { unitId } })
}

/** 本单元四种学习类型完成状态：{ flashcard, eng_ch, ch_eng, spell } 均为 true 时表示本单元新词全部学完 */
export function getUnitModeCompletion(unitId) {
  return request.get('/study/unit_mode_completion', { params: { unitId } })
}

/** 本单元各学习类型未完成/全部数量：{ flashcard: {total, incomplete}, ... } */
export function getModeStats(unitId) {
  return request.get('/study/mode_stats', { params: { unitId } })
}

/** 某模式下未完成的单词列表，limit 默认 100。用于未完成数为 1 时仅练习该词 */
export function getIncompleteWords(unitId, mode, limit = 100) {
  return request.get('/study/incomplete_words', { params: { unitId, mode, limit } })
}

/** 标记本单元某类型练习已完成（四种都过+错题复习完=本单元完成） */
export function completeMode(unitId, mode) {
  return request.post('/study/complete_mode', { unitId, mode })
}

/** 标记某日错题复习已完成（智能归纳完成后调用，后端持久化） */
export function markErrorReviewComplete(date, errorType) {
  const params = { date }
  if (errorType) params.errorType = errorType
  return request.post('/study/error_review_complete', null, { params })
}
