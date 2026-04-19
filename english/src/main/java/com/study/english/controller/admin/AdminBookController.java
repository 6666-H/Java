package com.study.english.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.english.common.Result;
import com.study.english.dto.BookUnitAggregateDto;
import com.study.english.dto.BookWordAggregateDto;
import com.study.english.context.TenantContext;
import com.study.english.dto.AdminBookOverviewDto;
import com.study.english.entity.Book;
import com.study.english.entity.TenantBookAuth;
import com.study.english.entity.Unit;
import com.study.english.entity.Word;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.UnitMapper;
import com.study.english.mapper.WordMapper;
import com.study.english.service.BookService;
import com.study.english.service.TenantBookAuthService;
import com.study.english.service.UnitService;
import com.study.english.service.WordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 超级管理员：书本的查看与增删改。
 */
@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;
    private final UnitService unitService;
    private final WordService wordService;
    private final TenantBookAuthService tenantBookAuthService;
    private final UnitMapper unitMapper;
    private final WordMapper wordMapper;

    @GetMapping
    public Result<List<Book>> list() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        return Result.ok(bookService.listBooksByTenantId(null));
    }

    @GetMapping("/overview")
    public Result<AdminBookOverviewDto> overview(@RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String keyword) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }

        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        int totalBookCount = Math.toIntExact(bookService.count());
        int totalWordCount = Math.toIntExact(wordService.count());
        int totalUnitCount = Math.toIntExact(unitService.count());
        int capacityRate = Math.min(100, (int) Math.round(totalWordCount * 100.0 / 51000.0));

        int filteredCount = Math.toIntExact(bookService.count(buildBookQuery(category, keyword)));
        if (filteredCount == 0) {
            safePage = 1;
        }

        int totalPages = Math.max(1, (int) Math.ceil(filteredCount / (double) safePageSize));
        safePage = Math.min(safePage, totalPages);
        int offset = Math.max(0, (safePage - 1) * safePageSize);

        List<Book> pageBooks = filteredCount == 0
                ? List.of()
                : bookService.list(buildBookQuery(category, keyword)
                .orderByDesc(Book::getUpdatedAt)
                .orderByDesc(Book::getId)
                .last("LIMIT " + offset + "," + safePageSize));

        List<Long> pageBookIds = pageBooks.stream()
                .map(Book::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, BookUnitAggregateDto> unitStatsByBook = pageBookIds.isEmpty()
                ? Map.of()
                : unitMapper.selectBookAggregates(pageBookIds).stream()
                .collect(Collectors.toMap(BookUnitAggregateDto::getBookId, item -> item));
        Map<Long, BookWordAggregateDto> wordStatsByBook = pageBookIds.isEmpty()
                ? Map.of()
                : wordMapper.selectBookAggregates(pageBookIds).stream()
                .collect(Collectors.toMap(BookWordAggregateDto::getBookId, item -> item));

        List<AdminBookOverviewDto.BookCard> pageCards = pageBooks.stream()
                .map(book -> toBookCard(book, unitStatsByBook.get(book.getId()), wordStatsByBook.get(book.getId())))
                .sorted(
                        Comparator.comparing(AdminBookOverviewDto.BookCard::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(AdminBookOverviewDto.BookCard::getId, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();

        return Result.ok(new AdminBookOverviewDto(
                totalBookCount,
                totalWordCount,
                totalUnitCount,
                capacityRate,
                filteredCount,
                safePage,
                safePageSize,
                pageCards
        ));
    }

    private AdminBookOverviewDto.BookCard toBookCard(Book book,
                                                     BookUnitAggregateDto unitStats,
                                                     BookWordAggregateDto wordStats) {
        int unitCount = unitStats != null && unitStats.getUnitCount() != null ? unitStats.getUnitCount() : 0;
        int wordCount = wordStats != null && wordStats.getWordCount() != null ? wordStats.getWordCount() : 0;
        LocalDateTime latestUpdatedAt = latest(book.getUpdatedAt(),
                unitStats != null ? unitStats.getLatestUnitUpdatedAt() : null,
                wordStats != null ? wordStats.getLatestWordUpdatedAt() : null);

        return new AdminBookOverviewDto.BookCard(
                book.getId(),
                book.getName(),
                book.getGrade(),
                book.getVersionName(),
                book.getDescription(),
                book.getCoverUrl(),
                unitCount,
                wordCount,
                wordCount > 0 ? "PUBLISHED" : "DRAFT",
                latestUpdatedAt
        );
    }

    private LocalDateTime latest(LocalDateTime... times) {
        LocalDateTime latest = null;
        for (LocalDateTime time : times) {
            if (time != null && (latest == null || time.isAfter(latest))) {
                latest = time;
            }
        }
        return latest;
    }

    private LambdaQueryWrapper<Book> buildBookQuery(String category, String keyword) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (!trimmedKeyword.isEmpty()) {
            wrapper.and(q -> q.like(Book::getName, trimmedKeyword));
        }

        if (category == null || category.isBlank() || "全部教材".equals(category)) {
            return wrapper;
        }

        return switch (category) {
            case "小学" -> applyStageFilter(wrapper, List.of(
                    "小学", "一年级", "二年级", "三年级", "四年级", "五年级", "六年级",
                    "小一", "小二", "小三", "小四", "小五", "小六"
            ));
            case "初中" -> applyStageFilter(wrapper, List.of(
                    "初中", "七年级", "八年级", "九年级", "初一", "初二", "初三"
            ));
            case "高中" -> applyStageFilter(wrapper, List.of(
                    "高中", "高一", "高二", "高三", "十年级", "十一年级", "十二年级"
            ));
            default -> wrapper;
        };
    }

    private LambdaQueryWrapper<Book> applyStageFilter(LambdaQueryWrapper<Book> wrapper, List<String> keywords) {
        wrapper.and(q -> {
            boolean firstKeyword = true;
            for (String keyword : keywords) {
                if (!firstKeyword) {
                    q.or();
                }
                q.like(Book::getGrade, keyword)
                        .or().like(Book::getName, keyword)
                        .or().like(Book::getVersionName, keyword)
                        .or().like(Book::getDescription, keyword);
                firstKeyword = false;
            }
        });
        return wrapper;
    }

    @PostMapping
    public Result<Book> create(@RequestBody BookCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Book book = new Book();
        book.setName(req.getName() != null ? req.getName() : "未命名");
        book.setGrade(req.getGrade());
        book.setVersionName(req.getVersionName());
        book.setCoverUrl(req.getCoverUrl());
        book.setDescription(req.getDescription());
        book.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        book.setVersion(1);
        bookService.save(book);
        return Result.ok(book);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BookCreateRequest req) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        Book book = bookService.getById(id);
        if (book == null) throw new BusinessException("书本不存在");
        if (req.getName() != null) book.setName(req.getName());
        if (req.getGrade() != null) book.setGrade(req.getGrade());
        if (req.getVersionName() != null) book.setVersionName(req.getVersionName());
        if (req.getCoverUrl() != null) book.setCoverUrl(req.getCoverUrl());
        if (req.getDescription() != null) book.setDescription(req.getDescription());
        if (req.getSortOrder() != null) book.setSortOrder(req.getSortOrder());
        bookService.updateById(book);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作");
        }
        if (bookService.getById(id) == null) throw new BusinessException("书本不存在");
        List<Long> unitIds = unitService.list(new LambdaQueryWrapper<Unit>()
                        .eq(Unit::getBookId, id))
                .stream()
                .map(Unit::getId)
                .toList();
        if (!unitIds.isEmpty()) {
            wordService.remove(new LambdaQueryWrapper<Word>().in(Word::getUnitId, unitIds));
            unitService.removeByIds(unitIds);
        }
        tenantBookAuthService.remove(new LambdaQueryWrapper<TenantBookAuth>().eq(TenantBookAuth::getBookId, id));
        bookService.removeById(id);
        return Result.ok();
    }

    @Data
    public static class BookCreateRequest {
        private String versionName;
        private String grade;
        private String name;
        private String coverUrl;
        private String description;
        private Integer sortOrder;
    }
}
