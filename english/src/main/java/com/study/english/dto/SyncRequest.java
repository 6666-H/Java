package com.study.english.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 词库同步请求：Book -> Unit -> Word，幂等（存在则更新、不存在则插入）。
 */
@Data
public class SyncRequest {

    /** 书籍列表（必填） */
    @Valid
    @NotEmpty(message = "书籍列表不能为空")
    private List<BookSyncItem> books;

    /** 书本项 */
    @Data
    public static class BookSyncItem {
        private String name;
        private String coverUrl;
        private String description;
        private Integer sortOrder = 0;
        @Valid
        private List<UnitSyncItem> units;
    }

    /** 单元项 */
    @Data
    public static class UnitSyncItem {
        private String name;
        private Integer sortOrder = 0;
        @Valid
        private List<WordSyncItem> words;
    }

    /** 单词项，word 为同单元内唯一标识 */
    @Data
    public static class WordSyncItem {
        private String word;
        private String phonetic;
        private String meaning;
        private String exampleSentence;
        private String audioUrl;
        private String imageUrl;
        private Integer sortOrder = 0;
    }
}
