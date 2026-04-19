package com.study.english.service;

import com.study.english.dto.SyncRequest;
import com.study.english.entity.Book;
import com.study.english.entity.Unit;
import com.study.english.entity.Word;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 超级管理员：幂等同步 Book -> Unit -> Word，按 (unit_id, word) 做单词 upsert。
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private final BookService bookService;
    private final UnitService unitService;
    private final WordService wordService;

    @Transactional(rollbackFor = Exception.class)
    public SyncResult sync(SyncRequest req) {
        int booksCount = 0, unitsCount = 0, wordsCount = 0;
        for (SyncRequest.BookSyncItem b : req.getBooks()) {
            String bookName = b.getName() != null ? b.getName() : "";
            Book book = bookService.getOrCreateByName(bookName, b.getCoverUrl(), b.getDescription(), b.getSortOrder() != null ? b.getSortOrder() : 0);
            booksCount++;
            if (b.getUnits() != null) {
                for (SyncRequest.UnitSyncItem u : b.getUnits()) {
                    String unitName = u.getName() != null ? u.getName() : "";
                    Unit unit = unitService.getOrCreateByBookIdAndName(book.getId(), unitName, u.getSortOrder() != null ? u.getSortOrder() : 0);
                    unitsCount++;
                    if (u.getWords() != null) {
                        for (SyncRequest.WordSyncItem w : u.getWords()) {
                            String wordText = w.getWord() != null ? w.getWord().trim() : "";
                            if (wordText.isEmpty()) continue;
                            wordService.upsertByUnitIdAndWord(
                                    unit.getId(),
                                    wordText,
                                    w.getPhonetic(),
                                    w.getMeaning(),
                                    w.getExampleSentence(),
                                    w.getAudioUrl(),
                                    w.getImageUrl(),
                                    w.getSortOrder() != null ? w.getSortOrder() : 0);
                            wordsCount++;
                        }
                    }
                }
            }
        }
        return new SyncResult(booksCount, unitsCount, wordsCount);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SyncResult {
        private int booksCount;
        private int unitsCount;
        private int wordsCount;
    }
}
