package com.study.english.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.Word;
import com.study.english.mapper.WordMapper;
import com.study.english.service.WordService;
import org.springframework.stereotype.Service;

@Service
public class WordServiceImpl extends ServiceImpl<WordMapper, Word> implements WordService {

    @Override
    public Word upsertByUnitIdAndWord(Long unitId, String wordText, String phonetic, String meaning,
                                     String exampleSentence, String audioUrl, String imageUrl, int sortOrder) {
        Word existing = baseMapper.selectByUnitIdAndWord(unitId, wordText);
        if (existing != null) {
            existing.setPhonetic(phonetic);
            existing.setMeaning(meaning != null ? meaning : existing.getMeaning());
            existing.setExampleSentence(exampleSentence);
            existing.setAudioUrl(audioUrl);
            existing.setImageUrl(imageUrl);
            existing.setSortOrder(sortOrder);
            updateById(existing);
            return existing;
        }
        Word word = new Word();
        word.setUnitId(unitId);
        word.setWord(wordText);
        word.setPhonetic(phonetic);
        word.setMeaning(meaning != null ? meaning : "");
        word.setExampleSentence(exampleSentence);
        word.setAudioUrl(audioUrl);
        word.setImageUrl(imageUrl);
        word.setSortOrder(sortOrder);
        save(word);
        return word;
    }
}
