package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.Book;

import java.util.List;

public interface BookService extends IService<Book> {

    Book getOrCreateByName(String name, String coverUrl, String description, int sortOrder);

    /** 平台书与租户无关，返回全部书本（按 sort_order）。tenantId 仅用于接口兼容，不参与过滤。 */
    List<Book> listBooksByTenantId(String tenantId);
}
