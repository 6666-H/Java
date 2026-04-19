package com.study.english.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.Book;
import com.study.english.entity.Unit;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.service.BookService;
import com.study.english.service.UnitService;
import com.study.english.service.WordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 超级管理员：单词的查看与增删改（按单元）。
 */
@RestController
@RequestMapping("/api/admin/words")
@RequiredArgsConstructor
public class AdminWordController {

    private final WordService wordService;
    private final UnitService unitService;
    private final BookService bookService;

    @GetMapping
    public Result<List<Word>> list(@RequestParam Long unitId) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        List<Word> list = wordService.list(new LambdaQueryWrapper<Word>()
                .eq(Word::getUnitId, unitId)
                .orderByAsc(Word::getSortOrder));
        return Result.ok(list);
    }

    @PostMapping
    public Result<Word> create(@RequestBody WordCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (req.getUnitId() == null) throw new BusinessException("unitId 不能为空");
        if (req.getWord() == null || req.getWord().isBlank()) throw new BusinessException("单词不能为空");
        Word word = new Word();
        word.setUnitId(req.getUnitId());
        word.setWord(req.getWord().trim());
        word.setPhonetic(req.getPhonetic());
        word.setPos(req.getPos());
        word.setMeaning(req.getMeaning() != null ? req.getMeaning() : "");
        word.setExampleSentence(req.getExampleSentence());
        word.setExampleZh(req.getExampleZh());
        word.setAudioUrl(req.getAudioUrl());
        word.setImageUrl(req.getImageUrl());
        word.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        wordService.save(word);
        return Result.ok(word);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody WordCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Word word = wordService.getById(id);
        if (word == null) throw new BusinessException("单词不存在");
        if (req.getWord() != null) word.setWord(req.getWord().trim());
        if (req.getPhonetic() != null) word.setPhonetic(req.getPhonetic());
        if (req.getPos() != null) word.setPos(req.getPos());
        if (req.getMeaning() != null) word.setMeaning(req.getMeaning());
        if (req.getExampleSentence() != null) word.setExampleSentence(req.getExampleSentence());
        if (req.getExampleZh() != null) word.setExampleZh(req.getExampleZh());
        if (req.getAudioUrl() != null) word.setAudioUrl(req.getAudioUrl());
        if (req.getImageUrl() != null) word.setImageUrl(req.getImageUrl());
        if (req.getSortOrder() != null) word.setSortOrder(req.getSortOrder());
        wordService.updateById(word);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (wordService.getById(id) == null) throw new BusinessException("单词不存在");
        wordService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWords(@RequestParam(required = false) Long unitId,
                                              @RequestParam(required = false) Long bookId) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        List<Word> words = loadWordsForExport(unitId, bookId);
        Map<Long, Unit> unitMap = unitService.listByIds(words.stream().map(Word::getUnitId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Unit::getId, item -> item));
        Set<Long> bookIds = unitMap.values().stream().map(Unit::getBookId).collect(Collectors.toSet());
        Map<Long, Book> bookMap = bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, item -> item));

        List<String> lines = new ArrayList<>();
        lines.add("book,unit,word,phonetic,pos,meaning,example_en,example_zh");
        for (Word word : words) {
            Unit unit = unitMap.get(word.getUnitId());
            Book book = unit != null ? bookMap.get(unit.getBookId()) : null;
            lines.add(String.join(",",
                    csv(book != null ? book.getName() : ""),
                    csv(unit != null ? unit.getName() : ""),
                    csv(word.getWord()),
                    csv(word.getPhonetic()),
                    csv(word.getPos()),
                    csv(word.getMeaning()),
                    csv(word.getExampleSentence()),
                    csv(word.getExampleZh())));
        }

        byte[] body = ("\uFEFF" + String.join("\n", lines)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=words.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> importWords(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) Long unitId,
                                                   @RequestParam(required = false) Long bookId) throws Exception {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (file.isEmpty()) throw new BusinessException("请上传 CSV 文件");
        String content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
        String[] lines = content.split("\\r?\\n");
        if (lines.length <= 1) throw new BusinessException("CSV 内容为空");

        int count = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) continue;
            List<String> cols = parseCsvLine(line);
            while (cols.size() < 8) cols.add("");
            String csvBook = cols.get(0).trim();
            String csvUnit = cols.get(1).trim();
            String wordText = cols.get(2).trim();
            if (wordText.isBlank()) continue;

            Unit targetUnit = resolveTargetUnit(unitId, bookId, csvBook, csvUnit);
            Word existing = wordService.lambdaQuery()
                    .eq(Word::getUnitId, targetUnit.getId())
                    .eq(Word::getWord, wordText)
                    .one();
            if (existing == null) {
                existing = new Word();
                existing.setUnitId(targetUnit.getId());
                existing.setWord(wordText);
                existing.setSortOrder(i);
            }
            existing.setPhonetic(blankToNull(cols.get(3)));
            existing.setPos(blankToNull(cols.get(4)));
            existing.setMeaning(cols.get(5));
            existing.setExampleSentence(blankToNull(cols.get(6)));
            existing.setExampleZh(blankToNull(cols.get(7)));
            wordService.saveOrUpdate(existing);
            count++;
        }
        return Result.ok(Map.of("importedCount", count));
    }

    private List<Word> loadWordsForExport(Long unitId, Long bookId) {
        if (unitId != null) {
            return wordService.list(new LambdaQueryWrapper<Word>()
                    .eq(Word::getUnitId, unitId)
                    .orderByAsc(Word::getSortOrder));
        }
        if (bookId != null) {
            List<Long> unitIds = unitService.lambdaQuery()
                    .eq(Unit::getBookId, bookId)
                    .list().stream().map(Unit::getId).toList();
            if (unitIds.isEmpty()) return List.of();
            return wordService.list(new LambdaQueryWrapper<Word>()
                    .in(Word::getUnitId, unitIds)
                    .orderByAsc(Word::getUnitId)
                    .orderByAsc(Word::getSortOrder));
        }
        return wordService.list(new LambdaQueryWrapper<Word>()
                .orderByAsc(Word::getUnitId)
                .orderByAsc(Word::getSortOrder));
    }

    private Unit resolveTargetUnit(Long unitId, Long bookId, String csvBook, String csvUnit) {
        if (unitId != null) {
            Unit unit = unitService.getById(unitId);
            if (unit == null) throw new BusinessException("导入目标单元不存在");
            return unit;
        }
        if (!csvUnit.isBlank() && !csvBook.isBlank()) {
            Book book = bookService.lambdaQuery().eq(Book::getName, csvBook).last("LIMIT 1").one();
            if (book == null) {
                book = new Book();
                book.setName(csvBook);
                book.setSortOrder(0);
                book.setVersion(1);
                bookService.save(book);
            }
            Unit unit = unitService.lambdaQuery()
                    .eq(Unit::getBookId, book.getId())
                    .eq(Unit::getName, csvUnit)
                    .last("LIMIT 1")
                    .one();
            if (unit == null) {
                unit = new Unit();
                unit.setBookId(book.getId());
                unit.setName(csvUnit);
                unit.setSortOrder(0);
                unitService.save(unit);
            }
            return unit;
        }
        if (bookId != null) {
            Unit unit = unitService.lambdaQuery().eq(Unit::getBookId, bookId).last("LIMIT 1").one();
            if (unit != null) return unit;
        }
        throw new BusinessException("请先选择导入单元，或在 CSV 中填写 book 和 unit");
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    @Data
    public static class WordCreateRequest {
        private Long unitId;
        private String word;
        private String phonetic;
        private String pos;
        private String meaning;
        private String exampleSentence;
        private String exampleZh;
        private String audioUrl;
        private String imageUrl;
        private Integer sortOrder;
    }
}
