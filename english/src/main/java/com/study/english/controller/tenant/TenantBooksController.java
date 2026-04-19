package com.study.english.controller.tenant;

import com.study.english.common.Result;
import com.study.english.context.TenantContext;
import com.study.english.entity.Book;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.service.BookService;
import com.study.english.service.TenantBookAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 校长端：查看平台书本列表。平台书与租户无关，每个学生都可学习任意书；grant/revoke 保留供后续扩展。
 */
@RestController
@RequestMapping("/api/tenant/books")
@RequiredArgsConstructor
public class TenantBooksController {

    private final TenantBookAuthService tenantBookAuthService;
    private final BookService bookService;

    /**
     * 查看平台书本列表。
     *
     * @return Result.data 平台全部书本（按 sort_order）
     */
    @GetMapping
    public Result<List<Book>> listGranted() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可查看");
        }
        return Result.ok(bookService.listBooksByTenantId(tenantId));
    }

    /**
     * 将书本授权给当前租户（保留接口，当前平台书与租户无关）。
     *
     * @param bookId 书本 ID
     * @return Result 无 data
     */
    @PostMapping("/grant")
    public Result<Void> grant(@RequestParam Long bookId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可操作");
        }
        tenantBookAuthService.grantBookToTenant(tenantId, bookId);
        return Result.ok();
    }

    /**
     * 撤销当前租户对书本的授权（保留接口）。
     *
     * @param bookId 书本 ID
     * @return Result 无 data
     */
    @DeleteMapping("/revoke")
    public Result<Void> revoke(@RequestParam Long bookId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BusinessException("未登录");
        if (!User.ROLE_ORG_ADMIN.equals(TenantContext.getRole()) && !User.ROLE_TENANT_ADMIN.equals(TenantContext.getRole())) {
            throw new BusinessException("仅校长/租户管理员可操作");
        }
        tenantBookAuthService.revokeBookFromTenant(tenantId, bookId);
        return Result.ok();
    }
}
