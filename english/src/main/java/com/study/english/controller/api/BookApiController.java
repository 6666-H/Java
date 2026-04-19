package com.study.english.controller.api;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.dto.BookProgressStatsDto;
import com.study.english.entity.Book;
import com.study.english.exception.BusinessException;
import com.study.english.service.BookService;
import com.study.english.service.UnitProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookApiController {

    private final BookService bookService;
    private final UnitProgressService unitProgressService;

    @GetMapping
    public Result<List<Book>> list(
            @RequestParam(required = false) String versionName,
            @RequestParam(required = false) String grade) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录或租户上下文缺失");
        List<Book> books = bookService.listBooksByTenantId(tenantId);
        if (versionName != null && !versionName.isBlank()) {
            books = books.stream().filter(b -> versionName.equals(b.getVersionName())).toList();
        }
        if (grade != null && !grade.isBlank()) {
            books = books.stream().filter(b -> grade.equals(b.getGrade())).toList();
        }
        return Result.ok(books);
    }

    @GetMapping("/versions")
    public Result<List<String>> listVersions() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        return Result.ok(bookService.listBooksByTenantId(tenantId).stream()
                .map(Book::getVersionName)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted()
                .toList());
    }

    @GetMapping("/grades")
    public Result<List<String>> listGrades(@RequestParam(required = false) String versionName) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        var books = bookService.listBooksByTenantId(tenantId);
        if (versionName != null && !versionName.isBlank()) {
            books = books.stream().filter(b -> versionName.equals(b.getVersionName())).toList();
        }
        return Result.ok(books.stream().map(Book::getGrade)
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .toList());
    }

    @GetMapping("/stats")
    public Result<BookProgressStatsDto> bookStats(@RequestParam Long bookId) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) throw new BusinessException("未登录");
        return Result.ok(unitProgressService.getBookProgressStats(tenantId, userId, bookId));
    }
}
