package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.Word;

public interface WordService extends IService<Word> {

    /**
     * 按 (unit_id, word) 幂等：存在则更新释义/图片等，不存在则新增
     */
    Word upsertByUnitIdAndWord(Long unitId, String wordText, String phonetic, String meaning,
                               String exampleSentence, String audioUrl, String imageUrl, int sortOrder);
}
