package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.ErrorLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ErrorLogService extends IService<ErrorLog> {

    void logError(String tenantId, Long userId, Long wordId, String errorType);

    /** 校长端：按租户、时间范围、可选学生、可选错误类型查询错误记录 */
    List<ErrorLog> listByTenantAndRange(String tenantId, Long userId, LocalDateTime start, LocalDateTime end, String errorType);

    /** 最近 days 天内有错题的日期列表，格式 yyyy-MM-dd，倒序 */
    List<String> getErrorDates(String tenantId, Long userId, int days);

    /** 某日错题明细：含 wordId, word, meaning, errorType */
    List<Map<String, Object>> getErrorsByDate(String tenantId, Long userId, String date);

    /** 某日错题按类型分组：返回 [{ type, items: [...] }]，按每类数量降序 */
    List<Map<String, Object>> getErrorsByDateGrouped(String tenantId, Long userId, String date);

    /** 某日错题涉及的 wordId 列表（去重、按时间倒序），用于重复训练 */
    List<Long> getWordIdsByDate(String tenantId, Long userId, String date);

    /** 按日期统计错题：返回 [{ date, totalCount, typeCounts: { DONT_KNOW: n, SPELLING_ERROR: n } }, ...]，日期倒序。search 可选，过滤 date 包含关键词 */
    List<Map<String, Object>> getErrorStatsByDate(String tenantId, Long userId, int days, String search);

    /** 按日期统计错题数量（支持书本、单元、时间筛选）：返回 [{ date, count }, ...]，日期倒序 */
    List<Map<String, Object>> getErrorStatsByDateFiltered(String tenantId, Long userId, int days,
            String startDate, String endDate, Long bookId, Long unitId);

    /** 智能归纳列表（待完成）：支持搜索、分页。返回 { list, total, hasPending, firstDate } */
    Map<String, Object> getSmartSummaryList(String tenantId, Long userId, int days,
            String startDate, String endDate, Long bookId, Long unitId,
            String search, int page, int pageSize);

    /** 历史错题列表（已完成）：支持搜索、分页。返回 { list, total } */
    Map<String, Object> getHistoryErrorList(String tenantId, Long userId, int days,
            String startDate, String endDate, Long bookId, Long unitId,
            String search, int page, int pageSize);

    /** 当前日期/类型下仍未修复的弱词数量。 */
    int countWeakWordsForDate(String tenantId, Long userId, String date, String errorType);
}
