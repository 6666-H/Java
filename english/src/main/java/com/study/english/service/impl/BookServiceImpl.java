package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.Book;
import com.study.english.mapper.BookMapper;
import com.study.english.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Override
    public Book getOrCreateByName(String name, String coverUrl, String description, int sortOrder) {
        Book one = getOne(new LambdaQueryWrapper<Book>().eq(Book::getName, name));
        if (one != null) {
            boolean changed = false;
            if (coverUrl != null) { one.setCoverUrl(coverUrl); changed = true; }
            if (description != null) { one.setDescription(description); changed = true; }
            if (sortOrder != one.getSortOrder()) { one.setSortOrder(sortOrder); changed = true; }
            if (changed) {
                one.setVersion((one.getVersion() == null ? 1 : one.getVersion()) + 1);
                updateById(one);
            }
            return one;
        }
        Book book = new Book();
        book.setName(name);
        book.setCoverUrl(coverUrl);
        book.setDescription(description);
        book.setVersion(1);
        book.setSortOrder(sortOrder);
        save(book);
        return book;
    }

    @Override
    public List<Book> listBooksByTenantId(String tenantId) {
        // 书本与租户无关，每个学生都可学习平台全部书本
        return list(new LambdaQueryWrapper<Book>().orderByAsc(Book::getSortOrder));
    }
}
